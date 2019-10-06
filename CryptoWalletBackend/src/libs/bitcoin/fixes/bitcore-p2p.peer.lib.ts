import { Peer } from 'bitcore-p2p'

const fnReadMessage: () => void = Peer.prototype['_readMessage']

Peer.prototype['_readMessage'] = function () {
    try {
        fnReadMessage.apply(this)
    } catch (e) {
        this._onError(e)
    }
}