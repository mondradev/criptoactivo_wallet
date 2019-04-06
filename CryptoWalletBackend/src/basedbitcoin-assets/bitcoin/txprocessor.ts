import { Transaction } from "bitcore-lib";
import { SupportedAssets, SupportedNetworks } from "../chaininfo-store";
import LoggerFactory from "../../services/loggin-factory";
import { BtcCoinProcessor } from "./coinprocessor";
import { BulkUpdate } from "../../data-access/storage";
import { ITransaction, BasedBtcTxStore } from "../tx-store";
import Utils from "../../utils/utils";
import ConfigService from "../../config";
import { BasedBtcCoinStore } from "../coin-store";


class Processor {

    private static Logger = LoggerFactory.getLogger('BtcTxProcessor');

    public async import(txs: Transaction[], params: {
        chain: SupportedAssets, network: SupportedNetworks, blockHash: string, blockHeight: number, blockTime: Date
    }) {
        const { chain, network, blockHash, blockHeight, blockTime } = params;
        const coinsOps = BtcCoinProcessor.processCoins(txs, params);
        const spendsOps = BtcCoinProcessor.processSpendCoins(txs, params, coinsOps);
        const txOps = new Array<BulkUpdate<ITransaction>>();

        for (const tx of txs) {
            const txid = tx.hash.toString('hex');
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
                fee: 0,
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

        try {
            if (spendsOps.length > 0) {
                await Promise.all(Utils.partition(spendsOps, spendsOps.length / ConfigService.mongoDb.poolSize)
                    .map(ops => BasedBtcCoinStore.collection.bulkWrite(ops, { ordered: false })));
                Processor.Logger.debug(`Saved spends lenght: ${spendsOps.length}`);
            }

            if (coinsOps.length > 0) {
                await Promise.all(Utils.partition(coinsOps, coinsOps.length / ConfigService.mongoDb.poolSize)
                    .map(ops => BasedBtcCoinStore.collection.bulkWrite(ops, { ordered: false })));
                Processor.Logger.debug(`Saved coins lenght: ${coinsOps.length}`);
            }

            let spends = await BasedBtcCoinStore.collection.aggregate<{ _id: string, amount: number }>([
                { $match: { spentTx: { $in: txs.map(t => t.hash.toString('hex')) }, chain, network } },
                { $group: { _id: "$spentTx", amount: { $sum: "$amount" } } }
            ]).toArray();

            let mapping = txOps.filter(tx => !tx.updateOne.update.$set.coinbase)
                .reduce((map, value) => {
                    map[value.updateOne.update.$set.txid] = value.updateOne.update.$set;
                    return map;
                }, new Map<string, ITransaction>());

            if (mapping.size > 0)
                for (const spend of spends)
                    mapping[spend._id].fee = spend.amount - mapping[spend._id].value;

            Processor.Logger.debug(`Fee calculated`);

            if (txOps.length > 0) {
                await Promise.all(Utils.partition(txOps, txOps.length / ConfigService.mongoDb.poolSize)
                    .map(ops => BasedBtcTxStore.collection.bulkWrite(ops, { ordered: false })));
                Processor.Logger.debug(`Saved transactions lenght: ${txOps.length}`);
            }

        } catch (ex) {
            Promise.reject(`Fail to save transactions from block [hash: ${params.blockHash}], ${ex}`);
        }
    }

}

export const BtcTxProcessor = new Processor();