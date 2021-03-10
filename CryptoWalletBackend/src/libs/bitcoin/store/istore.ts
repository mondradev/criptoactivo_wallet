import { Block, Transaction } from "bitcore-lib"

export type ChainLocators = {
    starts: Array<string>,
    stop?: string
}

export type ChainTip = {
    height: number,
    hash: string,
    txn: number,
    time: number
}


export interface IBlockStore {    
    connect(): Promise<void>
    disconnect(): Promise<void>

    getBlock(hash: Buffer): Promise<Block>
    getUnspentCoins(txid: Buffer): Promise<{ index: number, utxo: Transaction.Output }[]>

    getHash(height: number): Promise<Buffer>
    getHeight(hash: Buffer): Promise<number>
    getLocators(height: number): Promise<string[]>
    getLocalTip(): Promise<ChainTip>

    saveBlock(block: Block): Promise<boolean>
}