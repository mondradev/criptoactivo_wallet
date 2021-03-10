import BufferHelper from "../../../../utils/bufferhelper";

export class AddrindexEntry {

    public constructor(private _addr: Buffer, private _txid: Buffer, private _blockHash: Buffer, private _index: number) { }
    
    public get addr() { return this._addr; }
    public get txid() { return this._txid; }
    public get blockHash() { return this._blockHash; }
    public get index() { return this._index; }
    
    public toBuffer(): Buffer {
        return Buffer.from(this.addr)
            .append(this._txid)
            .appendUInt32LE(this._index)
            .append(this._blockHash);
    }

    public static fromBuffer(data: Buffer): AddrindexEntry {
        return new AddrindexEntry(data.read(21), data.read(32, 21), data.read(32, 57), data.readUInt32LE(53));
    }
}
