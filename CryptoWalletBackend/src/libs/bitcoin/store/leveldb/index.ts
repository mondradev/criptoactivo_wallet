import { IBlockStore, ChainTip } from "../istore";
import { Block, Transaction, Output } from "bitcore-lib";
import level, { LevelUpChain, LevelUp, AbstractLevelDOWN } from "level";
import { getDirectory } from "../../../../utils";
import LoggerFactory from 'log4js'
import Config from "../../../../../config";
import BufferHelper from "../../../../utils/bufferhelper";
import TimeCounter from "../../../../utils/timecounter";
import Constants from "../../constants";
import { OutputEntry } from "./outputentry";
import { Enconding } from "./enconding";
import { TxIndex } from "./txindex";
import { AddrIndex } from "./addrindex";

const Logger = LoggerFactory.getLogger('Bitcoin (LevelStore)')

const BLOCK_PATH = "db/bitcoin/blocks"
const UNDO_PATH = "db/bitcoin/blocks/undo"
const HEIGHT_FROM_NULL_HASH = -1

export class LevelStore implements IBlockStore {

    private _cacheTip: ChainTip
    private _cacheHeightByHash: Map<string, Buffer>
    private _cacheHashByHeight: Map<number, Buffer>

    private _db: LevelUp<AbstractLevelDOWN<Buffer, Buffer>>
    private _undoDb: LevelUp<AbstractLevelDOWN<Buffer, Buffer>>
    private _txIndex: TxIndex
    private _addrIndex: AddrIndex

    private async _getTxn(height: number, prevHeight: number) {
        let tip: any = this._cacheTip && this._cacheTip.height == prevHeight ? this._cacheTip : null

        if (height > 0) {

            if (!tip) tip = await this.get(Enconding.Tip.key(BufferHelper.numberToBuffer(prevHeight, 4, 'be')))
            if (!tip) tip = await this.get(Enconding.Tip.key())

            if (tip instanceof Buffer)
                tip = Enconding.Tip.decode(tip)
        }

        return tip ? tip.txn : 0
    }

    private async _getCoinAndUndo(transactions: Transaction[], hash: Buffer,
        trDb: LevelUpChain<Buffer, Buffer>, undoCoins: OutputEntry[]) {
        for (const tx of transactions) {
            for (const [index] of tx.outputs.entries()) {
                const output = new OutputEntry(tx._getHash(), index, hash)

                trDb.put(Enconding.CoinTxo.key(output.outpoint), Enconding.CoinTxo.encode(output))
            }

            for (const txi of tx.inputs)
                if (!txi.isNull()) {
                    const txo = new OutputEntry(Buffer.from(txi.prevTxId).reverse(), txi.outputIndex, hash)

                    trDb.del(Enconding.CoinTxo.key(txo.outpoint))

                    undoCoins.push(txo)
                }
        }
    }

    private _showStatus(timer: TimeCounter) {
        Logger.info(`Update chain [Block=%s, Height=%d, Txn=%d, MemUsage=%s MB in %s`,
            this._cacheTip.hash,
            this._cacheTip.height,
            this._cacheTip.txn,
            (process.memoryUsage().rss / 1048576).toFixed(2),
            timer.toLocalTimeString()
        )
    }

    private async _reorg(newHeight: number, hash: Buffer) {
        let tip = await this.getLocalTip()

        if (newHeight > tip.height || newHeight == 0)
            return

        if (newHeight == tip.height && hash.toReverseHex() === tip.hash)
            return

        Logger.warn("Blockchain requires be reorganization { newHeight: %d }", newHeight)

        while (newHeight <= tip.height) {

            const hash = await this.getHash(tip.height)

            await this._undoTxo(hash)

            tip = await this.getLocalTip()
        }
    }

