import LogginFactory from "../../utils/LogginFactory";
import { wait } from "../../utils/Extras";

import { Pool, Messages, Peer } from "bitcore-p2p";
import { BlockHeader, Block, Networks } from "bitcore-lib";
import { EventEmitter } from "events";

import Config from "../../config";
import { isNull } from "../../utils/Preconditions";
import BitcoinBlockDownloader from "./BlockDownloader";

const Logger = LogginFactory.getLogger('Bitcoin Network')

const TIMEOUT_WAIT_HEADERS = 10000;
const TIMEOUT_WAIT_BLOCKS = 10000;
const TIMEOUT_WAIT_CONNECT = 10000;
class Network extends EventEmitter {

    private _pool: Pool
    private _availableMessages: Messages
    private _connected = false

    public constructor() {
        super()

        this._availableMessages = new Messages({ network: Networks.get(Config.assets.bitcoin.network) })
        this._pool = new Pool({ network: Networks.get(Config.assets.bitcoin.network) })
    }

    public get bestHeight() {
        if (this._pool.numberConnected() == 0)
            return 0
        return Object.entries(this._pool['_connectedPeers'])
            .map(([, peer]: [string, Peer]) => peer.bestHeight)
            .reduce((left: number, right: number) => left > right ? left : right)
    }

    public get connected() { return this._connected }

    /**
    * Obtiene un bloque de la red de bitcoin.
    * 
    * @param {string} blockhash Hash del bloque a descargar de la red.
    * @returns {Promise<Block>} Bloque de Bitcoin.
    */
    public getBlock(blockhash: string): Promise<Block> {

        return new Promise<Block>(async (resolve) => {
            const peer = await this.getPeer()
            const fnCallback = (message: { block: Block }) => {
                if (message == null)
                    resolve(null)
                else if (message.block.hash === blockhash) {
                    Logger.trace(`Received block with hash ${blockhash}`)
                    resolve(message.block)
                } else
                    peer.once('block', fnCallback)
            }

            peer.once('block', fnCallback)
            peer.sendMessage(this._availableMessages.GetData.forBlock(blockhash))
            setTimeout(() => peer.emit('block', null), TIMEOUT_WAIT_BLOCKS)
        })
    }

    /**
     * Obtiene la cabecera de un bloque de la red de bitcoin.
     * 
     * @param {string[]} hashblock hash del bloque que se desea obtener su cabecera.
     * @returns {Promise<string[]>} La cabecera del bloque.
     */
    public getHeaders(hashblock: string[]): Promise<string[]> {

        return new Promise<string[]>(async (resolve) => {
            const peer = await this.getPeer()
            const listener = (message: { headers: BlockHeader[] }) => {
                if (message == null) {
                    resolve(null)
                    return
                }

                if (message.headers.length > 0)
                    Logger.trace(`Received ${message.headers.length} blockheaders`)

                resolve(message.headers.map((header) => header.hash))
            }

            peer.once('headers', listener)
            peer.sendMessage(this._availableMessages.GetHeaders({ starts: hashblock }))
            setTimeout(() => peer.emit('headers', null), TIMEOUT_WAIT_HEADERS)
        })
    }

    public getDownloader(hashes: string[]) {
        return new BitcoinBlockDownloader(hashes, this._availableMessages.GetData)
    }

    public connect() {
        Logger.trace('Connecting pool, waiting for peers')

        this._addEventHandlers()

        this._pool.connect()

        return new Promise((resolve) => {
            if (this._connected)
                resolve(true)
            else
                this.once('ready', () => resolve(true))

            setTimeout(() => resolve(false), TIMEOUT_WAIT_CONNECT)
        })
    }

    public disconnect() {
        return new Promise<void>(async (resolve) => {
            Logger.trace('Disconnecting pool from the peers')

            this._pool.disconnect()

            while (true) {
                if (this._pool.numberConnected() > 0)
                    await wait(1000)
                else
                    break
            }

            this._pool.removeAllListeners('peerready')
            this._connected = false

            resolve()
        })
    }

