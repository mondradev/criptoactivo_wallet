import { Block } from "bitcore-lib"
import { Peer } from "bitcore-p2p"
import LoggerFactory from "../../utils/LogginFactory"
import TimeCounter from "../../utils/TimeCounter"
import BtcNetwork from "./BtcNetwork"
import { isString, isStringArray } from "../../utils/Preconditions";

const Logger = LoggerFactory.getLogger('Blockdownloader')

const TIMEOUT_WAIT_BLOCK = 10000;
export default class BitcoinBlockDownloader {

    private _hashes = new Array<{ hash: string, block: Block }>()
    private _requested = -1
    private _position = 0

    private _internalCallback: () => void = null

    public hasNext() {
        return !(this._position >= this._hashes.length)
    }

    public async next() {
        if (this._internalCallback != null)
            throw new Error(`Request called`)

        return new Promise<Block>(async (resolve) => {
            if (this._position >= this._hashes.length)
                resolve(null)
            else {
                const pos = this._position
                const current = this._hashes[pos]
                this._position++

                if (current.block == null) {
                    this._requested = this._position - 1
                    this._internalCallback = () => {
                        const block = this._hashes[this._requested].block
                        this._hashes[this._requested] = null
                        this._internalCallback = null
                        this._requested = -1
                        resolve(block)
                    }
                    setTimeout(() => resolve(null), TIMEOUT_WAIT_BLOCK)
                }
                else {
                    this._hashes[pos] = null
                    resolve(current.block)
                }

            }
        })
    }

    public constructor(hashes: string[], getBlockMessage: { forBlock: (hash: string) => void }) {
        if (!isStringArray(hashes))
            throw new Error('Require only string values')

        this._hashes.push(...hashes.map((hash) => { return { hash, block: null } }))

        Logger.trace(`Request of ${hashes.length} blocks`)

        let downloaded = 0
        let position = 0
        let peer: Peer = null

        const timer = TimeCounter.begin()

        const listener = (message: { block: Block }) => {
            const pointer = this._hashes.find(h => h && h.hash === message.block.hash)

            if (!pointer)
                return

            pointer.block = message.block
            downloaded++

            if (downloaded >= this._hashes.length) {
                timer.stop()
                peer.removeListener('block', listener)
                Logger.debug(`Downloaded ${downloaded} blocks in ${timer.toLocalTimeString()}`)
            }

            if (this._internalCallback == null)
                return

            if (this._requested >= 0 && this._hashes[this._requested].block != null)
                this._internalCallback()
        }

        (async () => {
            do {
                try {
                    peer = await BtcNetwork.getPeer()

                    Logger.trace(`Connected to Peer [Host=${peer.host}, Height=${peer.bestHeight}] for download`)
                    peer.addListener('block', listener)

                    while (this._hashes.length > position) {
                        const item = this._hashes[position]
                        peer.sendMessage(getBlockMessage.forBlock(item.hash))
                        position++
                    }

                } catch (ex) {
                    Logger.warn(`Fail to download blocks in peer [Host=${peer.host}, Height=${peer.bestHeight}], disconnecting`)
                    position--
                    peer.removeListener('block', listener)
                    peer.disconnect()
                }

            } while (position < this._hashes.length)
        })()
    }
}