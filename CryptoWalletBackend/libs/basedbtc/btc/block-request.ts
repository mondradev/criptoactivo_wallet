import { Block } from "bitcore-lib"
import { EventEmitter } from "events";
import { Pool, Peer } from "bitcore-p2p";
import Utils from "../../../libs/utils";

const MAX_PROMISES = 10

export default class BlockRequest {

    private _hashes = new Array<{ hash: string, block: Block }>()
    private _position = 0
    private _notifier = new EventEmitter();

    public async next() {
        return new Promise<Block>((success) => {
            const resolve = (block: Block) => {
                this._position++
                success(block)
            }

            const current = this._hashes[this._position]

            if (current && current.block == null)
                this._notifier.once(current.hash, () => resolve(current.block))
            else if (this._hashes.length == this._position)
                resolve(null)
            else
                resolve(current.block)

        })
    }

    private _downloading = 0

    private _requireLock = () => new Promise((resolve) => {
        if (this._downloading >= MAX_PROMISES)
            this._notifier.on('downloaded', () => {
                if (this._downloading < MAX_PROMISES)
                    resolve()
            });
        else
            resolve()
    })

    public constructor(hashes: string[], pool: Pool, getBlockMessage: { forBlock: (hash: string) => void }) {
        this._hashes.push(...hashes.map((hash) => { return { hash, block: null } }));

        (async () => {
            let downloaded = 0
            let position = 0
            let notifier = new EventEmitter()

            const listener = (ignore: Peer, message: { block: Block }) => {
                this._downloading--
                this._notifier.emit('downloaded')
                notifier.emit(message.block.hash, message.block)

                if (downloaded >= this._hashes.length)
                    pool.removeListener('peerblock', listener)
            }

            pool.addListener('peerblock', listener)

            while (this._hashes.length > position) {
                const item = this._hashes[position]
                new Promise<void>(async (resolve) => {
                    let received = false

                    notifier.once(item.hash, (block: Block) => {
                        item.block = block
                        received = true
                        downloaded++

                        this._notifier.emit(item.hash);

                        resolve()
                    });

                    while (!received) {
                        pool.sendMessage(getBlockMessage.forBlock(item.hash))
                        await Utils.wait(1000)
                    }
                });
                position++
                this._downloading++

                await this._requireLock();
            }
        })()
    }
}