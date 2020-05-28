import { Networks, Block, Transaction, Address, PublicKey, Script, BlockHeader } from 'bitcore-lib'
import { IBlockStore, ChainTip } from '../istore'
import { MongoClient, Collection, Db, FilterQuery, UpdateQuery } from 'mongodb'

import fs from 'fs'
import LoggerFactory from 'log4js'
import Config from '../../../../../config'
import TimeCounter from '../../../../utils/timecounter'
import BufferHelper from '../../../../utils/bufferhelper'
import { BlockHeaderSchema, TxOutSchema as TxoSchema, TxInSchema as TxiSchema, TxSchema } from './schemas'
import Constants from '../../constants'
import { coalesce } from '../../../../utils'
import { BitcoinConfig } from '../../consensus'

type MongoConfig = {
    host: string
    port: number
    dbName: string
    sslCA: string
}

const TXO_CACHE_SIZE = 100000

const MongoDbConfig: MongoConfig = Config.dbConfig
const Logger = LoggerFactory.getLogger("(Bitcoin) Blockstore")

type BulkWriteUpdate<T> = {
    updateOne: {
        filter: FilterQuery<T>,
        update: UpdateQuery<T>,
        upsert?: boolean
    }
}

Logger.level = Config.logLevel

Networks.defaultNetwork = Networks.get(BitcoinConfig.network)

type TxDataMongo = { txid: string, time: number, blockHeight: number, blockHash: string, network: string, outputs: TxoDataMongo[], inputs: TxiDataMongo[] }
type TxoDataMongo = { address: string, index: number, value: number, scriptPubKey: string }
type TxiDataMongo = { address: string, index: number, value: number, scriptSig: string, sequenceNo: number }

export class MongoBlockStore implements IBlockStore {

    _txindex: Collection<TxSchema>

    public async getTxByAddress(address: string): Promise<any[]> {
        const network = BitcoinConfig.network

        const received = (await this._txos.aggregate([
            {
                $match: {
                    network,
                    parentTxid: { $in: this._txos.find({ address, network }).map((t) => t.parentTxid) },
                    parentIndex: 0
                }
            },
            {
                $lookup: {
                    from: 'blocks',
                    let: { block: '$blockHash' },
                    pipeline: [
                        { $match: { $expr: { $eq: ["$hash", '$$block'] } } },
                        { $project: { time: 1, _id: 0 } }
                    ],
                    as: 'blockData'
                }
            },
            {
                $lookup: {
                    from: 'txos',
                    let: { txid: '$parentTxid' },
                    pipeline: [
                        {
                            $match: { $expr: { $eq: ["$parentTxid", "$$txid"] } }
                        },
                        {
                            $project: {
                                _id: 0,
                                index: '$parentIndex',
                                address: 1,
                                scriptPubKey: 1,
                                value: 1
                            }
                        }
                    ],
                    as: 'outputs'
                }
            },
            {
                $lookup: {
                    from: 'txos',
                    let: { txid: '$parentTxid' },
                    pipeline: [
                        {
                            $match: {
                                $expr: {
                                    $eq: ["$spentTxid", "$$txid"]
                                }
                            }
                        },
                        {
                            $project: {
                                _id: 0,
                                index: '$spentIndex',
                                address: 1,
                                scriptSig: 1,
                                value: 1,
                                sequenceNo: 1
                            }
                        }
                    ],
                    as: 'inputs'
                }
            },
            {
                $project: {
                    _id: 0,
                    network: 1,
                    txid: '$parentTxid',
                    blockHash: 1,
                    blockHeight: 1,
                    outputs: 1,
                    inputs: 1,
                    time: { $arrayElemAt: ['$blockData.time', 0] }
                }
            },
            { $sort: { 'outputs.index': 1 } },
            { $sort: { 'inputs.index': 1 } }
        ]).toArray()).map(t => {
            const tx: TxDataMongo = t as any
            return {

            } as any
        })

        const sent = (await this._txos.aggregate([
            {
                $match: {
                    network,
                    parentTxid: { $in: this._txos.find({ address, network, spentTxid: { $exists: true } }).map((t) => t.spentTxid) },
                    parentIndex: 0
                }
            },
            {
                $lookup: {
                    from: 'blocks',
                    let: { block: '$blockHash' },
                    pipeline: [
                        { $match: { $expr: { $eq: ["$hash", '$$block'] } } },
                        { $project: { time: 1, _id: 0 } }
                    ],
                    as: 'blockData'
                }
            },
            {
                $lookup: {
                    from: 'txos',
                    let: { txid: '$parentTxid' },
                    pipeline: [
                        {
                            $match: { $expr: { $eq: ["$parentTxid", "$$txid"] } }
                        },
                        {
                            $project: {
                                _id: 0,
                                index: '$parentIndex',
                                address: 1,
                                scriptPubKey: 1,
                                value: 1
                            }
                        }
                    ],
                    as: 'outputs'
                }
            },
            {
                $lookup: {
                    from: 'txos',
                    let: { txid: '$parentTxid' },
                    pipeline: [
                        {
                            $match: {
                                $expr: {
                                    $eq: ["$spentTxid", "$$txid"]
                                }
                            }
                        },
                        {
                            $project: {
                                _id: 0,
                                index: '$spentIndex',
                                address: 1,
                                scriptSig: 1,
                                value: 1,
                                sequenceNo: 1
                            }
                        }
                    ],
                    as: 'inputs'
                }
            },
            {
                $project: {
                    _id: 0,
                    network: 1,
                    txid: '$parentTxid',
                    blockHash: 1,
                    blockHeight: 1,
                    outputs: 1,
                    inputs: 1,
                    time: { $arrayElemAt: ['$blockData.time', 0] }
                }
            },
            { $sort: { 'outputs.index': 1 } },
            { $sort: { 'inputs.index': 1 } }
        ]).toArray()).map(t => {
            const tx: TxDataMongo = t as any
            return {

            } as any
        })

        return [...received, ...sent]
    }

