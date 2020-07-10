import { Networks, Transaction, Output, Address, Block, PublicKey, Script } from "bitcore-lib"
import { TxData, IWalletProvider, ChainInfo } from "../../resources/iwalletprovider"
import { MongoClient, Db, Collection } from "mongodb"
import { Blockchain } from "./chain/blockchain"
import { Network } from "./network"
import { Mempool } from "./store/leveldb/mempool"
import { google } from 'googleapis'

import BufferHelper from "./../../utils/bufferhelper"
import NetworkStatus from "./network/networkstatus"
import LoggerFactory from 'log4js'
import Config from "../../../config"

import fs from 'fs'
import axios from 'axios'

const ASSET = "btc"
const Logger = LoggerFactory.getLogger('Bitcoin (Wallet)')
const MESSAGING_SCOPE = 'https://www.googleapis.com/auth/firebase.messaging'
const SCOPES = [MESSAGING_SCOPE]

export default class WalletProvider implements IWalletProvider {


    private static _scriptFnAddress = {
        'Pay to public key': (script: Script) => new Address(new PublicKey(script.getPublicKey()), Networks.defaultNetwork).toString(),
        'Pay to public key hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toString(),
        'Pay to script hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toString()
    }

    private static _toAddress(txo: Output): string {
        if (!txo.script)
            return null

        return (WalletProvider._scriptFnAddress[txo.script.classify().toString()]
            || (() => null))(txo.script)
    }

    private _db: MongoManager

    public async subscribeWallet(walletId: string, addresses: string, chain: string, network: string, pushToken: string) {
        if (!this._db.connected)
            throw new Error("Wallets database wasn't loaded")

        const addressesBtc = new Array<Address>()

        while (addresses.length > 42) {
            const buff = BufferHelper.fromHex(addresses.substr(0, 42))

            addressesBtc.push(Address.fromBuffer(buff, network))
            addresses = addresses.substr(42)
        }

        const wallet = await this._db.getWallet(walletId, chain, network)

        if (wallet) {
            wallet.addresses.push(...addressesBtc.map(a => a.toString()))
            wallet.pushTokens.push(pushToken)

            wallet.addresses.unique()
            wallet.pushTokens.unique()

            return this._db.updateWallet(wallet)
        }
        else
            return this._db.registerWallet({
                addresses: addressesBtc.map(a => a.toString()),
                pushTokens: [pushToken],
                chain,
                network,
                walletId
            })

    }

    public async getTransactionFromMempool(txid: string, network: string): Promise<TxData> {
        if (network !== Networks.defaultNetwork.name)
            return null

        const tx = await this.mempool.get(BufferHelper.fromHex(txid))

        if (!tx) return null

        return {
            block: "(Mempool)",
            height: -1,
            index: 0xffffffff,
            txid: tx.hash,
            time: new Date().getTime() / 1000,
            data: tx.toBuffer().toHex()
        }
    }

    public async getBlock(hash: string, network: string): Promise<{}> {
        if (network !== Networks.defaultNetwork.name)
            return null

        const block = await this.chain.getBlock(BufferHelper.fromHex(hash))

        if (!block) return null

        return { ...block.toJSON(), height: await this.chain.getHeight(block._getHash()) }
    }

    public constructor(private chain: Blockchain, private mempool: Mempool, private network: Network) {
        Logger.level = Config.logLevel
        this._db = new MongoManager()
    }

    public async connect() {
        return this._db.connect().then(() => {
            if (!this._db.connected) return

            this.chain.on('block', (block: Block) => this._notifyReceivedBlock(block))
            this.mempool.on('tx', (tx: Transaction) => this._notifyReceivedTx(tx))
        })
    }

    private async _notifyReceivedBlock(block: Block) {
        for (const tx of block.transactions)
            await this._notifyReceivedTx(tx)

        const tip = await this.getChainInfo(Networks.defaultNetwork.name)
        await this._notifyUpdateTip(tip)
    }

