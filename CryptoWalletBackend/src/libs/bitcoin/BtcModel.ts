import { Script, Input } from 'bitcore-lib';
import BufferHelper from '../../utils/BufferHelper';

export type Addr = {
    hash: string
    ip: { v4?: string, v6?: string }
    port: number
}

export type TxIn = {
    txInIdx: number
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
        let buf = BufferHelper.appendUInt8(BufferHelper.zero(), this._type)
        buf = BufferHelper.append(buf, this._txid)

        return BufferHelper.appendUInt32LE(buf, this._txindex)
    }

    public get outpoint() {
        let buf = BufferHelper.append(BufferHelper.zero(), this._txid)
        return BufferHelper.appendUInt32LE(buf, this._txindex)
    }

    public toOutpointHex() {
        return BufferHelper.toHex(this.outpoint)
    }

    public toHex() {
        return BufferHelper.toHex(this.toBuffer())
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
        return new UTXO(Buffer.from(input.prevTxId).reverse(), input.outputIndex)
    }
}