    private _cacheHeaderchainTip: ChainTip
    private _cacheBlockchainTip: ChainTip
    private _controller: MongoClient
    // private 
    _blocks: Collection<BlockHeaderSchema>
    private _txos: Collection<TxoSchema>
    private _db: Db
    private _connected: boolean
    private _cacheTxout: {
        [outpoint: string]: TxoSchema
    }

    private static _scriptFnAddress = {
        'Pay to public key': (script: Script) => new Address(new PublicKey(script.getPublicKey()), Networks.defaultNetwork).toString(),
        'Pay to public key hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toString(),
        'Pay to script hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toString(),
        'Pay to multisig': () => '(Multisig)',
        'Data push': () => '(Data only)',
        'Unknown': () => '(Nonstandard)'
    }


    private async _saveTxos(transactions: Transaction[], blockMeta: BlockHeaderSchema): Promise<boolean> {
        const network = BitcoinConfig.network
        const timer = TimeCounter.begin()

        try {
            const txiPending = new Array<TxiSchema>()
            const txOut: {
                [outpoint: string]: TxoSchema
            } = {}

            for (const tx of transactions) {
                for (const [index, txo] of tx.outputs.entries()) {
                    let address = null

                    if (txo.script)
                        address = coalesce(MongoBlockStore._scriptFnAddress[txo.script.classify().toString()], () => '(Nonstandard)')(txo.script)

                    const out: TxoSchema = {
                        network,
                        blockHash: blockMeta.hash,
                        blockHeight: blockMeta.height,
                        parentTxid: tx.hash,
                        parentIndex: index,
                        scriptPubKey: txo.script ? txo.script.toHex() : null,
                        value: txo.satoshis,
                        address
                    }

                    txOut[out.parentTxid + out.parentIndex.toHex(4)] = out
                }

                if (!tx.isCoinbase())
                    for (const [index, txi] of tx.inputs.entries()) {
                        let outpoint = txi.prevTxId.toHex() + txi.outputIndex.toHex(4)
                        let txout = this._cacheTxout[outpoint]

                        if (txout == null)
                            txout = txOut[outpoint]

                        if (txout != null) {
                            txout.spentBlockHash = blockMeta.hash
                            txout.spentBlockHeight = blockMeta.height
                            txout.spentTxid = tx.hash
                            txout.spentIndex = index
                            txout.scriptSig = txi.script.toHex()
                            txout.sequenceNo = txi.sequenceNumber

                            txOut[outpoint] = txout

                            delete this._cacheTxout[outpoint]
                        }
                        else
                            txiPending.push({
                                parentTxid: txi.prevTxId.toHex(),
                                parentIndex: txi.outputIndex,
                                spentBlockHash: blockMeta.hash,
                                spentBlockHeight: blockMeta.height,
                                spentTxid: tx.hash,
                                spentIndex: index,
                                scriptSig: txi.script.toHex(),
                                sequenceNo: txi.sequenceNumber,
                                network
                            })

                    }
            }

            let query: BulkWriteUpdate<TxoSchema>[] = txiPending.map(txi => {
                return {
                    updateOne: {
                        filter: {
                            parentTxid: txi.parentTxid,
                            parentIndex: txi.parentIndex,
                            network: txi.network
                        },
                        update: {
                            $set: txi
                        }
                    }
                }
            })

            const records: BulkWriteUpdate<TxoSchema>[] = Object.values(txOut).map(txout => {
                return {
                    updateOne: {
                        filter: {
                            parentTxid: txout.parentTxid,
                            parentIndex: txout.parentIndex,
                            network
                        },
                        update: {
                            $set: txout
                        },
                        upsert: true
                    }
                }
            })

            records.push(...query)

            const response = await this._txos.bulkWrite(records)
            const completed = (() => {
                const modified = records.length - response.upsertedCount
                return modified == 0 ? true : modified == response.matchedCount
            })()

            if (completed)
                Object.entries(txOut).filter(([_, txout]) => txout.spentTxid == null).forEach(([outpoint, txout]) => this._cacheTxout[outpoint] = txout)
            else {
                Logger.error("Fail to save txos %d from block %s, total %d", records.length - response.upsertedCount - response.matchedCount,
                    blockMeta.hash, records.length)

                process.exit(-2)
            }

            if (Object.keys(this._cacheTxout).length > (TXO_CACHE_SIZE * 2))
                Object.keys(this._cacheTxout).slice(0, TXO_CACHE_SIZE).forEach((key) => delete this._cacheTxout[key])

            return completed
        }
        finally {
            timer.stop()
            const logger = timer.milliseconds >= 1000 ? Logger.warn.bind(Logger) : Logger.trace.bind(Logger)
            logger("Mongo#importTxout in %s", timer.toLocalTimeString())
        }
    }

