import { EventEmitter } from "events"
import Config from "../../../config"
import BtcNetwork from "./BtcNetwork"
import LoggerFactory from "../../utils/LogginFactory"
import { Networks, Block } from 'bitcore-lib'
import { wait } from "../../utils/Extras"
import TimeCounter from "../../utils/TimeCounter"
import BufferHelper from "../../utils/BufferHelper"
import '../../utils/ArrayExtension'
import { Storages, Indexers } from "./stores";
import { BtcBlockStore } from "./stores/BtcBlockStore";

const Logger = LoggerFactory.getLogger('Bitcoin Blockchain')

class Blockchain extends EventEmitter {

    public addBlock(block: Block) {
        // TODO Procesar y agregar los bloques a la cadena de bloques
    }

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

            await this._requestBlocks(tip.height);

            // while (true) {
            //     if (disconnect)
            //         break

            //     timer.start()

            //     let { hash, height } = await Indexers.BlockIndex.getLocalTip()

            //     let locators = await Indexers.BlockIndex.getLastHashes(height)

            //     if (BtcNetwork.bestHeight <= height) {
            //         this._synchronizeInitial = true
            //         Logger.info(`Download finalized [Tip=${hash}, Height=${height}]`)
            //         this.emit('synchronized')
            //         return
            //     } else {
            //         let blockToDownload = []
            //         let i = 5

            //         while (i) {

            //             const headers = await BtcNetwork.getHashes(locators)

            //             if (!headers || headers.length == 0)
            //                 break

            //             if ([].concat(headers, locators).unique().length < locators.length + headers.length)
            //                 continue

            //             Logger.trace(`Received ${headers.length} headers`)

            //             blockToDownload.push(...headers)
            //             locators = headers.slice(headers.length - 30).reverse().concat(locators)

            //             i--
            //         }

            //         blockToDownload = blockToDownload.unique()

            //         if (blockToDownload.length == 0) {
            //             timer.stop()
            //             await wait(10000)
            //             continue
            //         }

            //         const blockDownloader = BtcNetwork.getDownloader(blockToDownload)

            //         try {
            //             Storages.BlockDb.isClosed() && await Storages.BlockDb.open()
            //             Storages.TxIdxDb.isClosed() && await Storages.TxIdxDb.open()
            //             Storages.AddrIdxDb.isClosed() && await Storages.AddrIdxDb.open()

            //             for (let i = 0, blockHash = blockToDownload[i]; blockDownloader.hasNext(); i++ , blockHash = blockToDownload[i]) {
            //                 const timer = TimeCounter.begin()
            //                 const block = await blockDownloader.get(blockHash)

            //                 if (!block) {
            //                     Logger.warn(`Fail in Bitcoin.Network#getBlock(${blockHash})`)
            //                     break
            //                 }

            //                 Logger.trace(`Processing block [Hash=${block.hash}]`)

            //                 const [height, txn] = await Indexers.BlockIndex.import(block)

            //                 timer.stop()

            //                 Logger.info(`UpdateTip [Height=${height}, Hash=${block.hash}, Txn=${txn}, Progress=${(height / BtcNetwork.bestHeight * 100).toFixed(2)}% MemUsage=${(process.memoryUsage().rss / 1048576).toFixed(2)} MB, Time=${timer.toLocalTimeString()}]`)
            //             }
            //         } finally {
            //             Storages.BlockDb.isOpen() && await Storages.BlockDb.close()
            //             Storages.TxIdxDb.isOpen() && await Storages.TxIdxDb.close()
            //             Storages.AddrIdxDb.isOpen() && await Storages.AddrIdxDb.close()
            //         }

            //         timer.stop()
            //         Logger.debug(`Processed Blocks [Count=${blockToDownload.length}, Time=${timer.toLocalTimeString()}]`)
            //     }

            // }
        } catch (ex) {
            Logger.error(`Error="Fail to download blockchain", Exception=${ex.stack}`)
            BtcNetwork.removeAllListeners('disconnected')
            await BtcNetwork.disconnect()
            return this.sync()
        } finally {
            timer && timer.stop()
        }

    }

    private async _requestBlocks(height: number) {
        const locators = await BtcBlockStore.getLastHashes(height);
        const { starts, stop } = this._getCheckpoints(locators, height);

        BtcNetwork.sendGetHeaders(starts, stop);
    }

    private _getCheckpoints(locators: Array<string>, height: number) {
        const checkpointData = [
            { height: 11111, hash: "0000000069e244f73d78e8fd29ba2fd2ed618bd6fa2ee92559f542fdb26e7c1d" },
            { height: 33333, hash: "000000002dd5588a74784eaa7ab0507a18ad16a236e7b1ce69f00d7ddfb5d0a6" },
            { height: 74000, hash: "0000000000573993a3c9e41ce34471c079dcf5f52a0e824a81e7f953b8661a20" },
            { height: 105000, hash: "00000000000291ce28027faea320c8d2b054b2e0fe44a773f3eefb151d6bdc97" },
            { height: 134444, hash: "00000000000005b12ffd4cd315cd34ffd4a594f430ac814c91184a0d42d2b0fe" },
            { height: 168000, hash: "000000000000099e61ea72015e79632f216fe6cb33d7899acb35b75c8303b763" },
            { height: 193000, hash: "000000000000059f452a5f7340de6682a977387c17010ff6e6c3bd83ca8b1317" },
            { height: 210000, hash: "000000000000048b95347e83192f69cf0366076336c639f9b7228e9ba171342e" },
            { height: 216116, hash: "00000000000001b4f4b433e81ee46494af945cf96014816a4e2370f11b23df4e" },
            { height: 225430, hash: "00000000000001c108384350f74090433e7fcf79a606b8e797f065b130575932" },
            { height: 250000, hash: "000000000000003887df1f29024b06fc2200b55f8af8f35453d7be294df2d214" },
            { height: 279000, hash: "0000000000000001ae8c72a0b0c301f67e3afca10e819efa9041e458e9bd7e40" },
            { height: 295000, hash: "00000000000000004d9b4ef50f0f9d686fd69db2e03af35a100370c64632a983" },
        ]

        for (const checkpoint of checkpointData)
            if (height < checkpoint.height)
                return { starts: locators, stop: checkpoint.hash }

        return { starts: locators, stop: Array(65).join('0') }
    }

    public async getTxRaw(txid: Buffer) {
        let txIndex = null

        try { txIndex = await Storages.TxIdxDb.get(txid) }
        catch (ex) { return null }

        if (!txIndex)
            return null

        const rawData = BufferHelper.fromHex(txIndex)

        const txIndexRecord = {
            hash: BufferHelper.read(rawData, 0, 32),
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