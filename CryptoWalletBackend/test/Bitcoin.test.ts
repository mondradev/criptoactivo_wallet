import Bitcoin from "../src/libs/bitcoin"

import 'mocha'
import { expect } from 'chai'

describe('Bitcoin', () => {
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

    }).timeout(10000)

    it('Disconnect from network', async () => {
        await Bitcoin.Network.disconnect()
        expect(Bitcoin.Network.connected).to.be.false
    }).timeout(10000)
})