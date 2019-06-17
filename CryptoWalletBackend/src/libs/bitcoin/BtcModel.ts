import { Script, Input } from 'bitcore-lib';
import BufferEx from '../../utils/BufferEx';

export type Addr = {
    hash: string
    ip: { v4?: string, v6?: string }
    port: number
}

export type TxIn = {
    txInIdx: number
    prevTx: Buffer
    txOutIdx: number
    txInID: Buffer
    uTxOut: UTXO
}

export type TxOut = {
    script: Script
    txOutIdx: number
}

export type Tx = {
    blockHash: Buffer
    blockHeight: number
    txIndex: number
    txID: Buffer
    nTxOut: number
    nTxIn: number
    txOut: TxOut[]
    txIn: TxIn[]
}


export enum UTXOState {
    Unspent,
    Spent
}

export class UTXO {
    private _type = UTXOState.Unspent

    private _txid = Buffer.alloc(32, 0)

    private _txindex = 0

    public constructor(txid: Buffer = Buffer.alloc(32, 0), index: number = 0) {
        this._type = UTXOState.Unspent
        this._txid = txid
        this._txindex = index
    }

    public get spent() {
        const stxo = new UTXO(this._txid, this._txindex)
        stxo._type = UTXOState.Spent

        return stxo
    }

    public toBuffer() {
        return BufferEx.zero()
            .appendUInt8(this._type)
            .append(this._txid)
            .appendUInt32LE(this._txindex)
            .toBuffer()
    }

    public get outpoint() {
        return BufferEx.zero()
            .append(this._txid)
            .appendUInt32LE(this._txindex)
            .toBuffer()
    }

    public toOutpointHex() {
        return BufferEx.zero()
            .append(this._txid)
            .appendUInt32LE(this._txindex)
            .toBuffer()
            .toString('hex')
    }

    public toHex() {
        return this.toBuffer().toString('hex')
    }

    public static fromBuffer(raw: Buffer) {
        const utxo = new this()

        utxo._type = raw.readUInt8(0)
        utxo._txid = raw.slice(1, 33)
        utxo._txindex = raw.readUInt32LE(33)

        return utxo
    }

    public static encode(type: UTXOState, txid: Buffer, txindex: number) {
        const utxo = new UTXO()
        utxo._type = type
        utxo._txid = txid
        utxo._txindex = txindex

        return utxo.toBuffer()
    }

    public static fromInput(input: Input) {
        return new UTXO(input.prevTxId, input.outputIndex)
    }
}