import Config from '../config'
import 'mocha'
import { expect } from 'chai'

describe('Config Module', () => {

    it('Bitcoin Config', () => {
        expect(Config.getAsset('bitcoin')).to.be.not.null.and.have.ownProperty('network', 'testnet')
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