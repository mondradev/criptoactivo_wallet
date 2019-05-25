import { EventEmitter } from "events";
import Config from "../../config";
import BtcNetwork from "./BtcNetwork";
import LoggerFactory from "../../utils/LogginFactory";
import { Networks } from 'bitcore-lib'
import { BtcBlockStore } from "./BtcBlockStore";
import { wait } from "../../utils/Extras";


const Logger = LoggerFactory.getLogger('Bitcoin Blockchain')

class Blockchain extends EventEmitter {

    private _synchronizeInitial = false

    public constructor() {
        super()
        Networks.defaultNetwork = Networks.get(Config.assets.bitcoin.network)
    }

    public get synchronized() { return this._synchronizeInitial }

    public async sync() {
        try {
            const connected = await BtcNetwork.connect()

            if (!connected)
                throw new Error(`Can't connect to network`)

            Logger.info('Connected to Bitcoin Network')

            while (true) {
                let { hash, height } = await BtcBlockStore.getLocalTip()

                Logger.info(`LocalTip [Hash=${hash}, Height=${height}]`)

                let locators = await BtcBlockStore.getLastHashes(height)

                if (!locators.includes(hash))
                    throw new Error(`Fail in chain state, can't find last block [Hash=${hash}]`)

                if (BtcNetwork.bestHeight <= height) {
                    this._synchronizeInitial = true
                    Logger.info(`Download finalized [Tip=${hash}, Height=${height}]`)
                    this.emit('synchronized')
                    return
                } else {
                    const headers = await BtcNetwork.getHeaders(locators)

                    if (!headers || headers.length == 0) {
                        await wait(10000)
                        continue
                    }

                    const blockDownloader = BtcNetwork.getDownloader(headers)

                    while (blockDownloader.hasNext()) {
                        const block = await blockDownloader.next()

                        if (!block) continue

                        height++
                        Logger.trace(`Received block [Hash=${block.hash}, Height=${height}]`)

                        await BtcBlockStore.import(block)
                    }
                }

            }
        } catch (ex) {
            await BtcNetwork.disconnect()
            Logger.error(`Error="Fail to download blockchain", Exception=${ex.stack}`)
            return this.sync()
        }

    }
}


const BtcBlockchain = new Blockchain()
export default BtcBlockchain