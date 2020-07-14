import level, { LevelUp, AbstractLevelDOWN } from "level";
import { getDirectory } from "../../../../utils";
import LoggerFactory from 'log4js'
import Config from "../../../../../config";
import BufferHelper from "../../../../utils/bufferhelper";
import { Enconding } from "./enconding";
import { Encoder } from "./encoder";
import { Transaction, Block, Script, Address, PublicKey, Networks, Input, Output } from "bitcore-lib";
import { AddrindexEntry } from "./addrindexentry";
import { Blockchain } from "../../chain/blockchain";
import { EventEmitter } from "events";
import { TxData } from "../../../../resources/iwalletprovider";

const Logger = LoggerFactory.getLogger('Bitcoin (Mempool)')

const MEMPOOL_PATH = "db/bitcoin/mempool"
const ADDRIDX_PATH = "db/bitcoin/mempool/addrIndex"

export class Mempool {

    private _txSize: number
    private _ready: boolean
    private _orphanedTxs: Map<string, Buffer>
    private _requireTxs: Map<string, Map<string, null>>
    private _db: LevelUp<AbstractLevelDOWN<Buffer, Buffer>>
    private _dbIdx: LevelUp<AbstractLevelDOWN<Buffer, Buffer>>
    private _chain: Blockchain
    private _notifier: EventEmitter

    private static _scriptFnAddress = {
        'Pay to public key': (script: Script) => new Address(new PublicKey(script.getPublicKey()), Networks.defaultNetwork).toBuffer(),
        'Pay to public key hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toBuffer(),
        'Pay to script hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toBuffer()
    }

    public constructor(chain: Blockchain) {
        this._db = level(getDirectory(MEMPOOL_PATH), { keyEncoding: 'binary', valueEncoding: 'binary' })
        this._dbIdx = level(getDirectory(ADDRIDX_PATH), { keyEncoding: 'binary', valueEncoding: 'binary' })

        Logger.level = Config.logLevel

        this._orphanedTxs = new Map()
        this._requireTxs = new Map()
        this._txSize = 0
        this._ready = false
        this._chain = chain
        this._notifier = new EventEmitter()
    }

    public load() {
        return new Promise<void>(done =>
            this._db.createKeyStream().on('data', () => this._txSize++)
                .on('end', () => {
                    this._ready = true

                    Logger.info("Mempool is loaded: %d txs", this._txSize)

                    done()
                }))
    }

    private async _getKey<TValue>(db: LevelUp<AbstractLevelDOWN<Buffer, Buffer>>, key: Buffer, encoder: Encoder<TValue>) {
        try {
            const value = await db.get(encoder.key(key))
            return encoder.decode(value ? value : BufferHelper.zero())
        }
        catch (ex) {
            return null;
        }
    }

    public async has(txid: Buffer): Promise<boolean> {
        if (!this._ready)
            throw new Error("Mempool is not ready")

        const tx = await this._getKey(this._db, txid, Enconding.Mempool)

        return tx != null
    }

    public async add(transaction: Transaction): Promise<void> {
        if (!this._ready)
            throw new Error("Mempool is not ready")

        const tx = new Transaction().fromBuffer(transaction.toBuffer())

        const txBatch = this._db.batch()
        const addrBatch = this._dbIdx.batch()

        const hash = transaction._getHash()

        this._txSize++

        txBatch.put(Enconding.Mempool.key(hash), Enconding.Mempool.encode(transaction))

        const dependencies: string[] = []

        for (const txi of tx.inputs) {
            const txo = await this._connectOutput(txi)

            if (!txo) {
                dependencies.push(txi.prevTxId.toHex())

                continue
            }

            const addr = this._toAddress(txo)

            addrBatch.put(Enconding.Addrindex.key(addr.append(hash)),
                Enconding.Addrindex.encode(new AddrindexEntry(addr, hash, Buffer.alloc(0, 32), 0xffffffff)))
        }

        if (dependencies.length > 0) {
            this._addOrphanTx(tx, dependencies)

            return
        }

        for (const [index, txo] of tx.outputs.entries()) {
            const addr = this._toAddress(txo)

            addrBatch.put(Enconding.Addrindex.key(addr.append(hash)),
                Enconding.Addrindex.encode(new AddrindexEntry(addr, hash, Buffer.alloc(0, 32), 0xffffffff)))
                .put(Enconding.TxAddrIndex.key(hash.appendUInt32BE(index)), Enconding.TxAddrIndex.encode(addr))
        }

        await Promise.all([addrBatch.write(), txBatch.write()])

        Logger.info("New transaction was added to mempool: %s", hash.toReverseHex())

        this._notifier.emit('tx', transaction)

        await this._resolveOrphans(transaction)
    }

