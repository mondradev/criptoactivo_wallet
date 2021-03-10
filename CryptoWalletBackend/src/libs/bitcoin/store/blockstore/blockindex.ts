import { threadId } from "worker_threads";
import { Block, BlockHeader } from "../../primitives/blocks";
import UInt256 from "../../primitives/uint256";
import FlatFilePos from "./flatfilepos";

export enum BlockStatus {
    //! Unused.
    BLOCK_VALID_UNKNOWN = 0,

    //! Reserved (was BLOCK_VALID_HEADER).
    BLOCK_VALID_RESERVED = 1,

    //! All parent headers found, difficulty matches, timestamp >= median previous, checkpoint. Implies all parents
    //! are also at least TREE.
    BLOCK_VALID_TREE = 2,

    /**
     * Only first tx is coinbase, 2 <= coinbase input script length <= 100, transactions valid, no duplicate txids,
     * sigops, size, merkle root. Implies all parents are at least TREE but not necessarily TRANSACTIONS. When all
     * parent blocks also have TRANSACTIONS, CBlockIndex::nChainTx will be set.
     */
    BLOCK_VALID_TRANSACTIONS = 3,

    //! Outputs do not overspend inputs, no double spends, coinbase output ok, no immature coinbase spends, BIP30.
    //! Implies all parents are also at least CHAIN.
    BLOCK_VALID_CHAIN = 4,

    //! Scripts & signatures ok. Implies all parents are also at least SCRIPTS.
    BLOCK_VALID_SCRIPTS = 5,

    //! All validity bits.
    BLOCK_VALID_MASK = BLOCK_VALID_RESERVED | BLOCK_VALID_TREE | BLOCK_VALID_TRANSACTIONS |
    BLOCK_VALID_CHAIN | BLOCK_VALID_SCRIPTS,

    BLOCK_HAVE_DATA = 8, //!< full block available in blk*.dat
    BLOCK_HAVE_UNDO = 16, //!< undo data available in rev*.dat
    BLOCK_HAVE_MASK = BLOCK_HAVE_DATA | BLOCK_HAVE_UNDO,

    BLOCK_FAILED_VALID = 32, //!< stage after last reached validness failed
    BLOCK_FAILED_CHILD = 64, //!< descends from failed block
    BLOCK_FAILED_MASK = BLOCK_FAILED_VALID | BLOCK_FAILED_CHILD,

    BLOCK_OPT_WITNESS = 128, //!< block data in blk*.data was received with a witness-enforcing client

}

export default class BlockIndex {
    protected _prev: BlockIndex

    private _hashBlock: UInt256
    private _skip: BlockIndex
    private _height: number
    private _file: number
    private _dataPos: number
    private _undoPos: number
    private _txs: number
    private _status: BlockStatus

    private _version: number
    private _hashMerkleRoot: UInt256
    private _time: number
    private _bits: number
    private _nonce: number

    private _chainTxs: number
    private _sequnceId: number
    private _timeMax: number

    public constructor()
    public constructor(block: BlockHeader)
    public constructor(index: BlockIndex)

    public constructor(block?: BlockHeader | BlockIndex) {
        if (block instanceof BlockIndex) {
            this._hashBlock = block._hashBlock
            this._prev = block._prev
            this._skip = block._skip
            this._height = block._height
            this._file = block._file
            this._dataPos = block._dataPos
            this._undoPos = block._undoPos
            this._txs = block._txs
            this._status = block._status
            this._version = block._version
            this._hashMerkleRoot = block._hashMerkleRoot
            this._time = block._time
            this._bits = block._bits
            this._nonce = block._nonce
            this._chainTxs = block._chainTxs
            this._sequnceId = block._sequnceId
            this._timeMax = block._timeMax
        } else if (block instanceof BlockHeader) {
            this._version = block.version
            this._hashMerkleRoot = block.hashMerkleRoot
            this._time = block.time
            this._bits = block.bits
            this._nonce = block.nonce
        }
    }

    public get prevBlock(): BlockIndex {
        return this._prev
    }


