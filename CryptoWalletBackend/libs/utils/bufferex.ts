import { ENODATA } from "constants";

export default class BufferEx extends Buffer {

    private _buf: Buffer;

    private static _writeUInt64LEInternal(buf: Buffer, value: number, offset: number) {
        const valueHex = Buffer.from(value.toString(16).padStart(16, '0'), 'hex').reverse()
        const endPos = offset + valueHex.length
        if (endPos < 0 || endPos > buf.length)
            throw RangeError(`The Buffer lenght is not valid, [Require Lenght=${endPos}, Current Lenght=${buf.length}`)

        buf.write(buf.toString('hex'), 0, 'hex')
        buf.write(valueHex.toString('hex'), buf.length, 'hex')

        return offset + valueHex.length
    }

    public appendVarintNum(value: number) {
        let txnBuf: Buffer = null

        if (value < 0xFD) {
            txnBuf = Buffer.alloc(1)
            txnBuf.writeUInt8(value, 0)
        } else if (value <= 0xFFFF) {
            txnBuf = Buffer.alloc(3)
            txnBuf.writeUInt8(0xFD, 0)
            txnBuf.writeUInt16LE(value, 1)
        } else if (value <= 0xFFFFFFFF) {
            txnBuf = Buffer.alloc(5)
            txnBuf.writeUInt8(0xFE, 0)
            txnBuf.writeUInt32LE(value, 1)
        } else {
            txnBuf = Buffer.alloc(9)
            txnBuf.writeUInt8(0xFF, 0)
            BufferEx._writeUInt64LEInternal(txnBuf, value, 1)
        }

        const newLength = this._buf.length + txnBuf.length
        const newBuf = Buffer.alloc(newLength)

        newBuf.write(this._buf.toString('hex'), 0, 'hex')
        newBuf.write(txnBuf.toString('hex'), this._buf.length, 'hex')

        this._buf = newBuf

        return this._buf
    }

    public appendUInt64LE(value: number) {
        const newBuf = Buffer.alloc(this._buf.length + 8)

        BufferEx._writeUInt64LEInternal(newBuf, value, this._buf.length)

        this._buf = newBuf

        return this._buf
    }

    public readUInt64LE(offset: number) {
        const self = this._buf
        const data = Buffer.from(self.slice(offset, 8))

        return parseInt(data.reverse().toString('hex'), 16)
    }


    public append(buf: Buffer) {
        const newBuf = Buffer.alloc(this._buf.length + buf.length)

        newBuf.write(this._buf.toString('hex'), 0, 'hex')
        newBuf.write(buf.toString('hex'), this._buf.length, 'hex')

        this._buf = newBuf

        return newBuf
    }

}