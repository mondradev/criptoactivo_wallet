import { IBlockStore, ChainTip } from "../istore";
import { Block, Transaction, Networks } from "bitcore-lib";
import level, { LevelUpChain, LevelUp, AbstractLevelDOWN, CodecOptions } from "level";
import { getDirectory } from "../../../../utils";
import LoggerFactory, { Logger } from 'log4js'
import Config from "../../../../../config";
import BufferHelper from "../../../../utils/bufferhelper";
import TimeCounter from "../../../../utils/timecounter";
import Constants from "../../constants";
import { OutputEntry } from "./outputentry";
import { Enconding } from "./enconding";
import { TxIndex } from "./txindex";
import { AddrIndex } from "./addrindex";
import { getKey } from './dbutils'
import { ChainParams } from "../../params";
import { requireNotNull } from "../../../../utils/preconditions";

/**
 * Tipo de codificado para los campos del indice de bloques.
 */
const BLOCK_DB_TYPE: CodecOptions = { keyEncoding: 'binary', valueEncoding: 'binary' }

/**
 * Tipo de codificado para los campos del indice de salidas gastadas.
 */
const UNDO_DB_TYPE: CodecOptions = { keyEncoding: 'binary', valueEncoding: 'binary' }

/**
 * Rutas de las bases de datos de los distintos tipos de red.
 */
const DB_PATHS: {
    [net: string]: { block: string, undo: string }
} = {
    'mainnet': {
        'block': "db/bitcoin/blocks",
        'undo': "db/bitcoin/blocks/undo"
    },
    'testnet': {
        'block': "db/bitcoin/testnet/blocks",
        'undo': "db/bitcoin/testnet/blocks/undo"
    }
}

export class LevelStore implements IBlockStore {

    /**
     * Instancia de la bitacora de la clase.
     */
    private _logger: Logger

    /**
     * Configuración de la red de Bitcoin.
     */
    private _params: ChainParams

    /**
     * Cache de la punta de la cadena.
     */
    private _cacheTip: ChainTip

    /**
     * Cache del indice de altura por hash.
     */
    private _cacheHeightByHash: Map<string, Buffer>

    /**
     * Cache del indice de hash por altura.
     */
    private _cacheHashByHeight: Map<number, Buffer>

    /**
     * Instancia de la base de datos de bloques.
     */
    private _db: LevelUp<AbstractLevelDOWN<Buffer, Buffer>>

    /**
     * Instancia de la base de datos de las transacciones gastadas.
     */
    private _undoDb: LevelUp<AbstractLevelDOWN<Buffer, Buffer>>

    private async _getTxn(height: number, prevHeight: number) {
        let tip = this._cacheTip && this._cacheTip.height == prevHeight ? this._cacheTip : null

        if (height > 0) {

            if (!tip) tip = await getKey(this._db, BufferHelper.numberToBuffer(prevHeight, 4, 'be'), Enconding.Tip)
            if (!tip) tip = await getKey(this._db, null, Enconding.Tip)
        }

        return tip ? tip.txn : 0
    }

