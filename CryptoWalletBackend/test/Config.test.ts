import Config from '../src/config'
import 'mocha'
import { expect } from 'chai'

describe('Config Module Default', () => {

    it('Default Bitcoin Network', () => {
        expect(Config.assets.bitcoin.network).to.equal('testnet')
    })
    it('Log Level Info passed by argument', () => {
        expect(Config.logLevel).to.equal('info')
    })
    it('Default Wallet Api hostname', () => {
        expect(Config.walletApi.host).to.equal('localhost')
    })
    it('Wallet Api port valid', () => {
        expect(Config.walletApi.port).greaterThan(0)
    })
})