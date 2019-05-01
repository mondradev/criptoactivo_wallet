import { Networks } from "bitcore-lib";
import LoggerFactory from "../../services/loggin-factory";
import IWalletService from "../wallet-service";
import ConfigService from "../../config";
import { SupportedAssets, SupportedNetworks, ChainInfoService } from "../chaininfo-store";
import { BtcP2pService } from "./btc-p2p-service";
import Utils from "../../utils/utils";
import { BtcBlockProcessor } from "./blockprocessor";
import { BlockHeaderObj } from "./btc-types";
import CountTime from "../../utils/counttime";

class Wallet implements IWalletService {


    private static Logger = LoggerFactory.getLogger('Bitcoin');

    constructor() {
        const network = ConfigService.networks[SupportedAssets.Bitcoin] as SupportedNetworks;
        this._enableNetwork(network);
    }

    getHistorial(address: string): Promise<[]> {
        throw new Error("Method not implemented.");
    }

    getBalance(address: string): Promise<number> {
        throw new Error("Method not implemented.");
    }

    getTransaction(txid: string): Promise<string> {
        throw new Error("Method not implemented.");
    }

    broadcastTrx(transaction: string): Promise<boolean> {
        throw new Error("Method not implemented.");
    }

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

    public async sync() {
        let lastProgressLog = 0;
        let timeElapsed = 0;
        let blockleft = 0;
        let tip = (await ChainInfoService.getTip(SupportedAssets.Bitcoin,
            ConfigService.networks.bitcoin as SupportedNetworks)).height;

        await BtcP2pService.connect();

        Wallet.Logger.info('Service started');

        try {
            while (true) {

                let headers: BlockHeaderObj[];
                let hashes = await ChainInfoService
                    .getHashes(SupportedAssets.Bitcoin, SupportedNetworks.testnet);

                while (true) {
                    headers = await BtcP2pService.getHeaders(hashes);

                    if (headers && headers.length == 0)
                        await Utils.wait(100);
                    else if (headers)
                        break;
                }

                const timer = CountTime.begin();
                const blocksEnumerable = BtcP2pService.getBlocks(headers.map(h => h.hash));

                let block = null;

                do {
                    block = await blocksEnumerable.next();

                    if (block == null)
                        break;

                    const prevHash = new Buffer(block.header.prevHash);

                    Wallet.Logger.trace(`Received block [Hash: ${block.hash}, Prev: ${prevHash.reverse().toString('hex')}]`);
                    await BtcBlockProcessor.process(block);

                    timeElapsed = Date.now() - lastProgressLog;

                    if (timeElapsed > 1000) {
                        let lastBlock = tip;
                        tip = (await ChainInfoService.getTip(SupportedAssets.Bitcoin, SupportedNetworks.testnet)).height;
                        const blockRate = tip - lastBlock;
                        blockleft = BtcP2pService.bestHeight - tip;
                        lastProgressLog = Date.now();

                        Wallet.Logger.info(`BlockRate ${blockRate} blk/s, ${blockleft} block left`);
                    }
                    
                } while (block != null);

                timer.stop();

                Wallet.Logger.debug(`Processed 2000 blocks in ${timer.toLocalTimeString()}`)

            }

        } catch (ex) {
            BtcP2pService.disconnect();
            Wallet.Logger.warn(`Fail to download blockchain: ${ex}`);
            return this.sync();
        }
    }

}

export const BtcWallet = new Wallet();