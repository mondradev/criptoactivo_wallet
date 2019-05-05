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

    /**
     * Obtiene el bloque con la estructura requerida para ser almacenado en la base de datos.
     * 
     * @param block Bloque a procesar.
     */
    private async _getOps(block: Block): Promise<BulkUpdate<IBlock>> {
        const chain = SupportedAssets.Bitcoin;
        const network = ConfigService.networks[chain] as SupportedNetworks;
        const header: BlockHeaderObj = block.header.toObject();
        const prevBlock = await BasedBtcBlockStore.collection.findOne({ hash: header.prevHash, chain, network })
        const height = prevBlock ? prevBlock.height + 1 : 1;

        if (prevBlock) {
            await BasedBtcBlockStore.collection.updateOne({ hash: prevBlock.hash, chain, network }, { $set: { nextBlock: header.hash } });
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

    /**
     * Importa un bloque a la base de datos.
     * 
     * @param block Bloque a importar a la base de datos.
     */
    public async import(block: Block) {
        const ops = await this._getOps(block);

        // TODO: Check Reorg

        const writeRes = await BasedBtcBlockStore.collection.bulkWrite([ops]);
        Processor.Logger.trace(`Block [hash: ${ops.updateOne.filter.hash}] saved`);

        if (writeRes.result.ok == 0)
            throw new Error(`Fail to save received block [${ops.updateOne.filter.hash}]`);

        const processedBlock = { ...ops.updateOne.filter, ...ops.updateOne.update.$set };

        await BtcTxProcessor.import(block.transactions, {
            network: processedBlock.network,
            blockHash: processedBlock.hash,
            blockHeight: processedBlock.height,
            blockTime: processedBlock.time
        });

        await BasedBtcBlockStore.collection.updateOne(ops.updateOne.filter, { $set: { processed: true } });
        ChainInfoService.updateCacheTip(processedBlock as IBlock);
    }

}

/**
 * Procesador de bloques de Bitcoin. Se encarga de importar los bloques a la base de datos.
 */
export const BtcBlockProcessor = new Processor();
