import NetworkStatus from './NetworkStatus'
import NetworkEvents from './NetworkEvents'
import Constants from './Constants'

import LogginFactory from "../../utils/LogginFactory"
import { wait } from "../../utils/Extras"

import { Messages, Peer } from "bitcore-p2p"
import { BlockHeader, Block, Networks } from "bitcore-lib"
import { EventEmitter } from "events"

import Config, { AssetConfig } from "../../../config"
import AsyncLock from 'async-lock'
import dns from 'dns'
import { requireNotNull } from "../../utils/Preconditions";
import BtcBlockchain from "./BtcBlockchain";
import TimeSpan from "../../utils/TimeSpan";

const ClassName = "Bitcoin Network"
const Logger = LogginFactory.getLogger(ClassName)
const Lock = new AsyncLock()

const fnReadMessage: () => void = Peer.prototype['_readMessage']

Peer.prototype['_readMessage'] = function () {
    try {
        fnReadMessage.apply(this)
    } catch (e) {
        this._onError(e)
    }
}

/**
 * Gestiona todas las funciones de la red de Bitcoin.
 */
class Network extends EventEmitter {

    private _availableMessages: Messages
    private _connected = false
    private _disconnecting = false
    private _peers = new Array<Peer>()
    private _configuration: AssetConfig
    private _availableNodes = new Array<string>()
    private _bestHeight = 0
    private _stoppingHash = Constants.NullHash
    private _workers = new Array<Peer>()
    private _blocksToDownload = new Array<string>()
    private _lastCheckpoint: Date
    private _isReady = false

    public get isReady(): boolean { return this._isReady }

    public constructor() {
        super()

        this._configuration = Config.getAsset(Constants.Bitcoin)

        if (!this._configuration)
            throw new Error(`Require Bitcoin configuration`)

        Networks.defaultNetwork = Networks.get(this._configuration.network)

        this._availableMessages = new Messages({ network: Networks.defaultNetwork })

        this.on(NetworkEvents.Headers, this._headerHandler)
    }

    /**
     * Obtiene la altura de la mejor cadena de los puntos del pool.
     */
    public get bestHeight(): number { return this._bestHeight }

    /**
     * Obtiene un valor que indica si el pool ya ha conectado.
     */
    public get connected(): boolean { return this._connected }

    /**
     * Número de nodos conectados
     */
    private get _connectedPeers(): number { return this._workers.length + this._peers.length }

    /**
     * Conecta el pool a la red de Bitcoin. Si se exceden los 15 segundos se finaliza la petición.
     * @returns {Promise<boolean>} Una promesa con un valor true que indica que se ha conectado el pool.
     */
    public connect(): Promise<boolean> {
        return new Promise(async (done) => {
            const timeoutHandler = setTimeout(() => this.emit('ready', false),
                Constants.Timeouts.WaitForConnectNet)

            const resolve = (connected: boolean) => {
                this._connected = true
                clearTimeout(timeoutHandler)
                done(connected)

                Logger.info('Connected to Bitcoin Network')
            }

            if (this._connected)
                resolve(true)
            else {
                this.once('ready', (connected) => resolve(connected))

                await this._discoveringPeers()
                this._fillConnections()
            }
        })
    }

    /**
     * Resuelve las IP desde las semillas del DNS.
     */
    private async _discoveringPeers() {
        Logger.info('Discovering peers from DNS')

        for (const seed of this._configuration.seeds) {
            const addresses = await this._resolveDns(seed)

            for (const node of addresses)
                this._availableNodes.push(node)
        }
    }

    /**
     * Rellena las conexiones del pool del nodo.
     */
    private async _fillConnections() {
        Logger.info('Connecting pool, waiting for peers')

        while (!this._disconnecting) {
            if (this._configuration.maxConnections > await Lock.acquire('peer', () => this._connectedPeers))
                await this._connectPeer(await Lock.acquire('peer', () => this._availableNodes.shift()))
            else
                await wait(1000)
        }
    }

