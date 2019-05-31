import { Block, BlockHeader } from 'bitcore-lib'
import { BtcTxIndexStore } from './BtcTxIndexStore'

import TimeCounter from '../../utils/TimeCounter'
import level from 'level'

import LoggerFactory from '../../utils/LogginFactory'
import * as Extras from '../../utils/Extras'
import { getDirectory } from '../../utils/Extras';

const blockDb = level(getDirectory('db/bitcoin/blocks'), { valueEncoding: 'hex' })
const blkIndexDb = level(getDirectory('db/bitcoin/blocks/index'), { keyEncoding: 'hex' })
const chainInfoDb = level(getDirectory('db/bitcoin/chaininfo'), { valueEncoding: 'json' })

const Logger = LoggerFactory.getLogger('Bitcoin BlockStore')

class BlockLevelDb {

    public async  getLocalTip() {
        const localTip = await Extras.callAsync(chainInfoDb.get, ['tip'], chainInfoDb)

        return localTip ? { hash: Buffer.from(localTip.hash).toString('hex'), height: localTip.height }
            : { hash: Array(65).join('0'), height: 0 }
    }

    public getLastHashes(bestHeight: number): Promise<string[]> {
        return new Promise<string[]>(async (resolve) => {
            if (bestHeight > 0) {
                const hashes = []
                blockDb.createReadStream({ gte: bestHeight - 30, lte: bestHeight }).on('data', (data: { key: string, value: string }) => {
                    const rawBlock = Buffer.from(data.value, 'hex')
                    const header = BlockHeader.fromBuffer(rawBlock)

                    hashes.push(header.hash)
                }).on('end', () => {
                    resolve(hashes)
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
        const hash = Buffer.from(block.header.hash, 'hex')

        const prevHashBlock = Buffer.from(block.header.prevHash).reverse()
        const prevIdx = await Extras.callAsync(blkIndexDb.get, [prevHashBlock], blkIndexDb)
        const height = prevIdx ? parseInt(prevIdx) + 1 : 1

        await BtcTxIndexStore.import(block.transactions, hash, height)

        await blkIndexDb.batch()
            .del(hash)
            .put(hash, height)
            .write()

        await blockDb.batch()
            .del(height)
            .put(height, block.toBuffer())
            .write()

        await chainInfoDb.batch()
            .del('tip')
            .put('tip', {
                hash,
                height,
                prevHashBlock: Buffer.from(block.header.prevHash).reverse()
            })
            .write()

        timer.stop()

        Logger.debug(`Block saved [Height=${height}, Hash=${block.hash}, Txn=${block.transactions.length}, Size=${block.toBuffer().length}, PrevBlock=${Buffer.from(block.header.prevHash).reverse().toString('hex')}, Time=${timer.toLocalTimeString()}]`)
    }

}

export const BtcBlockStore = new BlockLevelDb()
export const BtcChainStateDb = chainInfoDb
export const BtcBlockDb = blockDb
export const BtcBlkIndexDb = blkIndexDb