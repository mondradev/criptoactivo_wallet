import { Peer } from 'bitcore-p2p'
import { Block } from 'bitcore-lib'
import { Messages } from 'bitcore-p2p'
import Constants from '../constants'
import NetworkEvents from '../network/networkevents'

declare module 'bitcore-p2p' {
    interface Peer {
        _readMessage: () => void
        getBlocks: (hashes: string[]) => Promise<void>
        busy: Boolean
    }
}

const fnReadMessage: () => void = Peer.prototype._readMessage
const fnDisconnect: () => Peer = Peer.prototype.disconnect

Peer.prototype._readMessage = function () {
    try {
        fnReadMessage.apply(this)
    } catch (e) {
        this._onError(e)
    }
}

Peer.prototype.disconnect = function () {
    const self = this as Peer

    if (self.status != Peer.STATUS.DISCONNECTED)
        return fnDisconnect.apply(this)

    self.emit(NetworkEvents.DISCONNECT, null)

    return self
}

export class PendingBlocksError extends Error {
    public constructor(message: string, public pendingBlocks: string[]) {
        super(message)
    }
}

Peer.prototype.getBlocks = function (hashes: string[]): Promise<void> {
    if (hashes.length == 0)
        return Promise.resolve()

    return new Promise((resolve, reject) => {
        const self = this as Peer
        const request = new Map<string, boolean>()
        const method = new Messages({ network: self.network })

        const clearHandler = () => {
            clearTimeout(timeout)
            self.removeListener(NetworkEvents.BLOCK, blockHandler)
        }

        const blockHandler = (message: { block: Block }) => {
            timeout.refresh()

            const downloaded = request.has(message.block.hash)

            if (downloaded)
                request.delete(message.block.hash)

            if (request.size == 0) {
                clearHandler()
                resolve()
            }
        }

        let timeout = setTimeout(() => {
            const pending = request.size

            self.disconnect()

            clearHandler()
            reject(new PendingBlocksError('Timeout to request blocks from ' + self.host + ', pending blocks: ' + pending, [...request.keys()]))
        }, Constants.Timeouts.WAIT_FOR_BLOCKS)

        self.on('block', blockHandler.bind(self))

        for (const hash of hashes) {
            request.set(hash, true)
            self.sendMessage(method.GetData.forBlock(Buffer.from(hash, 'hex').reverse()))
        }

        timeout.refresh()

    })
}