    /**
     * Obtiene las direcciones a partir de la semilla DNS proporcionada.
     * @param {string} seed Semilla DNS a resolver.
     * @returns {Promise<Array<string>>} Promesa de un vector con las direcciones obtenidas del DNS.
     */
    private _resolveDns(seed: string): Promise<Array<string>> {
        return new Promise<Array<string>>(resolve => dns.resolve(seed, (err, addresses) => {
            if (err)
                resolve(new Array<string>(0))
            else
                resolve(addresses)
        }))
    }

    /**
     * Establece la conexión a un nodo.
     * @param {string} address Dirección del nodo a conectar.
     * @retuns {Promise} Promesa vacía.
     */
    private _connectPeer(address: string) {
        return new Promise<void>(async resolve => {
            if (this._disconnecting) {
                resolve()
                return
            }

            await Lock.acquire('connectPeer', async (unlock) => {
                if (this._connectedPeers >= this._configuration.maxConnections) {
                    unlock()
                    return
                }

                let peer = new Peer({
                    host: address,
                    port: this._configuration.port,
                    message: this._availableMessages,
                    network: Networks.defaultNetwork
                })

                let timeout = null

                this._addEventHandlers(peer)

                peer.on('ready', async () => {
                    clearTimeout(timeout)

                    const connectedPeers = await Lock.acquire<number>('peer', (unlock) => {
                        this._peers.push(peer)
                        unlock(null, this._connectedPeers)
                    })

                    Logger.info(`Peer connected {}/{} [Version={}, Agent={}, BestHeight={}, Host={}]`,
                        connectedPeers,
                        this._configuration.maxConnections,
                        peer.version,
                        peer.subversion,
                        peer.bestHeight,
                        peer.host
                    )

                    this._bestHeight = peer.bestHeight > this._bestHeight ? peer.bestHeight : this._bestHeight

                    if (!this._connected)
                        this.emit('ready', true)

                    peer.removeAllListeners('ready')

                    if (!this._connected)
                        peer.disconnect()

                    unlock()
                })

                peer.connect()

                await wait(100)

                timeout = setTimeout(() => {
                    peer.disconnect()

                    peer.removeAllListeners('error')
                    peer.removeAllListeners('addr')
                    peer.removeAllListeners('disconnect')
                    peer.removeAllListeners('ready')

                    unlock()
                }, Constants.Timeouts.WaitForConnectPeer)
            })

            resolve()
        })

    }

    /**
     * Establece los controladores de eventos al nodo especificado.
     * @param peer Node al que se requiere establecer los controladores de eventos.
     */
    private _addEventHandlers(peer: Peer) {
        peer.removeAllListeners('error')
        peer.removeAllListeners('addr')
        peer.removeAllListeners('disconnect')

        peer.on('error', (reason: Error) => Logger.warn(`${reason.message} [${peer.host}]`))

        peer.on('addr', (message: { addresses: Array<{ ip: { v6: string, v4: string }, port: number, time: Date }> }) => {
            let nAddresses = 0

            for (const address of message.addresses) {
                const v4 = address.ip.v4
                const v6 = address.ip.v6

                if (!this._availableNodes.includes(v4)) {
                    this._availableNodes.push(v4)
                    nAddresses++
                }

                if (!this._availableNodes.includes(v6)) {
                    this._availableNodes.push(v6)
                    nAddresses++
                }
            }

            if (nAddresses)
                Logger.info(`Received ${nAddresses} addresses from ${peer.host}`)
        })

        peer.on('disconnect', async () => {
            let connectedPeers = await Lock.acquire<number>('peer', (unlock) => {
                this._peers = this._peers.remove(peer)
                unlock(null, this._connectedPeers)
            })

            Logger.info(`Peer disconnected ${connectedPeers}/${this._configuration.maxConnections} [Host=${peer.host}]`)
        });
    }