    private async _notifyUpdateTip(tip: ChainInfo) {
        const wallets = await this._db.getWallets(ASSET, tip.network)
        const pushTokens = wallets.reduce((tokens, wallet) => { tokens.push(...wallet.pushTokens); return tokens }, [])

        return Promise.all(pushTokens.map(async pt => {
            return axios.post("https://fcm.googleapis.com/v1/projects/development-criptoactivo/messages:send",
                {
                    message: {
                        data: {
                            height: tip.height.toString(),
                            hash: tip.hash,
                            time: tip.time.toString(),
                            network: tip.network,
                            asset: ASSET
                        },
                        token: pt
                    }
                },
                {
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": "Bearer " + await this.getAccessToken()
                    }
                }).catch(async (reason) => {
                    const error: { code: number, message: string, status: string } = reason.response.data.error
                    Logger.warn("Fail to send a notification to server FCM: %s", error.message)

                    if (error.status === "NOT_FOUND")
                        await this._db.removePushToken(pt, ASSET, Networks.defaultNetwork.name)
                })
        }))
    }

    private async _notifyReceivedTx(tx: Transaction) {
        await this._createNotify(tx)
            .then(notify => this._sendNotify(notify))
    }

    public async _sendNotify(notify: { txid: string; addresses: string[] }) {
        const walletsQuery = notify.addresses.map(address =>
            this._db.getWalletByAddress(address, ASSET, Networks.defaultNetwork.name))

        let wallets = await Promise.all(walletsQuery)
        wallets = wallets.filter(w => w != null)

        if (wallets.length == 0) return

        const sents = {}

        for (const wallet of wallets)
            if (!sents[wallet.walletId])
                if (await this._notifyWallet(wallet.pushTokens, notify.txid)) {
                    Logger.debug("Sent notification to %s", wallet.walletId)
                    sents[wallet.walletId] = true
                }

    }

    private async _notifyWallet(pushTokens: string[], txid: string) {
        return new Promise<boolean>(done =>
            Promise.all(pushTokens.map(async pt => {
                return axios.post("https://fcm.googleapis.com/v1/projects/development-criptoactivo/messages:send",
                    {
                        message: {
                            data: {
                                txid,
                                network: Networks.defaultNetwork.name,
                                asset: ASSET
                            },
                            token: pt
                        }
                    },
                    {
                        headers: {
                            "Content-Type": "application/json",
                            "Authorization": "Bearer " + await this.getAccessToken()
                        }
                    }).catch(async (reason) => {
                        const error: { code: number, message: string, status: string } = reason.response.data.error
                        Logger.warn("Fail to send a notification to server FCM: %s", error.message)

                        if (error.status === "NOT_FOUND")
                            await this._db.removePushToken(pt, ASSET, Networks.defaultNetwork.name)
                        else
                            throw error
                    })
            })).then(() => done(true)).catch(() => done(false)))
    }

    private async _createNotify(tx: Transaction) {
        const hash = tx.hash

        const inputs = tx.inputs.map(txi => {
            return {
                outpoint: {
                    txid: txi.prevTxId.toHex(),
                    index: txi.outputIndex
                },
                address: null
            }
        })

        const outputs = tx.outputs.map(txo => {
            return { address: WalletProvider._toAddress(txo) }
        }).filter(txo => txo.address != null)

        await this._connectOutput(inputs.filter(txi => txi != null))

        outputs.push(...inputs)

        const notify = {
            txid: hash,
            addresses: outputs.map(txo => txo.address).unique()
        }

        return notify
    }

    private async _connectOutput(inputs: { outpoint: { txid: string, index: number }, address: string }[]) {
        for (const txi of inputs) {
            const prevTxIdx = await this.chain.TxIndex.getIndexByHash(BufferHelper.fromHex(txi.outpoint.txid).reverse())

            if (!prevTxIdx) continue

            const block = await this.chain.getBlock(prevTxIdx.blockHash)
            const prevTx = block.transactions[prevTxIdx.index]
            const txo = prevTx.outputs[txi.outpoint.index]

            if (!txo) continue

            txi.address = WalletProvider._toAddress(txo)
        }
    }

    public async getHistory(addresses: string, network: string, height: number): Promise<TxData[]> {
        if (network !== Networks.defaultNetwork.name)
            return null

        const txHistorial = new Array<TxData>()
        let addressesCount = 0

        while (addresses.length >= 42) {
            const address = addresses.substr(0, 42)

            addresses = addresses.substr(42)

            txHistorial.push(...await this.getHistoryByAddress(address, network, height))

            addressesCount++
        }

        Logger.debug("Addresses found: %d", addressesCount)

        return txHistorial
    }

    public async getTxDependencies(txid: string, network: string): Promise<TxData[]> {
        if (network !== Networks.defaultNetwork.name)
            return null

        const tx = await this.getTransaction(txid, network)
        const dependencies = new Array<TxData>()

        if (!tx) return null

        for (const inputs of new Transaction(tx.data).inputs) {
            if (inputs.isNull())
                continue

            const dependency = await this.getTransaction(inputs.prevTxId.toReverseHex(), network)

            if (!inputs)
                continue

            dependencies.push(dependency)
        }

        return dependencies
    }

    public async getChainInfo(network: string): Promise<ChainInfo> {
        if (network !== Networks.defaultNetwork.name)
            return null

        const tip = await this.chain.getLocalTip()
        const status = await this.network.getStatus()

        return {
            hash: tip.hash,
            height: tip.height,
            time: tip.time,
            txn: tip.txn,
            network: Networks.defaultNetwork.name,
            status: Object.keys(NetworkStatus).find(k => NetworkStatus[k] == status).toLocaleLowerCase()
        }
    }

    public async getTransaction(txid: string, network: string): Promise<TxData> {
        if (network !== Networks.defaultNetwork.name)
            return null

        const txidBuff = BufferHelper.fromHex(txid)
        const txIndex = await this.chain.TxIndex.getIndexByHash(txidBuff)

        if (!txIndex)
            return null

        const block = await this.chain.getBlock(txIndex.blockHash)
        const height = await this.chain.getHeight(txIndex.blockHash)

        return {
            height,
            block: block.hash,
            txid: txidBuff.toReverseHex(),
            index: txIndex.index,
            time: block.header.time,
            data: block.transactions[txIndex.index].toString()
        }
    }

    public async getHistoryByAddress(address: string, network: string, fromHeight: number): Promise<TxData[]> {
        if (network !== Networks.defaultNetwork.name)
            return null

        const addressBuff = BufferHelper.fromHex(address)
        const addrIndexes = await this.chain.AddrIndex.getIndexesByAddress(addressBuff)
        const txHistorial = new Array<TxData>()

        for (const addrIndex of addrIndexes) {
            const txIndex = await this.chain.TxIndex.getIndexByHash(addrIndex.txid)
            const height = await this.chain.getHeight(txIndex.blockHash)

            if (height < fromHeight) continue

            const block = await this.chain.getBlock(txIndex.blockHash)

            txHistorial.push({
                height,
                block: block.hash,
                txid: addrIndex.txid.toReverseHex(),
                time: block.header.time,
                data: block.transactions[txIndex.index].toString(),
                index: addrIndex.index
            })
        }

        return txHistorial
    }

    public async broadcastTx(transaction: string, network: string): Promise<boolean> {
        if (network !== Networks.defaultNetwork.name)
            return false

        const rawData = BufferHelper.fromHex(transaction)

        return this.network.broadcastTx(rawData);
    }

    private getAccessToken() {
        return new Promise<string>(function (resolve, reject) {
            const key = require('../../../config/service-account.json')

            new google.auth.JWT(
                key.client_email,
                null,
                key.private_key,
                SCOPES,
                null
            )
                .authorize(function (err, tokens) {
                    if (err) {
                        reject(err)
                        return
                    }
                    resolve(tokens.access_token)
                });
        });
    }

}

