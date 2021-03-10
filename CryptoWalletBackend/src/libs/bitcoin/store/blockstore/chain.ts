import UInt256 from "../../primitives/uint256";
import BlockIndex from "./blockindex";
import BlockLocator from "./blocklocator";


export default class Chain {

    private _chain: Array<BlockIndex>

    public get genesis(): BlockIndex {
        return this._chain.length > 0 ? this._chain[0] : null
    }

    public get tip(): BlockIndex {
        return this._chain.length > 0 ? this._chain.last() : null
    }

    public get(height: number): BlockIndex {
        if (height < 0 || height >= this._chain.length)
            return null

        return this._chain[height]
    }

    public contains(index: BlockIndex): boolean {
        return this.get(index.height) === index
    }

    public next(index: BlockIndex): BlockIndex {
        if (this.contains(index))
            return this.get(index.height + 1)
        else
            return null
    }

    public get height(): number {
        return this._chain.length - 1
    }

    public set tip(index: BlockIndex) {
        if (index == null) {
            this._chain.clear()
            return
        }

        while (index && this._chain[index.height] != index) {
            this._chain[index.height] = index
            index = index.prevBlock
        }
    }

    public getLocator(index?: BlockIndex): BlockLocator {
        let step = 1
        const have = new Array<UInt256>()

        if (!index)
            index = this.tip
        while (index) {
            have.push(index.blockHash)

            if (index.height == 0) break

            let height = Math.max(index.height - step, 0)
            
            if (this.contains(index))
                index = this.get(height)
            else
                index = index.getAncestor(height)

            if (have.length > 10)
                step *= 2
        }

        return new BlockLocator(have)
    }

    public findFork(index: BlockIndex): BlockIndex {
        if (index == null) return null

        if (index.height > this.height)
            index = index.getAncestor(this.height)

        while (index && !this.contains(index))
            index = index.prevBlock

        return index
    }

    public findEarliestAtLeast(time: number, height: number) {
        const lower = this._chain.reduce((previous, current) => {
            if (current.blockTimeMax < time || current.height < height)
                previous.push(current)

            return previous
        }, new Array<BlockIndex>())

        return lower.length == 0 ? null : lower
    }

}