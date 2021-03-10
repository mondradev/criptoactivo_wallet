import UInt256 from "../../primitives/uint256";

export default class BlockLocator {
    private _have: Array<UInt256>

    public constructor()
    public constructor(haveIn: Array<UInt256>)

    public constructor(haveIn?: Array<UInt256>) {
        if (haveIn != null)
            this._have = new Array(...haveIn)
        else
            this.setNull()
    }

    public setNull() {
        this._have = this._have || new Array()
        this._have.clear()
    }

    public isNull() {
        return this._have.length == 0
    }

}