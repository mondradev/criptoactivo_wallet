import { EventEmitter } from "events"
import Config from "../../../config"
import LoggerFactory from "../../utils/LogginFactory"
import { Networks, Block } from 'bitcore-lib'
import BufferHelper from "../../utils/BufferHelper"
import '../../utils/ArrayExtension'
import { Storages, Indexers } from "./stores"
import { BtcBlockStore } from "./stores/BtcBlockStore"
import AsyncLock from 'async-lock'

const Logger = LoggerFactory.getLogger('Bitcoin Blockchain')
const Lock = new AsyncLock()
const NULL_HASH = Array(65).join('0')

class Blockchain extends EventEmitter {
    public getLocalTip(): Promise<{ hash: string, height: number, txn: number, time: Date }> {
        return BtcBlockStore.getLocalTip()
    }

    public get orphan() { return Object.entries(this._orphanBlocks).length }

    public async getLocalHeight() {
        return (await BtcBlockStore.getLocalTip()).height
    }

    private _initialized: boolean = false
    public async getLocators(): Promise<{ starts: string[], stop: string }> {
        const { height } = await BtcBlockStore.getLocalTip()
        const locators = await BtcBlockStore.getLastHashes(height)
        return { starts: locators, stop: this._getCheckpoints(height) }
    }

    public async emitAsync(event: string, ...args: any[]) {
        this.emit(event, args)
    }

    private _orphanBlocks = {}
    private _searchNextBlock(hash: string) {
        if (this._orphanBlocks[hash])
            Lock.acquire('addBlock', async () => {
                const block = this._orphanBlocks[hash]
                const blockHash = block.hash

                delete this._orphanBlocks[hash]

                await BtcBlockStore.import(block)

                const height = await this.getLocalHeight()
                let checkpoint = this._getCheckpoints(height - 1)

                if (checkpoint !== NULL_HASH && checkpoint === blockHash)
                    this.emitAsync(blockHash)
                else if (checkpoint === NULL_HASH)
                    height % 10000 == 0 && this.emitAsync(height.toString())

                return block.hash
            }).then(hash => hash && this._searchNextBlock(hash))
    }

    public async addBlock(block: Block) {
        const prevHash = BufferHelper.reverseToHex(block.header.prevHash)

        this._orphanBlocks[prevHash] = block

        if (!Lock.isBusy('addBlock')) {
            const tip = await BtcBlockStore.getLocalTip()
            this._searchNextBlock(tip.hash)
        }
    }

    public constructor() {
        super()
        Networks.defaultNetwork = Networks.get(Config.getAsset('bitcoin').network)
    }

    private _getCheckpoints(height: number) {
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
                return checkpoint.hash

        return Array(65).join('0')
    }

    public async initialize() {
        if (this._initialized)
            return

        this._initialized = true

        let tip = await Indexers.BlockIndex.getLocalTip()

        if (tip.hash === NULL_HASH)
            tip = await BtcBlockStore.createGenesisBlock()

        await Indexers.AddrIndex.loadCache()

        Logger.info(`LocalTip [Hash=${tip.hash}, Height=${tip.height}]`)

        return tip
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

        Logger.debug(`Found tx [TxID=%, Outputs=%, Inputs=%, Coinbase=%, Amount=%`,
            tx.hash,
            tx.outputs.length,
            tx.inputs.length,
            tx.isCoinbase(),
            tx.outputAmount
        )

        return tx.toBuffer()

    }
}


const BtcBlockchain = new Blockchain()
export default BtcBlockchain