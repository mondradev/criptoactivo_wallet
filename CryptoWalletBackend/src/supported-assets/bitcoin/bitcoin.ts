import LoggerFactory from "../../services/loggin-factory";
import Config from '../../config';
import Utils from "../../utils";
import IWalletService from "../../services/iwallet-service";
import TimeSpan from "../../../libs/timespan";

import { Networks, Block, BlockHeader, Transaction, Address, PublicKey, Script } from "bitcore-lib";
import { BlockStore, IBlock } from "./blockstore";
import { TransactionStore, ITransaction } from "./transactionstore";
import { ChainInfoService, SupportedAssets, SupportedNetworks } from "../chainsync";
import { UpdateQuery } from "mongodb";
import { Peer, Messages, Pool } from "bitcore-p2p";
import { EventEmitter } from "events";
import { ICoin, CoinStore } from "./coinstore";

type BulkUpdate<T> = {
    updateOne: {
        filter: Partial<T>,
        update: { $set: Partial<T> },
        upsert: boolean
    }
};

/**
 * Servicio de billetera que permite sincronizar las billeteras almacenadas en el móvil de manera eficaz y rápida.
 */
export class Bitcoin implements IWalletService {

    public async getHistorial(address: string): Promise<[]> {
        //  OutputStore.Collection.aggregate([{ $match: address }, { $project: { _id: 0, parentTx: 1 } }])
        throw new Error("Method not implemented.");
    }

    public async getBalance(address: string): Promise<number> {
        throw new Error("Method not implemented.");
    }
    public async getTransaction(txid: string): Promise<any> {
        throw new Error("Method not implemented.");
    }
    public async broadcastTrx(transaction: any): Promise<boolean> {
        throw new Error("Method not implemented.");
    }

    private static Logger = LoggerFactory.getLogger('Bitcoin');

    private _eventEmitter: EventEmitter;
    private _networkMessages: any;
    private _pool: Pool;
    private _bestHeight: number = 0;
    public _isSynchronizing: boolean;

    /**
     * Activa el tipo de red.
     * 
     * @param networkName Nombre de la red de bitcoin.
     * 
     */
    private _enableNetwork(networkName: ('mainnet' | 'testnet' | 'regtest' | string)): void {
        switch (networkName) {
            case 'mainnet':
                Networks.defaultNetwork = Networks.mainnet;
                Networks.disableRegtest();
                break;

            case 'testnet':
                Networks.defaultNetwork = Networks.testnet;
                Networks.disableRegtest();
                break;

            case 'regtest':
                Networks.defaultNetwork = Networks.testnet;
                Networks.enableRegtest();
                break;
        }
    }

    /**
     * Obtiene un bloque de la red de bitcoin.
     * 
     * @param {string} blockhash Hash del bloque a descargar de la red.
     * @returns {Promise<Block>} Bloque de Bitcoin.
     */
    private _getBlock(blockhash: string): Promise<Block> {
        let received = false;
        return new Promise<Block>(async resolve => {
            this._eventEmitter.once('block', block => {
                received = true;
                resolve(block);
            });
            while (!received) {
                this._pool.sendMessage(this._networkMessages.GetData.forBlock(blockhash));
                await Utils.wait(1000);
            }
        });
    }

    /**
     * Obtiene la cabecera de un bloque de la red de bitcoin.
     * 
     * @param {string} hashblock hash del bloque que se desea obtener su cabecera.
     * @returns {Promise<BlockHeader>} La cabecera del bloque.
     */
    private _getHeaders(hashblock: string): Promise<BlockHeader[]> {
        let received = false;
        return new Promise(async resolve => {
            this._eventEmitter.once('headers', headers => {
                received = true;
                resolve(headers);
            });
            while (!received) {
                this._pool.sendMessage(this._networkMessages.GetHeaders({ starts: [hashblock] }));
                await Utils.wait(1000);
            }
        });
    }

