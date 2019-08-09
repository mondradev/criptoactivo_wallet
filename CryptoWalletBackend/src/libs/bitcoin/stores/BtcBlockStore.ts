import { Block, BlockHeader } from 'bitcore-lib'
import { BtcTxIndexStore } from './BtcTxIndexStore'

import TimeCounter from '../../../utils/TimeCounter'
import level, { AbstractLevelDOWN, LevelUp } from 'level'

import LoggerFactory from '../../../utils/LogginFactory'
import { getDirectory, ifNeg } from '../../../utils/Extras';
import BufferEx from '../../../utils/BufferEx';
import { BtcAddrIndexStore } from './BtcAddrIndexStore';
import { Tx, TxIn, TxOut, UTXO } from '../BtcModel';
import Config from '../../../../config';
import BtcNetwork from '../BtcNetwork';

type DbBinary = LevelUp<AbstractLevelDOWN<Buffer, Buffer>>

const blockDb: DbBinary = level(getDirectory('db/bitcoin/blocks'), { keyEncoding: 'binary', valueEncoding: 'binary' })
const blkIndexDb: DbBinary = level(getDirectory('db/bitcoin/blocks/index'), { keyEncoding: 'binary', valueEncoding: 'binary' })
const chainStateDb: LevelUp<AbstractLevelDOWN<string, { hash: string, height: number, txn: number }>> = level(getDirectory('db/bitcoin/chainstate'), { valueEncoding: 'json' })

const Logger = LoggerFactory.getLogger('Bitcoin BlockStore')

let cacheTip: {
    hash: string,
    height: number,
    txn: number
} = null

class BlockLevelDb {

    public async createGenesisBlock() {
        let block: Block;

        switch (Config.getAsset('bitcoin').network) {
            case 'testnet':
                block = Block.fromString('0100000000000000000000000000000000000000000000000000000000000000000000003ba3edfd7a7b12b27ac72c3e67768f617fc81bc3888a51323a9fb8aa4b1e5e4adae5494dffff001d1aa4ae180101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff4d04ffff001d0104455468652054696d65732030332f4a616e2f32303039204368616e63656c6c6f72206f6e206272696e6b206f66207365636f6e64206261696c6f757420666f722062616e6b73ffffffff0100f2052a01000000434104678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5fac00000000')
                break;
            case 'mainnet':
            case 'livenet':
                block = Block.fromString('0100000000000000000000000000000000000000000000000000000000000000000000003ba3edfd7a7b12b27ac72c3e67768f617fc81bc3888a51323a9fb8aa4b1e5e4a29ab5f49ffff001d1dac2b7c0101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff4d04ffff001d0104455468652054696d65732030332f4a616e2f32303039204368616e63656c6c6f72206f6e206272696e6b206f66207365636f6e64206261696c6f757420666f722062616e6b73ffffffff0100f2052a01000000434104678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5fac00000000')
                break;
        }

        if (block) {
            await this.import(block)
            Logger.info(`Block genesis created: ${block.hash}`)
        }

    }

    public async getLocalTip() {
        try {
            if (cacheTip)
                return cacheTip

            const localTip = await chainStateDb.get('tip')
            return (cacheTip = localTip ? { hash: Buffer.from(localTip.hash, 'hex').toString('hex'), height: localTip.height, txn: localTip.txn }
                : { hash: Array(65).join('0'), height: 0, txn: 0 })
        }
        catch (ignore) {
            return (cacheTip = { hash: Array(65).join('0'), height: 0, txn: 0 })
        }
    }

    public getLastHashes(bestHeight: number): Promise<string[]> {
        return new Promise<string[]>(async (done) => {

            const resolve = async (hashes: string[]) => {
                done(hashes)
            }

            if (bestHeight >= 0) {
                const hashes: Array<{ height: number, hash: string }> = []
                const min = BufferEx.zero().appendUInt32BE(ifNeg(bestHeight - 29, bestHeight)).toBuffer()
                const max = BufferEx.zero().appendUInt32BE(bestHeight).toBuffer()

                blockDb.createReadStream({ gte: min, lte: max })
                    .on('data', (data: { key: Buffer, value: Buffer }) => {
                        const rawBlock = data.value
                        const header = BlockHeader.fromBuffer(rawBlock)

                        hashes.push({ hash: header.hash, height: data.key.readUInt32BE(0) })
                    }).on('end', () => {
                        resolve(hashes.sort((left, right) => left.height > right.height ? -1 : left.height < right.height ? 1 : 0).map(h => h.hash))
                    })
            }
            else
                resolve([Array(65).join('0')])
        })
    }

    public async import(block: Block) {
        const timer = TimeCounter.begin()
        // Verify if require reorg

        // Import txs, if completed then save block
        const hash = block._getHash()

        const prevHashBlock = Buffer.from(block.header.prevHash).reverse()
        let prevIdx = null

        try {
            prevIdx = await blkIndexDb.get(prevHashBlock)
        } catch (ignore) {
            Logger.warn(`Block not found ${prevHashBlock.toString('hex')}`)
        }

        const height = prevIdx ? prevIdx.readUInt32BE(0) + 1 : 0
        const heightRaw = BufferEx.zero().appendUInt32BE(height).toBuffer()

        const txs: Tx[] = block.transactions.map((t, idx) => {
            const txID = t._getHash()
            const txIn: TxIn[] = t.inputs.filter((txin) => !txin.isNull()).map((txin, idx) => {
                return {
                    txInIdx: idx,
                    txInID: BufferEx.from(txID).appendUInt32LE(idx).toBuffer(),
                    uTxOut: UTXO.fromInput(txin)
                }
            })

            const txOut: TxOut[] = t.outputs.map((txout, idx) => {
                return {
                    script: txout.script,
                    txOutIdx: idx
                }
            })

            return {
                blockHash: hash,
                blockHeight: height,
                txIndex: idx,
                txID: txID,
                nTxOut: txOut.length,
                nTxIn: txIn.length,
                txOut,
                txIn
            }
        })

        await BtcTxIndexStore.import(txs)
        await BtcAddrIndexStore.import(txs)

        await blkIndexDb.batch()
            .del(hash)
            .put(hash, heightRaw)
            .write()

        await blockDb.batch()
            .del(heightRaw)
            .put(heightRaw, block.toBuffer())
            .write()


        cacheTip = cacheTip ? { hash: Buffer.from(hash).toString('hex'), height, txn: cacheTip.txn + txs.length }
            : { hash: Buffer.from(hash).toString('hex'), height, txn: txs.length }

        await chainStateDb.batch()
            .del('tip')
            .put('tip', cacheTip)
            .write()

        timer.stop()

        Logger.info(`Update chain [Block=${cacheTip.hash}, Height=${height}, Txn=${cacheTip.txn}, Progress=${(height / BtcNetwork.bestHeight * 100).toFixed(2)}%, MemUsage=${(process.memoryUsage().rss / 1048576).toFixed(2)} MB, Time=${timer.toLocalTimeString()}]`)
    }

}

export const BtcBlockStore = new BlockLevelDb()
export const BtcChainStateDb = chainStateDb
export const BtcBlockDb = blockDb
export const BtcBlkIndexDb = blkIndexDb