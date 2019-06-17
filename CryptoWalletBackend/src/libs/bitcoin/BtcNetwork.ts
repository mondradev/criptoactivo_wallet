import LogginFactory from "../../utils/LogginFactory";
import { wait } from "../../utils/Extras";

import { isIPv4, isIPv6 } from 'net';
import { Pool, Messages, Peer } from "bitcore-p2p";
import { BlockHeader, Block, Networks } from "bitcore-lib";
import { EventEmitter } from "events";

import Config from "../../../config";
import BitcoinBlockDownloader from "./BlockDownloader";
import { Addr } from "./BtcModel";
import { Hash256 } from "../crypto/Hash";
import { requireNotNull } from "../../utils/Preconditions";
import AsyncLock from 'async-lock'

const Logger = LogginFactory.getLogger('Bitcoin Network')
const Lock = new AsyncLock()

const TIMEOUT_WAIT_HEADERS = 30000;
const TIMEOUT_WAIT_BLOCKS = 2000;
const TIMEOUT_WAIT_CONNECT = 10000;

/**
 * Gestiona todas las funciones de la red de Bitcoin.
 */
class Network extends EventEmitter {

    private _pool: Pool
    private _availableMessages: Messages
    private _connected = false

    private _selectedPeer: { peer: Peer, addr: Addr }

    public constructor() {
        super()

        this._availableMessages = new Messages({ network: Networks.get(Config.getAsset('bitcoin').network) })
    }

    /**
     * Obtiene la altura de la mejor cadena de los puntos del pool.
     * @returns {number} Altura de la cadena.
     */
    public get bestHeight(): number {
        if (this._pool.numberConnected() == 0)
            return 0
        return Object.entries(this._pool['_connectedPeers'])
            .map(([, peer]: [string, Peer]) => peer.bestHeight)
            .reduce((left: number, right: number) => left > right ? left : right)
    }

    /**
     * Obtiene un valor que indica si el pool ya ha conectado.
     * @returns {boolean} Un valor true para indicar que está conectado.
     */
    public get connected(): boolean { return this._connected }

    /**
     * Envía un mensaje al pool de conexiones.
     * @param message Mensaje a enviar a la red.
     */
    public sendMessage(message: Messages) {
        if (this._selectedPeer)
            this._selectedPeer.peer.sendMessage(message)
        else
            this._pool && this._pool.sendMessage(message)
    }

    /**
    * Obtiene un bloque de la red de bitcoin especificado por el blockhash. La petición tiene un tiempo máximo de 2 segundos.
    * 
    * @param {string} blockhash Hash del bloque a descargar de la red.
    * @returns {Promise<Block>} Una promesa que contiene un bloque de Bitcoin al concluir la petición o un valor nulo
    *                            en caso que falle.
    */
    public getBlock(blockhash: string): Promise<Block> {
        return new Promise<Block>(async (done) => {
            let retryHandler = null
            const timeoutHandler = setTimeout(() => this.emit(blockhash, null), TIMEOUT_WAIT_BLOCKS)

            const resolve = (valueReturned?: Block) => {
                clearTimeout(timeoutHandler)
                retryHandler && clearInterval(retryHandler)
                done(valueReturned)
            }

            const listener = (block: Block) => {
                if (block == null)
                    resolve()
                else if (block.hash === blockhash) {
                    Logger.trace(`Received block ${blockhash}`)
                    resolve(block)
                }
            }

            try {
                this.once(blockhash, listener)
                this.sendMessage(this._availableMessages.GetData.forBlock(blockhash))

                retryHandler = setInterval(() => this.sendMessage(this._availableMessages.GetData.forBlock(blockhash)), 1000)
            } catch (ex) {
                Logger.warn(`Fail to get block from pool: ${ex}`)
                this.emit(blockhash, null)
            }
        })
    }

    /**
     * Obtiene las hashes de los bloques encontrados en el pool, esto se limita a un máximo de 2000 cabeceras por petición.
     * La petición tiene un tiempo máximo de 15 segundos.
     * 
     * @param {string[]} hashblock hashes de los bloques usados como referencia para obtener su cabecera.
     * @returns {Promise<string[]>} Una promesa que contiene las cabeceras de los bloques encontrados al concluir la petición o 
     *                              un valor nulo al fallar.
     */
    public getHashes(hashblock: string[]): Promise<string[]> {
        return new Promise<string[]>(async (done) => {
            let retryHandler = null
            const timeoutHandler = setTimeout(() => this.emit('headers', null), TIMEOUT_WAIT_HEADERS)

            const resolve = (valueReturned?: string[]) => {
                clearTimeout(timeoutHandler)
                retryHandler && clearInterval(retryHandler)
                done(valueReturned)
            }

            const listener = (headers: BlockHeader[]) => {
                if (headers == null)
                    resolve()
                else if (headers.length > 0)
                    resolve(headers.map((header) => header.hash))
            }

            try {
                this.once('headers', listener)
                this.sendMessage(this._availableMessages.GetHeaders({ starts: hashblock }))
                retryHandler = setInterval(() => this.sendMessage(this._availableMessages.GetHeaders({ starts: hashblock })), 1000)
            } catch (ex) {
                Logger.warn(`Fail to get headers from pool: ${ex}`)
                this.emit('headers', null)
                resolve()
            }
        })
    }

