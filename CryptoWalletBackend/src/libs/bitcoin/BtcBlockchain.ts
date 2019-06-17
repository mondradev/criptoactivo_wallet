import { EventEmitter } from "events"
import Config from "../../../config"
import BtcNetwork from "./BtcNetwork"
import LoggerFactory from "../../utils/LogginFactory"
import { Networks, Block } from 'bitcore-lib'
import { wait } from "../../utils/Extras"
import TimeCounter from "../../utils/TimeCounter"
import TimeSpan from "../../utils/TimeSpan"
import BufferEx from "../../utils/BufferEx"
import '../../utils/ArrayExtension'
import { Storages, Indexers } from "./stores";
import { BtcBlockStore } from "./stores/BtcBlockStore";
import { BtcTxIndexStore } from "./stores/BtcTxIndexStore";


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
        const info = { last: 0, current: 0 }

        let disconnect = false
        let receivedHeaders = false

        BtcNetwork.once('disconnected', () => disconnect = true)

        timer.on('second', async () => {
            const rate = info.current - info.last
            const remaining = BtcNetwork.bestHeight - info.current
            const progress = (info.current / BtcNetwork.bestHeight * 100).toFixed(2)
            const time = TimeSpan.fromSeconds(remaining / rate)

            if (info.last > 0 && remaining > 0 && !receivedHeaders)
                Logger.info(`Status [Height=${info.current}, Progress=${progress} % BlockRate=${rate} blk/s, Remaining=${remaining} blks, TimeLeft=${time}]`)

            info.last = info.current
        })

        try {
            const connected = await BtcNetwork.connect()

            if (!connected)
                throw new Error(`Can't connect to network`)

            Logger.debug('Connected to Bitcoin Network')

            await Indexers.AddrIndex.loadCache()

            const tip = await Indexers.BlockIndex.getLocalTip()

            Logger.info(`LocalTip [Hash=${tip.hash}, Height=${tip.height}]`)

            while (true) {
                if (disconnect)
                    break

                timer.start()

                let { hash, height } = await Indexers.BlockIndex.getLocalTip()

                let locators = await Indexers.BlockIndex.getLastHashes(height)

                if (!locators.includes(hash))
                    throw new Error(`Fail in chain state, can't find last block [Hash=${hash}]`)

                if (BtcNetwork.bestHeight <= height) {
                    this._synchronizeInitial = true
                    Logger.info(`Download finalized [Tip=${hash}, Height=${height}]`)
                    this.emit('synchronized')
                    return
                } else {
                    let blockToDownload = []
                    let i = 5

                    receivedHeaders = true

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

                    receivedHeaders = false
                    blockToDownload = blockToDownload.unique()

                    if (blockToDownload.length == 0) {
                        timer.stop()
                        await wait(10000)
                        continue
                    }

                    const blockDownloader = BtcNetwork.getDownloader(blockToDownload)

                    for (let i = 0, blockHash = blockToDownload[i]; blockDownloader.hasNext(); i++ , blockHash = blockToDownload[i]) {
                        const block = await blockDownloader.get(blockHash)

                        if (!block) {
                            Logger.warn(`Fail in Bitcoin.Network#getBlock(${blockHash})`)
                            break
                        }

                        Logger.trace(`Processing block [Hash=${block.hash}]`)

                        height = await Indexers.BlockIndex.import(block)
                        info.current = height
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

        try { txIndex = await BtcTxIndexStore.getTxIndex(txid) }
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

        try { blockRaw = await BtcBlockStore.getBlock(Buffer.from([txIndexRecord.height])) }
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