type MongoConfig = {
    host: string
    port: number
    dbName: string,
    user: string,
    pwd: string,
    sslCA: string,
    sslCert: string,
    sslKey: string
}

const MongoDbConfig: MongoConfig = Config.dbConfig

type WalletSchema = {
    walletId: string,
    network: string,
    chain: string,
    addresses: string[],
    pushTokens: string[]
}


class MongoManager {

    private _controller: MongoClient
    private _db: Db
    private _wallets: Collection<WalletSchema>
    private _connected: boolean

    public async connect() {
        const connectionString = `mongodb://${MongoDbConfig.host}:${MongoDbConfig.port}/${MongoDbConfig.dbName}`

        Logger.debug("Try connect to %s with user: %s and pass: ******* ", connectionString, MongoDbConfig.user)

        this._controller = await MongoClient.connect(connectionString, {
            ssl: MongoDbConfig.sslCA != null,
            sslValidate: MongoDbConfig.sslCA != null,
            sslCA: MongoDbConfig.sslCA != null ? [fs.readFileSync(MongoDbConfig.sslCA)] : null,
            sslCert: MongoDbConfig.sslCert != null ? fs.readFileSync(MongoDbConfig.sslCert) : null,
            sslKey: MongoDbConfig.sslKey != null ? fs.readFileSync(MongoDbConfig.sslKey) : null,
            auth: MongoDbConfig.user != null && MongoDbConfig.pwd != null ?
                { user: MongoDbConfig.user, password: MongoDbConfig.pwd } : null,
            checkServerIdentity: false,
            useNewUrlParser: true,
            noDelay: true,
            socketTimeoutMS: 0,
            poolSize: 100,
            useUnifiedTopology: true
        })

        this._connected = this._controller.isConnected()

        if (this._connected)
            Logger.info("Wallets database is connected")

        this._db = this._controller.db(MongoDbConfig.dbName)
        this._wallets = this._db.collection("wallets")

        await this._wallets.createIndex({ chain: 1, network: 1 }, { background: true })
        await this._wallets.createIndex({ walletId: 1, chain: 1, network: 1 }, { background: true })
        await this._wallets.createIndex({ addresses: 1, chain: 1, network: 1 }, { background: true })
        await this._wallets.createIndex({ pushTokens: 1, chain: 1, network: 1 }, { background: true })
    }

    public async getWallets(chain: string, network: string) {
        return this._wallets.find({ chain, network }).toArray()
    }

    public async removePushToken(token: string, chain: string, network: string) {
        const removedToken = await this._wallets.updateOne({ pushTokens: token, chain, network },
            { $pull: { pushTokens: token } })

        return removedToken.result.ok == 1
    }

    public async updateWallet(wallet: WalletSchema) {
        const updated = await this._wallets.updateOne({ walletId: wallet.walletId, network: wallet.network }, { $set: wallet })

        return updated.result.ok == 1
    }

    public get connected() {
        return this._connected
    }

    public async getWalletByAddress(address: string, chain: string, network: string) {
        const wallet = await this._wallets.findOne({ chain, network, addresses: address })

        return wallet
    }

    public async registerWallet(wallet: WalletSchema) {
        const inserted = await this._wallets.insertOne(wallet)

        return inserted.result.ok == 1
    }

    public async getWallet(walletId: string, chain: string, network: string) {
        const wallet = await this._wallets.findOne({ walletId, chain, network })

        return wallet
    }

}