    /**
     * Envía la petición de descarga de los encabezados de los bloques.
     * @param {Array<string>} starts Hash de los bloques utilizados para ubicar los bloques faltantes.
     * @param {string} stop Hash del bloque donde se detiene la descarga de encabezados.
     * @returns {Promise<void>} Promesa vacía. 
     */
    public async sendGetHeaders(starts: Array<string>, stop: string): Promise<void> {
        this._stoppingHash = stop ? stop : Constants.NullHash

        let completed = false

        while (!completed) {
            while (await Lock.acquire('peer', (unlock) => unlock(null, this._peers.length)) == 0 && this._connected)
                await wait(100)

            for (const peer of this._peers) {
                completed = await new Promise<boolean>((resolve) => {
                    if (peer.status == Peer.STATUS.DISCONNECTED)
                        resolve(false)
                    else {
                        let timeout = null

                        // Evento de recepción de las cabeceras
                        peer.on(NetworkEvents.Headers, (message: { headers: Array<BlockHeader> }) => {
                            if (!message)
                                return

                            clearTimeout(timeout)

                            peer.removeAllListeners(NetworkEvents.Headers)
                            this.emit(NetworkEvents.Headers, peer, message)
                            resolve(true)
                        })

                        // Envío de la petición de las cabeceras
                        peer.sendMessage(this._availableMessages.GetHeaders({ starts, stop }))

                        // Temporizador de espera de la respuesta
                        timeout = setTimeout(() => {
                            peer.removeAllListeners(NetworkEvents.Headers)
                            peer.disconnect()
                            resolve(false)
                        }, Constants.Timeouts.WaitForHeaders)
                    }
                })

                if (completed) break

                while (await Lock.acquire('peer', (unlock) => unlock(null, this._connectedPeers)) == 0 && this._connected)
                    await wait(100)
            }
        }
    }

    /**
     * Controlador del evento Headers.
     * @param {Peer} peer Nodo que responde a la petición de 'GetHeaders'.
     * @param {Messages} message Mensaje de respuesta con el vector de encabezados.
     */
    private _headerHandler(peer: Peer, message: { headers: Array<BlockHeader> }) {
        if (!message || !message.headers)
            return

        if (peer.network.name !== Networks.defaultNetwork.name)
            return

        Logger.info(`Received {} block headers [{}] from {}`, message.headers.length, message.headers[0].hash, peer.host)

        const hashes = message.headers.map(b => b.hash)

        if (message.headers.length == 2000) {
            // TODO Validar bloques recibidos 
            //if (await this._validateHeaders(message.headers)) return
            this.sendGetHeaders(new Array(...hashes).reverse().slice(0, 30), this._stoppingHash)
        } else
            this._stoppingHash = Constants.NullHash

        this._downloadBlocks(hashes)
    }

    /**
     * Desconecta todos los puntos remotos.
     * @returns {Promise<void>} Una promesa vacía.
     */
    public disconnect(): Promise<void> {
        return new Promise<void>(async (resolve) => {
            if (!this._connected) {
                resolve()
                return
            }

            this._disconnecting = true

            Logger.trace('Disconnecting the peers')

            this._connected = false

            for (const peer of this._peers)
                peer.disconnect()

            for (const peer of this._workers)
                peer.disconnect()

            while (await Lock.acquire('peer', () => this._peers.length) > 0)
                await wait(100)

            while (await Lock.acquire('worker', () => this._workers.length) > 0)
                await wait(100)

            this.emit('disconnected')

            this._disconnecting = false

            resolve()
        })
    }

    /**
     * Descarga los bloques especificados.
     * @param hashes Los hashes de los bloques a descargar.
     */
    public async _downloadBlocks(hashes: string[]) {
        if (!hashes || hashes.length == 0)
            return

        await Lock.acquire('blocks', () => this._blocksToDownload.push(...hashes))

        this._continueDownload()
    }

