import { Transaction, Script, PublicKey, Address, Networks, Network } from "bitcore-lib";
import { SupportedAssets, SupportedNetworks } from "../chaininfo-store";
import LoggerFactory from "../../services/loggin-factory";
import { BulkUpdate } from "../../data-access/storage";
import { ITransaction, BasedBtcTxStore } from "../tx-store";
import Utils from "../../utils/utils";
import ConfigService from "../../config";
import CountTime from "../../utils/counttime";


class Processor {

    private static Logger = LoggerFactory.getLogger('BtcTxProcessor');
    private static MAX_CACHE_TX_SIZE = 100000;
    private static CACHE_INITIAL_SIZE = 30000;

    private static _scriptFnAddress = {
        'Pay to public key': (script: Script) => new Address(new PublicKey(script.getPublicKey()), Networks.defaultNetwork).toString(),
        'Pay to public key hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toString(),
        'Pay to script hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toString()
    };

    private _cacheTx = {};
    private _loadedCache = false;
    private _requestExit: boolean = false;
    private _cacheTxSize = 0;

    constructor() {
        process.on('beforeExit', () => this._requestExit = true);

        (async () => {
            while (!this._requestExit) {
                if (Object.keys(this._cacheTx).length > Processor.MAX_CACHE_TX_SIZE)
                    Object.keys(this._cacheTx).slice(0, Processor.MAX_CACHE_TX_SIZE / 2).forEach(k => delete this._cacheTx[k]);

                this._cacheTxSize = Object.keys(this._cacheTx).length;
                await Utils.wait(60000);
            }
        })();
    }

    /**
     * Busca las transacciones especificadas por su hash. La busqueda se realiza en cache, mismo bloque y base de datos.
     * 
     * @param txids Hashes de las transacciones a buscar.
     * @param txs Transacciones del bloque actual.
     * @param network Red a la cual pertenencen las transacciones.
     */
    private async _findTxs(txids: string[], txs: Transaction[], network: SupportedNetworks): Promise<Array<Partial<ITransaction>>> {
        const findTimer = CountTime.begin();

        let foundTxs = new Array<Partial<ITransaction>>();
        txids = [...new Set(txids)]; // Removemos duplicados

        if (this._cacheTxSize > 0) // Buscamos las transacciones en caché
            txids.forEach(txid => {
                const tx = this._cacheTx[txid];

                if (!tx)
                    return;

                foundTxs.push(tx);
            });

        if (foundTxs.length < txids.length) { // Buscamos las transacciones en el mismo bloque
            const foundTxIds = foundTxs.map(f => f.txid);
            const missingTxs = txids.filter(txid => !foundTxIds.includes(txid));

            const btcTxs = txs.filter(tx => missingTxs.includes(tx.hash));

            foundTxs.push(...btcTxs.map(tx => ({ txid: tx.hash, hex: tx.toBuffer() })));
        }

        if (foundTxs.length < txids.length) { // Buscamos las transacciones en la base de datos
            const foundTxIds = foundTxs.map(f => f.txid);
            const missingTxs = txids.filter(txid => !foundTxIds.includes(txid));

            const txs = await BasedBtcTxStore.collection.find({ txid: { $in: missingTxs }, chain: SupportedAssets.Bitcoin, network }).project({ txid: 1, hex: 1, _id: 0 }).toArray();

            foundTxs.push(...txs);
        }

        if (foundTxs.length < txids.length)
            throw new Error(`${txids.length - foundTxs.length} transactions not found`);

        findTimer.stop();

        Processor.Logger.trace(`Found ${foundTxs.length} txs in ${findTimer.toLocalTimeString()}`);

        return foundTxs;
    }

    /**
     * Importa un conjunto de transacciones a la base de datos.
     * @param txs Transacciones a importar a la base de datos.
     * @param params Parametros requeridos para importar.
     * @param params.network Red a la cual pertenencen las transacciones.
     * @param params.blockHash Hash del bloque padre de las transacciones.
     * @param params.blockHeight Altura del bloque padre de las transacciones.
     * @param params.blockTime Fecha/Hora del bloque padre de las transacciones.
     */
    public async import(txs: Transaction[], params: {
        network: SupportedNetworks, blockHash: string, blockHeight: number, blockTime: Date
    }) {
        const importTimer = CountTime.begin();
        await this._loadCacheTx();

        const { network, blockHash, blockHeight, blockTime } = params;
        const txOps = new Array<BulkUpdate<ITransaction>>();

        for (const tx of txs) {
            const txid = tx.hash;
            const rawhex = tx.toBuffer();

            const txData = {
                _id: txid,
                blockHash: blockHash,
                blockHeight: blockHeight,
                blockTime: blockTime,
                coinbase: tx.isCoinbase(),
                lockTime: tx.nLockTime,
                txid,
                value: tx.outputAmount,
                size: rawhex.length,
                hex: rawhex,
                outputsCount: tx.outputs.length,
                inputsCount: tx.inputs.length
            };

            const txOp = {
                updateOne: {
                    filter: { txid, chain: SupportedAssets.Bitcoin, network },
                    update: { $set: txData },
                    upsert: true
                }
            };

            txOps.push(txOp);
        }

        await this._resolveAddressesAndFee(txOps.map(txOp => txOp.updateOne.update.$set), txs, network);

        try {
            if (txOps.length > 0) {

                const dbTimer = CountTime.begin();

                await Promise.all(Utils.partition(txOps, txOps.length / ConfigService.mongoDb.poolSize)
                    .map(ops => BasedBtcTxStore.collection.bulkWrite(ops, { ordered: false })));

                dbTimer.stop();

                Processor.Logger.trace(`Saved transactions lenght: ${txOps.length} in ${dbTimer.toLocalTimeString()}`);
            }

        } catch (ex) {
            throw new Error(`Fail to save transactions from block [hash: ${params.blockHash}], ${ex}`);
        }

        importTimer.stop();

        Processor.Logger.trace(`Imported ${txOps.length} tx from Block[${params.blockHash}], cacheTx: ${this._cacheTxSize} in ${importTimer.toLocalTimeString()}`);
    }

