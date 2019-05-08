import { BlockHeader, Block } from "../libs/basedbtc/blocks/block"
import { Hash256 } from "../libs/basedbtc/hash";


const b = new Block()

b['_bits'] = 0x1a44b9f2
b['_time'] = 1305998791
b['_nonce'] = 2504433986
b['_version'] = 1
b['_hashPrevBlock'] = Hash256.fromHex('00000000000008a3a41b85b8b29ad444def299fee21793cd8b9e567eab02cd81')
b['_hashMerkleRoot'] = Hash256.fromHex('2b12fcf1b09288fcaff797d71e950e71ae42b91e8bdb2304758dfcffc2b620e3')

const txs = b['_txs']

while (txs.length < 100)
    txs.push(Math.random())

const serialize = b.serialize()
const header = BlockHeader.deserialize(serialize)

console.log(b.Hash.toString())
console.log(serialize.toString('hex'))
console.log(header.Hash.toString())