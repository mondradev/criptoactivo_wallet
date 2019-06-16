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
        const progress = { last: 0, current: 0 }

        let disconnect = false

        BtcNetwork.once('disconnected', () => disconnect = true)

        timer.on('second', async () => {
            const rate = progress.current - progress.last
            const remaining = BtcNetwork.bestHeight - progress.current

            if (progress.last > 0 && remaining > 0)
                Logger.info(`Status [Height=${progress.current}, Progress=${(progress.current / BtcNetwork.bestHeight * 100).toFixed(2)} % BlockRate=${rate} blk/s, Remaining=${remaining} blks, TimeLeft=${TimeSpan.fromSeconds(remaining / rate)}]`)

            progress.last = progress.current
        })

        try {
            const connected = await BtcNetwork.connect()

            if (!connected)
                throw new Error(`Can't connect to network`)

            Logger.info('Connected to Bitcoin Network')

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
                    let i = 6

                    while (--i) {

                        const headers = await BtcNetwork.getHashes(locators)

                        if (!headers || headers.length == 0)
                            break

                        blockToDownload.push(...headers)
                        locators = headers.slice(headers.length - 30).reverse()
                    }

                    blockToDownload = blockToDownload.unique()

                    if (blockToDownload.length == 0) {
                        timer.stop()
                        await wait(10000)
                        continue
                    }

                    for (const blockhash of blockToDownload) {
                        const block = await BtcNetwork.getBlock(blockhash)

                        if (!block) {
                            Logger.warn(`Fail in Bitcoin#Network#getBlock(${blockhash})`)
                            break
                        }

                        Logger.trace(`Processing block [Hash=${block.hash}]`)

                        height = await Indexers.BlockIndex.import(block)
                        progress.current = height
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

        try { txIndex = await Storages.TxDb.get(txid) }
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