    private async _undoTxo(hash: Buffer) {
        try {
            const undo = await this._undoDb.get(Enconding.UndoTxo.key(hash))
            const outputs = Enconding.UndoTxo.decode(undo)
            const trDb = this._db.batch()
            const height = await this.getHeight(hash)
            const bHeight = BufferHelper.numberToBuffer(height, 4, "be")
            const bNewHeight = BufferHelper.numberToBuffer(height - 1, 4, "be")
            const tip = Enconding.Tip.decode(await this.get(Enconding.Tip.key(bNewHeight)))

            for (const output of outputs)
                trDb.put(Enconding.CoinTxo.key(output.outpoint), Enconding.CoinTxo.encode(output))

            await trDb.del(Enconding.BlockByHashIdx.key(hash))
                .del(Enconding.BlockHeightByHashIdx.key(hash))
                .del(Enconding.BlockHashByHeightIdx.key(bHeight))
                .del(Enconding.Tip.key(bHeight))
                .put(Enconding.Tip.key(), Enconding.Tip.encode(tip))
                .write()

            this._cacheTip = tip

            await this._undoDb.del(Enconding.UndoTxo.key(hash))

            Logger.info("Reorg blockchain { Hash: %s, Height: %d, Txn: %d }",
                tip.hash,
                tip.height,
                tip.txn
            )
        } catch (ex) {
            Logger.warn("Can't reorg, undo not found { hash: %s }", hash.toReverseHex())
        }

    }

    public constructor() {
        Logger.level = Config.logLevel

        this._txIndex = new TxIndex()
        this._addrIndex = new AddrIndex()
        this._cacheHeightByHash = new Map()
        this._cacheHashByHeight = new Map()
        this._db = level(getDirectory(BLOCK_PATH), { keyEncoding: 'binary', valueEncoding: 'binary' })
        this._undoDb = level(getDirectory(UNDO_PATH), { keyEncoding: 'binary', valueEncoding: 'binary' })
    }

    public async getUnspentCoins(txid: Buffer): Promise<{ index: number, utxo: Output }[]> {
        return new Promise<{ index: number, utxo: Output }[]>(resolve => {

            const coins = new Array<{ index: number, block: Buffer }>()

            this._db.createReadStream({
                gte: Enconding.CoinTxo.key(BufferHelper.zero().append(txid).appendHex(Array(4).fill(0).join(''))),
                lte: Enconding.CoinTxo.key(BufferHelper.zero().append(txid).appendHex(Array(4).fill(0xff).join('')))
            })
                .on('data', (data: { key: Buffer, value: Buffer }) => {
                    const utxo = Enconding.CoinTxo.decode(data.value)
                    coins.push({ index: utxo.index, block: utxo.blockHash })
                })
                .on('end', async () => {
                    const blockHashes = [...coins.map(c => c.block).reduce((map, value) => {
                        if (!map.has(value.toReverseHex()))
                            map.set(value.toReverseHex(), value)
                        return map
                    }, new Map<string, Buffer>()).values()]

                    const blocks = await Promise.all(blockHashes.map(hash => this.getBlock(hash)))

                    resolve(coins.map(coinEntry => {
                        const block = blocks.find(b => b._getHash().equals(coinEntry.block))
                        const tx = block.transactions.find(t => t._getHash().equals(txid))

                        return {
                            index: coinEntry.index,
                            utxo: tx.outputs[coinEntry.index]
                        }
                    }))
                })
        })
    }

    public get TxIndex() {
        return this._txIndex
    }

    public get AddrIndex() {
        return this._addrIndex
    }

    public disconnect(): Promise<void> {
        return new Promise<void>(async resolve => {
            Logger.info("Closing blockstore")

            if (!this._db.isClosed())
                await this._db.close()

            resolve()
        })
    }

    public async getBlock(hash: Buffer): Promise<Block> {
        const blockEntry = await this.get(Enconding.BlockByHashIdx.key(hash))

        if (!blockEntry)
            return null

        return Enconding.BlockByHashIdx.decode(blockEntry);
    }

    public async getHash(height: number): Promise<Buffer> {
        if (this._cacheHashByHeight && this._cacheHashByHeight.has(height))
            return this._cacheHashByHeight.get(height)

        const hashEntry = await this.get(Enconding.BlockHashByHeightIdx.key(BufferHelper.numberToBuffer(height, 4, "be")))

        if (!hashEntry)
            return null

        return Enconding.BlockHashByHeightIdx.decode(hashEntry)
    }

