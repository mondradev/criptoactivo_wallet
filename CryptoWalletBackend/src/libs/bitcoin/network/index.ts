import { Block, BlockHeader, Transaction, Networks } from "bitcore-lib"
import { EventEmitter } from "events"
import { Blockchain } from "../chain/blockchain"

import NetworkEvents from "./networkevents"
import NetworkStatus from "./networkstatus"
import LoggerFactory from 'log4js'
import Constants from "../constants"
import AsyncLock from "async-lock"
import Config from "../../../../config"
import DNS from 'dns'
import Ping from 'ping'
import checkLocalHost from 'check-localhost'

import crypto from 'crypto'


import { Messages, Peer, BlockMessage, InvMessage, HeadersMessage, GetdataMessage, Inventory, TxMessage } from "bitcore-p2p"
import { ChainTip } from "../store/istore"
import { wait as sleep } from "../../../utils"

import '../../../utils/extensions'
import '../fixes/bitcore-p2p.peer.lib'

import { PendingBlocksError, PendingTxsError } from "../fixes/bitcore-p2p.peer.lib"
import { BitcoinConfig } from "../consensus"
import { Mempool } from "../store/leveldb/mempool"
import { VersionMessage } from "bitcore-p2p"
import BufferHelper from "../../../utils/bufferhelper"
import { time } from "console"
import { GridFSBucketReadStream } from "mongodb"

type IPAddress = { v4: string, v6: string }
type Addr = { ip: IPAddress, port: number, time: Date }

export type BlockListener = (block: Block) => void
export type HeadersListener = (headers: BlockHeader[]) => void
export type TxListener = (tx: Transaction) => void
export type ReadyListener = (connected: Boolean) => void
export type DisconnectedListener = () => void
export type ErrorListener = (error: Error) => void
export type SyncListener = (tip: { height: number, block: Block }) => void

const Logger = LoggerFactory.getLogger('(Bitcoin) Network')
const Lock = new AsyncLock({ maxPending: Constants.DOWNLOAD_SIZE * 10 })

const LockKeys = {
    Peer: 'peers',
    ConnectingPeer: 'connectingPeer',
    BlockRequest: 'blockRequest',
    Status: 'status',
    AddressesReceived: 'addressesReceived',
    DownloadBlocks: 'downloadBlocks',
    DownloadTxs: 'downloadTx',
    InvHandle: 'invhandle',
    TxRequest: 'txRequest'
}

Networks.defaultNetwork = Networks.get(BitcoinConfig.network || 'mainnet')

export class Network {

    private _notifier: EventEmitter
    private _chain: Blockchain
    private _status: NetworkStatus
    private _bestHeight: number
    private _outboundPeers: Array<Peer>
    private _availableMessages: Messages
    private _peerAddresses: Array<string>
    private _downloadingBlocks: Map<string, boolean>
    private _downloadingTxs: Map<string, boolean>
    private _pendingBlocks: Array<string>
    private _txMempool: Mempool

    private async _continueDownload(tip: ChainTip) {
        let locator = await this._chain.getLocators(tip)

        if (tip.height >= this._bestHeight) {
            // TODO Validar descarga completa
            await this._setStatus(NetworkStatus.SYNCHRONIZED)

            Logger.info('Finalized syncronization [Height=%d]', tip.height)

            for (const peer of this._outboundPeers)
                this._requestMempool(peer)

            return
        }

        this.requestHeaders(locator.starts, locator.stop)
    }

    private async _monitorPeers() {
        while ((await this.getStatus()) >= NetworkStatus.CONNECTED) {
            Logger.debug("Monitoring each peers")

            if (this._outboundPeers.length == 0)
                await sleep(1000)
            else {
                this._outboundPeers.forEach(peer => {
                    peer.sendMessage(this._availableMessages.Ping())
                })

                await sleep(20 * 60000)
            }
        }
    }

    /**
     * Resuelve las IP desde las semillas del DNS.
     */
    private async _discoveringPeers() {
        Logger.info('Discovering peers from DNS')

        for (const seed of BitcoinConfig.seeds) {
            const resolvedAddresses = await this._resolveDns(seed)

            for (const address of resolvedAddresses)
                if (!(await checkLocalHost(address)))
                    this._peerAddresses.push(address)
        }
    }

