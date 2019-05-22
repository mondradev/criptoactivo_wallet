import { Block, BlockHeader } from 'bitcore-lib'
import { TxStore } from './TxStore'

import TimeCounter from '../../utils/TimeCounter'
import level from 'level'

import * as LoggerFactory from '../../utils/LogginFactory'
import * as Extras from '../../utils/Extras'

const blockDb = level('./db/bitcoin/blocks', { valueEncoding: 'hex' })
const idxBlockDb = level('./db/bitcoin/blocks/index', { keyEncoding: 'hex' })
const chainInfoDb = level('./db/bitcoin/chaininfo', { valueEncoding: 'json' })

const Logger = LoggerFactory.getLogger('Bitcoin BlockDb')

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
        // Verify if require reorg

        // Import txs, if completed then save block
        const hash = Buffer.from(block.header.hash, 'hex')

        await TxStore.import(block.transactions, hash)

        const prevHashBlock = Buffer.from(block.header.prevHash).reverse()
        const prevIdx = await Extras.callAsync(idxBlockDb.get, [prevHashBlock], idxBlockDb)
        const height = prevIdx ? parseInt(prevIdx) + 1 : 1

        const timer = TimeCounter.begin()

        await idxBlockDb.batch()
            .del(hash)
            .put(hash, height)
            .write()

        await blockDb.batch()
            .del(height)
            .put(height, (block.header as any).toBuffer())
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

        Logger.info(`Block saved [Height=${height}, Hash=${block.hash}, Txn=${block.transactions.length}, Size=${block.toBuffer().length}, PrevBlock=${Buffer.from(block.header.prevHash).reverse().toString('hex')}, Time=${timer.toLocalTimeString()}]`)
    }

}

export const BlockStore = new BlockLevelDb()