    /**
     * Carga transacciones en cache.
     */
    private async _loadCacheTx() {
        if (this._loadedCache)
            return;

        Processor.Logger.debug('Cache Txs is empty, loading');

        const timer = CountTime.begin();

        const cache = (await BasedBtcTxStore.collection.find().sort({ blockHeight: -1 }).limit(Processor.CACHE_INITIAL_SIZE).toArray());
        cache.forEach(tx => this._addCache(tx));

        timer.stop();

        Processor.Logger.info(`Cache Txs loaded ${Object.keys(this._cacheTx).length} in ${timer.toLocalTimeString()}`);
        this._loadedCache = true;
    }

    /**
     * Resuelve la direcciones públicas y la comisión gastada en las transacciones.
     * 
     * @param txsData Transacciones procesadas.
     * @param txs Transacciones del bloque con entradas y salidas.
     * @param network Red a la cual pertenencen las transacciones.
     */
    private async _resolveAddressesAndFee(txsData: Array<Partial<ITransaction>>, txs: Array<Transaction>, network: SupportedNetworks) {
        const inputs = txs.filter(tx => !tx.isCoinbase()).map(tx => tx.inputs).reduce((v, c) => {
            v.push(...c);
            return v;
        }, []);

        const prevTxIds = [...new Set(inputs.map(i => new Buffer(i.prevTxId).toString('hex')))];

        if (prevTxIds.length > 0) {
            let prevTxs = await this._findTxs(prevTxIds, txs, network);

            const mappingInputsTimer = CountTime.begin();

            let prevBtcTxs = [];

            for (let i = 0; i < prevTxs.length; i++)
                prevBtcTxs.push(new Transaction().fromBuffer(Buffer.from(prevTxs[i].hex.toString('hex'), 'hex')));

            const mapping = {};

            for (let i = 0; i < prevBtcTxs.length; i++)
                mapping[prevBtcTxs[i].hash] = prevBtcTxs[i];

            for (let i = 0; i < inputs.length; i++) {
                const prevTxId = new Buffer(inputs[i].prevTxId).toString('hex');
                const prevTx = mapping[prevTxId];
                if (!prevTx)
                    Processor.Logger.warn(`Tx not found [${prevTxId}]`);
                else {
                    inputs[i].output = prevTx.outputs[inputs[i].outputIndex];
                    this._removeCache(prevTxId);
                }
            }

            mappingInputsTimer.stop();

            Processor.Logger.trace(`Mapping ${inputs.length} inputs in ${mappingInputsTimer.toLocalTimeString()}`);
        }

        const resolveAddressTimer = CountTime.begin();

        let mappingTx = {};

        for (let i = 0; i < txs.length; i++)
            mappingTx[txs[i].hash] = txs[i];

        for (let i = 0; i < txsData.length; i++) {
            const txData = txsData[i];
            const tx = mappingTx[txData.txid];

            this._resolveAddresses(txData, tx);
            txData.fee = tx.getFee();

           this._addCache(txData);
        }

        resolveAddressTimer.stop();

        Processor.Logger.trace(`Address from ${txsData.length} txs resolved in ${resolveAddressTimer.toLocalTimeString()}`);

        Processor.Logger.debug(`Txs: ${txs.length}, Linked Inputs: ${inputs.length}, PrevTx: ${prevTxIds.length}`);

    }

    /**
     * Intenta remover una transacción de cache cuando todas sus salidas fueron gastadas.
     * 
     * @param txid Hash de la transacción a remover.
     */
    private _removeCache(txid: string) {
        delete this._cacheTx[txid];
        this._cacheTxSize--;
    }

    /**
     * Añade a cache una transacción.
     * 
     * @param tx Transacción a agregar.
     */
    private _addCache(tx: Partial<ITransaction>) {
        if (this._cacheTx[tx.txid])
            return;

        this._cacheTx[tx.txid] = { txid: tx.txid, hex: tx.hex };
        this._cacheTxSize++;
    }

    /**
     * Determine las direcciones públicas de cada transacción.
     * 
     * @param txData Transacciones procesadas.
     * @param tx Transacciones con entradas y salidas.
     */
    private _resolveAddresses(txData: Partial<ITransaction>, tx: Transaction) {
        let addresses = [];

        txData.addresses = txData.addresses || [];

        for (let i = 0; i < tx.outputs.length; i++)
            addresses.push(this._getAddressFromScript(tx.outputs[i].script));

        for (let i = 0; i < tx.inputs.length; i++)
            if (tx.inputs[i].output)
                addresses.push(this._getAddressFromScript(tx.inputs[i].output.script));

        txData.addresses.push(...new Set(addresses));
    }

    /**
     * Obtiene la dirección del script especificado.
     * 
     * @param script Script de la salida.
     */
    private _getAddressFromScript(script: Script) {
        if (!script)
            return null;

        // Obtenemos la función para resolver la dirección pública o devolvemos null.

        return (Processor._scriptFnAddress[script.classify().toString()] || (() => null))(script);
    }

}

/**
 * Procesador de transacciones de Bitcoin. Se encarga de importar a la base de datos cada una de 
 * las transacciones de un bloque.
 */
export const BtcTxProcessor = new Processor();