    /**
    * Rellena las conexiones del pool del nodo.
    */
    private async _fillPool() {
        Logger.info('Connecting pool, waiting for peers')

        while ((await this.getStatus()) >= NetworkStatus.CONNECTING) {
            const count = await Lock.acquire<number>(LockKeys.Peer, () => this._outboundPeers.length)
            if (BitcoinConfig.maxConnections > count) {
                Logger.info("Connecting to peer (%d/%d)", count, BitcoinConfig.maxConnections)
                await this._connectPeer(await Lock.acquire<string>(LockKeys.Peer, () => this._peerAddresses.shift()))
            }
            else
                await sleep(1000)
        }
    }

    private _connectPeer(address: string) {
        return new Promise<void>(async resolve => {
            if ((await this.getStatus()) <= NetworkStatus.DISCONNECTING) {
                resolve()
                return
            }

            await Lock.acquire(LockKeys.ConnectingPeer,
                (done) => this._connectAsync(done, address))

            resolve()
        })
    }

    private async _connectAsync(done: () => void, address: string) {
        if (this._outboundPeers.length >= BitcoinConfig.maxConnections) {
            done()
            return
        }

        let peer = new Peer({
            host: address,
            port: BitcoinConfig.port,
            message: this._availableMessages,
            network: BitcoinConfig.network
        })

        peer.setMaxListeners(2000)

        this._addEventHandlers(peer)
        this._readyHandler(peer, setTimeout(() => {
            Logger.warn("Timeout to connect peer [address=%s]", peer.host)
            peer.disconnect()

            peer.removeAllListeners(NetworkEvents.ADDR)
            peer.removeAllListeners(NetworkEvents.PONG)
            peer.removeAllListeners(NetworkEvents.DISCONNECT)
            peer.removeAllListeners(NetworkEvents.READY)

            done()
        }, Constants.Timeouts.WAIT_FOR_CONNECT_PEER), done)

        await sleep(100)

        peer.connect()
    }

    /**
     * Establece los controladores de eventos al nodo especificado.
     * @param peer Node al que se requiere establecer los controladores de eventos.
     */
    private _addEventHandlers(peer: Peer) {
        const self = this

        peer.removeAllListeners(NetworkEvents.ERROR)
        peer.removeAllListeners(NetworkEvents.ADDR)
        peer.removeAllListeners(NetworkEvents.PONG)
        peer.removeAllListeners(NetworkEvents.DISCONNECT)
        peer.removeAllListeners(NetworkEvents.BLOCK)
        peer.removeAllListeners(NetworkEvents.TX)
        peer.removeAllListeners(NetworkEvents.INV)
        peer.removeAllListeners(NetworkEvents.GETDATA)

        peer.on(NetworkEvents.ERROR, (reason: Error) => Logger.warn('Peer got an error [Message=%s, Host=%s]', reason.message, peer.host))
        peer.on(NetworkEvents.BLOCK, (message: BlockMessage) => this._blockHandler.apply(this, [peer, message]))
        peer.on(NetworkEvents.TX, (message: TxMessage) => this._txHandler.apply(this, [peer, message]))
        peer.on(NetworkEvents.INV, (message: InvMessage) => this._invHandler.apply(this, [message]))

        peer.on(NetworkEvents.ADDR, (message: { addresses: Array<Addr> }) => Lock.acquire(LockKeys.AddressesReceived, async () => {
            let nAddresses = 0

            for (const address of message.addresses) {
                const v4 = address.ip.v4
                const v6 = address.ip.v6

                if (!self._peerAddresses.includes(v4) && await Ping.promise.probe(v4) && !(await checkLocalHost(v4))) {
                    self._peerAddresses.push(v4)
                    nAddresses++
                }

                if (!self._peerAddresses.includes(v6) && await Ping.promise.probe(v6) && !(await checkLocalHost(v6))) {
                    self._peerAddresses.push(v6)
                    nAddresses++
                }
            }

            if (nAddresses)
                Logger.info("Received %d addresses from %s, [Addresses=%d]", nAddresses, peer.host, self._peerAddresses.length)
        }))

        peer.on(NetworkEvents.PONG, async () => {
            Logger.trace("Pong received from %s", peer.host)
        })

        peer.on(NetworkEvents.DISCONNECT, async () => {
            let connectedPeers = await Lock.acquire<number>(LockKeys.Peer, () => {
                this._outboundPeers = this._outboundPeers.remove(peer)
                return this._outboundPeers.length
            })

            peer.removeAllListeners(NetworkEvents.ERROR)
            peer.removeAllListeners(NetworkEvents.ADDR)
            peer.removeAllListeners(NetworkEvents.PONG)
            peer.removeAllListeners(NetworkEvents.DISCONNECT)
            peer.removeAllListeners(NetworkEvents.BLOCK)
            peer.removeAllListeners(NetworkEvents.TX)

            Logger.info("Peer disconnected (%d/%d) [Host=%s]", connectedPeers,
                BitcoinConfig.maxConnections, peer.host)
        });
    }

