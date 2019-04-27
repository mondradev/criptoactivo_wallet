import { Block } from "bitcore-lib";
import { IBlock, BasedBtcBlockStore } from "../block-store";
import { BlockHeaderObj } from "./btc-types";
import { SupportedAssets, SupportedNetworks, ChainInfoService } from "../chaininfo-store";
import ConfigService from "../../config";
import LoggerFactory from "../../services/loggin-factory";
import { BulkUpdate } from "../../data-access/storage";
import { BtcTxProcessor } from "./txprocessor";


class Processor {

    private static Logger = LoggerFactory.getLogger('BtcBlockProcessor');

    private async _getOps(block: Block): Promise<BulkUpdate<IBlock>> {
        const chain = SupportedAssets.Bitcoin;
        const network = ConfigService.networks[chain] as SupportedNetworks;
        const header: BlockHeaderObj = block.header.toObject();
        const prevBlock = await BasedBtcBlockStore.collection.findOne({ hash: header.prevHash })
        const height = prevBlock ? prevBlock.height + 1 : 1;

        if (prevBlock) {
            await BasedBtcBlockStore.collection.updateOne({ hash: prevBlock.hash }, { $set: { nextBlock: header.hash } });
            Processor.Logger.trace(`Next Blockhash setted [hash: ${prevBlock.hash}, next: ${header.hash}]`);
        }

        return {
            updateOne: {
                filter: { hash: header.hash, chain, network },
                update: {
                    $set: {
                        _id: header.hash,
                        version: header.version,
                        nonce: header.nonce,
                        reward: block.transactions[0].outputAmount,
                        ntx: block.transactions.length,
                        time: new Date(header.time * 1000),
                        prevBlock: header.prevHash,
                        bits: header.bits,
                        height: height,
                        merkletRoot: header.merkleRoot,
                        processed: false
                    }
                },
                upsert: true
            }
        };
    }

    public async process(block: Block) {
        const ops = await this._getOps(block);

        // TODO: Check Reorg

        const writeRes = await BasedBtcBlockStore.collection.bulkWrite([ops]);
        Processor.Logger.trace(`Block [hash: ${ops.updateOne.filter.hash}] saved`);

        if (writeRes.upsertedCount == 0 && writeRes.modifiedCount == 0) {
            Promise.reject('Fail to save received block');
            return;
        }

        const processedBlock = { ...ops.updateOne.filter, ...ops.updateOne.update.$set };

        await BtcTxProcessor.import(block.transactions, {
            chain: processedBlock.chain,
            network: processedBlock.network,
            blockHash: processedBlock.hash,
            blockHeight: processedBlock.height,
            blockTime: processedBlock.time
        });

        await BasedBtcBlockStore.collection.updateOne(ops.updateOne.filter, { $set: { processed: true } });
        ChainInfoService.updateCacheTip(processedBlock as IBlock);
    }

}

export const BtcBlockProcessor = new Processor();
