import './bitcore-p2p.lib/Peer.fix'

import { Peer, Messages } from 'bitcore-p2p'
import { Network, Networks } from 'bitcore-lib'
import { EventEmitter } from 'events'
import { wait } from '../../utils/Extras'

import LogginFactory from '../../utils/LogginFactory'
import NetworkEvents from './NetworkEvents'
import NetworkStatus from './NetworkStatus'
import Constants from './Constants'
import AsyncLock from 'async-lock'
import dns from 'dns'

const ClassName = "Btc Net Worker"
const Logger = LogginFactory.getLogger(ClassName)
const Lock = new AsyncLock()

const LockKeys = {
    Peer: 'peers'
}

class Pool extends EventEmitter {

    /**
     * Resuelve las IP desde las semillas del DNS.
     */
    private async _discoveringPeers() {
        Logger.info('Discovering peers from DNS')

        for (const seed of this._seeds) {
            const addresses = await this._resolveDns(seed)

            if (this._status == NetworkStatus.Disconnecting)
                return

            for (const node of addresses)
                this._availableNodes.push(node)
        }
    }

    /**
     * Rellena las conexiones del pool del nodo.
     */
    private async _fillConnections() {
        Logger.info('Connecting pool, waiting for peers')

        while (this._status != NetworkStatus.Disconnecting) {
            if (this._maxConnections > await Lock.acquire(LockKeys.Peer, () => this._connectedPeers))
                await this._connectPeer(await Lock.acquire(LockKeys.Peer, () => this._availableNodes.shift()))
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
            if (this._status == NetworkStatus.Disconnecting) {
                resolve()
                return
            }

            await Lock.acquire('connectPeer', async (unlock) => {
                if (this._connectedPeers >= this._maxConnections) {
                    unlock()
                    return
                }

                let peer = new Peer({
                    host: address,
                    port: this._port,
                    message: this._message,
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
                        this._maxConnections,
                        peer.version,
                        peer.subversion,
                        peer.bestHeight,
                        peer.host
                    )

                    this._bestHeight = peer.bestHeight > this._bestHeight ? peer.bestHeight : this._bestHeight

                    if (this._status < NetworkStatus.Connected)
                        this.emit('ready', true)

                    peer.removeAllListeners('ready')

                    if (this._status == NetworkStatus.Disconnecting)
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

            Logger.info(`Peer disconnected ${connectedPeers}/${this._maxConnections} [Host=${peer.host}]`)
        });
    }

    private _eventHandler(event: NetworkEvents, args: any) {
        switch (event) {
            case NetworkEvents.Start:
                this._startAsync()
                break;

            default:
                break;
        }
    }

    private async _startAsync() {
        const timeoutHandler = setTimeout(() => this.emit('ready', false),
            Constants.Timeouts.WaitForConnectNet)

        const resolve = (connected: boolean) => {
            this._status = connected ? NetworkStatus.Connected : NetworkStatus.Disconnected
            if (this._status == NetworkStatus.Connected) {
                clearTimeout(timeoutHandler)

                Logger.info('Connected to Bitcoin Network')
            }
            else
                Logger.warn(`Can't connect to peers`)
        }

        if (!this._status) {
            this._status = NetworkStatus.Connecting

            this.once('ready', resolve)

            await this._discoveringPeers()

            this._fillConnections()
        } else resolve(true)
    }

    private _availableNodes: string[]
    private _peers: Array<Peer>
    private _busyPeers: Array<Peer>
    private _seeds: Array<string>
    private _network: Network
    private _message: Messages
    private _maxConnections: number
    private _port: number
    private _bestHeight: number
    private _status: NetworkStatus

    /**
    * Número de nodos conectados
    */
    private get _connectedPeers(): number { return this._busyPeers.length + this._peers.length }

    public constructor(options: {
        network: Network,
        maxConnections: number,
        seeds: Array<string>
    }) {
        super()

        this._network = options.network
        this._maxConnections = options.maxConnections
        this._port = this._network.port
        this._seeds = options.seeds.length == 0 ? options.seeds : this._network.dnsSeeds
        this._bestHeight = 0
        this._message = new Messages({ network: this._network })
        this._status = NetworkStatus.Disconnected
        this._peers = new Array<Peer>()
        this._busyPeers = new Array<Peer>()
        this._availableNodes = new Array<string>()

        process.on('message', this._eventHandler)

        Networks.defaultNetwork = this._network
    }
}

if (!Networks.get(process.argv[2]))
    throw new Error(`Network is required: ${process.argv[2]}`)

if (!Number.isNaN(parseInt(process.argv[3])))
    throw new Error(`MaxConnections is required: ${process.argv[3]}`)

new Pool({
    network: Networks.get(process.argv[2]),
    maxConnections: parseInt(process.argv[3]),
    seeds: process.argv.slice(4) || []
})