    private _readyHandler(peer: Peer, timeout: NodeJS.Timeout, done: () => void) {
        peer.once(NetworkEvents.VERSION, (message: VersionMessage) => {
            peer.service = message.services.toNumber()
            peer.relay = message.relay
        })
        peer.once(NetworkEvents.READY, async () => {
            clearTimeout(timeout)

            if (peer.bestHeight < this._bestHeight) {
                Logger.warn("Peer requires updating your blockchain, disconnecting")
                peer.disconnect()

                return done()
            }

            if ((await this.getStatus()) <= NetworkStatus.DISCONNECTING) {
                Logger.debug("Pool is disconnecting, abort connection to peer [address=%s]", peer.host)
                peer.disconnect()

                return done()
            }

            const connectedPeers = await Lock.acquire<number>(LockKeys.Peer, () => {
                this._outboundPeers.push(peer)
                return this._outboundPeers.length
            })

            Logger.info("Peer connected (%d/%d) [Version=%s, Agent=%s, BestHeight=%d, Host=%s, Relay=%s, Service=%d]",
                connectedPeers,
                BitcoinConfig.maxConnections,
                peer.version,
                peer.subversion,
                peer.bestHeight,
                peer.host,
                peer.relay,
                peer.service
            )

            if (peer.bestHeight > this._bestHeight) {
                const diff = peer.bestHeight - this._bestHeight
                this._bestHeight = Math.max(peer.bestHeight, this._bestHeight)

                if ((await this.getStatus()) == NetworkStatus.SYNCHRONIZED && diff > 1)
                    this._continueDownload(await this._chain.getLocalTip())
            }


            if ((await this.getStatus()) == NetworkStatus.CONNECTING)
                this._notifier.emit(NetworkEvents.READY)

            this._updateBestheight()

            if ((await this.getStatus()) == NetworkStatus.SYNCHRONIZED)
                this._requestMempool(peer)

            done()
        })
    }

    /**
     * Obtiene las direcciones a partir de la semilla DNS proporcionada.
     * @param {string} seed Semilla DNS a resolver.
     * @returns {Promise<Array<string>>} Promesa de un vector con las direcciones obtenidas del DNS.
     */
    private _resolveDns(seed: string): Promise<Array<string>> {
        return new Promise<Array<string>>(resolve =>
            DNS.resolve(seed, (err: Error, addresses: string[]) => {
                if (err)
                    resolve(new Array<string>(0))
                else
                    resolve(addresses)
            }))
    }

    private async _setStatus(status: NetworkStatus) {
        await Lock.acquire(LockKeys.Status, () => this._status = status)
    }

    private _requestMempool(peer: Peer) {
        if (peer.service & 1 << 2) {
            Logger.info("Requesting tx from mempool (%s)", peer.host)
            peer.sendMessage(this._availableMessages.MemPool())
        }
    }

