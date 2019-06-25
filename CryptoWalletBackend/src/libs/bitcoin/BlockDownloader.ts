import { Block, Networks } from "bitcore-lib"
import { Messages } from "bitcore-p2p"
import LoggerFactory from "../../utils/LogginFactory"
import TimeCounter from "../../utils/TimeCounter"
import BtcNetwork from "./BtcNetwork"
import { isStringArray } from "../../utils/Preconditions"
import * as Extras from '../../utils/Extras'
import { EventEmitter } from "events"
import Config from "../../../config"
import AsyncLock from 'async-lock'

const Logger = LoggerFactory.getLogger('Blockdownloader')
const lock = new AsyncLock()

const MAX_BLOCKS = Config.getAsset('bitcoin').maxParallelDownloadBlock
const MB = (1024 * 1024)
const MAX_SIZE = Config.getAsset('bitcoin').cacheBlockSizeMB * MB
const TIMEWAIT_NEW_REQUEST = 5000

export default class BitcoinBlockDownloader {

    private _notifier = new EventEmitter

    private _left = 0

    private _hashes = {}

    private _size = 0

    public hasNext(): boolean {
        return this._left > 0
    }

    public async get(hash: string): Promise<Block> {

        return new Promise<Block>((resolve) => {
            lock.acquire('get', (unlock) => {
                if (this._left == 0)
                    resolve(null)
                else if (this._hashes[hash]) {
                    const block = this._hashes[hash]
                    delete this._hashes[hash]
                    this._size -= block.toBuffer().length
                    this._left--
                    resolve(block)
                } else {
                    this._notifier.once(hash, (block: Block) => {
                        delete this._hashes[hash]
                        this._size -= block.toBuffer().length
                        this._left--
                        resolve(block)
                    })
                }

                unlock()
            })
        })
    }

    public constructor(hashes: string[]) {
        const timer = TimeCounter.begin()

        const getBlockMessage = new Messages({ network: Networks.get(Config.getAsset('bitcoin').network) }).GetData

        if (!isStringArray(hashes))
            throw new Error('Require only string values')

        for (const hash of hashes)
            this._hashes[hash] = null

        this._left = hashes.length

        Logger.trace(`Request of ${hashes.length} blocks`);

        (async () => {
            let downloading = 0

            for (let i = 0; i < hashes.length; i++) {
                const hash = hashes[i]

                if (this._hashes[hash]) continue

                (async (hashblock: string) => {
                    let received = false

                    BtcNetwork.once(hashblock, async (block: Block) => {
                        received = true
                        downloading--
                        downloading = downloading < 0 ? 0 : downloading

                        await lock.acquire('get', (unlock) => {
                            this._hashes[hashblock] = block
                            this._size += block.toBuffer().length
                            this._notifier.emit(hashblock, block)

                            unlock()
                        })

                        if (hashblock === hashes[hashes.length - 1]) {
                            timer.stop()
                            Logger.debug(`Downloaded ${hashes.length} blocks in ${timer.toLocalTimeString()}`)
                        }
                    })

                    while (!received) {
                        BtcNetwork.sendMessage(getBlockMessage.forBlock(hashblock))
                        await Extras.wait(TIMEWAIT_NEW_REQUEST)
                    }
                })(hash)


                downloading++

                while (downloading > MAX_BLOCKS || this._size > MAX_SIZE)
                    await Extras.wait(10)
            }

        })().catch(() => timer && timer.stop())
    }
}