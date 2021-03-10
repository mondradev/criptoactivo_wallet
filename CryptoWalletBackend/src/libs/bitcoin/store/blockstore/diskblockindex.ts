import UInt256 from "../../primitives/uint256";
import BlockIndex from "./blockindex";


export default class DiskBlockIndex extends BlockIndex {

    private _hashPrev: UInt256

    public constructor()
    public constructor(index: BlockIndex)

    public constructor(index?: BlockIndex) {
        super(index)
        this._hashPrev = (this._prev ? this._prev.blockHash : UInt256.null())
    }

    public get blockHash(): UInt256 {
        return this.blockHeader.hash
    }

    public toString(): string {
        return `DiskBlockIndex (${super.toString()}, hashPrev=${this._hashPrev.toHex()})`
    }
}