    private async _requestHeaders(starts: string[], stop: string) {
        return new Promise<void>(async done => {
            const peer = await this._getAvailablePeer()

            if (!peer) {
                done()

                return
            }

            const createTimeout = () => setTimeout(() => {
                Logger.warn('Timeout on request headers to peer (%s)', peer.host)

                peer.busy = false
                peer.removeAllListeners(NetworkEvents.HEADERS)
                peer.disconnect()

                done(this._requestHeaders(starts, stop))
            }, Constants.Timeouts.WAIT_FOR_HEADERS)

            let timeout = createTimeout()

            peer.addListener(NetworkEvents.HEADERS, async (message: HeadersMessage) => {
                clearTimeout(timeout)

                if (message.headers.length == 0) {
                    peer.removeAllListeners(NetworkEvents.HEADERS)
                    peer.busy = false

                    done()

                    return
                }

                Logger.info("Received %s headers [From=%s, To=%s]",
                    message.headers.length,
                    message.headers.first().hash,
                    message.headers.last().hash)

                this._pendingBlocks.push(...message.headers.map(header => header.hash))

                if (stop == Constants.NULL_HASH || message.headers[message.headers.length - 1].hash === stop) {

                    peer.removeAllListeners(NetworkEvents.HEADERS)
                    peer.busy = false

                    done()
                }
                else {
                    timeout = createTimeout()

                    starts = message.headers.slice(message.headers.length - 30).reverse().map(h => h.hash)

                    Logger.debug("Request headers from %s to %s Peer(%s)", starts[0], stop, peer.host)

                    peer.sendMessage(this._availableMessages.GetHeaders({ starts, stop }))
                }

            })

            Logger.debug("Request headers from %s to %s Peer(%s)", starts[0], stop, peer.host)

            peer.sendMessage(this._availableMessages.GetHeaders({ starts, stop }))
        })
    }

    private async _getAvailablePeer(): Promise<Peer> {
        while ((await this.getStatus()) >= NetworkStatus.CONNECTING) {
            const peer = await Lock.acquire(LockKeys.Peer, () => {
                const peer = this._outboundPeers.find(p => !p.busy)

                if (peer) peer.busy = true

                return peer
            })

            if (peer)
                return peer

            Logger.debug("Peers not available")

            await sleep(10)
        }
    }

    private _downloadWindow = new Map<string, Buffer>()
    private _lastReport = {
        count: 0,
        time: Date.now()
    }

    private async _invHandler(message: InvMessage) {
        return Lock.acquire(LockKeys.InvHandle, async () => {
            if (this._status < NetworkStatus.SYNCHRONIZED)
                return

            let newBlocks = new Array<string>()
            let newTxs = new Array<string>()

            for (const inventory of message.inventory) {
                if (inventory.type == Inventory.TYPE.TX) {
                    if (await this._txMempool.has(inventory.hash))
                        continue

                    if (this._downloadingTxs.has(inventory.hash.toReverseHex()))
                        continue

                    newTxs.push(inventory.hash.toReverseHex())
                } else if (inventory.type == Inventory.TYPE.BLOCK) {
                    const height = await this._chain.getHeight(inventory.hash)

                    if (height ||
                        this._downloadWindow.has(inventory.hash.toReverseHex()) ||
                        this._downloadingBlocks.has(inventory.hash.toReverseHex()))
                        continue

                    Logger.debug("A new block was announced { hash: %s }", inventory.hash.toReverseHex())

                    newBlocks.push(inventory.hash.toReverseHex())
                }
            }

            if (newBlocks.length > 0)
                await this._downloadBlocks(newBlocks)

            if (newTxs.length > 0)
                await this._downloadTxs(newTxs)
        })
    }

