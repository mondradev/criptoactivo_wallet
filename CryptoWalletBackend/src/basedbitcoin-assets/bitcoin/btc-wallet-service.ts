import { Networks } from "bitcore-lib";
import LoggerFactory from "../../services/loggin-factory";
import IWalletService from "../wallet-service";
import ConfigService from "src/config";
import { SupportedAssets, SupportedNetworks, ChainInfoService } from "../chaininfo-store";
import { BtcP2pService } from "./btc-p2p-service";
import Utils from "src/utils/utils";
import { BtcBlockProcessor } from "./blockprocessor";
import { BlockHeaderObj } from "./btc-types";

class Wallet implements IWalletService {


    private static Logger = LoggerFactory.getLogger('Bitcoin');

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
        const network = ConfigService.networks[SupportedAssets.Bitcoin] as SupportedNetworks;

        this._enableNetwork(network);

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
                        await Utils.wait(1000);
                    else if (headers)
                        break;
                }

                for (const header of headers) {
                    const block = await BtcP2pService.getBlock(header.hash);
                    await BtcBlockProcessor.process(block);
                }

            }

        } catch (ex) {
            BtcP2pService.disconnect();
            return this.sync();
        }
    }

}

export const BtcWallet = new Wallet();