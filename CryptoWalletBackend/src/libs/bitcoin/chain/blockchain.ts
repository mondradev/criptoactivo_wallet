import { IBlockStore, ChainTip, ChainLocators } from '../store/istore'
import { Block, Networks, BlockHeader } from 'bitcore-lib'
import { BitcoinConfig, BlockGenesis, Checkpoints } from '../consensus'
import { EventEmitter } from 'events'

import LoggerFactory from 'log4js'
import Constants from '../constants'
import AsyncLock from 'async-lock'
import Config from '../../../../config'
import crypto from 'crypto'

const Logger = LoggerFactory.getLogger('(Bitcoin) Blockchain')
const Lock = new AsyncLock()
const KeyLocks = { AddingBlock: "adding-block" }

export class Blockchain {
    private _blockNotifier: EventEmitter
    private _store: IBlockStore


    private _dSha256(data: Buffer) {
        const hash = crypto.createHash('sha256').update(data).digest()
        return crypto.createHash('sha256').update(hash).digest()
    }

    private _computeMerkle(txids: Array<Buffer>) {
        const hashes: Array<Buffer> = []

        for (let i = 0; i < txids.length; i += 2) {
            const hash1 = txids[i]
            const hash2 = txids[i + 1] ? txids[i + 1] : hash1
            const hash = this._dSha256(hash1.append(hash2))

            hashes.push(hash)
        }

        return hashes
    }

    private _calculateMerkleRoot(txs: Array<Buffer>) {
        if (!txs || txs.length == 0)
            return Buffer.alloc(32, 0)

        let data = txs

        while (data.length > 1)
            data = this._computeMerkle(data)

        return data[0]
    }

    private async _validateBlock(block: Block): Promise<boolean> {
        const hash = block._getHash()

        const height = await this.getHeight(hash)

        if (height > 0)
            return false

        const merkleRoot = this._calculateMerkleRoot(block.transactions.map(tx => tx._getHash()))

        if (!merkleRoot.equals(block.header.merkleRoot)) {
            Logger.warn("Block's merkleRoot is invalid: %s != %s", merkleRoot.toHex(), block.header.merkleRoot.toHex())

            return false
        }

        return true
    }

    /**
     *
     */
    public constructor(store: IBlockStore) {
        this._store = store
        this._blockNotifier = new EventEmitter()

        this._blockNotifier.setMaxListeners(1000)

        Logger.level = Config.logLevel
        Networks.defaultNetwork = Networks.get(BitcoinConfig.network)
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

    public async addBlock(block: Block): Promise<boolean> {
        if (!(await this._validateBlock(block))) {
            Logger.warn("Bad block [Hash=%s]", block.hash)

            return false
        }
        else
            return await Lock.acquire<boolean>(KeyLocks.AddingBlock,
                () => this._store.saveBlock(block))
                .then((added) => {
                    if (!added) return false

                    this._blockNotifier.emit(block.hash)
                    return true
                });
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
        this._blockNotifier.on(blockHash, listener)
    }

    public onceBlock(blockHash: string, listener: () => void) {
        this._blockNotifier.once(blockHash, listener)
    }

    public removeBlockListener(blockHash: string, listener: () => void) {
        this._blockNotifier.removeListener(blockHash, listener)
    }

    public removeAllBlockListeners(blockHash: string) {
        this._blockNotifier.removeAllListeners(blockHash)
    }
}