import { Block, Networks } from "bitcore-lib"
import { Messages } from "bitcore-p2p"
import LoggerFactory from "../../utils/LogginFactory"
import TimeCounter from "../../utils/TimeCounter"
import BtcNetwork from "./BtcNetwork"
import { isStringArray } from "../../utils/Preconditions";
import * as Extras from '../../utils/Extras'
import { EventEmitter } from "events";
import Config from "../../../config";
import AsyncLock from 'async-lock'

const Logger = LoggerFactory.getLogger('Blockdownloader')
const lock = new AsyncLock()

const MAX_BLOCKS = 200
export default class BitcoinBlockDownloader {

    private _notifier = new EventEmitter

    private _left = 0

    private _hashes = {}

    public hasNext(): boolean {
        return this._left > 0
    }

    public async get(hash: string): Promise<Block> {

        return new Promise<Block>(async (resolve) => {
            lock.acquire('get', (unlock) => {
                if (this._left == 0)
                    resolve(null)
                else if (this._hashes[hash]) {
                    const block = this._hashes[hash]
                    delete this._hashes[hash]
                    this._left--
                    resolve(block)
                } else {
                    this._notifier.once(hash, (block: Block) => {
                        delete this._hashes[hash]
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
            for (const hash of hashes) {
                BtcNetwork.once(hash, async (block: Block) => {
                    downloading--
                    downloading = downloading < 0 ? 0 : downloading

                    await lock.acquire('get', (unlock) => {

                        this._hashes[hash] = block
                        this._notifier.emit(hash, block)

                        unlock()
                    })
                })

                BtcNetwork.sendMessage(getBlockMessage.forBlock(hash))
                downloading++

                while (downloading > MAX_BLOCKS)
                    await Extras.wait(10)

            }
            timer.stop()

            Logger.debug(`Downloaded ${hashes.length} blocks in ${timer.toLocalTimeString()}`)
        })().catch(() => timer && timer.stop())
    }
}