    private _addEventHandlers() {
        let bestHeight = 0

        this._pool
            .on('peerready', (peer: Peer) => {
                if (bestHeight < peer.bestHeight) {
                    Logger.debug(`Peer ready[BestHeight=${peer.bestHeight}, Version=${peer.version}, Host=${peer.host}, Port=${peer.port}, Network=${peer.network}]`)

                    bestHeight = peer.bestHeight

                    this._connected = true
                    this.emit('ready')
                }
            })
    }

    public getPeer() {
        return new Promise<Peer>((success) => {

            const resolve = (peer: Peer) => {
                Logger.debug(`Peer selected[Status=${peer.status}, BestHeight=${peer.bestHeight}, Version=${peer.version}, Host=${peer.host}, Port=${peer.port}, Network=${peer.network}]`)
                success(peer)
            }

            let peer: Peer = null
            let i = 0
            let size = this._pool.numberConnected()
            let entries: [string, Peer][] = Object.entries(this._pool['_connectedPeers'])

            while (isNull(peer) || peer.status == Peer.STATUS.DISCONNECTED)
                if (i < size)
                    peer = entries[i++][1]
                else
                    break

            if (isNull(peer))
                throw new Error(`Can't find a connected peer `)

            if (peer.status != Peer.STATUS.READY)
                peer.once('ready', () => resolve(peer))
            else
                resolve(peer)
        })
    }


}

const BtcNetwork = new Network()
export default BtcNetwork



// TODO Finalizar la función de sincronización
// public async sync() {
//     const timer = new TimeCounter()
//     let i = 0
//     let last = 0


//     timer.on('second', async () => {
//         const blockRate = i - last
//         const blockleft = BtcNetwork.bestHeight - i

//         if (blockRate > 0 && i > 0 && !this._synchronized) {
//             Logger.info(`BlockRate=${blockRate} blk/s, Remaining=${blockleft} blks, TimeLeft=${TimeSpan.fromSeconds(blockleft / blockRate)}`)
//             last = i
//         }
//     })

//     await BtcNetwork.connect()

//     Logger.info('Initializing blockchain download')

//     try {
//         while (true) {
//             let { hash, height } = await BitcoinBlockStore.getLocalTip()
//             Logger.info(`LocalTip [Hash=${hash}, Height=${height}]`)

//             let headers: string[]
//             let hashes = await BitcoinBlockStore.getLastHashes(height)

//             if (!hashes.includes(hash))
//                 throw new Error(`Fail in chain state, can't find last block [Hash=${hash}]`)

//             timer.start()

//             while (true) {
//                 if (BtcNetwork.bestHeight <= height) {
//                     if (!this._synchronized) {
//                         this._synchronized = true
//                         this._notifier.emit('downloaded')
//                         Logger.info(`Download finalized [Tip=${hash}, Height=${height}]`)
//                     }

//                     headers = await BtcNetwork.getHeaders([hash])

//                     if (headers)
//                         if (headers.length == 0)
//                             await Extras.wait(10000)
//                         else
//                             break
//                 } else {
//                     headers = await BtcNetwork.getHeaders(hashes)

//                     if (headers && headers.length == 0)
//                         await Extras.wait(10000)
//                     else if (headers)
//                         break
//                 }
//             }

//             const blockRequest = BtcNetwork.getDownloader(headers)
//             let block = null

//             do {
//                 block = await blockRequest.next()

//                 if (block) {
//                     height++
//                     Logger.trace(`Received block [Hash=${block.hash}, Height=${height}]`)

//                     await BitcoinBlockStore.import(block)
//                     i = height
//                 }

//             } while (block != null)

//             timer.stop()

//             Logger.debug(`ProcessedBlocks=${headers.length}, Time=${timer.toLocalTimeString()}`)

//         }

//     } catch (ex) {
//         BtcNetwork.disconnect()
//         Logger.error(`Error="Fail to download blockchain", Exception=${ex.stack}`)
//         return this.sync()
//     }
// }