import BufferHelper from "../../../../utils/bufferhelper";

export class Encoder<TValue> {
    
    public constructor(private _id: string, private _valueEnconder: {
        encode: (value: TValue) => Buffer;
        decode: (data: Buffer) => TValue;
    }) { }

    public key(keyData?: Buffer): Buffer {
        const bKey = BufferHelper.zero()
            .appendUInt8(this._id.charCodeAt(0));
        return keyData ? bKey.append(keyData) : bKey;
    }

    public encode(value: TValue): Buffer {
        return this._valueEnconder.encode(value);
    }

    public decode(data: Buffer): TValue {
        return this._valueEnconder.decode(data);
    }

    public decodeKey(data: Buffer): Buffer {
        return data.slice(1);
    }
}
