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
        this.pool = new Pool({ network: Networks.defaultNetwork });
        this.event = new EventEmitter();
        this.availableMessages = new Messages({ network: Networks.defaultNetwork });

        this.pool
            .on('peerheaders', (peer: Peer, message: { headers: BlockHeader[] }) => {
                this.event.emit('headers', message.headers.map((h: BlockHeader) => h.toObject()));
            })
            .on('peerblock', (peer: Peer, message: { block: Block }) => {
                this.event.emit('block', message.block);
            })
            .on('peerready', async (peer: Peer) => {
                P2pService.Logger.debug(`Peer connected ${peer.bestHeight} blocks`);
                this.event.emit('ready');
            });
    }


    /**
     * Obtiene un bloque de la red de bitcoin.
     * 
     * @param {string} blockhash Hash del bloque a descargar de la red.
     * @returns {Promise<Block>} Bloque de Bitcoin.
     */
    public async getBlock(blockhash: string): Promise<Block> {
        let receivedBlock: Block;
        let received = false;
        this.event.once('block', block => {
            received = true;
            receivedBlock = block;
        });

        let time = 0;

        while (!received) {
            if (time % 1000 == 0) {
                this.pool.sendMessage(this.availableMessages.GetData.forBlock(blockhash));
                time = 0;
            }
            await Utils.wait(10);
            time += 10;
        }

        return receivedBlock;
    }

    /**
     * Obtiene la cabecera de un bloque de la red de bitcoin.
     * 
     * @param {string} hashblock hash del bloque que se desea obtener su cabecera.
     * @returns {Promise<BlockHeader>} La cabecera del bloque.
     */
    public async getHeaders(hashblock: string[]): Promise<BlockHeaderObj[]> {
        let received = false;
        let receivedHeaders: BlockHeaderObj[];

        this.event.once('headers', headers => {
            received = true;
            receivedHeaders = headers;
        });

        let time = 0;

        while (!received) {
            if (time % 1000 == 0) {
                this.pool.sendMessage(this.availableMessages.GetHeaders({ starts: hashblock }));
                time = 0;
            }
            await Utils.wait(10);
            time += 10;
        }

        return receivedHeaders;
    }

    public get bestHeight() {
        return Object.entries(this.pool['_connectedPeers'])
            .map(([, peer]: [string, Peer]) => peer.bestHeight)
            .reduce((left: number, right: number) => left > right ? left : right);
    }

    public async connect(): Promise<void> {
        this.pool.connect();

        return new Promise(resolve => {
            this.event.once('ready', () => resolve());
        });
    }

    public disconnect() {
        this.pool.disconnect();
    }
}

export const BtcP2pService = new P2pService();