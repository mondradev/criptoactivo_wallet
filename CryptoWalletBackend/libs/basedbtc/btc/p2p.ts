import { Pool, Messages, Peer } from "bitcore-p2p";
import { Networks, Block, BlockHeader } from "bitcore-lib";
import { EventEmitter } from "events";
import LoggerFactory from "../../utils/loggin-factory";
import Utils from "../../utils";
import BlockRequest from "./block-request";

const Logger = LoggerFactory.getLogger('Bitcoin PeerToPeer');

class PeerToPeerController extends EventEmitter {

    private _pool: Pool;
    private _availableMessages: Messages;
    private _connected = false;

    /**
     * Obtiene un bloque de la red de bitcoin.
     * 
     * @param {string} blockhash Hash del bloque a descargar de la red.
     * @returns {Promise<Block>} Bloque de Bitcoin.
     */
    public getBlock(blockhash: string): Promise<Block> {
        let received = false;

        return new Promise<Block>(async (resolve) => {
            this.once(blockhash, (block: Block) => {
                received = true;
                resolve(block);
            });

            while (!received) {
                this._pool.sendMessage(this._availableMessages.GetData.forBlock(blockhash));
                await Utils.wait(1000);
            }
        });
    }

    public getRequestBlocks(hashes: string[]) {
        return new BlockRequest(hashes, this._pool, this._availableMessages.GetData);
    }

    /**
     * Obtiene la cabecera de un bloque de la red de bitcoin.
     * 
     * @param {string} hashblock hash del bloque que se desea obtener su cabecera.
     * @returns {Promise<BlockHeader>} La cabecera del bloque.
     */
    public getHeaders(hashblock: string[]): Promise<string[]> {
        return new Promise<string[]>(async (resolve) => {
            let received = false;

            this.once('headers', (headers: string[]) => {
                received = true;
                if (headers.length > 0) Logger.trace(`Received ${headers.length} blockheaders`)
                resolve(headers);
            });

            while (!received) {
                this._pool.sendMessage(this._availableMessages.GetHeaders({ starts: hashblock }));
                await Utils.wait(1000);
            }
        });
    }

    public get bestHeight() {
        if (Object.entries(this._pool['_connectedPeers']).length == 0)
            return 0;
        return Object.entries(this._pool['_connectedPeers'])
            .map(([, peer]: [string, Peer]) => peer.bestHeight)
            .reduce((left: number, right: number) => left > right ? left : right);
    }

    public async connect(): Promise<void> {
        Logger.trace('Connecting pool');

        this._addListeners();
        this._pool.connect();

        return new Promise(resolve => {
            if (this._connected)
                resolve();
            else
                this.once('ready', () => resolve());
        });
    }

    public disconnect() {
        Logger.trace('Disconnecting pool');
        this._pool.disconnect();
    }

    private _addListeners() {
        let bestHeight = 0;
        this._availableMessages = new Messages({ network: Networks.defaultNetwork });
        this._pool = new Pool({ network: Networks.defaultNetwork });
        this._pool
            .on('peerheaders', (peer: Peer, message: { headers: BlockHeader[] }) => {
                this.emit('headers', message.headers.map((h: BlockHeader) => h.hash));
            })
            .on('peerblock', (peer: Peer, message: { block: Block }) => {
                this.emit(message.block.hash, message.block);
            })
            .on('peerready', (peer: Peer) => {
                if (bestHeight < peer.bestHeight) {
                    Logger.debug(`Peer ready[BestHeight=${peer.bestHeight}, Version=${peer.version}, Host=${peer.host}, Port=${peer.port}, Network=${peer.network}]`);

                    bestHeight = peer.bestHeight;

                    this._connected = true;
                    this.emit('ready');
                }
            });
    }
}

export const PeerToPeer = new PeerToPeerController();