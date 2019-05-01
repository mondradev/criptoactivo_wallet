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
    private static MAX_CACHE_TX_SIZE = 200000;

    private cacheTx = new Array<Partial<ITransaction>>();
    private loadedCache = false;

    private async findTxs(txids: string[], txs: Transaction[], chain: SupportedAssets, network: SupportedNetworks): Promise<Array<Partial<ITransaction>>> {
        const findTimer = CountTime.begin();

        let foundTxs = new Array<Partial<ITransaction>>();
        txids = [...new Set(txids)];

        if (this.cacheTx && this.cacheTx.length > 0)
            txids.forEach(txid => foundTxs.push(this.cacheTx[txid]));

        if (foundTxs.length < txids.length) {
            const foundTxIds = foundTxs.map(f => f.txid);
            const missingTxs = txids.filter(txid => !foundTxIds.includes(txid));

            const btcTxs = txs.filter(tx => missingTxs.includes(tx.hash));

            foundTxs.push(...btcTxs.map(tx => ({ txid: tx.hash, hex: tx.toBuffer() })));
        }

        if (foundTxs.length < txids.length) {
            const foundTxIds = foundTxs.map(f => f.txid);
            const missingTxs = txids.filter(txid => !foundTxIds.includes(txid));

            const txs = await BasedBtcTxStore.collection.find({ txid: { $in: missingTxs } }).project({ txid: 1, hex: 1, _id: 0 }).toArray();

            foundTxs.push(...txs);
        }

        if (foundTxs.length < txids.length)
            throw new Error(`Transactions not found`);

        findTimer.stop();

        Processor.Logger.trace(`Found ${foundTxs.length} txs in ${findTimer.toLocalTimeString()}`);

        return foundTxs;
    }

    public async import(txs: Transaction[], params: {
        chain: SupportedAssets, network: SupportedNetworks, blockHash: string, blockHeight: number, blockTime: Date
    }) {
        const importTimer = CountTime.begin();

        await this._loadCacheTx();

        const { chain, network, blockHash, blockHeight, blockTime } = params;
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
                    filter: { txid, chain, network },
                    update: { $set: txData },
                    upsert: true
                }
            };

            txOps.push(txOp);
        }

        await this._resolveAddressesAndFee(txOps.map(txOp => txOp.updateOne.update.$set), txs, chain, network);

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

        Processor.Logger.trace(`Imported ${txOps.length} tx from Block[${params.blockHash}], cacheTx: ${Object.keys(this.cacheTx).length} in ${importTimer.toLocalTimeString()}`);
    }

    public async _loadCacheTx() {
        if (this.loadedCache)
            return;

        Processor.Logger.debug('Cache Txs is empty, loading');

        const timer = CountTime.begin();

        const cache = (await BasedBtcTxStore.collection.find().sort({ blockHeight: -1 }).limit(10000).toArray());
        cache.forEach(tx => this._addCache(tx));

        timer.stop();

        Processor.Logger.info(`Cache Txs loaded ${Object.keys(this.cacheTx).length} in ${timer.toLocalTimeString()}`);
        this.loadedCache = true;

        (async () => {
            while (true) {
                if (Object.keys(this.cacheTx).length > Processor.MAX_CACHE_TX_SIZE)
                    Object.keys(this.cacheTx).slice(0, Processor.MAX_CACHE_TX_SIZE / 2).forEach(k => delete this.cacheTx[k]);
                await Utils.wait(60000);
            }
        })();
    }

    private async _resolveAddressesAndFee(txsData: Array<Partial<ITransaction>>, txs: Array<Transaction>, chain: SupportedAssets, network: SupportedNetworks) {
        const inputs = txs.filter(tx => !tx.isCoinbase()).map(tx => tx.inputs).reduce((v, c) => {
            v.push(...c);
            return v;
        }, []);

        const prevTxIds = [...new Set(inputs.map(i => new Buffer(i.prevTxId).toString('hex')))];

        if (prevTxIds.length > 0) {
            let prevTxs = await this.findTxs(prevTxIds, txs, chain, network);

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

                    this._removeCache(prevTxId, inputs[i].outputIndex);
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

    private async _removeCache(txid: string, index: number) {
        const tx = this.cacheTx[txid];

        if (!tx)
            return;

        tx['spends'] = tx['spends'] || [];

        tx['spends'][index] = true;

        const spentTx = tx['spends'].filter((s: boolean) => s).length == tx['spends'].lenght;

        if (spentTx)
            delete this.cacheTx[txid];
    }

    private _addCache(tx: Partial<ITransaction>) {
        if (!this.cacheTx[tx.txid])
            this.cacheTx[tx.txid] = { txid: tx.txid, hex: tx.hex };

        tx['spends'] = [];
    }

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

    private scriptClassify = {
        'Pay to public key': (script: Script) => new Address(new PublicKey(script.getPublicKey()), Networks.defaultNetwork).toString(),
        'Pay to public key hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toString(),
        'Pay to script hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toString()
    };

    /**
     * Obtiene la dirección del script especificado.
     * 
     * @param script Script de la salida.
     */
    private _getAddressFromScript(script: Script) {
        if (!script)
            return null;

        // Obtenemos la función para resolver la dirección pública o devolvemos null.

        return (this.scriptClassify[script.classify().toString()] || (() => null))(script);
    }

}

export const BtcTxProcessor = new Processor();