    private async _blockHandler(peer: Peer, message: BlockMessage) {
        return Lock.acquire(LockKeys.BlockRequest, async () => {
            const hash = message.block.hash
            const tip = await this._chain.getLocalTip()

            if (!this._downloadingBlocks.has(hash)) return

            this._downloadingBlocks.delete(hash)

            if (tip.hash === hash || this._downloadWindow.has(hash)) return

            this._downloadWindow.set(hash, message.block.toBuffer())

            if (await this._isValidBlock(hash, message.block.header.prevHash, tip, peer)) {

                Logger.debug("Received block [Hash=%s, Size=%d, Txn=%d]", message.block.hash, message.block.toBuffer().length, message.block.transactions.length)

                if ((await this.getStatus()) != NetworkStatus.SYNCHRONIZED) {

                    let time = Date.now() - this._lastReport.time

                    if (time >= 1000) {

                        time /= 1000
                        let rate = (this._downloadWindow.size - this._lastReport.count) / time

                        this._lastReport.time = Date.now()
                        this._lastReport.count = this._downloadWindow.size

                        Logger.debug("Downloading %d blk/s (%d/%d) Usage %s MB", rate.toFixed(2), this._downloadWindow.size, this._downloadWindow.size + this._downloadingBlocks.size,
                            ((process.memoryUsage().rss) / (1024 * 1024)).toFixed(2))
                    }
                }
            }

            if (this._downloadingBlocks.size == 0) {

                this._lastReport.count = 0

                const blocks = new Array(...this._downloadWindow.values())

                for (const raw of blocks) {
                    const block = Block.fromBuffer(raw)
                    if (await this._chain.addBlock(block))
                        await this._txMempool.remove(block)
                    else {
                        this._pendingBlocks.clear()

                        break
                    }
                }

                this._downloadWindow.clear()

                const blockTip = await this._chain.getLocalTip()

                this._bestHeight = Math.max(this._bestHeight, blockTip.height)

                if (await this.getStatus() == NetworkStatus.SYNCHRONIZED)
                    return

                if (this._pendingBlocks.length > 0)
                    this._downloadPendingBlocks()
                else
                    this._continueDownload(blockTip)
            }

        })
    }

    private async _isValidBlock(hash: string, prevHash: Buffer, tip: ChainTip, peer: Peer): Promise<boolean> {
        if (await this.getStatus() == NetworkStatus.SYNCHRONIZED) {
            const height = await this._chain.getHeight(prevHash)

            if (height < tip.height) {
                this._updateBestheight()

                if (height < this._bestHeight) {
                    peer.disconnect()
                    this._downloadWindow.delete(hash)

                    return false
                }
            }
        }

        return true
    }



    private async _txHandler(peer: Peer, message: TxMessage) {
        return Lock.acquire(LockKeys.TxRequest, async () => {
            const hash = message.transaction.hash

            if (!this._downloadingTxs.has(hash)) return

            Logger.debug("A new transaction was announced: %s", message.transaction.hash)

            this._downloadingTxs.delete(hash)

            if (!(await this._txMempool.has(message.transaction._getHash())))
                await this._txMempool.add(message.transaction)
        })
    }

    private _updateBestheight(): void {
        const heights = this._outboundPeers.map(peer => peer.bestHeight)
        const heightVotes: { [index: number]: number } = {}

        for (const height of heights)
            heightVotes[height] = (heightVotes[height] || 0) + 1

        const bestHeight = Object.entries(heightVotes).reduce((prev, current) => {
            const currentKey = Number.parseInt(current[0])

            if (current[1] > prev[1])
                return current
            else if (prev[1] > current[1])
                return prev
            else if (prev[0] > currentKey)
                return prev
            else
                return current
        }, [0, 0])

        const prevHeight = this._bestHeight
        this._bestHeight = bestHeight[0] as number

        if (prevHeight != this._bestHeight)
            Logger.debug("Best height detected: " + this._bestHeight)
    }

    private async _downloadPendingBlocks() {
        const blockHashes = this._pendingBlocks.slice(0, Constants.DOWNLOAD_SIZE)
        this._pendingBlocks = this._pendingBlocks.slice(Constants.DOWNLOAD_SIZE)

        await this._downloadBlocks(blockHashes)
    }

    private async _downloadBlocks(blockHashes: string[]) {
        return Lock.acquire(LockKeys.DownloadBlocks, async () => {
            const self = this

            if (blockHashes.length == 0)
                return

            blockHashes.forEach(hash => this._downloadingBlocks.set(hash, false))

            while (blockHashes.length > 0) {
                try {
                    let peer = await self._getAvailablePeer()

                    if (!peer) return

                    if (peer.status !== Peer.STATUS.DISCONNECTED) {
                        if (blockHashes.length > 1)
                            Logger.debug("Downloanding %s blocks [From=%s, To=%s]",
                                blockHashes.length,
                                blockHashes.first(),
                                blockHashes.last())

                        await peer.getBlocks(blockHashes)

                        peer.busy = false

                        if (this._downloadingBlocks.size == 0)
                            break
                    }
                } catch (_e) {
                    let err: PendingBlocksError = _e

                    Logger.warn(err.message)

                    blockHashes.clear()
                    blockHashes.push(...err.pendingBlocks)
                }
            }
        })
    }

