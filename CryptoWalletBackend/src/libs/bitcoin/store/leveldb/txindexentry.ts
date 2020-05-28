import BufferHelper from "../../../../utils/bufferhelper";

export class TxindexEntry {

    public constructor(private _blockHash: Buffer, private _index: number) { }

    public get blockHash() { return this._blockHash; }
    public get index() { return this._index; }

    public toBuffer(): Buffer {
        return BufferHelper.zero()
            .appendUInt32LE(this._index)
            .append(this._blockHash);
    }
    
    public static fromBuffer(data: Buffer): TxindexEntry {
        return new TxindexEntry(data.read(32, 4), data.readUInt32LE(0));
    }
}
