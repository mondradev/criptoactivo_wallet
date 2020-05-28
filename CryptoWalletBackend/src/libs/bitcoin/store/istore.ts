import { Block, Output } from "bitcore-lib"
import { AddrIndex } from "./leveldb/addrindex"
import { TxIndex } from "./leveldb/txindex"

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
    AddrIndex: AddrIndex
    TxIndex: TxIndex

    connect(): Promise<void>
    disconnect(): Promise<void>

    getBlock(hash: Buffer): Promise<Block>
    getUnspentCoins(txid: Buffer): Promise<{ index: number, utxo: Output }[]>

    getHash(height: number): Promise<Buffer>
    getHeight(hash: Buffer): Promise<number>
    getLocators(height: number): Promise<string[]>
    getLocalTip(): Promise<ChainTip>

    saveBlock(block: Block): Promise<boolean>
}