    /**
     * Obtiene la dirección del script especificado.
     * 
     * @param script Script de la salida.
     */
    private _getAddressFromScript(script: Script) {
        let address: string;

        switch (script.classify().toString()) {
            case 'Pay to public key':
                address = new Address(new PublicKey(script.getPublicKey()), Networks.defaultNetwork).toString();
                break;
            case 'Pay to public key hash':
            case 'Pay to script hash':
                address = script.toAddress(Networks.defaultNetwork).toString();
                break;
            case 'Pay to multisig':
                address = "(Multisign)";
                break;
            case 'Data push':
                address = "(Data only)";
                break;
            default:
                Bitcoin.Logger.debug({
                    msg: `Can't resolve address from script ${script.classify()}`
                });
            case 'Unknown':
                address = '(Nonstandard)';
                break;
        }

        return address;
    }

    /**
     * Procesa las salidas de la transacción especificada.
     * 
     * @param {Transaction} tx Transacción a procesar las salidas.
     */
    private async _processOutputs(tx: Transaction) {
        const outputs = tx.outputs;
        const txid = tx.hash.toString('hex');

        type outputCoin = { amount: number, address: string, script: Buffer, multi: boolean };
        const outOps: Array<BulkUpdate<ICoin>> = [];

        for (const [index, output] of outputs.entries()) {
            let newValue: outputCoin;

            if (Utils.isNull(output.script)) {
                newValue = { amount: output.satoshis, address: '(Nonstandard)', script: null, multi: false };
            } else {
                let address: string = this._getAddressFromScript(output.script);
                let isP2SM = address === '(Multisign)';

                newValue = { amount: output.satoshis, address, script: output.script.toBuffer(), multi: isP2SM };
            }
            outOps.push({ updateOne: { filter: { parentTx: txid, index }, upsert: true, update: { $set: newValue } } });
        }

        return outOps;
    }

    /**
     * Procesa las entradas de la transacción especificada.
     * 
     * @param {Transaction} tx Transacción a procesar las entradas.
     */
    public async _processInputs(tx: Transaction) {
        if (tx.isCoinbase())
            return null;

        const inputs = tx.inputs;
        const txid = tx.hash.toString('hex');
        const inOps: Array<BulkUpdate<ICoin>> = [];

        for (const input of inputs) {

            let prevTxid = input.prevTxId.toString('hex');

            inOps.push({
                updateOne: {
                    filter: { index: input.outputIndex, parentTx: prevTxid },
                    update: { $set: { spendTx: txid } },
                    upsert: true
                }
            });
        }

        return inOps;
    }

    /**
     * Importa las entradas y salidas de las transacciones especificadas.
     * 
     * @param {Array<Transaction>} txs Transacciones a obtener las entradas y salidas.
     */
    private async _dataImport(txs: Array<Transaction>): Promise<void> {
        let outOps: Array<BulkUpdate<ICoin>> = [];
        let inOps: Array<BulkUpdate<ICoin>> = [];

        for (const tx of txs) {
            let outops = await this._processOutputs(tx);
            outOps.push(...outops);

            let inops = await this._processInputs(tx);
            if (inops && inops.length > 0) inOps.push(...inops);
        }

        let mapOutOps: BulkUpdate<ICoin>[] = outOps
            .reduce((map, v) => {
                map[v.updateOne.filter.parentTx + '_' + v.updateOne.filter.index] = v.updateOne.update.$set; return map
            }, []);

        for (const [index, inops] of inOps.entries()) {
            let outOps = mapOutOps[inops.updateOne.filter.parentTx + '_' + inops.updateOne.filter.index];

            if (outOps) {
                outOps.spendTx = inops.updateOne.update.$set.spendTx;
                delete inOps[index];
            }
        }

        if (outOps.length > 0)
            await Promise.all(Utils.partition(outOps, Config.MongoDb.PoolSize)
                .map(ops => CoinStore.Collection.bulkWrite(ops, { ordered: false })));

        inOps = inOps.filter(i => i);

        if (inOps.length > 0)
            await Promise.all(Utils.partition(inOps, Config.MongoDb.PoolSize)
                .map(ops => CoinStore.Collection.bulkWrite(ops, { ordered: false })));
    }

