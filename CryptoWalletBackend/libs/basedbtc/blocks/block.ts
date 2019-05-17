import { Hash256 } from "../hash"
import Utils from "../../../libs/utils"

import '../../utils/array-ext'
import BufferEx from "../../../libs/utils/bufferex";
import { Buffer } from "buffer";

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
        const data = new BufferEx()
        data.appendInt32LE(this._version)
        data.append(this._hashPrevBlock.toBuffer())
        data.append(this._hashMerkleRoot.toBuffer())
        data.appendUInt32LE(this._time)
        data.appendUInt32LE(this._bits)
        data.appendUInt32LE(this._nonce)

        return data.toBuffer()
    }

    public static deserialize(data: Buffer | string) {
        let raw: Buffer = null
        if (Utils.isString(data))
            raw = Buffer.from(data as string, 'hex')
        else
            raw = data as Buffer

        const header = BlockHeader.create({
            version: raw.readInt32LE(0),
            hashPrevBlock: Hash256.fromBuffer(raw.slice(4, 36)),
            hashMerkleRoot: Hash256.fromBuffer(raw.slice(36, 68)),
            time: raw.readUInt32LE(68),
            bits: raw.readUInt32LE(72),
            nonce: raw.readUInt32LE(76)
        })
        
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
        let data = BufferEx.fromBuffer(super.serialize())
        let txn = this._txs.length

        data.appendVarintNum(txn)

        return data.toBuffer()
    }

    public static deserialize(data: Buffer | string) {
        const header = super.deserialize(data)
        const block = new Block()
        const buf = Utils.isString(data) ? BufferEx.fromHex(data as string) : BufferEx.fromBuffer(data as Buffer)

        block._bits = header['_bits']
        block._nonce = header['_nonce']
        block._time = header['_time']
        block._version = header['_version']
        block._hashMerkleRoot = header['_hashMerkleRoot']
        block._hashPrevBlock = header['_hashPrevBlock']

        block._txs = new Array(buf.readVarintNum(80))

        return block
    }

    public constructor() {
        super()
        this.setNull()
    }

    public get Hash() { return Hash256.sha256sha256(super.serialize()) }

}