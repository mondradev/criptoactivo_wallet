import { BitcoinWalletService } from '../src/service/BitcoinWalletService'

import 'mocha'
import chai, { expect } from 'chai'
import chaiAsPromised from 'chai-as-promised'
import Bitcoin from '../src/libs/bitcoin';

chai.use(chaiAsPromised)
chai.should()

// describe('Bitcoin', () => {

//     describe('Node', () => {
//         it('Connect to network', () => {
//             return Bitcoin.Network.connect().should.eventually.be.true
//         }).timeout(10000)

//         it('Get BestHeight from peers', () => {
//             expect(Bitcoin.Network.bestHeight).to.greaterThan(0)
//         }).timeout(10000)

//         it('Get hashes of new blocks from peers', () => {
//             return Bitcoin.Network.getHashes([Array(65).join('0')]).should.eventually.have.length(2000)
//         }).timeout(10000)

//         it('Get block from peers', async () => {
//             Bitcoin.Network.getBlock('00000000add42ab5ebe18c689fba7375f7f8b13c1aad57781c277e3bbe83811b').should.eventually.to.be.not.null
//         }).timeout(10000)

//         it('Get 2000 blocks from peers', async () => {
//             let hashes = await Bitcoin.Network.getHashes([Array(65).join('0')])

//             let downloader = Bitcoin.Network.getDownloader(hashes)
//             let i = 0

//             for (const hash of hashes)
//                 if ((await downloader.get(hash)).hash === hash)
//                     i++

//             expect(i).to.be.equal(hashes.length).and.to.be.greaterThan(0)

//         }).timeout(60000)

//         it('Disconnect from network', () => {
//             return Bitcoin.Network.disconnect().should.eventually.fulfilled
//         }).timeout(10000)
//     })

//     describe('Wallet API', () => {
//         it('Request with GET method', () => {
//             return new Promise<any>((resolve) =>
//                 BitcoinWalletService({ method: 'GET', body: null, params: null }, (statusCode, data) => resolve(data.payload))
//             ).should.eventually.to.be.null
//         })

//         it('Request with POST method while Bitcoin Sync', () => {
//             return new Promise<string>((resolve) =>
//                 BitcoinWalletService({ method: 'POST', body: null, params: { request: 'tx' } }, (statusCode, data) => resolve(data.message))
//             ).should.eventually.to.be.equals("Bitcoin Synchronizing")
//         })

//         it('Get Tx with hash "451740bdd473c33c2ad9213d14137887de2d8682d1af83632344bb4ac412d488"', () => {
//             const txRaw = '01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0e045c354a4d017a062f503253482fffffffff0100f2052a010000002321027d886b785ddf10f085ce709810750fe3dc938c8dbe40d96b07f1a7ab909cb05fac00000000'
//             Bitcoin.Blockchain.getTxRaw = async (txid: Buffer) => Buffer.from(txRaw, 'hex') // Evitamos consultar la DB

//             return Bitcoin.Wallet.getRawTransaction('451740bdd473c33c2ad9213d14137887de2d8682d1af83632344bb4ac412d488')
//                 .should.eventually.to.equals(txRaw)
//         })
//     })

//     after(async () => {
//         await Bitcoin.Stores.stopService()
//     })

// })

import {Network} from '../src/libs/bitcoin/network/network'
import Constants from '../src/libs/bitcoin/constants';

let net = new Network({
    getLocators: (tip)=> {
        return {
            starts: [Constants.NULL_HASH],
            stop : Constants.NULL_HASH
        }
    },
    getTip : () => {
        return {
            height : 0,
            blockHash : Constants.NULL_HASH
        }
    }
});

net.connect().then(net.startSync)