    /**
     * Descarga los bloques por un nodo especificado.
     * @param peer Nodo a realizar la tarea de descarga
     */
    private async _createJob(peer: Peer) {
        requireNotNull(peer, "El nodo no puede ser nulo")

        await Lock.acquire<number>('worker', () => this._workers.push(peer))

        let blockHash: string

        while (blockHash = await Lock.acquire('blocks', () => this._blocksToDownload.shift())) {

            blockHash = await new Promise<string>(async (resolve) => {
                let timeout: NodeJS.Timeout;

                peer.on('block', async (message: { block: Block }) => {

                    if (!message) return

                    clearTimeout(timeout)

                    await BtcBlockchain.addBlock(message.block)

                    peer.removeAllListeners('block')

                    resolve()
                })

                peer.sendMessage(this._availableMessages.GetData.forBlock(blockHash))

                timeout = setTimeout(async () => {
                    Logger.warn(`Don't response to request {} (GetData)`, peer.host)
                    peer.removeAllListeners('block')
                    peer.disconnect()

                    await Lock.acquire<Array<Peer>>('worker', () => this._workers = this._workers.includes(peer) ? this._workers.remove(peer) : this._workers)

                    resolve(blockHash)
                }, Constants.Timeouts.WaitForBlocks)
            })

            if (blockHash) {
                await Lock.acquire('blocks', () => this._blocksToDownload = [blockHash, ...this._blocksToDownload])
                return
            }

        }

        Logger.debug('Finalizing job ({})', peer.host)

        await Lock.acquire<Array<Peer>>('worker', () => this._workers = this._workers.includes(peer) ? this._workers.remove(peer) : this._workers)
        await Lock.acquire<Array<Peer>>('peer', () => !this._peers.includes(peer) ? this._peers.push(peer) : null)
    }

    /**
     * Inicia o remota la tarea de descargar los bloques
     */
    private async _continueDownload() {
        while (await Lock.acquire('worker', () => this._workers.length) < this._configuration.maxConnections) {

            if (await Lock.acquire('blocks', () => this._blocksToDownload.length) == 0)
                break

            let peer: Peer

            while (!peer)
                if (await Lock.acquire('peer', () => this._peers.length) > 0)
                    peer = await Lock.acquire('peer', () => this._peers.shift())
                else
                    await wait(1000)

            this._createJob(peer).then(this._continueDownload.bind(this))
        }
    }

    /**
     * Inicia la sincronización de la cadena de bloques.
     */
    public async startSync() {
        if (!this.connected && !await this.connect())
            throw new Error(`Please connect to Bitcoin network`)

        if (await BtcBlockchain.getLocalHeight() == this.bestHeight) {
            // TODO Initialize node
        } else {
            await BtcBlockchain.initialize()

            Logger.debug('Starting blocks download')

            let lastHeight = await BtcBlockchain.getLocalHeight()

            setInterval(async () => {
                let { hash, height, txn, time } = await BtcBlockchain.getLocalTip()
                Logger.info(`Height={}, Progress={}, Rate={} Orphan={}, Time={}, Txn={}, Hash={}, MemUsage={} MB`,
                    height,
                    (height / this.bestHeight * 100).toFixed(2),
                    height - lastHeight,
                    BtcBlockchain.orphan,
                    time.toLocaleString(),
                    txn,
                    hash,
                    (process.memoryUsage().rss / 1048576).toFixed(2)
                )
                lastHeight = height
            }, 1000)

            this._requestBlocks()
        }

    }

    /**
     * Realiza una petición de bloques en el proceso de 'descarga inicial'.
     */
    private async _requestBlocks() {
        const { starts, stop } = await BtcBlockchain.getLocators()
        const height = await BtcBlockchain.getLocalHeight()

        this._lastCheckpoint = new Date()

        let event = stop

        if (event === Constants.NullHash)
            event = (((height / 10000) + 1) * 10000).toFixed(0)

        BtcBlockchain.on(event, () => {
            Logger.info('Checkpoint completed in {} from {} to {}',
                TimeSpan.fromMiliseconds(Date.now() - (this._lastCheckpoint || new Date()).getTime()).toString(),
                starts[0],
                stop
            )

            BtcBlockchain.removeAllListeners(event)
            this._requestBlocks()
        })

        Logger.info('Request headers from {} to {}', starts[0], stop)

        this.sendGetHeaders(starts, stop)

    }
}

const BtcNetwork = new Network()
export default BtcNetwork