    public _downloadTxs(txids: string[]) {
        return Lock.acquire(LockKeys.DownloadTxs, async () => {
            const self = this

            if (txids.length == 0)
                return

            txids.forEach(hash => this._downloadingTxs.set(hash, false))

            while (txids.length > 0) {
                try {
                    let peer = await self._getAvailablePeer()

                    if (!peer) return

                    if (peer.status !== Peer.STATUS.DISCONNECTED) {
                        if (txids.length > 1)
                            Logger.debug("Downloanding %s txs", txids.length)

                        await peer.getTxs(txids)

                        peer.busy = false

                        break
                    }
                } catch (_e) {
                    txids.clear()

                    let err: PendingTxsError = _e

                    Logger.warn(err.message)

                    for (const tx of err.pendingTxs) {
                        if (await this._txMempool.has(BufferHelper.fromHex(tx).reverse())) continue

                        Logger.trace("Fail to download tx: %s", tx)
                        txids.push(tx)

                    }
                }
            }
        })
    }

    public constructor(chain: Blockchain, mempool: Mempool) {
        this._notifier = new EventEmitter()
        this._chain = chain
        this._status = NetworkStatus.DISCONNECTED
        this._bestHeight = 0

        this._pendingBlocks = new Array()
        this._outboundPeers = new Array()
        this._downloadingTxs = new Map()
        this._downloadingBlocks = new Map()
        this._peerAddresses = new Array()
        this._txMempool = mempool
        this._availableMessages = new Messages()

        this._notifier.setMaxListeners(2000);

        Logger.level = Config.logLevel
    }

    public async getStatus() {
        return await Lock.acquire(LockKeys.Status, () => this._status)
    }

    public async start() {
        if ((await this.getStatus()) == NetworkStatus.SYNC) {
            Logger.warn('Initial block download was started')
            return
        }

        if ((await this.getStatus()) < NetworkStatus.CONNECTED) {
            Logger.warn('Requires connecting to some nodes to start the synchronization')
            return
        }

        await this._setStatus(NetworkStatus.SYNC)

        let blockTip = await this._chain.getLocalTip()

        if (this._bestHeight <= blockTip.height) {
            await this._setStatus(NetworkStatus.SYNCHRONIZED)
            Logger.info('Chain is full downloaded: Height=%d', blockTip.height)

            return
        }

        if (blockTip.hash === Constants.NULL_HASH) {
            await this._chain.createGenesisBlock()

            blockTip = await this._chain.getLocalTip()

            Logger.info("Created block genesis %s", blockTip.hash)
        }

        Logger.info('Chain requires be synchronize: Pending=%d', this._bestHeight - blockTip.height)

        if (this._pendingBlocks.length > 0)
            this._downloadPendingBlocks()
        else
            this._continueDownload(blockTip)
    }

    public connect() {
        Logger.info("Initializing network manager [Network=%s]", Networks.defaultNetwork.name)

        return new Promise<void>(async done => {
            if ((await this.getStatus()) >= NetworkStatus.CONNECTED) {
                Logger.warn("Already connect to Bitcoin's network")

                done()

                return
            }

            if ((await this.getStatus()) == NetworkStatus.CONNECTING) {
                Logger.warn("The network is trying to connect")

                done()

                return
            }

            const timeoutHandler = setTimeout(() => {
                this.removeAllListeners(NetworkEvents.READY)
                this._notifier.emit(NetworkEvents.DISCONNECT)

                Logger.warn("Can't connect to Bitcoin's network")

                done()
            }, Constants.Timeouts.WAIT_FOR_CONNECT_NET)

            await this._setStatus(NetworkStatus.CONNECTING)

            Logger.info("Trying to connect to Bitcoin network")

            const resolve = async () => {
                clearTimeout(timeoutHandler)

                this._notifier.emit(NetworkEvents.READY)

                await this._setStatus(NetworkStatus.CONNECTED)

                Logger.info("Pool is connected to Bitcoin's network")

                this._monitorPeers();

                done()
            }

            this.once(NetworkEvents.READY, resolve)

            await this._discoveringPeers()
            this._fillPool()
        })
    }

