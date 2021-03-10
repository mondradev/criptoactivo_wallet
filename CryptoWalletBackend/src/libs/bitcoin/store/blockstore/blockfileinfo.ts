import ISerializable, { Stream } from "../../primitives/serializable"

export default class BlockFileInfo implements ISerializable {

    public static deserialize(stream: Stream): BlockFileInfo {
        const bfl = new BlockFileInfo()
        bfl._blocks = stream.deappendVarInt()
        bfl._size = stream.deappendVarInt()
        bfl._undoSize = stream.deappendVarInt()
        bfl._heightFirst = stream.deappendVarInt()
        bfl._heightLast = stream.deappendVarInt()
        bfl._timeFirst = stream.deappendVarInt()
        bfl._timeLast = stream.deappendVarInt()

        return bfl
    }

    private _blocks: number
    private _size: number
    private _undoSize: number
    private _heightFirst: number
    private _heightLast: number
    private _timeFirst: number
    private _timeLast: number

    public constructor() {
        this.setNull()
    }

    public serialize(stream: Stream): void {
        stream.appendVarInt(this._blocks)
            .appendVarInt(this._size)
            .appendVarInt(this._undoSize)
            .appendVarInt(this._heightFirst)
            .appendVarInt(this._heightLast)
            .appendVarInt(this._timeFirst)
            .appendVarInt(this._timeLast)
    }

    public setNull() {
        this._blocks = 0
        this._size = 0
        this._undoSize = 0
        this._heightFirst = 0
        this._heightLast = 0
        this._timeFirst = 0
        this._timeLast = 0
    }

    public addBlock(heightIn: number, timeIn: number) {
        if (this._blocks == 0 || this._heightFirst > heightIn)
            this._heightFirst = heightIn
        if (this._blocks == 0 || this._timeFirst > timeIn)
            this._timeFirst = timeIn
        this._blocks++
        if (heightIn > this._heightLast)
            this._heightLast = heightIn
        if (timeIn > this._timeLast)
            this._timeLast = timeIn
    }

    public toString(): string {
        return "BlockFileInfo (blocks=" + this._blocks
            + ", size=" + this._size
            + ", heights=" + this._heightFirst
            + "..." + this._heightLast
            + ", times=" + new Date(this._timeFirst * 1000).toLocaleString()
            + "..." + new Date(this._timeLast * 1000).toLocaleString()
            + ")"
    }
}