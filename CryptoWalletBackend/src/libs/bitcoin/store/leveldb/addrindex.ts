import { Transaction, Script, Address, PublicKey, Networks } from "bitcore-lib";
import level, { LevelUp, AbstractLevelDOWN } from "level";
import { AddrindexEntry } from "./addrindexentry";
import { Enconding } from "./enconding";
import LoggerFactory from 'log4js'
import BufferHelper from "../../../../utils/bufferhelper";
import Config from "../../../../../config";
import TimeCounter from "../../../../utils/timecounter";
import { getDirectory } from "../../../../utils";

const Logger = LoggerFactory.getLogger('Bitcoin (AddrIndex)')
const MAX_SIZE_CACHE = 200000
const ADDRINDEX_PATH = 'db/bitcoin/addrindex'


export class AddrIndex {

    private static _scriptFnAddress = {
        'Pay to public key': (script: Script) => new Address(new PublicKey(script.getPublicKey()), Networks.defaultNetwork).toBuffer(),
        'Pay to public key hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toBuffer(),
        'Pay to script hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toBuffer()
    }

    private _db: LevelUp<AbstractLevelDOWN<Buffer, Buffer>>
    private _cacheAddrs: Map<string, Buffer>

    /**
     * Obtiene la dirección del script especificado.
     * 
     * @param script Script de la salida.
     */
    public toAddress(script: Script): Buffer {
        if (!script)
            return Buffer.alloc(32, 0)

        // Obtenemos la función para resolver la dirección pública o devolvemos null.

        return (AddrIndex._scriptFnAddress[script.classify().toString()]
            || (() => Buffer.alloc(32, 0)))(script)
    }

    public constructor() {
        this._db = level(getDirectory(ADDRINDEX_PATH), { keyEncoding: 'binary', valueEncoding: 'binary' })
        this._cacheAddrs = new Map()

        Logger.level = Config.logLevel
    }

    public connect() {
        return new Promise<void>(resolve => {
            this._db.createReadStream({
                gte: Enconding.TxAddrIndex.key(BufferHelper.fromHex(Array(36).fill(0).join(''))),
                lte: Enconding.TxAddrIndex.key(BufferHelper.fromHex(Array(36).fill(0xff).join(''))),
                limit: MAX_SIZE_CACHE / 2
            })
                .on('data', (data: { key: Buffer, value: Buffer }) =>
                    this._cacheAddrs.set(data.key.toHex(), data.value))
                .on('end', () => {
                    Logger.info("Loaded %d addrsIndex in cache", this._cacheAddrs.size)
                    resolve()
                })
        })
    }

    public writeCacheCoin(outpoint: Buffer, addr: Buffer) {
        if (!this._cacheAddrs.has(outpoint.toHex()))
            this._cacheAddrs.set(outpoint.toHex(), addr)

        if (this._cacheAddrs.size > MAX_SIZE_CACHE) {
            const keys = [...this._cacheAddrs.keys()].slice(this._cacheAddrs.size - MAX_SIZE_CACHE / 2)

            while (keys.length > 0)
                this._cacheAddrs.delete(keys.shift())
        }
    }

    public async getCoinAddress(outpoint: Buffer): Promise<Buffer> {
        let coin: Buffer
        const key = Enconding.TxAddrIndex.key(outpoint)

        if (this._cacheAddrs.has(key.toHex())) {
            coin = this._cacheAddrs.get(key.toHex())
            this._cacheAddrs.delete(key.toHex())
        }
        else
            coin = await this.get(key)

        return coin
    }

    public async resolveInput(transactions: Transaction[], blockHash: Buffer) {
        if (transactions.length == 0)
            return

        const timer = TimeCounter.begin()
        const batchDb = this._db.batch()

        let countInputs = 0

        const inputsData: Array<{ txid: Buffer, txindex: number, coinbase: boolean, prevTxid: Buffer, prevIndex: number, outpoint: Buffer }>
            = transactions.reduce((collection, tx, txindex) => {
                const hash = tx._getHash()
                collection.push(...tx.inputs.map(input => {
                    const prevTxid = Buffer.from(input.prevTxId).reverse()
                    return {
                        txid: hash,
                        txindex,
                        coinbase: input.isNull(),
                        prevTxid,
                        prevIndex: input.outputIndex,
                        outpoint: prevTxid.appendUInt32BE(input.outputIndex)
                    }
                }))

                return collection
            }, [])

        for (const input of inputsData) {
            if (input.coinbase)
                continue

            let addr: Buffer
            addr = await this.getCoinAddress(input.outpoint)

            if (!addr) continue

            countInputs++

            batchDb
                .del(Enconding.TxAddrIndex.key(input.outpoint))
                .put(Enconding.Addrindex.key(addr.append(input.txid)),
                    Enconding.Addrindex.encode(new AddrindexEntry(addr, input.txid, blockHash, input.txindex)))
        }

        await batchDb.write()

        timer.stop()

        Logger.debug("Resolved %d inputs in %s", countInputs, timer.toLocalTimeString())
    }

    public async indexing(transactions: Transaction[], blockHash: Buffer) {
        const batchDb = this._db.batch()

        for (const [index, transaction] of transactions.entries()) {
            const hash = transaction._getHash()

            for (const [idx, txo] of transaction.outputs.entries()) {
                const addr = this.toAddress(txo.script)
                const addrTx = Enconding.Addrindex.key(addr.append(hash))
                const outpoint = Enconding.TxAddrIndex.key(hash.appendUInt32BE(idx))

                batchDb
                    .put(outpoint, Enconding.TxAddrIndex.encode(addr))
                    .put(addrTx, Enconding.Addrindex.encode(new AddrindexEntry(addr, hash, blockHash, index)))

                this.writeCacheCoin(outpoint, Enconding.TxAddrIndex.encode(addr))
            }
        }

        await batchDb.write()

        Logger.debug("Addresses cache size: %d", this._cacheAddrs.size)
    }

    public getIndexesByAddress(address: Buffer): Promise<AddrindexEntry[]> {
        const txdata = new Array<AddrindexEntry>()

        return new Promise<AddrindexEntry[]>(resolve => {
            this._db.createReadStream({
                gte: Enconding.Addrindex.key(Buffer.from(address).appendHex(Array(32).fill(0).join(''))),
                lte: Enconding.Addrindex.key(Buffer.from(address).appendHex(Array(32).fill(0xff).join('')))
            })
                .on('data', (data: { key: Buffer, value: Buffer }) =>
                    txdata.push(Enconding.Addrindex.decode(data.value)))
                .on('end', () => resolve(txdata))
        })
    }
}