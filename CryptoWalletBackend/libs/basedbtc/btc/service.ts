import { Networks } from "bitcore-lib"
import LoggerFactory from "../../utils/loggin-factory"
import IWalletService from "../wallet-service"
import ConfigService from "../../../config.json"
import Utils from "../../../libs/utils"
import TimeCounter from "../../utils/timecounter"
import { EventEmitter } from "events"
import { PeerToPeer } from "./p2p"
import { BlockStore } from "../blocks"
import TimeSpan from "../../../libs/utils/timespan"
import { TxStore } from "../txs";

const Logger = LoggerFactory.getLogger('Bitcoin Backend')

class Wallet implements IWalletService {

    getRawTransactionsByAddress(address: string): Promise<string[]> {
        throw new Error("Method not implemented.")
    }
    public async getRawTransaction(txid: string): Promise<string> {
        const txidRaw = Buffer.from(txid, 'hex')
        const rawTx = await TxStore.getRawTx(txidRaw)

        return rawTx ? rawTx.toString('hex') : null
    }

    broadcastTrx(transaction: string): Promise<boolean> {
        throw new Error("Method not implemented.")
    }

    private _currentNetwork: string
    private _notifier = new EventEmitter()
    private _synchronized: boolean

    constructor() {
        this._currentNetwork = ConfigService.networks['bitcoin']
        this._enableNetwork(this._currentNetwork)
    }


    /**
   * Activa el tipo de red.
   * 
   * @param networkName Nombre de la red de bitcoin.
   * 
   */
    private _enableNetwork(networkName: ('mainnet' | 'testnet' | 'regtest' | string)): void {
        switch (networkName) {
            case 'mainnet':
                Networks.defaultNetwork = Networks.mainnet
                Networks.disableRegtest()
                break

            case 'testnet':
                Networks.defaultNetwork = Networks.testnet
                Networks.disableRegtest()
                break

            case 'regtest':
                Networks.defaultNetwork = Networks.testnet
                Networks.enableRegtest()
                break
        }
    }

    public async sync() {
        const timer = new TimeCounter()
        let i = 0
        let last = 0


        timer.on('second', async () => {
            const blockRate = i - last
            const blockleft = PeerToPeer.bestHeight - i

            if (blockRate > 0 && i > 0 && !this._synchronized) {
                Logger.info(`BlockRate=${blockRate} blk/s, Remaining=${blockleft} blks, TimeLeft=${TimeSpan.FromSeconds(blockleft / blockRate)}`)
                last = i
            }
        })

        await PeerToPeer.connect()

        Logger.info('Initializing blockchain download')
        
        try {
            while (true) {
                let { hash, height } = await BlockStore.getLocalTip()
                Logger.info(`LocalTip [Hash=${hash}, Height=${height}]`)

                let headers: string[]
                let hashes = await BlockStore.getLastHashes(height)

                if (!hashes.includes(hash))
                    throw new Error(`Fail in chain state, can't find last block [Hash=${hash}]`)

                timer.start()

                while (true) {
                    if (PeerToPeer.bestHeight <= height) {
                        if (!this._synchronized) {
                            this._synchronized = true
                            this._notifier.emit('downloaded')
                            Logger.info(`Download finalized [Tip=${hash}, Height=${height}]`)
                        }

                        headers = await PeerToPeer.getHeaders([hash])

                        if (headers)
                            if (headers.length == 0)
                                await Utils.wait(10000)
                            else
                                break
                    } else {
                        headers = await PeerToPeer.getHeaders(hashes)

                        if (headers && headers.length == 0)
                            await Utils.wait(10000)
                        else if (headers)
                            break
                    }
                }

                const blockRequest = PeerToPeer.getRequestBlocks(headers)
                let block = null

                do {
                    block = await blockRequest.next()

                    if (block) {
                        height++
                        Logger.trace(`Received block [Hash=${block.hash}, Height=${height}]`)

                        await BlockStore.import(block)
                        i = height
                    }

                } while (block != null)

                timer.stop()

                Logger.debug(`ProcessedBlocks=${headers.length}, Time=${timer.toLocalTimeString()}`)

            }

        } catch (ex) {
            PeerToPeer.disconnect()
            Logger.error(`Error="Fail to download blockchain", Exception=${ex.stack}`)
            return this.sync()
        }
    }

    public onDownloaded(fnCallback: () => void) {
        this._notifier.on('downloaded', fnCallback)
    }

}

export const BtcWallet = new Wallet()