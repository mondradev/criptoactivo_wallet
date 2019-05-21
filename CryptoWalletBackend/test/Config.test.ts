import Config from '../src/config'
import 'mocha'
import { expect } from 'chai'

describe('Config Module Default', () => {

    it('Default Bitcoin Network', () => {
        expect(Config.assets.bitcoin.network).to.equal('testnet')
    })
    it('Default Log Level', () => {
        expect(Config.logLevel).to.equal('Trace')
    })
    it('Default Wallet Api Host', () => {
        expect(Config.walletApi.host).to.equal('localhost')
    })
    it('Default Wallet Api Port', () => {
        expect(Config.walletApi.port).greaterThan(0)
    })
})