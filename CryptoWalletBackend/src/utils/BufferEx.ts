export default class BufferEx {

    private _buf: Buffer;

    public static alloc(length: number, fill: string | number = 0) {
        const buf = new BufferEx()

        buf._buf = Buffer.alloc(length, fill)

        return buf
    }

    public static fromBuffer(buf: Buffer) {
        const newBuf = new BufferEx()

        newBuf._buf = Buffer.from(buf)

        return newBuf
    }

    public static fromHex(data: string) {
        const newBuf = new BufferEx()

        newBuf._buf = Buffer.from(data, 'hex')

        return newBuf
    }

    private static _writeUInt64LEInternal(buf: Buffer, value: number, offset: number) {
        const valueHex = Buffer.from(value.toString(16).padStart(16, '0'), 'hex').reverse()

        if (valueHex.length != 8)
            throw Error('Invalid format')

        const endPos = offset + valueHex.length

        if (endPos < 0 || endPos > buf.length)
            throw RangeError(`The Buffer lenght is not valid, [Require Lenght=${endPos}, Current Lenght=${buf.length}`)

        BufferEx._copyInternal(valueHex, buf, offset)

        return endPos
    }

    private static _copyInternal(src: Buffer, des: Buffer, offset: number = 0) {
        if (des.length < src.length)
            throw RangeError(`Bad length of destination`)

        des.write(src.toString('hex'), offset, 'hex')
    }

    public constructor() {
        this._buf = Buffer.alloc(0)
    }

    public appendVarintNum(value: number) {
        let varn: Buffer = null

        if (value < 0xFD) {
            varn = Buffer.alloc(1)
            varn.writeUInt8(value, 0)
        } else if (value <= 0xFFFF) {
            varn = Buffer.alloc(3)
            varn.writeUInt8(0xFD, 0)
            varn.writeUInt16LE(value, 1)
        } else if (value <= 0xFFFFFFFF) {
            varn = Buffer.alloc(5)
            varn.writeUInt8(0xFE, 0)
            varn.writeUInt32LE(value, 1)
        } else {
            varn = Buffer.alloc(9)
            varn.writeUInt8(0xFF, 0)
            BufferEx._writeUInt64LEInternal(varn, value, 1)
        }

        const newLength = this._buf.length + varn.length
        const newBuf = Buffer.alloc(newLength)

        BufferEx._copyInternal(this._buf, newBuf)
        BufferEx._copyInternal(varn, newBuf, this._buf.length)

        this._buf = newBuf

        return this
    }

    public appendInt32LE(value: number) {
        const newBuf = Buffer.alloc(this._buf.length + 4)

        BufferEx._copyInternal(this._buf, newBuf)
        newBuf.writeInt32LE(value, this._buf.length)

        this._buf = newBuf

        return this
    }

    public appendUInt32LE(value: number) {
        const newBuf = Buffer.alloc(this._buf.length + 4)

        BufferEx._copyInternal(this._buf, newBuf)
        newBuf.writeUInt32LE(value, this._buf.length)

        this._buf = newBuf

        return this
    }

    public appendUInt64LE(value: number) {
        const newBuf = Buffer.alloc(this._buf.length + 8)

        BufferEx._writeUInt64LEInternal(newBuf, value, this._buf.length)

        this._buf = newBuf

        return this
    }

    public append(buf: Buffer) {
        const newBuf = Buffer.alloc(this._buf.length + buf.length)

        newBuf.write(this._buf.toString('hex'), 0, 'hex')
        newBuf.write(buf.toString('hex'), this._buf.length, 'hex')

        this._buf = newBuf

        return this
    }

    public readUInt64LE(offset: number) {
        const self = this._buf
        const data = Buffer.from(self.slice(offset, offset + 8))

        return parseInt(data.reverse().toString('hex'), 16)
    }

    public readUInt32LE(offset: number) {
        return this._buf.readUInt32LE(offset)
    }

    public readInt32LE(offset: number) {
        return this._buf.readInt32LE(offset)
    }

    public readVarintNum(offset: number, sizeRef?: number) {
        const value = this._buf.readUInt8(offset)

        if (value < 0xFD) {
            sizeRef = 1
            return value
        }
        else if (value == 0xFD) {
            sizeRef = 3
            return this._buf.readUInt16LE(offset + 1)
        }
        else if (value == 0xFE) {
            sizeRef = 5
            return this._buf.readUInt32LE(offset + 1)
        }
        else if (value == 0xFF) {
            sizeRef = 9
            return parseInt(this._buf.slice(offset + 1, offset + 9).reverse().toString('hex'), 16)
        }

        throw Error('Invalid format')
    }

    public toBuffer() {
        return this._buf
    }

}