    /**
     * Importa las transacciones en la base de datos.
     * 
     * @param {Array<Transaction>} txs Transacciones a cargar en la base de datos.
     * @param {string} blockhash Hash del bloque al cual corresponden las transacciones.
     * @param {number} blockheight Altura del bloque al cual corresponden las transacciones.
     * @param {Date} blocktime Fecha/Hora en la que se generó el bloque.
     */
    private async _transactionImport(txs: Array<Transaction>, blockhash: string, blockheight: number, blocktime: Date): Promise<void> {
        let txid: string;
        let rawhex: Buffer;
        let txParent: ITransaction;

        await this._dataImport(txs);

        let txsOps: Array<BulkUpdate<ITransaction>> = txs.map(tx => {
            txid = tx.hash.toString('hex');
            rawhex = tx.toBuffer();

            txParent = {
                blockHash: blockhash,
                blockHeight: blockheight,
                blockTime: blocktime,
                coinbase: tx.isCoinbase(),
                lockTime: tx.nLockTime,
                txid,
                value: tx.outputAmount,
                size: rawhex.length,
                fee: -1,
                hex: rawhex,
                outputsCount: tx.outputs.length,
                inputsCount: tx.inputs.length
            };

            return {
                updateOne: {
                    filter: { txid },
                    update: { $set: txParent },
                    upsert: true
                }
            };
        });

        let txids = txsOps.filter(tx => !tx.updateOne.update.$set.coinbase)
            .map(tx => tx.updateOne.update.$set.txid);

        let spends = await CoinStore.Collection.aggregate<{ _id: string, amount: number }>([
            { $match: { spendTx: { $in: txids } } },
            { $group: { _id: "$spendTx", amount: { $sum: "$amount" } } }
        ]).toArray();

        let mapping = txsOps.reduce((map, value) => {
            map[value.updateOne.update.$set.txid] = value.updateOne.update.$set;
            return map;
        }, []);

        for (const spend of spends)
            mapping[spend._id].fee = spend.amount - mapping[spend._id].value;

        await Promise.all(Utils.partition(txsOps, Config.MongoDb.PoolSize)
            .map(ops => TransactionStore.Collection.bulkWrite(ops, { ordered: false })));
    }

