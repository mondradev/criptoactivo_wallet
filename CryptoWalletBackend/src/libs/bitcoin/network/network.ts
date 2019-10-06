import { Block, BlockHeader, Transaction } from "bitcore-lib"
import { fork, ChildProcess } from 'child_process'
import { EventEmitter } from "events"
import { Peer } from "bitcore-p2p"

import NetworkEvents from "./networkevents"
import NetworkStatus from "./networkstatus"
import PoolMethods from "./poolmethods"
import LoggerFactory from 'log4js'

import '../fixes/bitcore-p2p.peer.lib'

export type AddrListener = (peer: Peer, addresses: string[]) => void
export type BlockListener = (peer: Peer, block: Block) => void
export type HeadersListener = (peer: Peer, headers: BlockHeader[]) => void
export type TxListener = (peer: Peer, tx: Transaction) => void
export type ReadyListener = (connected: Boolean) => void
export type DisconnectedListener = () => void
export type ErrorListener = (error: Error) => void
export type SyncListener = (tip: { height: number, block: Block }) => void
export type StartListener = () => void

const CHILD_MODULE = 'pool.js'

const Logger = LoggerFactory.getLogger('Bitcoin Network')

export class Network {

    private _notifer: EventEmitter
    private _child: ChildProcess
    private _chain: Blockchain
    private _status: NetworkStatus
    private _bestHeight: number

    public constructor(chain: Blockchain) {
        this._notifer = new EventEmitter()
        this._chain = chain

        this._init()
    }

    private _init() {
        if (this._child)
            return

        this._child = fork(CHILD_MODULE, [])

        process.on('message', this._eventHandlers)
    }

    private _eventHandlers(message: any, sendHandler: any) {

    }

    private _sendMessage(method: PoolMethods, message?: Buffer) {
        let body = method.toString(16)

        if (message)
            body += message.toString('hex')

        this._child.send(body, (error) => {
            if (!error)
                return

            this._child.kill()

            if (this._child.connected)
                this._child.disconnect()

            this._child = null
            this._child = fork(CHILD_MODULE, [])
        })
    }

    public async startSync() {
        if (this._status == NetworkStatus.SYNC) {
            Logger.warn('Initial block download was started')
            return
        }

        if (this._status < NetworkStatus.CONNECTED) {
            Logger.warn('Requires connecting to some nodes to start the synchronization')
            return
        }

        this._status = NetworkStatus.SYNC

        let tip = this._chain.getTip()

        if (this._bestHeight == tip.height) {
            Logger.info('Chain is full downloaded: Height=%d', tip.height)
            return
        }

        Logger.info('Chain requires be synchronize: Height=%d Pending=%d', tip.height, this._bestHeight - tip.height)

        let locator = this._chain.getLocators(tip)

        this.requestHeaders(locator.starts, locator.stop)
    }

    public async connect() {
        if (this._status > NetworkStatus.CONNECTING) {
            Logger.warn("The network isn't disconnected")
            return
        }

        if (this._status == NetworkStatus.CONNECTING) {
            Logger.warn("The network is trying to connect")
            return
        }

        this._status = NetworkStatus.CONNECTING
        this._sendMessage(PoolMethods.CONNECT)
    }

    public async disconnect() {
        if (this._status < NetworkStatus.DISCONNECTING) {
            Logger.warn("The network isn't connected")
            return
        }

        if (this._status == NetworkStatus.DISCONNECTING) {
            Logger.warn("The network is trying to disconnect")
            return
        }

        this._status = NetworkStatus.DISCONNECTING
        this._sendMessage(PoolMethods.DISCONNECT)
    }

    public requestBlock(blockHash: string) {

    }

    public requestBlocks(blockHashes: string[]) {

    }

    public requestHeaders(startHash: string, stopHash?: string) {

    }

    public requestTx(txid: string) {

    }

    public on(event: NetworkEvents, listener: (AddrListener | BlockListener | HeadersListener
        | TxListener | ReadyListener | DisconnectedListener | ErrorListener | SyncListener
        | StartListener)) {
        this._notifer.on(event, listener)
    }

    public once(event: NetworkEvents, listener: (AddrListener | BlockListener | HeadersListener
        | TxListener | ReadyListener | DisconnectedListener | ErrorListener | SyncListener
        | StartListener)) {
        this._notifer.once(event, listener)
    }

    public removeListener(event: NetworkEvents, listener: (AddrListener | BlockListener | HeadersListener
        | TxListener | ReadyListener | DisconnectedListener | ErrorListener | SyncListener
        | StartListener)) {
        this._notifer.removeListener(event, listener)
    }

    public removeAllListeners(event: NetworkEvents) {
        this._notifer.removeAllListeners(event)
    }

}