    public async getHeight(hash: Buffer): Promise<number> {
        if (BufferHelper.isNull(hash))
            return HEIGHT_FROM_NULL_HASH

        if (this._cacheHeightByHash && this._cacheHeightByHash.has(hash.toHex()))
            return Enconding.BlockHeightByHashIdx.decode(this._cacheHeightByHash.get(hash.toHex()))

        const heightEntry = await this.get(Enconding.BlockHeightByHashIdx.key(hash))

        if (!heightEntry)
            return null

        return Enconding.BlockHeightByHashIdx.decode(heightEntry)
    }

    public async connect(): Promise<void> {
        Logger.info("Initializing blockstore (LevelStore)")

        if (this._db.isOpen())
            return await this._db.open()
    }

    public async getLocators(height: number): Promise<string[]> {
        if (height < 1)
            return [Constants.NULL_HASH]

        const tip = await this.getLocalTip()

        if (tip.height < height)
            height = tip.height

        const locators = new Array<string>()
        let size = Math.min(30, height)

        while (locators.length < size)
            locators.push((await this.getHash(height - locators.length)).toReverseHex())

        return locators
    }

    public async getLocalTip(): Promise<ChainTip> {
        try {
            if (this._cacheTip)
                return this._cacheTip

            const raw = await this._db.get(Enconding.Tip.key())

            this._cacheTip = Enconding.Tip.decode(raw)

            return this._cacheTip
        } catch (ignore) {
            return { hash: Constants.NULL_HASH, height: 0, txn: 0, time: 0 }
        }
    }

    public async get(key: Buffer): Promise<Buffer> {
        try {
            return await this._db.get(key)
        } catch (ignored) {
            return null
        }
    }

    public async saveBlock(block: Block): Promise<boolean> {
        const timer = TimeCounter.begin()

        const hash = block._getHash()

        const prevHashBlock = Buffer.from(block.header.prevHash)
        const prevHeight = await this.getHeight(prevHashBlock)

        if (prevHeight == null && !BufferHelper.isNull(prevHashBlock)) {
            Logger.warn(`Found orphan block ${hash.toReverseHex()}`)
            timer.stop()

            return false
        }

        const height = prevHeight + 1

        // TODO: Crear función para verificar al agregar bloque
        await this._reorg(height, hash)

        const bHeight = BufferHelper.numberToBuffer(height, 4, 'be')

        const tip = {
            hash: block.hash,
            height,
            time: block.header.time,
            txn: await this._getTxn(height, prevHeight) + block.transactions.length
        }

        await this._txIndex.indexing(block.transactions, hash)
        await this._addrIndex.indexing(block.transactions, hash)
        await this._addrIndex.resolveInput(block.transactions, hash)

        const trDb = this._db.batch()

        trDb.put(Enconding.BlockByHashIdx.key(hash), Enconding.BlockByHashIdx.encode(block))
            .put(Enconding.BlockHeightByHashIdx.key(hash), Enconding.BlockHeightByHashIdx.encode(height))
            .put(Enconding.BlockHashByHeightIdx.key(bHeight), Enconding.BlockHashByHeightIdx.encode(hash))
            .put(Enconding.Tip.key(bHeight), Enconding.Tip.encode(tip))
            .put(Enconding.Tip.key(), Enconding.Tip.encode(tip))

        const undoCoins = new Array<OutputEntry>()

        await this._getCoinAndUndo(block.transactions, hash, trDb, undoCoins)

        Logger.trace("Writting %d operations", trDb.length)

        await trDb.write()
        await this._undoDb.put(Enconding.UndoTxo.key(hash), Enconding.UndoTxo.encode(undoCoins))

        if (undoCoins.length > 0)
            Logger.debug("Wrote %d txo in undo index", undoCoins.length)

        this._cacheTip = tip
        this._cacheHeightByHash.set(hash.toHex(), Enconding.BlockHeightByHashIdx.encode(height))
        this._cacheHashByHeight.set(height, hash)

        timer.stop()

        this._showStatus(timer)

        return true
    }


}