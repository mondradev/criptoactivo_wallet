export default class BufferHelper {
    public static isNull(src: Buffer): boolean {
        for (const byte of src)
            if (byte != 0)
                return false

        return true
    }

    public static zero() {
        return Buffer.alloc(0)
    }

    public static fromHex(data: string) {
        return Buffer.from(data, 'hex')
    }

    private static _writeUInt64LEInternal(buf: Buffer, value: number, offset: number = 0) {
        const valueHex = Buffer.from(value.toString(16).padStart(16, '0'), 'hex').reverse()

        if (valueHex.length != 8)
            throw Error('Invalid format')

        const endPos = offset + valueHex.length

        if (endPos < 0 || endPos > buf.length)
            throw RangeError(`The Buffer lenght is not valid, [Require Lenght=${endPos}, Current Lenght=${buf.length}`)

        BufferHelper._copyInternal(valueHex, buf, offset)

        return endPos
    }

    private static _copyInternal(src: Buffer, des: Buffer, offset: number = 0) {
        if (des.length < src.length)
            throw RangeError(`Bad length of destination`)

        des.write(src.toString('hex'), offset, 'hex')
    }

    public static appendUInt8(src: Buffer, value: number) {
        const newBuf = Buffer.alloc(src.length + 1)

        BufferHelper._copyInternal(src, newBuf)
        newBuf.writeUInt8(value, src.length)

        return newBuf
    }

    public static appendHex(src: Buffer, value: string) {
        const newBuf = Buffer.alloc(src.length + value.length / 2)

        BufferHelper._copyInternal(src, newBuf)
        newBuf.write(value, src.length, 'hex')

        return newBuf
    }

    public static appendVarintNum(src: Buffer, value: number) {
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
            BufferHelper._writeUInt64LEInternal(varn, value, 1)
        }

        const newLength = src.length + varn.length
        const newBuf = Buffer.alloc(newLength)

        BufferHelper._copyInternal(src, newBuf)
        BufferHelper._copyInternal(varn, newBuf, src.length)

        return newBuf
    }

    public static appendInt32LE(src: Buffer, value: number) {
        const newBuf = Buffer.alloc(src.length + 4)

        BufferHelper._copyInternal(src, newBuf)
        newBuf.writeInt32LE(value, src.length)

        return newBuf
    }

    public static appendUInt32LE(src: Buffer, value: number) {
        const newBuf = Buffer.alloc(src.length + 4)

        BufferHelper._copyInternal(src, newBuf)
        newBuf.writeUInt32LE(value, src.length)

        return newBuf
    }

    public static appendUInt32BE(src: Buffer, value: number) {
        const newBuf = Buffer.alloc(src.length + 4)

        BufferHelper._copyInternal(src, newBuf)
        newBuf.writeUInt32BE(value, src.length)

        return newBuf
    }

    public static appendUInt64LE(src: Buffer, value: number) {
        const newBuf = Buffer.alloc(src.length + 8)

        BufferHelper._writeUInt64LEInternal(newBuf, value, src.length)

        return newBuf
    }

    public static append(src: Buffer, buf: Buffer) {
        const newBuf = Buffer.alloc(src.length + buf.length)

        newBuf.write(src.toString('hex'), 0, 'hex')
        newBuf.write(buf.toString('hex'), src.length, 'hex')

        return newBuf
    }

    public static read(src: Buffer, lenght: number, offset: number = 0) {
        const data = src.slice(offset, offset + lenght)

        return data
    }

    public static readUInt64LE(src: Buffer, offset: number = 0) {
        const data = Buffer.from(src.slice(offset, offset + 8))

        return parseInt(data.reverse().toString('hex'), 16)
    }

    public static readVarintNum(src: Buffer, offset: number = 0, sizeRef?: { size: number }) {
        const value = src.readUInt8(offset)

        if (value < 0xFD) {
            sizeRef && (sizeRef.size = 1)
            return value
        }
        else if (value == 0xFD) {
            sizeRef && (sizeRef.size = 3)
            return src.readUInt16LE(offset + 1)
        }
        else if (value == 0xFE) {
            sizeRef && (sizeRef.size = 5)
            return src.readUInt32LE(offset + 1)
        }
        else if (value == 0xFF) {
            sizeRef && (sizeRef.size = 9)
            return parseInt(src.slice(offset + 1, offset + 9).reverse().toString('hex'), 16)
        }

        throw Error('Invalid format')
    }

    public static toHex(src: Buffer) {
        return src.toString('hex')
    }

}