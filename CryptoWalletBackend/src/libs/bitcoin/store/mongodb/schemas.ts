
export type BlockHeaderSchema = {
    hash: string
    height: number
    version: number
    prevBlock: string
    merkle: string
    time: number
    bits: number
    nonce: number,
    txn: number,
    network: string
}

export type TxOutSchema = {
    network: string
    blockHash: string
    blockHeight: number
    address: string

    parentTxid: string
    parentIndex: number
    value: number
    scriptPubKey: string

    spentBlockHash?: string
    spentBlockHeight?: number

    spentTxid?: string
    spentIndex?: number
    scriptSig?: string
    sequenceNo?: number
}


export type TxInSchema = {
    network: string
    parentTxid: string
    parentIndex: number

    spentBlockHash: string
    spentBlockHeight: number

    spentTxid: string
    spentIndex: number
    scriptSig: string
    sequenceNo: number
}

export type TxSchema = {
    network: string
    blockHash: string
    blockHeight: number

    txid: string
    version: number
    isWitness: boolean
    txInC: number
    txOutC: number
    lockTime: number
    total: number
}