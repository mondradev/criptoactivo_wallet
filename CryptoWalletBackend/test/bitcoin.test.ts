import { expect } from 'chai'
import 'mocha'
import { MainParams, Test3Params } from '../src/libs/bitcoin/params'
import Block from '../src/libs/bitcoin/primitives/blocks/block'
import ISerializable, { Stream } from '../src/libs/bitcoin/primitives/serializable'
import Transaction from '../src/libs/bitcoin/primitives/tx/transaction'
import UInt256 from '../src/libs/bitcoin/primitives/uint256'
import BufferHelper from '../src/utils/bufferhelper'
import blockDataTest from './resources/block.json'
import txDataTest from './resources/tx.json'




class ItemSerializable implements ISerializable {

    public value32: number
    public value64: number
    public value256: UInt256

    public inspect() {
        return `ItemSerializable[value32=${this.value32}, value64=${this.value64}, value256=${this.value256}]`
    }

    constructor(value32: number, value64: number, value256: UInt256) {
        this.value32 = value32
        this.value64 = value64
        this.value256 = value256
    }

    public serialize(stream: Stream): void {
        stream
            .appendUInt32(this.value32)
            .appendUInt64(this.value64)
            .appendUInt256(this.value256)
    }

    public static deserialize(stream: Stream): ItemSerializable {
        return new ItemSerializable(
            stream.deappendUInt32(),
            stream.deappendUInt64(),
            stream.deappendUInt256())
    }

}

function toFriendlyString(value: number) {
    let i = 0
    let size = value

    while (size > 1024) { size /= 1024; i++ }

    const units = ['B', 'KB', 'MB']

    return size.toFixed(2) + " " + units[i]
}

describe("Bitcoin tests", () => {
    describe("Chain parameters", () => {
        it("Mainnet Genesis", () => expect(MainParams.hashGenesis.value)
            .to.eql(BufferHelper.fromHex("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f").reverse()))

        it("Testnet3 Genesis", () => expect(Test3Params.hashGenesis.value)
            .to.eql(BufferHelper.fromHex("000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943").reverse()))
    })

    describe("Serialize methods", () => {
        const stream = Stream.empty()
        const a = 10
        const b = 200
        const c = new UInt256(BufferHelper.fromHex('90129910'))

        it("Append numbers", () => {
            stream.appendUInt32(a)
                .appendUInt64(b)
                .appendUInt256(c)

            expect(stream.size).to.eql(44)
        })

        it("Deappend numbers", () => {
            expect(stream.size, "Size not equals to 44").to.eql(44)

            expect(stream.deappendUInt32(), "Value not equals to 10").to.eql(a)
            expect(stream.deappendUInt64(), "Value not equals to 200").to.eql(b)
            expect(UInt256.equal(c, stream.deappendUInt256()))
        })

        const generator = function () {
            return Math.round(1 + Math.random() * 100000)
        }

        it("Vector append and deappend", () => {
            const vector = [
                new ItemSerializable(generator(), generator(), new UInt256(generator().toLocaleString())),
                new ItemSerializable(generator(), generator(), new UInt256(generator().toLocaleString())),
                new ItemSerializable(generator(), generator(), new UInt256(generator().toLocaleString())),
                new ItemSerializable(generator(), generator(), new UInt256(generator().toLocaleString()))
            ]

            const stream = Stream.empty().appendVector(vector)
            const vector2 = stream.deappendVector(ItemSerializable.deserialize)

            expect(vector2[0].value64)
                .to.equals(vector[0].value64)
        })
    })

    describe('API UInt256', () => {

        it('Get number from instance', () => {
            expect(UInt256
                .fromNumber(0xfffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff)
                .toNumber()).to.equals(7.237005577332262e+75)
        })

    })

    describe('Bitcoin Primitives', () => {
        it('Block with segwit support', () => {
            const stream = Stream.fromBuffer(BufferHelper.fromHex(blockDataTest.segwit))
            const block = Block.deserialize(stream)

            expect(block.hash.toReverseHex()).to.equals('000000000000000000079d887c89e3fa21286c8521242aaddf8ac7b0dd474bac')
        }).timeout(5000)

        it('Legacy block ', () => {
            const stream = Stream.fromBuffer(BufferHelper.fromHex(blockDataTest.legacy))
            const block = Block.deserialize(stream)

            expect(block.hash.toReverseHex()).to.equals('000000000000034a7dedef4a161fa058a2d67a173a90155f3a2fe6fc132e0ebf')
        })

        it('Transaction with segwit support', () => {
            const stream = Stream.fromBuffer(BufferHelper.fromHex(txDataTest.segwit))
            const tx = Transaction.deserialize(stream)

            stream.clear()
            tx.serialize(stream)

            expect(tx.hasWitness()).to.be.true
            expect(tx.witnessHash.toReverseHex()).to.equals("ef3a45e1818706193491ee6a7d3484fb391771a05831a8d24888a065190ccbfb")
            expect(tx.hash.toReverseHex()).to.equals("b96286babb791de38d63e1baed3d2bbe321c015bc3a9535fc70d45a7753329e1")
        })

        it('Legacy transaction', () => {
            const stream = Stream.fromBuffer(BufferHelper.fromHex(txDataTest.legacy))
            const tx = Transaction.deserialize(stream)

            expect(tx.hasWitness()).to.be.false
            expect(tx.hash.toReverseHex()).to.equals("25ca9ce6e118225fd0e95febe6d835cdb95bf9e57aa2ca99ea2f140a86ca334f")
        })
    })

})