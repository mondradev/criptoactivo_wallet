import { Network } from "bitcore-lib";

export class BloomFilter {
    static BLOOM_UPDATE_ALL: number;
    static BLOOM_UPDATE_NONE: number;
    static BLOOM_UPDATE_P2PUBKEY_ONLY: number;
    static LN2: number;
    static LN2SQUARED: number;
    static MAX_BLOOM_FILTER_SIZE: number;
    static MAX_HASH_FUNCS: number;
    static MIN_HASH_FUNCS: number;
    static MurmurHash3(seed: any, data: any): any;
    static create(elements: any, falsePositiveRate: any, nTweak: any, nFlags: any): any;
    static fromBuffer(payload: any): any;
    constructor(arg: any);
    vData: any;
    nHashFuncs: any;
    nTweak: any;
    nFlags: any;
    clear(): void;
    contains(data: any): any;
    hash(nHashNum: any, vDataToHash: any): any;
    insert(data: any): any;
    inspect(): any;
    toBuffer(): any;
    toObject(): any;
}
export class Inventory {
    static TYPE: {
        BLOCK: number;
        ERROR: number;
        FILTERED_BLOCK: number;
        TX: number;
    };
    static TYPE_NAME: string[];
    static forBlock(hash: any): any;
    static forFilteredBlock(hash: any): any;
    static forItem(type: any, hash: any): any;
    static forTransaction(hash: any): any;
    static fromBuffer(payload: any): any;
    static fromBufferReader(br: any): any;
    constructor(obj: any);
    type: any;
    hash: any;
    toBuffer(): any;
    toBufferWriter(bw: any): any;
}
export class Messages {
    Pong(nonce: Buffer): Messages
    Ping(): Messages
    GetAddr(): Messages
    static MINIMUM_LENGTH: number;
    static Message(options: any): void;
    static PAYLOAD_START: number;
    static builder(options: any): any;
    constructor(options: any);
    builder: any;
    network: any;
    add(key: any, name: any, Command: any): void;
    parseBuffer(dataBuffer: any): any;

    GetData: {
        forBlock: (hash: string) => Messages
    }
    GetHeaders: (params: { starts: Array<string>, stop: string }) => Messages
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
    disconnect(): any;
    emit(type: any, args: any): any;
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
    sendMessage(message: any): void;
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
