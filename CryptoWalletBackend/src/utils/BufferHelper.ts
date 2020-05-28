declare global {

    interface Buffer {
        getSubBuffers(offset: number, size: number): Buffer[]
        toReverseHex(): string
        toHex(): string
        append(buffer: Buffer): Buffer
        appendHex(hex: string): Buffer,
        appendInt32LE(int32: number): Buffer
        appendUInt16LE(int16: number): Buffer
        appendUInt32LE(int32: number): Buffer
        appendUInt8(uint8: number): Buffer
        appendUInt16BE(uint16: number): Buffer
        appendUInt32BE(uint32: number): Buffer
        appendUInt64LE(uint64: number): Buffer
        appendVarintNum(num: number): Buffer
        read(lenght: number, offset?: number): Buffer
        readHex(lenght: number, offset?: number): string
        readUInt64LE(offset: number): number
        readVarintNum(offset: number): number

    }
}

Buffer.prototype.getSubBuffers = function (offset: number, size: number): Buffer[] {
    let src = this
    let buffer = src.slice(offset)
    let buffers = new Array<Buffer>()

    while (buffer.length >= size) {
        buffers.push(buffer.slice(0, size))
        buffer = buffer.slice(size)
    }

    if (buffer && buffer.length > 0)
        buffers.push(buffer)

    return buffers
}

Buffer.prototype.toReverseHex = function (): string {
    const buf = Buffer.from(this)
    return buf.reverse().toString('hex')
}

Buffer.prototype.toHex = function (): string {
    return this.toString('hex')
}

Buffer.prototype.readHex = function (lenght: number, offset: number = 0): string {
    return this.slice(offset, offset + lenght).toString('hex')
}

function _writeUInt64LEInternal(buf: Buffer, value: number, offset: number = 0) {
    const valueHex = Buffer.from(value.toString(16).padStart(16, '0'), 'hex').reverse()

    if (valueHex.length != 8)
        throw Error('Invalid format')

    const endPos = offset + valueHex.length

    if (endPos < 0 || endPos > buf.length)
        throw RangeError(`The Buffer lenght is not valid, [Require Lenght=${endPos}, Current Lenght=${buf.length}`)

    _copyInternal(valueHex, buf, offset)

    return endPos
}

function _copyInternal(src: Buffer, des: Buffer, offset: number = 0) {
    if (des.length < src.length)
        throw RangeError(`Bad length of destination`)

    des.write(src.toHex(), offset, 'hex')
}

Buffer.prototype.appendUInt8 = function (value: number) {
    const src = this as Buffer
    const newBuf = Buffer.alloc(src.length + 1)

    _copyInternal(src, newBuf)
    newBuf.writeUInt8(value, src.length)

    return newBuf
}

Buffer.prototype.appendHex = function (value: string) {
    const src = this as Buffer
    const newBuf = Buffer.alloc(src.length + value.length / 2)

    _copyInternal(src, newBuf)
    newBuf.write(value, src.length, 'hex')

    return newBuf
}

Buffer.prototype.appendVarintNum = function (value: number) {
    const src = this as Buffer
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
        _writeUInt64LEInternal(varn, value, 1)
    }

    const newLength = src.length + varn.length
    const newBuf = Buffer.alloc(newLength)

    _copyInternal(src, newBuf)
    _copyInternal(varn, newBuf, src.length)

    return newBuf
}

Buffer.prototype.appendInt32LE = function (value: number) {
    const src = this as Buffer
    const newBuf = Buffer.alloc(src.length + 4)

    _copyInternal(src, newBuf)
    newBuf.writeInt32LE(value, src.length)

    return newBuf
}


Buffer.prototype.appendUInt16BE = function (value: number) {
    const src = this as Buffer
    const newBuf = Buffer.alloc(src.length + 2)

    _copyInternal(src, newBuf)
    newBuf.writeUInt16BE(value, src.length)

    return newBuf
}

Buffer.prototype.appendUInt32BE = function (value: number) {
    const src = this as Buffer
    const newBuf = Buffer.alloc(src.length + 4)

    _copyInternal(src, newBuf)
    newBuf.writeUInt32BE(value, src.length)

    return newBuf
}

Buffer.prototype.appendUInt16LE = function (value: number) {
    const src = this as Buffer
    const newBuf = Buffer.alloc(src.length + 2)

    _copyInternal(src, newBuf)
    newBuf.writeUInt16LE(value, src.length)

    return newBuf
}

Buffer.prototype.appendUInt32LE = function (value: number) {
    const src = this as Buffer
    const newBuf = Buffer.alloc(src.length + 4)

    _copyInternal(src, newBuf)
    newBuf.writeUInt32LE(value, src.length)

    return newBuf
}

Buffer.prototype.appendUInt64LE = function (value: number) {
    const src = this as Buffer
    const newBuf = Buffer.alloc(src.length + 8)

    _writeUInt64LEInternal(newBuf, value, src.length)

    return newBuf
}

Buffer.prototype.append = function (buf: Buffer) {
    const src = this as Buffer
    const newBuf = Buffer.alloc(src.length + buf.length)

    newBuf.write(src.toString('hex'), 0, 'hex')
    newBuf.write(buf.toString('hex'), src.length, 'hex')

    return newBuf
}

Buffer.prototype.read = function (lenght: number, offset: number = 0) {
    const src = this as Buffer
    const data = src.slice(offset, offset + lenght)

    return data
}

Buffer.prototype.readUInt64LE = function (offset: number = 0) {
    const src = this as Buffer
    const data = Buffer.from(src.slice(offset, offset + 8))

    return parseInt(data.reverse().toString('hex'), 16)
}

Buffer.prototype.readVarintNum = function (offset: number = 0, sizeRef?: { size: number }) {
    const src = this as Buffer
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
        return parseInt(src.slice(offset + 1, offset + 9).toReverseHex(), 16)
    }

    throw Error('Invalid format')
}

export default class BufferHelper {

    public static numberToBuffer(value: number, size: (1 | 2 | 4) = 4, mode: 'be' | 'le'): Buffer {
        let buff = this.zero()

        if (mode === 'be' && size >= 2)
            switch (size) {
                case 4: return buff.appendUInt32BE(value)
                case 2: return buff.appendUInt16BE(value)
            }
        else if (mode === 'le' && size >= 2)
            switch (size) {
                case 4: return buff.appendUInt32LE(value)
                case 2: return buff.appendUInt16LE(value)
            }
        else if (size == 2)
            return buff.appendUInt8(value)
    }

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
}