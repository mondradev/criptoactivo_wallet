import { Networks, Block } from "bitcore-lib";
import LoggerFactory from "../../services/loggin-factory";
import IWalletService from "../wallet-service";
import ConfigService from "../../config";
import { SupportedAssets, SupportedNetworks, ChainInfoService } from "../chaininfo-store";
import { BtcP2pService } from "./btc-p2p-service";
import Utils from "../../utils/utils";
import { BtcBlockProcessor } from "./blockprocessor";
import { BlockHeaderObj } from "./btc-types";
import CountTime from "../../utils/counttime";
import { BasedBtcTxStore } from "../tx-store";
import { EventEmitter } from "events";

class Wallet implements IWalletService {
    private _downloaded: boolean;

    private static Logger = LoggerFactory.getLogger('Bitcoin');

    private _currentNetwork: SupportedNetworks;
    private _notifier = new EventEmitter();


    constructor() {
        this._currentNetwork = ConfigService.networks[SupportedAssets.Bitcoin] as SupportedNetworks;
        this._enableNetwork(this._currentNetwork);
    }

    public async getRawTransaction(txid: string): Promise<string> {
        const tx = await BasedBtcTxStore.collection.findOne({ txid, chain: SupportedAssets.Bitcoin, network: this._currentNetwork });

        return tx.hex.toString('hex');
    }

    public async getRawTransactionsByAddress(address: string): Promise<string[]> {
        const txs = await BasedBtcTxStore.collection.find({ addresses: address, chain: SupportedAssets.Bitcoin, network: this._currentNetwork }).toArray();

        return txs.map(tx => tx.hex.toString('hex'));
    }

    public async broadcastTrx(rawTx: string): Promise<boolean> {

        return false;
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
        const timer = new CountTime();

        let tip = (await ChainInfoService.getTip(SupportedAssets.Bitcoin,
            ConfigService.networks.bitcoin as SupportedNetworks)).height;

        timer.on('second', async () => {
            let lastBlock = tip;
            tip = (await ChainInfoService.getTip(SupportedAssets.Bitcoin, SupportedNetworks.testnet)).height;
            const blockRate = tip - lastBlock;
            const blockleft = BtcP2pService.bestHeight - tip;

            Wallet.Logger.info(`BlockRate ${blockRate} blk/s, ${blockleft} block left`);
        });

        await BtcP2pService.connect();

        Wallet.Logger.info('Service started');

        this._downloaded = false;

        try {
            while (true) {

                timer.start();

                let headers: BlockHeaderObj[];
                let hashes = await ChainInfoService
                    .getHashes(SupportedAssets.Bitcoin, SupportedNetworks.testnet);

                while (true) {
                    headers = await BtcP2pService.getHeaders(hashes);

                    if (headers && headers.length == 0)
                        await Utils.wait(1000);
                    else if (headers)
                        break;
                }

                for (const header of headers) {
                    Wallet.Logger.trace(`Received block [Hash: ${header.hash}, Prev: ${header.prevHash}]`);
                    const block = await BtcP2pService.getBlock(header.hash);
                    await BtcBlockProcessor.import(block);
                }

                timer.stop();

                Wallet.Logger.debug(`Processed ${headers.length} blocks in ${timer.toLocalTimeString()}`)

            }

        } catch (ex) {
            BtcP2pService.disconnect();
            Wallet.Logger.error(`Fail to download blockchain: ${ex}`);
            return this.sync();
        }
    }

    public onDownloaded(fnCallback: () => void) {
        this._notifier.on('downloaded', fnCallback);
    }

}

export const BtcWallet = new Wallet();