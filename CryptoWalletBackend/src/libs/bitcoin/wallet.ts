import { TxData, IWalletProvider, ChainInfo } from "../../resources/iwalletprovider"
import { Blockchain } from "./chain/blockchain"
import BufferHelper from "./../../utils/bufferhelper"
import { Networks, Transaction, Output } from "bitcore-lib"
import { Network } from "./network"
import NetworkStatus from "./network/networkstatus"
import LoggerFactory from 'log4js'
import Config from "../../../config"
import { Mempool } from "./store/leveldb/mempool"

const Logger = LoggerFactory.getLogger('Bitcoin (Wallet)')

export default class WalletProvider implements IWalletProvider {

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
}
