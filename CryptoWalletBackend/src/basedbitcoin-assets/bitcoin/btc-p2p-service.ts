import { Pool, Messages, Peer } from "bitcore-p2p";
import { Networks, Block, BlockHeader } from "bitcore-lib";
import { EventEmitter } from "events";
import LoggerFactory from "../../services/loggin-factory";
import Utils from "../../utils/utils";
import { BlockHeaderObj } from "./btc-types";

class P2pService {
    private static Logger = LoggerFactory.getLogger('Bitcoin-P2P');

    private pool: Pool;
    private event: EventEmitter;
    private availableMessages: any;

    constructor() {
        this.event = new EventEmitter();
    }

    /**
     * Obtiene un bloque de la red de bitcoin.
     * 
     * @param {string} blockhash Hash del bloque a descargar de la red.
     * @returns {Promise<Block>} Bloque de Bitcoin.
     */
    public getBlock(blockhash: string): Promise<Block> {
        let received = false;

        return new Promise<Block>(async (resolve) => {
            this.event.once(blockhash, (block: Block) => {
                received = true;
                resolve(block);
            });

            while (!received) {
                this.pool.sendMessage(this.availableMessages.GetData.forBlock(blockhash));
                await Utils.wait(1000);
            }
        });
    }

    /**
     * Obtiene la cabecera de un bloque de la red de bitcoin.
     * 
     * @param {string} hashblock hash del bloque que se desea obtener su cabecera.
     * @returns {Promise<BlockHeader>} La cabecera del bloque.
     */
    public getHeaders(hashblock: string[]): Promise<BlockHeaderObj[]> {
        let received = false;
        return new Promise<BlockHeaderObj[]>(async (resolve) => {
            this.event.once('headers', (headers: BlockHeaderObj[]) => {
                received = true;
                resolve(headers);
            });

            while (!received) {
                this.pool.sendMessage(this.availableMessages.GetHeaders({ starts: hashblock }));
                await Utils.wait(1000);
            }
        });
    }

    public get bestHeight() {
        if (Object.entries(this.pool['_connectedPeers']).length == 0)
            return 0;
        return Object.entries(this.pool['_connectedPeers'])
            .map(([, peer]: [string, Peer]) => peer.bestHeight)
            .reduce((left: number, right: number) => left > right ? left : right);
    }

    public async connect(): Promise<void> {
        this._preparePool();
        this.pool.connect();

        return new Promise(resolve => {
            this.event.once('ready', () => resolve());
        });
    }

    private _preparePool() {
        let firstConnected = false;
        this.availableMessages = new Messages({ network: Networks.defaultNetwork });
        this.pool = new Pool({ network: Networks.defaultNetwork });
        this.pool
            .on('peerheaders', (peer: Peer, message: { headers: BlockHeader[] }) => {
                this.event.emit('headers', message.headers.map((h: BlockHeader) => h.toObject()));
            })
            .on('peerblock', (peer: Peer, message: { block: Block }) => {
                this.event.emit(message.block.hash, message.block);
            })
            .on('peerready', (peer: Peer) => {
                if (!firstConnected || this.bestHeight < peer.bestHeight) {
                    firstConnected = true;
                    P2pService.Logger.debug(`Peer connected ${peer.bestHeight} blocks`);
                    this.event.emit('ready');
                }
            });
    }

    public disconnect() {
        this.pool.disconnect();
    }
}

export const BtcP2pService = new P2pService();