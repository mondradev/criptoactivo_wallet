import { Blockchain } from "../libs/bitcoin/chain/blockchain"
import { Network } from "../libs/bitcoin/network"
import { Router, Request, Response, NextFunction } from "express"
import { LevelStore } from "../libs/bitcoin/store/leveldb"

import Config from "../../config/index"
import NetworkStatus from "../libs/bitcoin/network/networkstatus"
import WalletProvider from "../libs/bitcoin/wallet"
import LoggerFactory from 'log4js'
import { Mempool } from "../libs/bitcoin/store/leveldb/mempool"

type ParamValid = { message?: string, error: boolean, code?: number }

const VERSION = "v0.6-beta" // 2020-02-29
const VERSION_API = "v1" // 2020-04-01
const URL_BASE = `/api/${VERSION_API}/btc/`

const Logger = LoggerFactory.getLogger('(Bitcoin) Service')

Logger.level = Config.logLevel
Logger.info("Bitcoin node %s", VERSION)

const chain = new Blockchain(new LevelStore())
const mempool = new Mempool(chain)
const net = new Network(chain, mempool)
const wallet = new WalletProvider(chain, mempool, net)

async function start() {
    await chain.connect()
    await mempool.load()
    await net.connect()
    net.start()
}

function errorHandler(error: Error) {
    Logger.error(JSON.stringify(error.message))

    if (error.stack)
        Logger.debug(error.stack)

    exitHandler()
}

async function exitHandler() {
    await chain.disconnect()
    await net.disconnect()

    process.exit(0)
}

function validateAddress(address: string): ParamValid {
    if (!address || address === "")
        return {
            error: true,
            code: 400,
            message: "The address wasn't specified"
        }

    if (address.length < 42 || !new RegExp("[0-9A-Fa-f]+").test(address))
        return {
            error: true,
            code: 400,
            message: "The address isn't valid"
        }

    return { error: false }
}

function validateNetwork(network: string): ParamValid {
    if (!network || network === "")
        return {
            error: true,
            code: 400,
            message: "The network wasn't specified"
        }

    return { error: false }
}

function validateHash(hash: string): ParamValid {
    if (!hash || hash === "")
        return {
            message: "The hash wasn't specified",
            code: 400,
            error: true
        }

    if (hash.length < 64 || !new RegExp("[0-9A-Fa-f]+").test(hash))
        return {
            message: "The hash isn't valid",
            code: 400,
            error: true
        }

    return { error: false }
}

process.on('SIGINT', exitHandler)
process.on('SIGTERM', exitHandler)
process.on('uncaughtException', errorHandler)

const router = Router()
    .get(URL_BASE + ":network/*", async (req: Request, res: Response, next: NextFunction) => {
        const network = req.params.network;
        const networkValid = validateNetwork(network);

        Logger.trace("Received request: %s", req.path.substring(0, Math.min(100, req.path.length)))

        if (networkValid.error)
            res.status(networkValid.code).json({ message: networkValid.message });
        else if (req.path === URL_BASE + network + "/chaininfo")
            next()
        else if ((await net.getStatus()) < NetworkStatus.SYNCHRONIZED)
            res.status(423).json({ message: 'Bitcoin Synchronizing' });
        else
            next();
    })
    .get(URL_BASE + ':network/chaininfo', async (req: Request, res: Response, next: NextFunction) => {
        const network: string = req.params.network

        Logger.debug("Request received [Op=chaininfo, Param={ network: %s }]", network)

        res.status(200).json(await wallet.getChainInfo(network))
    })
    .get(URL_BASE + ":network/history/:addresses", async (req: Request, res: Response, next: NextFunction) => {
        const height = parseInt(req.query.height as string) >> 0
        const addresses: string = req.params.addresses
        const network = req.params.network;

        Logger.debug("Request received [Op=history, Param={ addresses: %s, network: %s }]",
            "byte[" + addresses.length / 2 + "]", network)

        if (addresses.length < 42)
            res.status(400).json({ message: "Any address wasn't specified" })
        else
            res.status(200).json(await wallet.getHistory(addresses, network, height))

    })
    .get(URL_BASE + ":network/txhistory/:address", async (req: Request, res: Response, next: NextFunction) => {
        const height = parseInt(req.query.height as string) >> 0
        const address: string = req.params.address
        const network: string = req.params.network
        const addressValid = validateAddress(address)

        Logger.debug("Request received [Op=txhist, Param={ address: %s, network: %s }]", address, network)

        if (addressValid.error)
            res.status(addressValid.code).json({ message: addressValid.message })
        else
            res.status(200).json(await wallet.getHistoryByAddress(address, network, height))
    })
    .get(URL_BASE + ":network/tx/:txid", async (req: Request, res: Response, next: NextFunction) => {
        const txid: string = req.params.txid
        const network: string = req.params.network
        const hashValid = validateHash(txid)

        Logger.debug("Request received [Op=tx, Param={ txid: %s, network: %s }]", txid, network)

        if (hashValid.error)
            res.status(hashValid.code).json({ message: hashValid.message })
        else {
            let tx = await wallet.getTransaction(txid, network)

            tx = tx || await wallet.getTransactionFromMempool(txid, network)

            res.status(200).json(tx)
        }
    })
    .get(URL_BASE + ":network/txdeps/:txid", async (req: Request, res: Response, next: NextFunction) => {
        const txid: string = req.params.txid
        const network: string = req.params.network
        const hashValid = validateHash(txid)

        Logger.debug("Request received [Op=txdeps, Param={ txid: %s, network: %s }]", txid, network)

        if (hashValid.error)
            res.status(hashValid.code).json({ message: hashValid.message })
        else
            res.status(200).json(await wallet.getTxDependencies(txid, network))
    })
    .put(URL_BASE + ":network/broadcast", async (req: Request, res: Response, next: NextFunction) => {
        const raw = req.body.hex || ""
        const network = req.params.network

        Logger.debug("Request received [Op=broadcastTx, Param={ raw: %s, network: %s }]", "byte[" + raw.length / 2 + "]", network)

        if (raw.length < 24)
            res.status(400).json({ message: "Any transaction wasn't specified" })
        else
            res.status(200).json({ sent: await wallet.broadcastTx(raw, network) })
    })
    .get(URL_BASE + ":network/block/:hash", async (req: Request, res: Response, next: NextFunction) => {
        const hash = req.params.hash
        const network = req.params.network
        const hashValid = validateHash(hash)

        if (hashValid.error)
            res.status(hashValid.code).json({ message: hashValid.message })
        else
            res.status(200).json(await wallet.getBlock(hash, network))
    })

start()

export default router