import BufferHelper from "../../../../utils/bufferhelper";

export class OutputEntry {

    public constructor(private _txid: Buffer, private _index: number, private _blockHash: Buffer) { }

    public get txid() { return this._txid; }
    public get blockHash() { return this._blockHash; }
    public get index() { return this._index; }
    public get outpoint() { return BufferHelper.zero().append(this._txid).appendUInt32BE(this._index); }

    public toBuffer(): Buffer {
        return BufferHelper.zero().append(this._txid).appendUInt32LE(this._index).append(this._blockHash);
    }
    
    public static fromBuffer(data: Buffer): OutputEntry {
        if (data.length < 68) 
            throw new Error("Data isn't valid, size is less than 68")

        return new OutputEntry(data.read(32), data.readUInt32LE(32), data.read(32, 36));
    }
}
