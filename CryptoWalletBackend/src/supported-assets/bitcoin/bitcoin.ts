import LoggerFactory from "../../services/loggin-factory";
import Config from '../../config';
import Utils from "../../utils";
import IWalletService from "../../services/iwallet-service";
import TimeSpan from "../../../libs/timespan";

import { Networks, Block, BlockHeader, Transaction, Address, PublicKey } from "bitcore-lib";
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
        update: UpdateQuery<T>,
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

    private _events: EventEmitter;
    private _messages: any;
    private _pool: Pool;
    private _bestHeight: number;
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
            this._events.once('block', block => {
                received = true;
                resolve(block);
            });
            while (!received) {
                this._pool.sendMessage(this._messages.GetData.forBlock(blockhash));
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
            this._events.once('headers', headers => {
                received = true;
                resolve(headers);
            });
            while (!received) {
                this._pool.sendMessage(this._messages.GetHeaders({ starts: [hashblock] }));
                await Utils.wait(1000);
            }
        });
    }

    /**
     * Importa las entradas de las transacciones especificadas.
     * 
     * @param {Transaction[]} txs Transacciones que contiene las entradas a cargar.
     */
    private async _inputImport(txs: Transaction[]): Promise<void> {
        let inOps: BulkUpdate<ICoin>[] = [];

        for (const tx of txs) {

            if (tx.isCoinbase())
                continue;

            const inputs = tx.inputs;
            const txid = tx.hash.toString('hex');

            for (const input of inputs) {

                let prevTxid = input.prevTxId.toString('hex');

                inOps.push({
                    updateOne: {
                        filter: {
                            index: input.outputIndex,
                            parentTx: prevTxid
                        },
                        update: {
                            $set: {
                                spendTx: txid
                            }
                        },
                        upsert: true
                    }
                });
            }
        }

        if (inOps.length > 0)
            await Promise.all(Utils.partition(inOps, Config.MongoDb.PoolSize)
                .map(ops => CoinStore.Collection.bulkWrite(ops, { ordered: false })));
    }

    /**
     * Importa las salidas de las transacciones especificadas.
     * 
     * @param {Transaction[]} txs Transacciones a obtener las salidas.
     */
    private async _outputImport(txs: Transaction[]): Promise<void> {
        let outOps: Array<BulkUpdate<ICoin>> = [];

        let addOutOp = (index: number, amount: number, parentTx: string, address: string, script: Buffer, multi: boolean = false) => {
            outOps.push({
                updateOne: {
                    filter: { parentTx, index },
                    update: {
                        $set: {
                            address,
                            amount,
                            multi,
                            script
                        }
                    },
                    upsert: true
                }
            });
        };

        for (const tx of txs) {

            const outputs = tx.outputs;
            const txid = tx.hash.toString('hex');

            for (const [index, output] of outputs.entries()) {
                if (Utils.isNull(output.script)) {
                    addOutOp(index, output.satoshis, txid, '(Nonstandard)', null);
                } else {
                    let address: string;
                    let isP2SM = false;

                    switch (output.script.classify().toString()) {
                        case 'Pay to public key':
                            address = new Address(new PublicKey(output.script.getPublicKey()), Networks.defaultNetwork).toString();
                            break;
                        case 'Pay to public key hash':
                        case 'Pay to script hash':
                            address = output.script.toAddress(Networks.defaultNetwork).toString();
                            break;
                        case 'Pay to multisig':
                            address = "(Multisign)";
                            isP2SM = true;
                            break;
                        case 'Data push':
                            address = "(Data only)";
                            break;
                        default:
                            Bitcoin.Logger.debug({
                                msg: `Can't resolve address from script ${output.script.classify()} 
                                                    of output ${index} of tx: ${txid}`
                            });
                        case 'Unknown':
                            address = '(Nonstandard)';
                            break;
                    }

                    addOutOp(index, output.satoshis, txid, address, output.script.toBuffer(), isP2SM);
                }
            }
        }

        await Promise.all(Utils.partition(outOps, Config.MongoDb.PoolSize)
            .map(ops => CoinStore.Collection.bulkWrite(ops, { ordered: false })));
    }

    /**
     * Importa las transacciones en la base de datos.
     * 
     * @param {Transaction[]} txs Transacciones a cargar en la base de datos.
     * @param {string} blockhash Hash del bloque al cual corresponden las transacciones.
     * @param {number} blockheight Altura del bloque al cual corresponden las transacciones.
     * @param {Date} blocktime Fecha/Hora en la que se generó el bloque.
     */
    private async _transactionImport(txs: Transaction[], blockhash: string, blockheight: number, blocktime: Date): Promise<void> {

        let txsOps: Array<BulkUpdate<ITransaction>> = [];
        let txid: string;
        let rawhex: Buffer;
        let txParent: ITransaction;
        let txOp: BulkUpdate<ITransaction>;


        await this._outputImport(txs);
        await this._inputImport(txs);


        for (const tx of txs) {

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

            if (!txParent.coinbase) {
                let inputs = await CoinStore.Collection.aggregate<{ amount: number }>([
                    { $match: { spendTx: txParent.txid } },
                    { $group: { _id: "$spendTx", amount: { $sum: "$amount" } } }
                ]).toArray();
                let inputAmount = inputs.shift().amount;
                txParent.fee = inputAmount - txParent.value;
            }
            else
                txParent.fee = 0;

            txOp = {
                updateOne: {
                    filter: { txid },
                    update: { $set: txParent },
                    upsert: true
                }
            };

            txsOps.push(txOp);
        }

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

        let showProgress = () => {

            let end = new Date();
            let seconds = end.getTime() - ini.getTime();

            if (seconds >= 1000) {
                let block_sec = (nBlocks * 1000) / seconds;
                block_sec = Math.trunc(block_sec);

                Bitcoin.Logger.info({
                    msg: `Processing ${block_sec} blocks/sec, left time: ${block_sec > 0
                        ? TimeSpan.FromSeconds(Math.trunc((this._bestHeight - status.height) / block_sec)).toString() : 'calculating'}`
                        + `, Blocks: ${status.height}`
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

                    showProgress();
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

        this._messages = new Messages({ network: Networks.defaultNetwork });
        this._events = new EventEmitter();
    }

    /**
     * Inicia el servicio de billetera de Bitcoin.
     */
    public static async start() {
        let instance = new Bitcoin();

        let dbConfig = {
            username: Config.MongoDb.Username,
            password: Config.MongoDb.Password,
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

            instance._pool = new Pool({ network: Networks.defaultNetwork, maxSize: 5 });

            instance._pool
                .on('peerheaders', (peer: Peer, message: { headers: BlockHeader[] }) => {
                    instance._events.emit('headers', message.headers);
                })
                .on('peerblock', (peer: Peer, message: { block: Block }) => {
                    instance._events.emit('block', message.block);
                })
                .on('peerready', async (peer: Peer) => {
                    Bitcoin.Logger.info({ msg: `Peer connected, begin to download ${peer.bestHeight} blocks` });
                    instance._bestHeight = peer.bestHeight;

                    if (!instance._isSynchronizing)
                        instance._sync().catch((reason: Error) => {
                            Bitcoin.Logger.warn({ msg: `Fail to sync: ${reason.message}` });
                            instance._isSynchronizing = false;
                        });
                });

            instance._pool.connect();
        }

        return instance;
    }
}