    /**
     * Obtiene una instancia de administrador de descarga de bloques.
     * 
     * @param hashes Conjunto de hashes de los bloques que se desean descargar.
     * @returns {BitcoinBlockDownloader}
     */
    public getDownloader(hashes: string[]): BitcoinBlockDownloader {
        return new BitcoinBlockDownloader(hashes)
    }

    /**
     * Conecta el pool a la red de Bitcoin. Si se exceden los 10 segundos se finaliza la petición.
     * @returns {Promise<boolean>} Una promesa con un valor true que indica que se ha conectado el pool.
     */
    public connect(): Promise<boolean> {
        Logger.trace('Connecting pool, waiting for peers')

        this._pool = new Pool({ network: Networks.get(Config.getAsset('bitcoin').network) })

        this._addEventHandlers()

        this._pool.connect()

        return new Promise((done) => {
            const timeoutHandler = setTimeout(() => this.emit('ready', false), TIMEOUT_WAIT_CONNECT)

            const resolve = (valueReturned: boolean) => {
                clearTimeout(timeoutHandler)
                done(valueReturned)
            }

            if (this._connected)
                resolve(true)
            else
                this.once('ready', (connected) => resolve(connected))
        })
    }

    /**
     * Desconecta todos los puntos remotos del pool de conexiones.
     * @returns {Promise<void>} Una promesa vacía.
     */
    public disconnect(): Promise<void> {
        return new Promise<void>(async (resolve) => {
            if (!this._connected) {
                resolve()
                return
            }

            Logger.trace('Disconnecting pool from the peers')

            this._pool.disconnect()

            while (true) {
                if (this._pool.numberConnected() > 0)
                    await wait(1000)
                else
                    break
            }

            this._pool.removeAllListeners()

            this._connected = false
            this._pool = null

            this.emit('disconnected')
            resolve()
        })
    }

    private _addEventHandlers() {
        let bestHeight = 0

        this._pool
            .on('peerready', (peer: Peer, addr: Addr) => {
                Lock.acquire('ready', (unlock) => {

                    if (bestHeight < peer.bestHeight) {
                        Logger.debug(`Peer ready[BestHeight=${peer.bestHeight}, Version=${peer.version}, Host=${peer.host}, Port=${peer.port}, Network=${peer.network}]`)

                        bestHeight = peer.bestHeight

                        this._registerPeer(peer, addr)

                        this._connected = true
                        this.emit('ready', true)
                    }

                    unlock()
                })
            })
            .on('peerblock', (peer: Peer, message: { block: Block }) => {
                !this._selectedPeer && this._registerPeer(peer)

                if (message && message.block)
                    this.emit(message.block.hash, message.block)
            })
            .on('peerheaders', (peer: Peer, message: { headers: BlockHeader[] }) => {
                !this._selectedPeer && this._registerPeer(peer)

                if (message && message.headers)
                    this.emit('headers', message.headers)
            })
            .on('peerdisconnect', (peer: Peer, addr: Addr) => {
                if (this._selectedPeer)
                    if (addr.hash === this._selectedPeer.addr.hash)
                        this._selectedPeer = null
            })
    }
    private _registerPeer(peer: Peer, addr?: Addr) {
        requireNotNull(peer)

        if (!addr) {
            const ipv4 = isIPv4(peer.host) ? peer.host : undefined
            const ipv6 = isIPv6(peer.host) ? peer.host : undefined
            const port = peer.port
            const hash = Hash256.sha256(Buffer.from(ipv6 + ipv4 + port)).toHex()

            addr = { hash, port, ip: { v4: ipv4, v6: ipv6 } }
        }

        this._selectedPeer = { peer, addr }

        Logger.info(`Peer selected [Hash=${addr.hash}, Host=${addr.ip.v6 || addr.ip.v4}, Port=${addr.port}, BestHeight=${peer.bestHeight}]`)
    }

}

const BtcNetwork = new Network()
export default BtcNetwork