    private async _getCoinAndUndo(transactions: Transaction[], hash: Buffer,
        trDb: LevelUpChain<Buffer, Buffer>, undoCoins: OutputEntry[]) {
        for (const tx of transactions) {
            const txid = tx._getHash()

            for (const [index] of tx.outputs.entries()) {
                const output = new OutputEntry(txid, index, hash)

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
        this._logger.info(`Update chain [Block=%s, Height=%d, Txn=%d, MemUsage=%s MB in %s`,
            this._cacheTip.hash,
            this._cacheTip.height,
            this._cacheTip.txn,
            (process.memoryUsage().rss / Constants.DataSizes.MB).toFixed(2),
            timer.toLocalTimeString()
        )
    }

    private async _reorg(newHeight: number, hash: Buffer) {
        let tip = await this.getLocalTip()

        if (newHeight > tip.height || newHeight == 0)
            return

        if (newHeight == tip.height && hash.toReverseHex() === tip.hash)
            return

        this._logger.warn("Blockchain requires be reorganization { newHeight: %d }", newHeight)

        while (newHeight <= tip.height) {

            const hash = await this.getHash(tip.height)

            await this._undoBlock(hash, newHeight)

            tip = await this.getLocalTip()
        }
    }

    private async _undoBlock(hash: Buffer, target: number) {
        try {
            const undo = await getKey(this._undoDb, hash, Enconding.UndoTxo)
            const dbBatch = this._db.batch()
            const height = await this.getHeight(hash)
            const binHeight = BufferHelper.numberToBuffer(height, 4, "be")
            const binNewHeight = BufferHelper.numberToBuffer(height - 1, 4, "be")
            const newTip = await getKey(this._db, binNewHeight, Enconding.Tip)

            for (const output of undo)
                dbBatch.put(Enconding.CoinTxo.key(output.outpoint), Enconding.CoinTxo.encode(output))

            await dbBatch.del(Enconding.BlockByHashIdx.key(hash))
                .del(Enconding.BlockHeightByHashIdx.key(hash))
                .del(Enconding.BlockHashByHeightIdx.key(binHeight))
                .del(Enconding.Tip.key(binHeight))
                .put(Enconding.Tip.key(), Enconding.Tip.encode(newTip))
                .write()

            this._cacheTip = newTip

            await this._undoDb.del(Enconding.UndoTxo.key(hash))

            this._logger.info("Reorg blockchain { Hash: %s, Height: %d, Txn: %d, Target: %d }",
                newTip.hash,
                newTip.height,
                newTip.txn,
                target
            )
        } catch (ex) {
            this._logger.warn("Can't reorg, undo not found { hash: %s }", hash.toReverseHex())
        }

    }

    /**
     * Crea una nueva instancia del almacenamiento.
     * 
     * @param params Configuración de la red.
     */
    public constructor(params: ChainParams) {
        requireNotNull(params)

        this._params = params
        this._cacheHeightByHash = new Map()
        this._cacheHashByHeight = new Map()

        this._db = level(getDirectory(DB_PATHS[this._params.name].block), BLOCK_DB_TYPE)
        this._undoDb = level(getDirectory(DB_PATHS[this._params.name].undo), UNDO_DB_TYPE)
        this._logger = LoggerFactory.getLogger('Bitcoin[' + this._params.name + '] (LevelStore)')
    }

    public async getUnspentCoins(txid: Buffer): Promise<{ index: number, utxo: Transaction.Output }[]> {
        return new Promise<{ index: number, utxo: Transaction.Output }[]>(resolve => {
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

    public disconnect(): Promise<void> {
        return new Promise<void>(async resolve => {
            this._logger.info("Closing blockstore")

            if (!this._db.isClosed())
                await this._db.close()

            resolve()
        })
    }

    public async getBlock(hash: Buffer): Promise<Block> {
        return await getKey(this._db, hash, Enconding.BlockByHashIdx)
    }

    public async getHash(height: number): Promise<Buffer> {
        if (this._cacheHashByHeight && this._cacheHashByHeight.has(height))
            return this._cacheHashByHeight.get(height)

        return await getKey(this._db, BufferHelper.numberToBuffer(height, 4, "be"), Enconding.BlockHashByHeightIdx)
    }

    public async getHeight(hash: Buffer): Promise<number> {
        if (BufferHelper.isNull(hash))
            return Constants.HEIGHT_FROM_NULL_HASH

        if (this._cacheHeightByHash && this._cacheHeightByHash.has(hash.toHex()))
            return Enconding.BlockHeightByHashIdx.decode(this._cacheHeightByHash.get(hash.toHex()))

        return await getKey(this._db, hash, Enconding.BlockHeightByHashIdx)
    }

    public async connect(): Promise<void> {
        this._logger.info("Initializing blockstore")

        if (this._db.isOpen())
            return await this._db.open()
    }

    private async resync() {
        const bestHeight = 1747136

        /*
        this._cacheTip = { hash: Constants.NULL_HASH, height: 0, txn: 0, time: 0 }

        await this.saveBlock(BlockGenesis[Networks.defaultNetwork.name]) */

        this._cacheTip = await getKey(this._db, null, Enconding.Tip)

        this._logger.info("Current chain: %d - %s", this._cacheTip.height, this._cacheTip.hash)

        for (let i = this._cacheTip.height + 1; i <= bestHeight; i++) {
            const hash = await this.getHash(i)

            if (hash == null) {
                this._logger.warn("Block no found: %d", i)
                break
            }

            const block = await this.getBlock(hash)

            this._logger.info("Saving block: %s", block.hash)

            if (!(await this.saveBlock(block)))
                this._logger.error("Fail to save block: %s", block.hash)
        }
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

            this._cacheTip = await getKey(this._db, null, Enconding.Tip)

            return this._cacheTip
        } catch (ignore) {
            return { hash: Constants.NULL_HASH, height: 0, txn: 0, time: 0 }
        }
    }

    public async saveBlock(block: Block): Promise<boolean> {
        try {
            const timer = TimeCounter.begin()

            const hash = block._getHash()

            const prevHashBlock = Buffer.from(block.header.prevHash)
            const prevHeight = await this.getHeight(prevHashBlock)

            if (prevHeight == null && !BufferHelper.isNull(prevHashBlock)) {
                this._logger.warn(`Found orphan block ${hash.toReverseHex()}`)
                timer.stop()

                return false
            }

            const height = prevHeight + 1

            // TODO: Crear función para verificar al agregar bloque
            await this._reorg(height, hash)

            const binHeight = BufferHelper.numberToBuffer(height, 4, 'be')

            const tip = {
                hash: block.hash,
                height,
                time: block.header.time,
                txn: await this._getTxn(height, prevHeight) + block.transactions.length
            }

            const dbBatch = this._db.batch()

            dbBatch.put(Enconding.BlockByHashIdx.key(hash), Enconding.BlockByHashIdx.encode(block))
                .put(Enconding.BlockHeightByHashIdx.key(hash), Enconding.BlockHeightByHashIdx.encode(height))
                .put(Enconding.BlockHashByHeightIdx.key(binHeight), Enconding.BlockHashByHeightIdx.encode(hash))
                .put(Enconding.Tip.key(binHeight), Enconding.Tip.encode(tip))
                .put(Enconding.Tip.key(), Enconding.Tip.encode(tip))

            const undoCoins = new Array<OutputEntry>()

            await this._getCoinAndUndo(block.transactions, hash, dbBatch, undoCoins)

            this._logger.trace("Writting %d operations", dbBatch.length)

            await dbBatch.write()
            await this._undoDb.put(Enconding.UndoTxo.key(hash), Enconding.UndoTxo.encode(undoCoins))

            if (undoCoins.length > 0)
                this._logger.debug("Wrote %d txo in undo index", undoCoins.length)

            this._cacheTip = tip
            this._cacheHeightByHash.set(hash.toHex(), Enconding.BlockHeightByHashIdx.encode(height))
            this._cacheHashByHeight.set(height, hash)

            timer.stop()

            this._showStatus(timer)

            return true
        } catch (error) {
            this._logger.error("Fail in saveBlock: " + JSON.stringify(error))
            return false
        }
    }

    public async  deleteBlock(height: number) {
        const hash = await this.getHash(height);
        const binHeight = BufferHelper.numberToBuffer(height, 4, 'be');
        const binNewHeight = BufferHelper.numberToBuffer(height - 1, 4, 'be');
        const undo = await getKey(this._undoDb, hash, Enconding.UndoTxo);
        const dbBatch = this._db.batch();

        if (undo)
            for (const output of undo)
                dbBatch.put(Enconding.CoinTxo.key(output.outpoint), Enconding.CoinTxo.encode(output));

        const tip = await getKey(this._db, binNewHeight, Enconding.Tip);

        if (tip) {
            dbBatch.del(Enconding.Tip.key(binHeight))
                .put(Enconding.Tip.key(), Enconding.Tip.encode(tip));
        }

        this._logger.info("Height: %d, Hash: %s", height, hash ? hash.toString('hex') : 'n/a')

        if (!hash) {
            this._logger.warn("Height %d no found", height);

            if (dbBatch.length > 0)
                await dbBatch.write();

            return;
        }

        await dbBatch.del(Enconding.BlockByHashIdx.key(hash))
            .del(Enconding.BlockHeightByHashIdx.key(hash))
            .del(Enconding.BlockHashByHeightIdx.key(binHeight))
            .write();

        await this._undoDb.del(Enconding.UndoTxo.key(hash));
    }

}