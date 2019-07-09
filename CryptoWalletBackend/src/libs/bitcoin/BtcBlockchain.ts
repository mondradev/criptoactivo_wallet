import { EventEmitter } from "events"
import Config from "../../../config"
import BtcNetwork from "./BtcNetwork"
import LoggerFactory from "../../utils/LogginFactory"
import { Networks, Block } from 'bitcore-lib'
import { wait } from "../../utils/Extras"
import TimeCounter from "../../utils/TimeCounter"
import BufferEx from "../../utils/BufferEx"
import '../../utils/ArrayExtension'
import { Storages, Indexers } from "./stores";
import { BtcBlockStore } from "./stores/BtcBlockStore";

const Logger = LoggerFactory.getLogger('Bitcoin Blockchain')

class Blockchain extends EventEmitter {

    private _synchronizeInitial = false

    public constructor() {
        super()
        Networks.defaultNetwork = Networks.get(Config.getAsset('bitcoin').network)
    }

    public get synchronized() { return this._synchronizeInitial }

    public async sync() {
        const timer = new TimeCounter()
        let disconnect = false

        BtcNetwork.once('disconnected', () => disconnect = true)

        try {
            const connected = await BtcNetwork.connect()

            if (!connected)
                throw new Error(`Can't connect to network`)

            Logger.debug('Connected to Bitcoin Network')

            let tip = await Indexers.BlockIndex.getLocalTip()

            if (tip.hash === Array(65).join('0')) {
                await BtcBlockStore.createGenesisBlock()
                tip = await Indexers.BlockIndex.getLocalTip()
            }

            await Indexers.AddrIndex.loadCache()

            Logger.info(`LocalTip [Hash=${tip.hash}, Height=${tip.height}]`)

            while (true) {
                if (disconnect)
                    break

                timer.start()

                let { hash, height } = await Indexers.BlockIndex.getLocalTip()

                let locators = await Indexers.BlockIndex.getLastHashes(height)

                if (BtcNetwork.bestHeight <= height) {
                    this._synchronizeInitial = true
                    Logger.info(`Download finalized [Tip=${hash}, Height=${height}]`)
                    this.emit('synchronized')
                    return
                } else {
                    let blockToDownload = []
                    let i = 5

                    while (i) {

                        const headers = await BtcNetwork.getHashes(locators)

                        if (!headers || headers.length == 0)
                            break

                        if ([].concat(headers, locators).unique().length < locators.length + headers.length)
                            continue

                        Logger.trace(`Received ${headers.length} headers`)

                        blockToDownload.push(...headers)
                        locators = headers.slice(headers.length - 30).reverse().concat(locators)

                        i--
                    }

                    blockToDownload = blockToDownload.unique()

                    if (blockToDownload.length == 0) {
                        timer.stop()
                        await wait(10000)
                        continue
                    }

                    const blockDownloader = BtcNetwork.getDownloader(blockToDownload)

                    try {
                        Storages.BlockDb.isClosed() && await Storages.BlockDb.open()
                        Storages.TxIdxDb.isClosed() && await Storages.TxIdxDb.open()
                        Storages.AddrIdxDb.isClosed() && await Storages.AddrIdxDb.open()

                        for (let i = 0, blockHash = blockToDownload[i]; blockDownloader.hasNext(); i++ , blockHash = blockToDownload[i]) {
                            const timer = TimeCounter.begin()
                            const block = await blockDownloader.get(blockHash)

                            if (!block) {
                                Logger.warn(`Fail in Bitcoin.Network#getBlock(${blockHash})`)
                                break
                            }

                            Logger.trace(`Processing block [Hash=${block.hash}]`)

                            const [height, txn] = await Indexers.BlockIndex.import(block)

                            timer.stop()

                            Logger.info(`UpdateTip [Height=${height}, Hash=${block.hash}, Txn=${txn}, Progress=${(height / BtcNetwork.bestHeight * 100).toFixed(2)}% MemUsage=${(process.memoryUsage().rss / 1048576).toFixed(2)} MB, Time=${timer.toLocalTimeString()}]`)
                        }
                    } finally {
                        Storages.BlockDb.isOpen() && await Storages.BlockDb.close()
                        Storages.TxIdxDb.isOpen() && await Storages.TxIdxDb.close()
                        Storages.AddrIdxDb.isOpen() && await Storages.AddrIdxDb.close()
                    }

                    timer.stop()
                    Logger.debug(`Processed Blocks [Count=${blockToDownload.length}, Time=${timer.toLocalTimeString()}]`)
                }

            }
        } catch (ex) {
            Logger.error(`Error="Fail to download blockchain", Exception=${ex.stack}`)
            BtcNetwork.removeAllListeners('disconnected')
            return this.sync()
        } finally {
            timer && timer.stop()
        }

    }

    public async getTxRaw(txid: Buffer) {
        let txIndex = null

        try { txIndex = await Storages.TxIdxDb.get(txid) }
        catch (ex) { return null }

        if (!txIndex)
            return null

        const rawData = BufferEx.fromHex(txIndex)

        const txIndexRecord = {
            hash: rawData.read(0, 32),
            index: rawData.readUInt32LE(32),
            height: rawData.readUInt32LE(36)
        }

        let blockRaw = null

        try { blockRaw = await Storages.BlockDb.get(Buffer.from([txIndexRecord.height])) }
        catch (ex) { return null }

        if (!blockRaw)
            return null

        const block = Block.fromBuffer(blockRaw)

        if (block.transactions.length <= txIndexRecord.index)
            return null

        const tx = block.transactions[txIndexRecord.index]

        Logger.debug(`Found tx [TxID=${tx.hash}, Outputs=${tx.outputs.length}, Inputs=${tx.inputs.length}, Coinbase=${tx.isCoinbase()}, Amount=${tx.outputAmount}]`)

        return tx.toBuffer()

    }
}


const BtcBlockchain = new Blockchain()
export default BtcBlockchain