    private _validateConnection() {
        if (!this._connected)
            throw new Error("Require connect to MongoBlockStore")
    }

    public constructor() {
        this._db = null
        this._connected = false
        this._cacheTxout = {}
        this._cacheHeaderchainTip = null
        this._cacheBlockchainTip = null
    }
    getUnspentCoins(txid: Buffer): Promise<{ index: number; utxo: import("bitcore-lib").Output }[]> {
        throw new Error("Method not implemented.")
    }
    AddrIndex: import("../leveldb/addrindex").AddrIndex
    TxIndex: import("../leveldb/txindex").TxIndex
    disconnect(): Promise<void> {
        throw new Error("Method not implemented.")
    }
    getBlock(hash: Buffer): Promise<Block> {
        throw new Error("Method not implemented.")
    }
    getHash(height: number): Promise<Buffer> {
        throw new Error("Method not implemented.")
    }
    getHeight(hash: Buffer): Promise<number> {
        throw new Error("Method not implemented.")
    }
    getLocalTip(): Promise<ChainTip> {
        throw new Error("Method not implemented.")
    }

    public async connect() {
        const timer = TimeCounter.begin()
        const network = BitcoinConfig.network

        try {
            const connectionString = `mongodb://${MongoDbConfig.host}:${MongoDbConfig.port}/${MongoDbConfig.dbName}`

            Logger.info("Connecting to Mongodb Store %s", connectionString)

            this._controller = await MongoClient.connect(connectionString, {
                ssl: MongoDbConfig.sslCA != null,
                sslValidate: MongoDbConfig.sslCA != null,
                sslCA: MongoDbConfig.sslCA != null ? [fs.readFileSync(MongoDbConfig.sslCA)] : null,
                useNewUrlParser: true,
                noDelay: true,
                socketTimeoutMS: 0,
                poolSize: 100,
                useUnifiedTopology: true
            })

            this._db = this._controller.db(MongoDbConfig.dbName)
            this._blocks = this._db.collection('blocks')
            this._txos = this._db.collection('txos')
            this._txindex = this._db.collection('txindex')

            await this._blocks.createIndex({ hash: 1, network: 1 }, { background: true })
            await this._blocks.createIndex({ prevBlock: 1, network: 1 }, { background: true })
            await this._blocks.createIndex({ height: 1, network: 1 }, { background: true })

            await this._txos.createIndex({ parentTxid: 1, network: 1 }, { background: true })
            await this._txos.createIndex({ parentTxid: 1, parentIndex: 1, network: 1 }, { background: true })
            await this._txos.createIndex({ blockHash: 1, network: 1 }, { background: true })
            await this._txos.createIndex({ blockHeight: 1, network: 1 }, { background: true })
            await this._txos.createIndex({ spentTxid: 1, spentIndex: 1, network: 1 }, { background: true })
            await this._txos.createIndex({ spentBlockHash: 1, network: 1 }, { background: true })
            await this._txos.createIndex({ spentBlockHeight: 1, network: 1 }, { background: true })
            await this._txos.createIndex({ address: 1, network: 1 }, { background: true })

            await this._txindex.createIndex({ blockHeight: 1, network: 1 }, { background: true })

            this._connected = true

            const height = (await this.getBlockChainTip()).height

            Logger.debug("Cleaning trash data from utxos, current height: %d", height)

            // Borrado de las TXOs sin bloque padre
            await this._txos.deleteMany({ blockHeight: { $gt: height }, network })
            await this._txos.updateMany({ spentBlockHeight: { $gt: height }, network }, {
                $set: {
                    spentBlockHash: null, spentBlockHeight: null, spentIndex: null,
                    spentTxid: null, sequenceNo: null, scriptSig: null
                }
            })

            Logger.debug("Loading utxos cache")

            const cacheTxos = await this._txos.find({ $and: [{ network: { $eq: network } }, { spentTxid: { $eq: null } }] })
                .sort({ blockHeight: -1 }).limit(TXO_CACHE_SIZE).toArray()

            cacheTxos.forEach(txo => this._cacheTxout[txo.parentTxid + txo.parentIndex.toHex(4)] = txo)

            Logger.info("Utxos cache: %d", Object.keys(cacheTxos).length)

        } finally {
            timer.stop()
            Logger.trace("Mongo#connect in %s", timer.toLocalTimeString())
        }

    }

