import { Hash256 } from "../hash"
import Utils from "../../../libs/utils"

import '../../utils/array-ext'

export class BlockHeader {

    public static create(params: {
        version: number,
        hashPrevBlock: string | Buffer | Hash256,
        hashMerkleRoot: string | Buffer | Hash256,
        time: number | Date,
        bits: number,
        nonce: number
    }) {
        const { version, hashPrevBlock, hashMerkleRoot, time, bits, nonce } = params
        const header = new BlockHeader()

        header._version = version
        header._hashPrevBlock = Utils.isString(hashPrevBlock) ? Hash256.fromHex(hashPrevBlock as string)
            : Utils.isBuffer(hashPrevBlock) ? Hash256.fromBuffer(hashPrevBlock as Buffer)
                : hashPrevBlock as Hash256
        header._hashMerkleRoot = Utils.isString(hashMerkleRoot) ? Hash256.fromHex(hashMerkleRoot as string)
            : Utils.isBuffer(hashMerkleRoot) ? Hash256.fromBuffer(hashMerkleRoot as Buffer)
                : hashMerkleRoot as Hash256
        header._bits = bits
        header._nonce = nonce
        header._time = Utils.isDate(time) ? (time as Date).getTime() / 1000 : time as number

        return header;
    }

    protected _version: number
    protected _hashPrevBlock: Hash256
    protected _hashMerkleRoot: Hash256
    protected _time: number
    protected _bits: number
    protected _nonce: number

    constructor() {
        this.setNull();
    }

    public get Hash() { return Hash256.sha256sha256(this.serialize()) }
    public get BlockTime() { return new Date(this._time * 1000) }

    public isNull = () => this._bits == 0

    public setNull() {
        this._version = 0
        this._hashMerkleRoot = Hash256.NULL
        this._hashMerkleRoot = Hash256.NULL
        this._time = 0
        this._bits = 0
        this._nonce = 0
    }

    public serialize(): Buffer {
        const data = Buffer.alloc(80)
        data.writeInt32LE(this._version, 0)
        data.write(this._hashPrevBlock.toBufferString(), 4, 'hex')
        data.write(this._hashMerkleRoot.toBufferString(), 36, 'hex')
        data.writeUInt32LE(this._time, 68)
        data.writeUInt32LE(this._bits, 72)
        data.writeUInt32LE(this._nonce, 76)

        return data
    }

    public static deserialize(data: Buffer | string) {
        let raw: Buffer = null
        if (Utils.isString(data))
            raw = Buffer.from(data as string, 'hex')
        else
            raw = data as Buffer

        const header = new BlockHeader()

        header._version = raw.readInt32LE(0)
        header._hashPrevBlock = Hash256.fromBuffer(raw.slice(4, 36))
        header._hashMerkleRoot = Hash256.fromBuffer(raw.slice(36, 68))
        header._time = raw.readUInt32LE(68)
        header._bits = raw.readUInt32LE(72)
        header._nonce = raw.readUInt32LE(76)

        return header
    }
}

export class Block extends BlockHeader {

    private _txs: any[]

    public setNull() {
        this._txs = this._txs || []
        this._txs.clear()

        super.setNull()
    }

    public get BlockHeader() {
        return BlockHeader.create({
            version: this._version,
            hashPrevBlock: this._hashPrevBlock,
            hashMerkleRoot: this._hashMerkleRoot,
            time: this._time,
            bits: this._bits,
            nonce: this._nonce
        })
    }

    public serialize() {
        let data = super.serialize()
        let txn = this._txs.length
        
        data.writeVarintNum(txn, data.length)

        return data
    }

    public constructor() {
        super()
        this.setNull()
    }

    public get Hash() { return Hash256.sha256sha256(super.serialize()) }

}