    public disconnect() {
        return new Promise<void>(async done => {
            if ((await this.getStatus()) < NetworkStatus.DISCONNECTING) {
                Logger.warn("The network isn't connected")

                done()

                return
            }

            if ((await this.getStatus()) == NetworkStatus.DISCONNECTING) {
                Logger.warn("The network is trying to disconnect")

                done()

                return
            }

            await this._setStatus(NetworkStatus.DISCONNECTING)

            Logger.debug('Disconnecting the peers')

            while (await Lock.acquire<number>(LockKeys.Peer, () => this._outboundPeers.length) > 0) {
                for (const peer of this._outboundPeers)
                    peer.disconnect()

                await sleep(1000)
            }

            await this._setStatus(NetworkStatus.DISCONNECTED)
            this._notifier.emit(NetworkEvents.DISCONNECT)

            Logger.info("The network is disconnected")

            done()
        })
    }

    public async requestHeaders(startHashes: string[], stopHash?: string) {
        startHashes = startHashes || [Constants.NULL_HASH]
        startHashes = startHashes.length == 0 ? [Constants.NULL_HASH] : startHashes
        stopHash = stopHash || Constants.NULL_HASH

        await this._requestHeaders(startHashes, stopHash)

        this._downloadPendingBlocks()
    }

    public async broadcastTx(rawData: Buffer): Promise<boolean> {
        if (!rawData)
            throw new TypeError("The tx's data can't be null")

        const transaction = new Transaction().fromBuffer(rawData)

        if (await this._txMempool.has(transaction._getHash()))
            return true

        await this._txMempool.add(transaction)

        let respond = false

        return new Promise<boolean>(async sent => {

            for (let peer of this._outboundPeers) {
                if (peer.status !== Peer.STATUS.READY)
                    continue

                if (await this._sendTx(peer, transaction) && !respond) {
                    Logger.info("Sent tx: %s", transaction._getHash().toReverseHex())
                    respond = true
                    sent(true)
                }
            }

            if (!respond) sent(false)
        })

    }

    private async _sendTx(peer: Peer, tx: Transaction) {
        const hash = tx._getHash()

        return new Promise<boolean>(sent => {
            const listener = (message: GetdataMessage) => {
                if (message.inventory.filter(i => i.hash.equals(hash))) {
                    peer.sendMessage(this._availableMessages.Transaction(tx))

                    destroyTimeout()
                    sent(true)
                }
            }

            const timeout = setTimeout(() => {
                destroyTimeout()
                sent(false)
            }, Constants.Timeouts.WAIT_FOR_SEND_TX)

            const destroyTimeout = function () {
                clearTimeout(timeout)
                peer.removeListener(NetworkEvents.GETDATA, listener)
            }

            peer.addListener(NetworkEvents.GETDATA, listener)
            peer.sendMessage(this._availableMessages.Inventory.forTransaction(hash))
        })
    }

    public on(event: NetworkEvents, listener: (BlockListener | HeadersListener
        | TxListener | ReadyListener | DisconnectedListener | ErrorListener | SyncListener)) {
        this._notifier.on(event, listener)
    }

    public once(event: NetworkEvents, listener: (BlockListener | HeadersListener
        | TxListener | ReadyListener | DisconnectedListener | ErrorListener | SyncListener)) {
        this._notifier.once(event, listener)
    }

    public removeListener(event: NetworkEvents, listener: (BlockListener | HeadersListener
        | TxListener | ReadyListener | DisconnectedListener | ErrorListener | SyncListener)) {
        this._notifier.removeListener(event, listener)
    }

    public removeAllListeners(event: NetworkEvents) {
        this._notifier.removeAllListeners(event)
    }

}