    public async getBlockChainTip(): Promise<ChainTip> {
        this._validateConnection()

        const timer = TimeCounter.begin()
        try {
            const network = BitcoinConfig.network

            if (this._cacheBlockchainTip != null)
                return this._cacheBlockchainTip

            const tip = (await this._blocks.aggregate([
                {
                    $match: { txn: { $exists: true } }
                },
                {
                    $group: {
                        _id: "$network", height: { $max: "$height" }, txn: { $sum: "$txn" }
                    }
                },
                {
                    $match: { _id: network }
                },
                {
                    $lookup: {
                        from: "blocks",
                        let: { blockHeight: "$height" },
                        pipeline: [{
                            $match: {
                                $expr: {
                                    $eq: [
                                        "$height", "$$blockHeight"
                                    ]
                                }
                            }
                        }],
                        as: "tipHash"
                    }
                },
                {
                    $replaceRoot: {
                        newRoot: {
                            $mergeObjects: [{ $arrayElemAt: ["$tipHash", 0] }, "$$ROOT"]
                        }
                    }
                },
                {
                    $project: { hash: 1, height: 1, txn: 1, time: 1, _id: 0 }
                }
            ]).toArray()).shift()

            this._cacheBlockchainTip = tip || { hash: Constants.NULL_HASH, height: -1, time: null, txn: 0 }

            return this._cacheBlockchainTip
        } finally {
            timer.stop()
            const logger = timer.milliseconds >= 1000 ? Logger.warn.bind(Logger) : Logger.trace.bind(Logger)
            logger("Mongo#getTip in %s", timer.toLocalTimeString())
        }
    }

    public async getHeaderChainTip(): Promise<ChainTip> {
        this._validateConnection()

        const timer = TimeCounter.begin()
        try {
            const network = BitcoinConfig.network

            if (this._cacheHeaderchainTip != null)
                return this._cacheHeaderchainTip

            const tip = (await this._blocks.aggregate([
                {
                    $group: {
                        _id: "$network", height: { $max: "$height" }, txn: { $sum: "$txn" }
                    }
                },
                {
                    $match: { _id: network }
                },
                {
                    $lookup: {
                        from: "blocks",
                        let: { blockHeight: "$height" },
                        pipeline: [{
                            $match: {
                                $expr: {
                                    $eq: [
                                        "$height", "$$blockHeight"
                                    ]
                                }
                            }
                        }],
                        as: "tipHash"
                    }
                },
                {
                    $replaceRoot: {
                        newRoot: {
                            $mergeObjects: [{ $arrayElemAt: ["$tipHash", 0] }, "$$ROOT"]
                        }
                    }
                },
                {
                    $project: { hash: 1, height: 1, txn: 1, time: 1, _id: 0 }
                }
            ]).toArray()).shift()

            this._cacheHeaderchainTip = tip || { hash: Constants.NULL_HASH, height: -1, time: null, txn: 0 }

            return this._cacheHeaderchainTip
        } finally {
            timer.stop()
            const logger = timer.milliseconds >= 1000 ? Logger.warn.bind(Logger) : Logger.trace.bind(Logger)
            logger("Mongo#getTip in %s", timer.toLocalTimeString())
        }
    }

