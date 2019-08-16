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

const Logger = LogginFactory.getLogger('Bitcoin Network')
const Lock = new AsyncLock()

const TIMEOUT_WAIT_HEADERS = 3000
const TIMEOUT_WAIT_BLOCKS = 2000
const TIMEOUT_WAIT_CONNECT = 20000
const TIMEOUT_WAIT_CONNECT_PEER = 5000
const MAX_BLOCKS_PER_PEER = 16
const NULL_HASH = Array(65).join('0');

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
    private _stoppingHash = NULL_HASH
    private _workers = new Array<Peer>()
    private _blocksToDownload = new Array<string>()
    private _lastCheckpoint: Date
    private _isReady = false

    public get isReady(): boolean { return this._isReady }

    public constructor() {
        super()

        this._configuration = Config.getAsset('bitcoin')

        if (!this._configuration)
            throw new Error(`Require Bitcoin configuration`)

        Networks.defaultNetwork = Networks.get(this._configuration.network)

        this._availableMessages = new Messages({ network: Networks.defaultNetwork })

        this.on('headers', this._headerHandler)
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
        return new Promise((done) => {
            const timeoutHandler = setTimeout(() => this.emit('ready', false), TIMEOUT_WAIT_CONNECT)

            const resolve = (connected: boolean) => {
                this._connected = true
                clearTimeout(timeoutHandler)
                done(connected)

                Logger.info('Connected to Bitcoin Network')
            }

            if (this._connected)
                resolve(true)
            else {
                Logger.info('Connecting pool, waiting for peers')
                this.once('ready', (connected) => resolve(connected))
            }

            this._discoveringPeers();
        })
    }

    /**
     * Resuelve las IP desde las semillas del DNS.
     */
    private async _discoveringPeers() {
        for (const seed of this._configuration.seeds) {
            const addresses = await this._resolveDns(seed)

            for (const node of addresses) {
                let connectedPeers = await Lock.acquire<number>('peer', (unlock) => unlock(null, this._connectedPeers))

                if (this._configuration.maxConnections > connectedPeers)
                    await this._connectPeer(node)
                else
                    this._availableNodes.push(node)
            }
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

                    Logger.info(`Peer connected ${connectedPeers}/${this._configuration.maxConnections} [Version=${peer.version}, Agent=${peer.subversion}, BestHeight=${peer.bestHeight}, Host=${peer.host}]`)
                    this._bestHeight = peer.bestHeight > this._bestHeight ? peer.bestHeight : this._bestHeight

                    if (!this._connected)
                        this.emit('ready', true)

                    peer.removeAllListeners('ready')

                    if (!this._connected)
                        peer.disconnect()

                    unlock()
                })

                try { peer.connect() } catch (ex) {
                    peer.disconnect()
                }

                await wait(100)

                timeout = setTimeout(() => {
                    peer.disconnect()

                    peer.removeAllListeners('error')
                    peer.removeAllListeners('addr')
                    peer.removeAllListeners('disconnect')
                    peer.removeAllListeners('ready')

                    unlock()
                }, TIMEOUT_WAIT_CONNECT_PEER)
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

            while (this._connected // Conectado a la red
                && this._configuration.maxConnections > connectedPeers // No llegar al limite de conexiones
                && this._availableNodes.length > 0) { // Direcciones de nodos disponibles por conectar

                const IP = this._availableNodes.shift()
                await this._connectPeer(IP)

                connectedPeers = await Lock.acquire<number>('peer', (unlock) => unlock(null, this._connectedPeers))
                await wait(100)
            }
        });
    }

    /**
     * Envía la petición de descarga de los encabezados de los bloques.
     * @param {Array<string>} starts Hash de los bloques utilizados para ubicar los bloques faltantes.
     * @param {string} stop Hash del bloque donde se detiene la descarga de encabezados.
     * @returns {Promise<void>} Promesa vacía. 
     */
    public async sendGetHeaders(starts: Array<string>, stop: string): Promise<void> {
        this._stoppingHash = stop ? stop : NULL_HASH

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
                        peer.on('headers', (message: { headers: Array<BlockHeader> }) => {
                            if (!message)
                                return

                            clearTimeout(timeout)

                            peer.removeAllListeners('headers')
                            this.emit('headers', peer, message)
                            resolve(true)
                        })

                        // Envío de la petición de las cabeceras
                        peer.sendMessage(this._availableMessages.GetHeaders({ starts, stop }))

                        // Temporizador de espera de la respuesta
                        timeout = setTimeout(() => {
                            peer.removeAllListeners('headers')
                            peer.disconnect()
                            resolve(false)
                        }, TIMEOUT_WAIT_HEADERS)
                    }
                })

                if (completed) break

                while (await Lock.acquire('peer', (unlock) => unlock(null, this._connectedPeers)) == 0 && this._connected)
                    await wait(100)
            }
        }
    }

    /**
     * Controlador del evento 'Headers'.
     * @param {Peer} peer Nodo que responde a la petición de 'GetHeaders'.
     * @param {Messages} message Mensaje de respuesta con el vector de encabezados.
     */
    private _headerHandler(peer: Peer, message: { headers: Array<BlockHeader> }) {
        if (!message || !message.headers)
            return

        if (peer.network.name !== Networks.defaultNetwork.name)
            return

        Logger.debug(`Received ${message.headers.length} block headers [${message.headers[0].hash}] from ${peer.host}`)

        if (message.headers.length == 2000) {
            // TODO Validar bloques recibidos 
            //if (await this._validateHeaders(message.headers)) return
            this.sendGetHeaders(message.headers.reverse().slice(0, 30).map(b => b.hash), this._stoppingHash)
        } else
            this._stoppingHash = NULL_HASH

        this._downloadBlocks(message.headers.map(header => header.hash).reverse())
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

        let block: string

        while (block = await Lock.acquire('blocks', () => this._blocksToDownload.shift())) {

            block = await new Promise<string>((resolve) => {
                let timeout: NodeJS.Timeout;

                peer.on('block', async (message: { block: Block }) => {

                    if (!message) return

                    clearTimeout(timeout)

                    BtcBlockchain.addBlock(message.block)

                    peer.removeAllListeners('block')

                    resolve()
                })

                peer.sendMessage(this._availableMessages.GetData.forBlock(block))

                timeout = setTimeout(async () => {
                    Logger.warn(`Don't response to request {} (GetData)`, peer.host)
                    peer.removeAllListeners('block')
                    peer.disconnect()

                    await Lock.acquire<Array<Peer>>('worker', () => this._workers = this._workers.includes(peer) ? this._workers.remove(peer) : this._workers)

                    resolve(block)
                }, TIMEOUT_WAIT_BLOCKS)
            })

            if (block) {
                await Lock.acquire('blocks', () => this._blocksToDownload = [block, ...this._blocksToDownload])
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
            Logger.debug('Initializing blocks download')

            let lastHeight = await BtcBlockchain.getLocalHeight()

            setInterval(async () => {
                let { hash, height, txn, time } = await BtcBlockchain.getLocalTip()
                Logger.info(`Height=${height}, Progress=${(height / this.bestHeight * 100).toFixed(2)}, Rate=${height - lastHeight} Orphan=${BtcBlockchain.orphan}, Time=${time.toLocaleString()}, Txn=${txn}, Hash=${hash}`)
                lastHeight = height
            }, 1000)

            await BtcBlockchain.initialize()
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

        if (event === NULL_HASH)
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

        Logger.debug(`Request headers from ${starts[0]} to ${stop}`)

        this.sendGetHeaders(starts, stop)

    }
}

const BtcNetwork = new Network()
export default BtcNetwork