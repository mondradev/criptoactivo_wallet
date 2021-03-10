import { Block, BlockHeader, MemBlock } from "../primitives/blocks";
import { ChainParams } from '../params'

import Config from "../../../../config"
import NetworkEvents from "./networkevents"
import NetworkStatus from "./networkstatus"
import LoggerFactory from 'log4js'
import AsyncLock from "async-lock"
import { Blockchain } from "../chain/blockchain";
import { ChainTip } from "../store/istore";

class PeerManager {

    private _chain: Blockchain

    public constructor(chain: Blockchain) {
        this._chain = chain
    }

    public startDownloadHeaders(localTip : ChainTip) {

    }

    public startDownloadBlocks() {

    }

}

/**
 * 
 */
export default class NetworkManager {

    private _params: ChainParams
    private _chain: Blockchain
    private _mempool: any // TODO Create a class for Mempool

    public constructor(params: ChainParams, chain: Blockchain, mempool: any) {
        this._params = params
        this._chain = chain
        this._mempool = mempool
    }


}