    private async _resolveOrphans(tx: Transaction) {
        const txid = tx._getHash()

        if (!this._requireTxs.has(txid.toReverseHex())) return

        const orphanedTxs = this._requireTxs.get(txid.toReverseHex())

        this._requireTxs.delete(txid.toReverseHex())

        for (const orphaned of orphanedTxs.keys()) {
            const orphanedTx = new Transaction().fromBuffer(this._orphanedTxs.get(orphaned))

            for (const txi of orphanedTx.inputs)
                if (this._requireTxs.has(txi.prevTxId.toHex()))
                    return

            this._orphanedTxs.delete(orphanedTx.hash)

            Logger.info("The orphaned transaction [%s] found a dependency: %s", orphanedTx.hash, txid.toReverseHex())

            await this.add(orphanedTx)
        }

    }

    private _addOrphanTx(tx: Transaction, dependencies: string[]) {
        if (!this._orphanedTxs.has(tx.hash)) {
            this._orphanedTxs.set(tx.hash, tx.toBuffer())

            Logger.warn("Received orphaned tx: %s", tx.hash)

            for (const dependency of dependencies) {
                let txList: Map<string, null> = this._requireTxs.has(dependency) ?
                    this._requireTxs.get(dependency) : new Map()

                txList.set(tx.hash, null)

                this._requireTxs.set(dependency, txList)
            }
        }
    }

    public get size(): number {
        if (!this._ready)
            throw new Error("Mempool is not ready")

        return this._txSize
    }

    public async remove(block: Block): Promise<void> {
        if (!this._ready)
            throw new Error("Mempool is not ready")

        const txBatch = this._db.batch()
        const addrBatch = this._dbIdx.batch()

        let txs = 0

        for (const tx of block.transactions) {
            const hash = tx._getHash()

            if (!await this.has(hash)) {
                if (this._orphanedTxs.has(hash.toReverseHex()))
                    this._orphanedTxs.delete(hash.toReverseHex())

                continue
            }

            this._txSize--
            txs++

            txBatch.del(Enconding.Mempool.key(hash))

            for (const txi of tx.inputs) {
                const txo = await this._connectOutput(txi)

                if (!txo) continue

                const addr = this._toAddress(txo)

                addrBatch.del(Enconding.Addrindex.key(addr.append(hash)))
            }

            for (const [index, txo] of tx.outputs.entries()) {
                const addr = this._toAddress(txo)

                addrBatch.del(Enconding.Addrindex.key(addr.append(hash)))
                    .del(Enconding.TxAddrIndex.key(hash.appendUInt32BE(index)))
            }
        }

        await Promise.all([addrBatch.write(), txBatch.write()])

        if (txs > 0)
            Logger.info("Block contains %d transactions from mempool", txs)
    }

    private _toAddress(txo: Output): Buffer {
        if (!txo.script)
            return Buffer.alloc(32, 0)

        return (Mempool._scriptFnAddress[txo.script.classify().toString()]
            || (() => Buffer.alloc(32, 0)))(txo.script)
    }

    private async _connectOutput(txi: Input): Promise<Output> {
        const prevTxid = txi.prevTxId.reverse()

        let tx: Transaction = null

        if (await this.has(prevTxid))
            tx = await this.get(prevTxid)
        else {
            const txindex = await this._chain.TxIndex.getIndexByHash(prevTxid)

            if (txindex) {
                const block = await this._chain.getBlock(txindex.blockHash)

                if (block)
                    tx = block.transactions[txindex.index]
            }
        }

        if (!tx) {
            Logger.debug("Tx no found: %s", txi.prevTxId.toReverseHex())

            return null
        }

        return tx.outputs[txi.outputIndex]
    }

    public async get(txid: Buffer): Promise<Transaction> {
        if (!this._ready)
            throw new Error("Mempool is not ready")

        return this._getKey(this._db, txid, Enconding.Mempool)
    }

    public async getTxsByAddress(address: Buffer): Promise<Array<TxData>> {

        const addrIndexes = await new Promise<AddrindexEntry[]>(resolve => {
            const indexes = new Array<AddrindexEntry>()
            this._db.createReadStream({
                gte: Enconding.Addrindex.key(Buffer.from(address).append(Buffer.alloc(32, 0))),
                lte: Enconding.Addrindex.key(Buffer.from(address).append(Buffer.alloc(32, 0xff)))
            })
                .on('data', (data: { key: Buffer, value: Buffer }) =>
                    indexes.push(Enconding.Addrindex.decode(data.value)))
                .on('end', () => resolve(indexes))
        })

        const txsData = new Array<TxData>()

        for (const idx of addrIndexes) {
            const tx = await this.get(idx.txid)

            if (!tx) continue

            txsData.push({
                block: "(Mempool)",
                height: -1,
                index: -1,
                txid: tx.hash,
                time: Math.round(new Date().getTime() / 1000),
                data: tx.toBuffer().toHex()
            })
        }

        return txsData
    }

    public on(event: 'tx', listener: (tx: Transaction) => Promise<void>) {
        this._notifier.on(event, listener)
    }
}