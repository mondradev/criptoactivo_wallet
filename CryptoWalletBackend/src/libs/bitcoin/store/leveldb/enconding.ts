import { Encoder } from "./encoder"
import { TxindexEntry } from "./txindexentry"
import { AddrindexEntry } from "./addrindexentry"
import { ChainTip } from "../istore"
import { Block } from "bitcore-lib"
import { OutputEntry } from "./outputentry"

import BufferHelper from "../../../../utils/bufferhelper"

export const Enconding = {
    Txindex: new Encoder<TxindexEntry>('t',
        {
            encode: (entry: TxindexEntry) => entry.toBuffer(),
            decode: (data: Buffer) => TxindexEntry.fromBuffer(data)
        }),

    Addrindex: new Encoder<AddrindexEntry>('a',
        {
            encode: (entry: AddrindexEntry) => entry.toBuffer(),
            decode: (data: Buffer) => AddrindexEntry.fromBuffer(data)
        }),

    TxAddrIndex: new Encoder<Buffer>('A', {
        encode: (value: Buffer) => value,
        decode: (data: Buffer) => data
    }),

    Tip: new Encoder<ChainTip>('c', {
        encode: (value: ChainTip) => {
            return BufferHelper.zero()
                .appendUInt32LE(value.height)
                .appendUInt32LE(value.time)
                .appendUInt32LE(value.txn)
                .appendHex(value.hash)
        },
        decode: (data: Buffer) => {
            return {
                height: data.readUInt32LE(0),
                time: data.readUInt32LE(4),
                txn: data.readUInt32LE(8),
                hash: data.readHex(32, 12)
            }
        }
    }),

    BlockByHashIdx: new Encoder<Block>('B', {
        encode: (block: Block) => block.toBuffer(),
        decode: (data: Buffer) => Block.fromBuffer(data)
    }),

    BlockHeightByHashIdx: new Encoder<number>('H', {
        encode: (height: number) => BufferHelper.numberToBuffer(height, 4, 'le'),
        decode: (data: Buffer) => data.readUInt32LE(0)
    }),

    BlockHashByHeightIdx: new Encoder<Buffer>('h', {
        encode: (hash: string | Buffer) => hash instanceof Buffer ? hash : BufferHelper.fromHex(hash),
        decode: (data: Buffer) => data
    }),

    UndoTxo: new Encoder<OutputEntry[]>('U', {
        encode: (values: OutputEntry[]) => values.reduce((buffer, output) => buffer.append(output.toBuffer()), BufferHelper.zero()),
        decode: (data: Buffer) => {
            const outputs = new Array<OutputEntry>()

            if (data.length > 0)
                for (let i = 0; i < data.length / 68; i++)
                    outputs.push(OutputEntry.fromBuffer(data.slice(i * 68, i * 68 + 68)))

            return outputs
        }
    }),

    CoinTxo: new Encoder<OutputEntry>('c', { // TODO: Change from c to C
        encode: (value: OutputEntry) => value.toBuffer(),
        decode: (data: Buffer) => OutputEntry.fromBuffer(data)
    })
}