import { Script, PublicKey, Address, Networks, Transaction } from "bitcore-lib";
import LoggerFactory from "../../services/loggin-factory";
import { BulkUpdate } from "../../data-access/storage";
import { ICoin } from "../coin-store";
import { SupportedAssets, SupportedNetworks } from "../chaininfo-store";
import Utils from "../../utils/utils";

class Processor {

    public processCoins(txs: Transaction[], params: {
        chain: SupportedAssets; network: SupportedNetworks; blockHash: string; blockHeight: number; blockTime: Date;
    }) {
        const coinOps = new Array<BulkUpdate<ICoin>>();
        const { blockHeight, chain, network } = params;

        for (const tx of txs) {
            const outputs = tx.outputs;
            const txid = tx.hash.toString('hex');

            for (const [index, output] of outputs.entries()) {
                let coin: Partial<ICoin>;

                if (Utils.isNull(output.script))
                    coin = { address: '(Nonstandard)', script: null, multi: false };
                else {
                    let address: string = this._getAddressFromScript(output.script);
                    let isP2SM = address === '(Multisign)';

                    coin = { address, script: output.script.toHex(), multi: isP2SM };
                }

                coin.amount = output.satoshis;
                coin.height = blockHeight;
                coin._id = `${txid}_${index}`;
                coin.chain = chain;
                coin.network = network;

                coinOps.push({
                    updateOne: {
                        filter: { parentTx: txid, index },
                        update: { $set: coin },
                        upsert: true
                    }
                });
            }
            return coinOps;
        }
    }

    public processSpendCoins(txs: Transaction[], params: {
        chain: SupportedAssets; network: SupportedNetworks; blockHash: string; blockHeight: number; blockTime: Date;
    }, coinsOps: Array<BulkUpdate<ICoin>>) {
        const { blockHeight, chain, network } = params;
        const spendOps = new Array<BulkUpdate<ICoin>>();
        const mapCoinOps: Map<string, ICoin> = coinsOps
            .reduce((map, v) => {
                map[v.updateOne.filter.parentTx + '_' + v.updateOne.filter.index] = v.updateOne.update.$set; return map
            }, new Map<string, ICoin>());

        for (const tx of txs) {
            if (tx.isCoinbase())
                continue;

            const inputs = tx.inputs;
            const txid = tx.hash.toString('hex');

            for (const input of inputs) {
                let prevTxid = input.prevTxId.toString('hex');

                const spendOp = {
                    updateOne: {
                        filter: {
                            index: input.outputIndex,
                            parentTx: prevTxid,
                            chain,
                            network
                        },
                        update: {
                            $set: {
                                spentTx: txid,
                                spentHeight: blockHeight,
                                _id: `${prevTxid}_${input.outputIndex}`
                            }
                        },
                        upsert: true
                    }
                };

                const coinOp: ICoin = mapCoinOps[
                    spendOp.updateOne.filter.parentTx + '_' + spendOp.updateOne.filter.index
                ];

                if (coinOp) {
                    coinOp.spentTx = spendOp.updateOne.update.$set.spentTx;
                    coinOp.spentHeight = spendOp.updateOne.update.$set.spentHeight;
                } else
                    spendOps.push(spendOp);
            }
        }

        return spendOps;
    }


    private static Logger = LoggerFactory.getLogger('BtcCoinProcessor');

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
                Processor.Logger.debug(`Can't resolve address from script ${script.classify()}`);
            case 'Unknown':
                address = '(Nonstandard)';
                break;
        }

        return address;
    }


}

export const BtcCoinProcessor = new Processor();