    public async  getLocators(height: number): Promise<string[]> {
        this._validateConnection()

        const timer = TimeCounter.begin()

        try {
            const network = BitcoinConfig.network

            return await this._blocks.find({ network }).sort({ height: -1 })
                .limit(30).map(blk => blk.hash).toArray()
        } finally {
            timer.stop()
            Logger.trace("Mongo#getLocators in %s", timer.toLocalTimeString())
        }
    }

    public async getPendingHeaders(): Promise<string[]> {
        const network = BitcoinConfig.network
        return this._blocks.find({ network, txn: { $exists: false } }).limit(Constants.DOWNLOAD_SIZE).map(block => block.hash).toArray()
    }

    public async saveBlock(block: Block): Promise<boolean> {
        this._validateConnection()

        const timer = TimeCounter.begin()

        // Import txs, if completed then save block
        const network = BitcoinConfig.network
        const hash = block._getHash()

        const prevHashBlock = block.header.prevHash.toReverseHex()

        let prevBlock = BufferHelper.isNull(block.header.prevHash) ? null
            : await this._blocks.findOne({ hash: prevHashBlock, network })

        if (!prevBlock && prevHashBlock !== Constants.NULL_HASH) {

            Logger.warn("Found orphan block %s", hash.toReverseHex())
            timer.stop()

            return false
        }

        const height = prevBlock ? prevBlock.height + 1 : 0

        const newBlock: BlockHeaderSchema = {
            hash: hash.toReverseHex(),
            height,
            prevBlock: prevHashBlock,
            version: block.header.version,
            merkle: block.header.merkleRoot.toHex(),
            bits: block.header.bits,
            time: block.header.time,
            nonce: block.header.nonce,
            txn: block.transactions.length,
            network
        }

        if (!await this._saveTxos(block.transactions, newBlock)) {
            Logger.warn("Fail to save the txout from %s", newBlock.hash)
            timer.stop()

            return false
        }

        const response = await this._blocks.bulkWrite([{ updateOne: { filter: { hash: newBlock.hash, network }, update: { $set: newBlock }, upsert: true } }])

        if (response.matchedCount == 0) {
            Logger.error("Fail to write block in blockstore")
            timer.stop()

            return false
        }

        this._cacheBlockchainTip = {
            hash: newBlock.hash,
            height: newBlock.height,
            time: newBlock.time,
            txn: (this._cacheBlockchainTip ? this._cacheBlockchainTip.txn : 0) + newBlock.txn
        }

        timer.stop()

        Logger.info(`Update chain [Block=%s, Height=%d, Txn=%d, Time=%s] (%dutxos) in %s`,
            this._cacheBlockchainTip.hash,
            this._cacheBlockchainTip.height,
            this._cacheBlockchainTip.txn,
            new Date(this._cacheBlockchainTip.time * 1000).toUTCString(),
            Object.keys(this._cacheTxout).length,
            timer.toLocalTimeString()
        )

        return true
    }

    public async saveHeader(header: BlockHeader): Promise<boolean> {
        this._validateConnection()

        const network = BitcoinConfig.network
        const prevHash = header.prevHash.toReverseHex()
        const prev = await this._blocks.findOne({ hash: prevHash, network })

        if (!prev && prevHash !== Constants.NULL_HASH)
            Logger.error("Block not valid [Hash=%s]", header.hash)
        else {
            const result = await this._blocks.bulkWrite([{
                updateOne: {
                    filter: {
                        network,
                        hash: header.hash
                    },
                    update: {
                        $set: {
                            bits: header.bits,
                            hash: header.hash,
                            merkle: header.merkleRoot.toHex(),
                            nonce: header.nonce,
                            version: header.version,
                            prevBlock: header.prevHash.toReverseHex(),
                            time: header.time,
                            height: prev == null ? 0 : prev.height + 1,
                            network
                        }
                    },
                    upsert: true
                }
            } as BulkWriteUpdate<BlockHeaderSchema>])



            const wrote = result.upsertedCount == 1

            if (wrote)
                this._cacheHeaderchainTip = {
                    hash: header.hash,
                    height: prev == null ? 0 : prev.height + 1,
                    time: header.time,
                    txn: null
                }

            return wrote
        }

        return false
    }
}