    public get blockPos(): FlatFilePos {
        const ret = new FlatFilePos()

        if (this._status & BlockStatus.BLOCK_HAVE_DATA) {
            ret.file = this._file
            ret.pos = this._dataPos
        }

        return ret
    }

    public get undoPos(): FlatFilePos {
        const ret = new FlatFilePos()

        if (this._status & BlockStatus.BLOCK_HAVE_UNDO) {
            ret.file = this._file
            ret.pos = this._dataPos
        }

        return ret
    }

    public get height(): number {
        return this._height
    }

    public get blockHeader(): BlockHeader {
        const block = new BlockHeader()
        block.version = this._version

        if (this._prev)
            block.hashPrevBlock = this._prev.blockHash
        block.hashMerkleRoot = this._hashMerkleRoot
        block.time = this._time
        block.bits = this._bits
        block.nonce = this._nonce

        return block
    }

    public get blockHash(): UInt256 {
        return this._hashBlock
    }

    public haveTxsDownloaded(): boolean {
        return this._chainTxs > 0
    }

    public get blockTime(): number {
        return this._time
    }

    public get blockTimeMax(): number {
        return this._timeMax
    }

    private static _medianTimeSpan = 11

    private _orderedMedianTime(median: number[]): Array<number> {
        const ordered = median.filter(v => typeof v !== 'undefined')
            .sort((a, b) => a - b)

        return ordered
    }

    public get medianTimePast(): number {
        const median = new Array<number>(BlockIndex._medianTimeSpan)
        let index: BlockIndex = this

        for (let i = 0; i < BlockIndex._medianTimeSpan && index; i++, index = index._prev)
            median[i] = index.blockTime

        const orderedMedian = this._orderedMedianTime(median)

        return orderedMedian[Math.round(orderedMedian.length / 2)]
    }

    public toString(): string {
        return `BlockIndex (prev=${this._prev.blockHash.toHex().slice(0, 8)
            }, height=${this._height}, merkle=${this._hashMerkleRoot.toHex()}, hashBlock=${this.blockHash.toHex()})`
    }

    public isValid(upTo: BlockStatus = BlockStatus.BLOCK_VALID_TRANSACTIONS): boolean {
        if (this._status & BlockStatus.BLOCK_FAILED_MASK)
            return false
        return ((this._status & BlockStatus.BLOCK_VALID_MASK) >= upTo)
    }

    public raiseValidity(upTo: BlockStatus) {
        if (!(upTo & ~BlockStatus.BLOCK_VALID_MASK))
            throw new TypeError("Flag don't allowed")

        if ((this._status & BlockStatus.BLOCK_VALID_MASK) < upTo) {
            this._status = (this._status & ~BlockStatus.BLOCK_VALID_MASK) | upTo
            return true
        }

        return false
    }

    public buildSkip(): void {
        if (this._prev)
            this._skip = this._prev.getAncestor(BlockIndex._getSkipHeight(this._height))
    }

    public getAncestor(height: number): BlockIndex {
        if (height > this._height || height < 0)
            return null

        let indexWalk: BlockIndex = this
        let heightWalk = this._height

        while (heightWalk > height) {
            let heightSkip = BlockIndex._getSkipHeight(heightWalk)
            let heightSkipPrev = BlockIndex._getSkipHeight(heightWalk - 1)

            if (indexWalk._skip != null &&
                (heightSkip == height ||
                    (heightSkip > height && !(heightSkipPrev < heightSkip - 2 &&
                        heightSkipPrev >= height)))) {
                indexWalk = indexWalk._skip
                heightWalk = heightSkip
            } else {
                if (indexWalk._prev == null) throw new TypeError("Prev is null")
                indexWalk = indexWalk._prev
                heightWalk--
            }
        }

        return indexWalk
    }

    private static _invertLowestOne(n: number) { return n & (n - 1) }

    private static _getSkipHeight(height: number) {
        if (height < 2) return 0

        return (height & 1)
            ? BlockIndex._invertLowestOne(BlockIndex._invertLowestOne(height - 1)) + 1
            : BlockIndex._invertLowestOne(height)
    }
}