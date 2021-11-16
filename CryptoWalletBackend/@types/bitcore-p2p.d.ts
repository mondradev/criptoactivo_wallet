declare module "bitcore-p2p" {

    import { Network, Block, encoding, BlockHeader, Transaction, BN } from "bitcore-lib"

    export class Inventory {

        public static TYPE: {
            ERROR: number,
            TX: number,
            BLOCK: number,
            FILTERED_BLOCK: number
        }

        public static TYPE_NAME: Array<string>

        public get type(): number
        public get hash(): Buffer

        public constructor(obj: { type: number, hash: Buffer })

        public toBuffer(): Buffer
        public toBufferWriter(): encoding.BufferWriter

        public static fromBuffer(payload: Buffer): Inventory
        public static fromBufferReader(payload: encoding.BufferWriter): Inventory
        public static forItem(type: number, hash: Buffer): Inventory
        public static forBlock(hash: Buffer): Inventory
        public static forFilteredBlock(hash: Buffer): Inventory
        public static forTransaction(hash: Buffer): Inventory
    }

    export class Message {
        public toBuffer(): Buffer
        public serialize(): Buffer
        public setPayload(payload: Buffer): void
        public getPayload(): Buffer

        public constructor(options: { command: string, network: Network })
    }

    export class MempoolMessage extends Message { }

    class InventoryMessage<T> extends Message {
        public forTransaction(hash: Buffer): T
        public forBlock(hash: Buffer): T
        public forFilteredBlock(hash: Buffer): T
    }

    export class BlockMessage extends Message {
        public get block(): Block

        public constructor(arg: Block, options: { network: Network, Block: Function })
    }

    export class VersionMessage extends Message {
        public get version(): number
        public get nonce(): Buffer
        public get services(): BN
        public get timestamp(): Date
        public get subversion(): string
        public get startHeight(): number
        public get relay(): boolean
    }

    export class TxMessage extends Message {
        public get transaction(): Transaction

        public constructor(arg: Transaction, options: { network: Network, Transaction: Function })
    }

    export class InvMessage extends Message {
        public get inventory(): Inventory[]
        public constructor(arg: Array<any>, options: { network: Network })
    }

    export class GetdataMessage extends Message {
        public get inventory(): Inventory[]
        public constructor(arg: Array<Inventory>, options: { network: Network })
    }

    export class HeaderMessage extends Message {
        public constructor(arg: BlockHeader[], options: { network: Network, BlockHeader: Function })
    }

    export class PingMessage extends Message {
        public constructor(arg: Buffer, options: { network: Network })
    }

    export class PongMessage extends Message {
        public constructor(arg: Buffer, options: { network: Network })
    }

    export class TransactionMessage extends Message {
        public constructor(arg: Transaction, options: { network: Network, Transaction: Function })
    }

    export class GetheadersMessage extends Message {
        public constructor(arg: { starts: string[], stop: string }, options: { network: Network, protocolVersion: number })
    }

    export class HeadersMessage extends Message {
        public get headers(): BlockHeader[]
        public constructor(arg: BlockHeader[], { network: Network, BlockHeader: Function })
    }

    export class Messages {
        public constructor(options?: {
            network?: Network,
            Block?: Function,
            BlockHeader?: Function,
            MerkleBlock?: Function,
            Transaction?: Function,
            protocolVersion?: number
        })

        public parseBuffer(dataBuffer: Buffer): Message
        public add(key: string, name: string, Command: Function): void

        Ping: (arg?: Buffer, options?: { network: Network }) => PingMessage
        Pong: (arg?: Buffer, options?: { network: Network }) => PongMessage
        Transaction: (arg: Transaction, options?: { network: Network, Transaction: Function }) => TransactionMessage
        Block: (arg: Block, options?: { network: Network, Block: Function }) => BlockMessage
        GetData: InventoryMessage<GetdataMessage>
        Inventory: InventoryMessage<InvMessage>
        Headers: (arg: BlockHeader[], options: { network: Network, BlockHeader: Function }) => HeaderMessage
        GetHeaders: (arg: { starts: string[], stop?: string }, options?: { network: Network, protocolVersion: number }) => GetheadersMessage
        MemPool: () => MempoolMessage
    }

    export class Peer {
        static MAX_RECEIVE_BUFFER: number;
        static STATUS: {
            CONNECTED: string;
            CONNECTING: string;
            DISCONNECTED: string;
            READY: string;
        };
        constructor(options: any);
        socket: any;
        host: string;
        port: number;
        status: string;
        network: Network;
        messages: Messages;
        dataBuffer: any;
        version: any;
        bestHeight: number;
        subversion: string;
        relay: boolean;
        versionSent: any;
        addListener(type: any, listener: any): any;
        connect(): any;
        disconnect(): Peer;
        emit(type: any, ...args: any[]): any;
        eventNames(): any;
        getMaxListeners(): any;
        listenerCount(type: any): any;
        listeners(type: any): any;
        off(type: any, listener: any): any;
        on(type: any, listener: any): any;
        once(type: any, listener: any): any;
        prependListener(type: any, listener: any): any;
        prependOnceListener(type: any, listener: any): any;
        rawListeners(type: any): any;
        removeAllListeners(type: any, ...args: any[]): any;
        removeListener(type: any, listener: any): any;
        sendMessage(message: Message): void;
        setMaxListeners(n: any): any;
        setProxy(host: any, port: any): any;
    }
    export class Pool {
        static MaxConnectedPeers: number;
        static PeerEvents: string[];
        static RetrySeconds: number;
        constructor(options: any);
        keepalive: any;
        listenAddr: any;
        dnsSeed: any;
        maxSize: any;
        messages: any;
        network: any;
        relay: any;
        addListener(type: any, listener: any): any;
        connect(): Pool;
        disconnect(): Pool;
        emit(type: any, args: any): any;
        eventNames(): any;
        getMaxListeners(): any;
        inspect(): string;
        listen(): void;
        listenerCount(type: any): any;
        listeners(type: any): any;
        numberConnected(): number;
        off(type: any, listener: any): any;
        on(type: 'peerready' | 'peerdisconnect' | 'peerversion' | 'peerinv' | 'peergetdata' | 'peerping' | 'peerpong' | 'peeraddr' |
            'peergetaddr' | 'peerverack' | 'peerreject' | 'peeralert' | 'peerheaders' | 'peerblock' | 'peermerkleblock' |
            'peertx' | 'peergetblocks' | 'peergetheaders' | 'peererror' | 'peerfilterload' | 'peerfilteradd' |
            'filterclear', listener: any): any;
        once(type: 'peerready' | 'peerdisconnect' | 'peerversion' | 'peerinv' | 'peergetdata' | 'peerping' | 'peerpong' | 'peeraddr' |
            'peergetaddr' | 'peerverack' | 'peerreject' | 'peeralert' | 'peerheaders' | 'peerblock' | 'peermerkleblock' |
            'peertx' | 'peergetblocks' | 'peergetheaders' | 'peererror' | 'peerfilterload' | 'peerfilteradd' |
            'filterclear', listener: any): any;
        prependListener(type: any, listener: any): any;
        prependOnceListener(type: any, listener: any): any;
        rawListeners(type: any): any;
        removeAllListeners(type?: 'peerready' | 'peerdisconnect' | 'peerversion' | 'peerinv' | 'peergetdata' | 'peerping' | 'peerpong' | 'peeraddr' |
            'peergetaddr' | 'peerverack' | 'peerreject' | 'peeralert' | 'peerheaders' | 'peerblock' | 'peermerkleblock' |
            'peertx' | 'peergetblocks' | 'peergetheaders' | 'peererror' | 'peerfilterload' | 'peerfilteradd' |
            'filterclear', ...args: any[]): any;

        removeListener(type: 'peerready' | 'peerdisconnect' | 'peerversion' | 'peerinv' | 'peergetdata' | 'peerping' | 'peerpong' | 'peeraddr' |
            'peergetaddr' | 'peerverack' | 'peerreject' | 'peeralert' | 'peerheaders' | 'peerblock' | 'peermerkleblock' |
            'peertx' | 'peergetblocks' | 'peergetheaders' | 'peererror' | 'peerfilterload' | 'peerfilteradd' |
            'filterclear', listener: any): any;

        sendMessage(message: any): void;
        setMaxListeners(n: any): any;
    }
}