    /**
     * Sincroniza la blockchain de Bitcoin.
     */
    private async _sync() {
        const chainInfo = ChainInfoService.getInstance();

        this._isSynchronizing = true;

        let status = await chainInfo.getStatus(SupportedAssets.Bitcoin, SupportedNetworks.testnet);
        let headers = new Array<BlockHeader>();
        let progress = 0;
        let nBlocks = 0;
        let ini: Date;

        let showProgress = async (blockTime: Date, ntx: number) => {

            let end = new Date();
            let seconds = end.getTime() - ini.getTime();

            if (seconds >= 1000) {
                let block_sec = (nBlocks * 1000) / seconds;
                block_sec = block_sec;

                Bitcoin.Logger.info({
                    msg: `Processing ${Math.trunc(block_sec)} blocks/sec, left time: ${block_sec > 0
                        ? TimeSpan.FromSeconds(Math.trunc((this._bestHeight - status.height) / block_sec)).toString() : 'calculating'}`
                        + `, nTxs: ${ntx}, Blocks: ${status.height} of ${this._bestHeight}, Date: ${blockTime.toLocaleString()}`
                });

                ini = new Date();
                nBlocks = 0;
            }

            if (status.height > this._bestHeight)
                return;

            let currentProgress = (status.height / this._bestHeight) * 100;
            currentProgress = Math.trunc(currentProgress);

            if (currentProgress > progress) {
                progress = currentProgress;

                Bitcoin.Logger.info({ msg: `Saved ${status.height}, done ${progress}% of blockchain` });
            } else if (status.height % 10000 == 0)
                Bitcoin.Logger.info({ msg: `Saved ${status.height} blocks` });
        };

        while (true) {
            try {

                while (headers.length == 0) {
                    await Utils.wait(1000);

                    headers = await this._getHeaders(status.lastBlock);
                }

                ini = new Date();

                for (const header of headers) {

                    let block = await this._getBlock(header.hash);
                    nBlocks++;

                    let prevHash = header.prevHash.reverse().toString('hex');
                    let height = prevHash == '000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943' ? 1
                        : ((await BlockStore.Collection.findOne({ hash: prevHash })) || status).height + 1

                    if (height > 1)
                        await BlockStore.Collection.updateOne({ hash: prevHash }, { $set: { nextBlock: block.hash } });

                    let blockObj: IBlock = {
                        hash: block.hash,
                        height: height,
                        merkletRoot: header.merkleRoot.reverse().toString('hex'),
                        nextBlock: '',
                        prevBlock: prevHash,
                        bits: header.bits,
                        nonce: header.nonce,
                        version: header.version,
                        time: new Date(header.time * 1000),
                        ntx: block.transactions.length,
                        reward: block.transactions[0].outputAmount
                    };

                    let response = await BlockStore.Collection.updateOne(
                        { hash: blockObj.hash },
                        { $set: blockObj },
                        { upsert: true }
                    );

                    await this._transactionImport(block.transactions, blockObj.hash, blockObj.height, blockObj.time);

                    status.lastBlock = blockObj.hash;
                    status.height = blockObj.height;

                    if (response)
                        await chainInfo.setStatus(status);
                    else
                        throw Error(`Can't synchronize the block ${status.lastBlock}`);

                    showProgress(blockObj.time, blockObj.ntx);
                }

                headers = [];
            } catch (reason) {
                Bitcoin.Logger.warn({ msg: `Fail to sync: ${reason.message}` });
            }
        }
    }

    /**
     * Crea una instancia nueva del servicio de billetera de Bitcoin.
     */
    public constructor() {
        this._enableNetwork(Config.Bitcoin.Network);

        this._networkMessages = new Messages({ network: Networks.defaultNetwork });
        this._eventEmitter = new EventEmitter();
    }

    /**
     * Inicia el servicio de billetera de Bitcoin.
     */
    public static async start() {
        let btcService = new Bitcoin();

        let dbConfig = {
            host: Config.MongoDb.Host,
            port: Config.MongoDb.Port,
            dbname: Config.MongoDb.Db,
            schema: 'bitcoin'
        };

        await BlockStore.init(dbConfig);
        await TransactionStore.init(dbConfig);
        await CoinStore.init(dbConfig);
        await ChainInfoService.start(dbConfig);

        if ([BlockStore.isConnected, TransactionStore.isConnected, CoinStore.isConnected]
            .reduce((pv, cv) => pv && cv)) {

            Bitcoin.Logger.info({ msg: 'BlockchainSync service started' });

            btcService._pool = new Pool({ network: Networks.defaultNetwork, maxSize: 5 });

            btcService._pool
                .on('peerheaders', (peer: Peer, message: { headers: BlockHeader[] }) => {
                    btcService._eventEmitter.emit('headers', message.headers);
                })
                .on('peerblock', (peer: Peer, message: { block: Block }) => {
                    btcService._eventEmitter.emit('block', message.block);
                })
                .on('peerready', async (peer: Peer) => {
                    Bitcoin.Logger.info({ msg: `Peer connected, begin to download ${peer.bestHeight} blocks` });
                    btcService._bestHeight = peer.bestHeight > btcService._bestHeight
                        ? peer.bestHeight : btcService._bestHeight;

                    if (!btcService._isSynchronizing)
                        btcService._sync().catch((reason: Error) => {
                            Bitcoin.Logger.error({ msg: `Fail to sync: ${reason.message}` });
                            btcService._pool.disconnect();
                            btcService._isSynchronizing = false;

                            process.exit(1);
                        });
                });

            btcService._pool.connect();
        }

        return btcService;
    }
}