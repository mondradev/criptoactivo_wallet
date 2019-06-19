import { Block, BlockHeader } from 'bitcore-lib'
import { BtcTxIndexStore } from './BtcTxIndexStore'

import TimeCounter from '../../../utils/TimeCounter'
import level, { AbstractLevelDOWN, LevelUp } from 'level'

import LoggerFactory from '../../../utils/LogginFactory'
import { getDirectory } from '../../../utils/Extras';
import BufferEx from '../../../utils/BufferEx';
import { BtcAddrIndexStore } from './BtcAddrIndexStore';
import { Tx, TxIn, TxOut, UTXO } from '../BtcModel';

type DbBinary = LevelUp<AbstractLevelDOWN<Buffer, Buffer>>

const blockDb: DbBinary = level(getDirectory('db/bitcoin/blocks'), { keyEncoding: 'binary', valueEncoding: 'binary' })
const blkIndexDb: DbBinary = level(getDirectory('db/bitcoin/blocks/index'), { keyEncoding: 'binary', valueEncoding: 'binary' })
const chainInfoDb: LevelUp<AbstractLevelDOWN<string, { hash: string, height: number }>> = level(getDirectory('db/bitcoin/chaininfo'), { valueEncoding: 'json' })

const Logger = LoggerFactory.getLogger('Bitcoin BlockStore')

class BlockLevelDb {

    public async  getLocalTip() {
        try {
            const localTip = await chainInfoDb.get('tip')
            return localTip ? { hash: Buffer.from(localTip.hash, 'hex').toString('hex'), height: localTip.height }
                : { hash: Array(65).join('0'), height: 0 }
        }
        catch (ignore) {
            return { hash: Array(65).join('0'), height: 0 }
        }
    }

    public getLastHashes(bestHeight: number): Promise<string[]> {
        return new Promise<string[]>(async (done) => {
            blockDb.isClosed() && await blockDb.open()

            const resolve = async (hashes: string[]) => {
                blockDb.isOpen() && await blockDb.close()
                done(hashes)
            }

            if (bestHeight > 0) {
                const hashes: Array<{ height: number, hash: string }> = []
                const min = BufferEx.zero().appendUInt32BE(bestHeight - 29).toBuffer()
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

        const prevHashBlock = block.header.prevHash
        let prevIdx = null

        try {
            prevIdx = await blkIndexDb.get(prevHashBlock)
        } catch (ignore) {
            Logger.warn(`Block not found ${Buffer.from(prevHashBlock).reverse().toString('hex')}`)
        }

        const height = prevIdx ? prevIdx.readUInt32BE(0) + 1 : 1
        const heightRaw = BufferEx.zero().appendUInt32BE(height).toBuffer()

        const txs: Tx[] = block.transactions.map((t, idx) => {
            const txID = t._getHash()
            const txIn: TxIn[] = t.inputs.filter((txin) => !txin.isNull()).map((txin, idx) => {
                return {
                    txInIdx: idx,
                    prevTx: txin.prevTxId,
                    txOutIdx: txin.outputIndex,
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
        // const stxoBatch = await BtcAddrIndexStore.import(txs)

        await blkIndexDb.batch()
            .del(hash)
            .put(hash, heightRaw)
            .write()

        await blockDb.batch()
            .del(heightRaw)
            .put(heightRaw, block.toBuffer())
            .write()

        await chainInfoDb.batch()
            .del('tip')
            .put('tip', {
                hash: Buffer.from(hash).reverse().toString('hex'),
                height,
                prevHashBlock: Buffer.from(block.header.prevHash).reverse().toString('hex')
            })
            .write()

        timer.stop()

        Logger.debug(`Block saved [Height=${height}, Hash=${block.hash}, Txn=${block.transactions.length}, Size=${block.toBuffer().length}, PrevBlock=${Buffer.from(block.header.prevHash).reverse().toString('hex')}, Time=${timer.toLocalTimeString()}]`)

        return [height, txs.length]
    }

}

export const BtcBlockStore = new BlockLevelDb()
export const BtcChainStateDb = chainInfoDb
export const BtcBlockDb = blockDb
export const BtcBlkIndexDb = blkIndexDb