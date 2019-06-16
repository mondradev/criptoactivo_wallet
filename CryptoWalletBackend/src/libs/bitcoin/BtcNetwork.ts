import LogginFactory from "../../utils/LogginFactory";
import { wait } from "../../utils/Extras";

import { Pool, Messages, Peer } from "bitcore-p2p";
import { BlockHeader, Block, Networks } from "bitcore-lib";
import { EventEmitter } from "events";

import Config from "../../../config";
import BitcoinBlockDownloader from "./BlockDownloader";

const Logger = LogginFactory.getLogger('Bitcoin Network')

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
            const timeoutHandler = setTimeout(() => this.emit(blockhash, null), TIMEOUT_WAIT_BLOCKS)

            const resolve = (valueReturned?: Block) => {
                clearTimeout(timeoutHandler)
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
                this._pool.sendMessage(this._availableMessages.GetData.forBlock(blockhash))
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
            const timeoutHandler = setTimeout(() => this.emit('headers', null), TIMEOUT_WAIT_HEADERS)

            const resolve = (valueReturned?: string[]) => {
                clearTimeout(timeoutHandler)
                done(valueReturned)
            }

            const listener = (headers: BlockHeader[]) => {
                if (headers == null)
                    resolve()
                else if (headers.length > 0) {
                    Logger.trace(`Received ${headers.length} headers`)

                    resolve(headers.map((header) => header.hash))
                }
            }

            try {
                this.once('headers', listener)
                this._pool.sendMessage(this._availableMessages.GetHeaders({ starts: hashblock }))
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
            .on('peerready', (peer: Peer) => {
                if (bestHeight < peer.bestHeight) {
                    Logger.debug(`Peer ready[BestHeight=${peer.bestHeight}, Version=${peer.version}, Host=${peer.host}, Port=${peer.port}, Network=${peer.network}]`)

                    bestHeight = peer.bestHeight

                    this._connected = true
                    this.emit('ready', true)
                }
            })
            .on('peerblock', (peer: Peer, message: { block: Block }) => {
                if (message && message.block)
                    this.emit(message.block.hash, message.block)

            })
            .on('peerheaders', (peer: Peer, message: { headers: BlockHeader[] }) => {
                if (message && message.headers)
                    this.emit('headers', message.headers)
            })
    }

}

const BtcNetwork = new Network()
export default BtcNetwork