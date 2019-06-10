import { EventEmitter } from "events"
import Config from "../../config"
import BtcNetwork from "./BtcNetwork"
import LoggerFactory from "../../utils/LogginFactory"
import { Networks, Block } from 'bitcore-lib'
import { BtcBlockStore, BtcBlockDb } from "./BtcBlockStore"
import { wait } from "../../utils/Extras"
import TimeCounter from "../../utils/TimeCounter"
import TimeSpan from "../../utils/TimeSpan"
import { BtcTxIndexDb } from "./BtcTxIndexStore"
import BufferEx from "../../utils/BufferEx"
import { BtcAddrIndexStore } from "./BtcAddrIndexStore";


const Logger = LoggerFactory.getLogger('Bitcoin Blockchain')

class Blockchain extends EventEmitter {

    private _synchronizeInitial = false

    public constructor() {
        super()
        Networks.defaultNetwork = Networks.get(Config.assets.bitcoin.network)
    }

    public get synchronized() { return this._synchronizeInitial }

    public async sync() {
        const timer = new TimeCounter()
        const progress = { last: 0, current: 0 }


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

            await BtcAddrIndexStore.loadCache()

            const tip = await BtcBlockStore.getLocalTip()

            Logger.info(`LocalTip [Hash=${tip.hash}, Height=${tip.height}]`)

            while (true) {
                timer.start()

                let { hash, height } = await BtcBlockStore.getLocalTip()

                let locators = await BtcBlockStore.getLastHashes(height)

                if (!locators.includes(hash))
                    throw new Error(`Fail in chain state, can't find last block [Hash=${hash}]`)

                if (BtcNetwork.bestHeight <= height) {
                    this._synchronizeInitial = true
                    Logger.info(`Download finalized [Tip=${hash}, Height=${height}]`)
                    this.emit('synchronized')
                    return
                } else {
                    const headers = await BtcNetwork.getHeaders(locators)

                    if (!headers || headers.length == 0) {
                        timer.stop()
                        await wait(10000)
                        continue
                    }

                    const blockDownloader = BtcNetwork.getDownloader(headers)

                    while (blockDownloader.hasNext()) {
                        const block = await blockDownloader.next()

                        if (!block)
                            break

                        Logger.trace(`Processing block [Hash=${block.hash}]`)

                        height = await BtcBlockStore.import(block)
                        progress.current = height
                    }

                    timer.stop()
                    Logger.debug(`Processed Blocks [Count=${headers.length}, Time=${timer.toLocalTimeString()}]`)
                }

            }
        } catch (ex) {
            await BtcNetwork.disconnect()
            Logger.error(`Error="Fail to download blockchain", Exception=${ex.stack}`)
            return this.sync()
        }

    }

    public async getTxRaw(txid: Buffer) {
        let txIndex = null

        try { txIndex = await BtcTxIndexDb.get(txid) }
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

        try { blockRaw = await BtcBlockDb.get(Buffer.from([txIndexRecord.height])) }
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