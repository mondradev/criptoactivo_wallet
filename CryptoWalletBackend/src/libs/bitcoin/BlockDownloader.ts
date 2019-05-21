import { Block } from "bitcore-lib"
import { Pool, Peer } from "bitcore-p2p"
import * as Extras from "../../utils/Extras"
import * as LoggerFactory from "../../utils/LogginFactory"
import TimeCounter from "../../utils/TimeCounter"
import { PeerToPeerController } from "./PeerToPeerController"

const Logger = LoggerFactory.getLogger('BlockRequest')

export default class BlockDownloader {

    private _hashes = new Array<{ hash: string, block: Block }>()
    private _requested = -1
    private _position = 0

    private _internalCallback: (block: Block) => void = null

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
                    this._internalCallback = (block: Block) => {
                        this._hashes[this._requested] = null
                        this._internalCallback = null
                        this._requested = -1
                        resolve(block)
                    }
                }
                else {
                    this._hashes[pos] = null
                    resolve(current.block)
                }

            }
        })
    }

    public constructor(hashes: string[], pool: Pool, getBlockMessage: { forBlock: (hash: string) => void }) {
        this._hashes.push(...hashes.map((hash) => { return { hash, block: null } }))

        Logger.trace(`Request of ${hashes.length} blocks`);

        (async () => {
            const timer = TimeCounter.begin()

            let downloaded = 0
            let position = 0
            let peer = null as Peer
            let peerPos = 0

            const getPeer = () => {

                const peers = Object.entries(pool['_connectedPeers']) as []
                const bestHeight = PeerToPeerController.bestHeight

                if (peer && peer.status === 'ready' && peer.bestHeight >= bestHeight)
                    return peer

                while ((peer == null || peer.status !== 'ready' || peer.bestHeight < bestHeight) && peerPos <= peers.length)
                    peer = Extras.coalesce(peers[peerPos++], [null, null])[1]

                if (peer == null)
                    throw new Error('Fail connect to peer')

                Logger.trace(`Connected to Peer [Host=${peer.host}, Height=${peer.bestHeight}]`)

                const listener = (message: { block: Block }) => {
                    const pointer = this._hashes.find(h => h && h.hash === message.block.hash)

                    if (pointer == null || pointer.block != null)
                        return

                    pointer.block = message.block
                    downloaded++

                    if (downloaded >= this._hashes.length) {
                        timer.stop()
                        peer.removeListener('block', listener)
                        Logger.debug(`Downloaded blocks=${downloaded} in ${timer.toLocalTimeString()}`)
                    }

                    if (this._internalCallback == null)
                        return

                    if (this._requested >= 0) {
                        const block = this._hashes[this._requested].block
                        block == null || this._internalCallback(block)
                    }
                }

                peer.addListener('block', listener)

                return peer
            }

            while (this._hashes.length > position) {
                const item = this._hashes[position]
                getPeer().sendMessage(getBlockMessage.forBlock(item.hash))
                position++
                await Extras.wait(0.1)
            }
        })()
    }
}