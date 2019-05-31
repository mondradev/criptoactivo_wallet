import Bitcoin from "../src/libs/bitcoin"
import { BitcoinWalletService } from '../src/service/BitcoinWalletService'
import { Transaction } from 'bitcore-lib'

import 'mocha'
import { expect } from 'chai'

describe('Bitcoin - Node', () => {
    it('Connect to network', async () => {
        await Bitcoin.Network.connect()
        expect(Bitcoin.Network.connected).to.be.true
    }).timeout(10000)

    it('Get BestHeight from peers', () => {
        expect(Bitcoin.Network.bestHeight).to.greaterThan(0)
    }).timeout(10000)

    it('Get headers from some peer', async () => {
        let headers = await Bitcoin.Network.getHeaders([Array(65).join('0')])
        expect(headers).length(2000)
    }).timeout(10000)

    it('Get block from some peer', async () => {
        expect(await Bitcoin.Network.getBlock('00000000add42ab5ebe18c689fba7375f7f8b13c1aad57781c277e3bbe83811b')).have.property('hash')
    }).timeout(10000)

    it('Get 2000 blocks from some peer', async () => {
        let headers = await Bitcoin.Network.getHeaders([Array(65).join('0')])
        expect(headers).length(2000)

        let downloader = Bitcoin.Network.getDownloader(headers)
        let i = 0

        while (await downloader.next()) i++

        expect(i).to.be.equal(headers.length)

    }).timeout(60000)

    it('Disconnect from network', async () => {
        await Bitcoin.Network.disconnect()
        expect(Bitcoin.Network.connected).to.be.false
    }).timeout(10000)
})


describe('Bitcoin - Wallet API', () => {
    it('Request with GET method', async () => {
        expect(await new Promise<any>((resolve) => {
            BitcoinWalletService({ method: 'GET', body: null, params: null },
                (statusCode, data) => {
                    resolve(data.payload)
                })
        })).to.be.null
    })

    it('Request with POST method while Bitcoin Sync', async () => {
        expect(await new Promise<any>((resolve) => {
            BitcoinWalletService({ method: 'POST', body: null, params: { request: 'tx' } },
                (statusCode, data) => {
                    resolve(data.message)
                })
        })).to.be.equals("Bitcoin Synchronizing")
    })

    it('Get Tx with hash "451740bdd473c33c2ad9213d14137887de2d8682d1af83632344bb4ac412d488"', async () => {
        Bitcoin.Blockchain.getTxRaw = async (txid: Buffer) => Buffer.from('01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0e045c354a4d017a062f503253482fffffffff0100f2052a010000002321027d886b785ddf10f085ce709810750fe3dc938c8dbe40d96b07f1a7ab909cb05fac00000000', 'hex')

        const txRaw = await Bitcoin.Wallet.getRawTransaction('451740bdd473c33c2ad9213d14137887de2d8682d1af83632344bb4ac412d488');
        const tx = new Transaction(txRaw)

        expect(tx.hash)
            .to.be.equals('451740bdd473c33c2ad9213d14137887de2d8682d1af83632344bb4ac412d488')
    })
})