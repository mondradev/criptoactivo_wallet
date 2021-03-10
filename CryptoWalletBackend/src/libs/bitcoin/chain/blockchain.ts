import { IBlockStore, ChainTip, ChainLocators } from '../store/istore'
import { Block, Networks, BlockHeader } from 'bitcore-lib'
import { BitcoinConfig } from '../consensus'
import { EventEmitter } from 'events'

import LoggerFactory from 'log4js'
import Constants from '../constants'
import AsyncLock from 'async-lock'
import Config from '../../../../config'

const Logger = LoggerFactory.getLogger('(Bitcoin) Blockchain')
const Lock = new AsyncLock()
const KeyLocks = { AddingBlock: "adding-block" }

export class Blockchain {

    private _notifier: EventEmitter
    private _store: IBlockStore

    private async _validateHeader(header: BlockHeader): Promise<boolean> {
        // TODO: Validación de cabeceras de bloques
        return true
    }

    /**
     *
     */
    public constructor(store: IBlockStore) {
        this._store = store
        this._notifier = new EventEmitter()

        this._notifier.setMaxListeners(1000)

        Logger.level = Config.logLevel
        Networks.defaultNetwork = Networks.get(BitcoinConfig.isTest)
    }

    public get TxIndex() {
        return this._store.TxIndex
    }

    public get AddrIndex() {
        return this._store.AddrIndex
    }

    public async connect() {
        return this._store.connect()
    }

    public async disconnect() {
        return this._store.disconnect()
    }

    public async getBlock(hash: Buffer): Promise<Block> {
        return this._store.getBlock(hash)
    }

    public async getHeight(hash: Buffer): Promise<number> {
        return this._store.getHeight(hash)
    }

    public async addBlock(block: Block): Promise<void> {
        if (!(await this._validateHeader(block.header)))
            Logger.warn("Bad block [Hash=%s]")
        else
            await Lock.acquire<boolean>(KeyLocks.AddingBlock,
                () => this._store.saveBlock(block))
                .then((added) => {
                    if (added)
                        this._notifier.emit(block.hash)
                })
    }

    public async getLocators(tip: ChainTip): Promise<ChainLocators> {
        return {
            starts: await this._store.getLocators(tip.height),
            stop: this.getCheckpoints(tip.height)
        }
    }

    public async getLocalTip(): Promise<ChainTip> {
        return this._store.getLocalTip()
    }

    public async createGenesisBlock() {
        return this._store.saveBlock(BlockGenesis[Networks.defaultNetwork.name])
    }

    public getCheckpoints(height: number) {
        const checkpointData = Checkpoints[Networks.defaultNetwork.name]

        if (!checkpointData)
            return Constants.NULL_HASH

        for (const checkpoint of checkpointData)
            if (height < checkpoint.height)
                return checkpoint.hash

        return Constants.NULL_HASH
    }

    public async getUnspentCoins(txid: Buffer) {
        return this._store.getUnspentCoins(txid)
    }


    public onBlock(blockHash: string, listener: () => void) {
        this._notifier.on(blockHash, listener)
    }

    public onceBlock(blockHash: string, listener: () => void) {
        this._notifier.once(blockHash, listener)
    }

    public removeBlockListener(blockHash: string, listener: () => void) {
        this._notifier.removeListener(blockHash, listener)
    }

    public removeAllBlockListeners(blockHash: string) {
        this._notifier.removeAllListeners(blockHash)
    }
}