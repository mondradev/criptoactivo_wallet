import { Networks, Block } from "bitcore-lib";
import LoggerFactory from "../../utils/loggin-factory";
import IWalletService from "../wallet-service";
import ConfigService from "../../../config.json";
//import { SupportedAssets, SupportedNetworks, ChainInfoService } from "../chaininfo-store";
//import { BtcP2pService } from "./btc-p2p-service";
import Utils from "../../../libs/utils";
//import { BtcBlockProcessor } from "./blockprocessor";
//import { BlockHeaderObj } from "./btc-types";
import CountTime from "../../utils/counttime";
//import { BasedBtcTxStore } from "../tx-store";
import { EventEmitter } from "events";
import { PeerToPeer } from "./p2p";
import { BlockStore } from "../blocks";
import TimeSpan from "../../../libs/utils/timespan";

const Logger = LoggerFactory.getLogger('Bitcoin Backend');

class Wallet implements IWalletService {
    getRawTransactionsByAddress(address: string): Promise<string[]> {
        throw new Error("Method not implemented.");
    }
    getRawTransaction(txid: string): Promise<string> {
        throw new Error("Method not implemented.");
    }
    broadcastTrx(transaction: string): Promise<boolean> {
        throw new Error("Method not implemented.");
    }
    private _downloaded: boolean;



    private _currentNetwork: string;
    private _notifier = new EventEmitter();


    constructor() {
        this._currentNetwork = ConfigService.networks['bitcoin'];
        this._enableNetwork(this._currentNetwork);
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
        let i = 0;
        let last = 0;


        timer.on('second', async () => {
            const blockRate = i - last;
            const blockleft = PeerToPeer.bestHeight - i;

            if (blockRate > 0) {
                Logger.info(`BlockRate=${blockRate} blk/s, BlockLeft=${blockleft}, TimeLeft=${TimeSpan.FromSeconds(blockleft / blockRate)}`);
                last = i;
            }
        });

        await PeerToPeer.connect();

        Logger.info('Initializing blockchain download');

        this._downloaded = false;

        try {
            let lastHashes = null;

            let headers: string[];
            let hashes = lastHashes || [Array(65).join('0')]
            let height = 0;
            
            while (true) {

                timer.start();

                while (true) {
                    headers = await PeerToPeer.getHeaders(hashes);

                    if (headers && headers.length == 0)
                        await Utils.wait(1000);
                    else if (headers)
                        break;
                }

                const blockRequest = PeerToPeer.getRequestBlocks(headers);
                let block = null;

                do {
                    block = await blockRequest.next();

                    if (block) {
                        lastHashes = [block.hash]
                        height++;
                        Logger.trace(`Received block [Hash=${block.hash}, Height=${height}]`);

                        await BlockStore.import(block);
                        i++;
                    }

                } while (block != null)

                timer.stop();

                Logger.debug(`ProcessedBlocks=${headers.length}, Time=${timer.toLocalTimeString()}`)

            }

        } catch (ex) {
            PeerToPeer.disconnect();
            Logger.error(`Error="Fail to download blockchain", Exception=${ex.stack}`);
            return this.sync();
        }
    }

    public onDownloaded(fnCallback: () => void) {
        this._notifier.on('downloaded', fnCallback);
    }

}

export const BtcWallet = new Wallet();