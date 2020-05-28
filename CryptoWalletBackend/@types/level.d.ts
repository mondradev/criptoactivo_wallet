export default function level(location: string, options: CodecOptions, callback?: ErrorCallback): LevelUp;

export interface AbstractOptions {
    readonly [k: string]: any;
}

type Encodings = 'utf8' | 'json' | 'binary' | 'hex' | 'ascii' | 'base64' | 'ucs2' | 'utf16le' | 'utf-16le'

export interface CodecOptions {
    keyEncoding?: Encodings;
    valueEncoding?: Encodings;
}

export interface AbstractOpenOptions extends AbstractOptions {
    createIfMissing?: boolean;
    errorIfExists?: boolean;
}

export interface AbstractGetOptions extends AbstractOptions {
    asBuffer?: boolean;
}

export interface AbstractIteratorOptions<K = any> extends AbstractOptions {
    gt?: K;
    gte?: K;
    lt?: K;
    lte?: K;
    reverse?: boolean;
    limit?: number;
    keys?: boolean;
    values?: boolean;
    keyAsBuffer?: boolean;
    valueAsBuffer?: boolean;
}

export interface AbstractChainedBatch<K = any, V = any> extends AbstractOptions {
    put: (key: K, value: V) => this;
    del: (key: K) => this;
    clear: () => this;
    write(cb: ErrorCallback): any;
    write(options: any, cb: ErrorCallback): any;
}

export interface PutBatch<K = any, V = any> {
    readonly type: 'put';
    readonly key: K;
    readonly value: V;
}

export interface DelBatch<K = any, V = any> {
    readonly type: 'del';
    readonly key: K;
}

export type AbstractBatch<K = any, V = any> = PutBatch<K, V> | DelBatch<K, V>;

export interface AbstractIterator<K, V> extends AbstractOptions {
    db: AbstractLevelDOWN<K, V>;
    next(cb: ErrorKeyValueCallback<K, V>): this;
    end(cb: ErrorCallback): void;
}


export interface AbstractLevelDOWN<K = any, V = any> extends AbstractOptions {
    open(cb: ErrorCallback): void;
    open(options: AbstractOpenOptions, cb: ErrorCallback): void;

    close(cb: ErrorCallback): void;

    get(key: K, cb: ErrorValueCallback<V>): void;
    get(key: K, options: AbstractGetOptions, cb: ErrorValueCallback<V>): void;

    put(key: K, value: V, cb: ErrorCallback): void;
    put(key: K, value: V, options: AbstractOptions, cb: ErrorCallback): void;

    del(key: K, cb: ErrorCallback): void;
    del(key: K, options: AbstractOptions, cb: ErrorCallback): void;

    batch(): AbstractChainedBatch<K, V>;
    batch(array: ReadonlyArray<AbstractBatch<K, V>>, cb: ErrorCallback): AbstractChainedBatch<K, V>;
    batch(
        array: ReadonlyArray<AbstractBatch<K, V>>,
        options: AbstractOptions,
        cb: ErrorCallback,
    ): AbstractChainedBatch<K, V>;

    iterator(options?: AbstractIteratorOptions<K>): AbstractIterator<K, V>;
}


export interface AbstractGetOptions extends AbstractOptions {
    asBuffer?: boolean;
}

export type ErrorCallback = (err: Error | undefined) => void;
export type ErrorValueCallback<V> = (err: Error | undefined, value: V) => void;
export type ErrorKeyValueCallback<K, V> = (err: Error | undefined, key: K, value: V) => void;
type LevelUpPut<K, V, O> =
    ((key: K, value: V, callback: ErrorCallback) => void) &
    ((key: K, value: V, options: O, callback: ErrorCallback) => void) &
    ((key: K, value: V, options?: O) => Promise<void>);

type LevelUpGet<K, V, O> =
    ((key: K, callback: ErrorValueCallback<V>) => void) &
    ((key: K, options: O, callback: ErrorValueCallback<V>) => void) &
    ((key: K, options?: O) => Promise<V>);

type LevelUpDel<K, O> =
    ((key: K, callback: ErrorCallback) => void) &
    ((key: K, options: O, callback: ErrorCallback) => void) &
    ((key: K, options?: O) => Promise<void>);

type LevelUpBatch<K, O> =
    ((key: K, callback: ErrorCallback) => void) &
    ((key: K, options: O, callback: ErrorCallback) => void) &
    ((key: K, options?: O) => Promise<void>);

type InferDBPut<DB> =
    DB extends { put: (key: infer K, value: infer V, options: infer O, cb: any) => void } ?
    LevelUpPut<K, V, O> :
    LevelUpPut<any, any, AbstractOptions>;

type InferDBGet<DB> =
    DB extends { get: (key: infer K, options: infer O, callback: ErrorValueCallback<infer V>) => void } ?
    LevelUpGet<K, V, O> :
    LevelUpGet<any, any, AbstractGetOptions>;

type InferDBDel<DB> =
    DB extends { del: (key: infer K, options: infer O, callback: ErrorCallback) => void } ?
    LevelUpDel<K, O> :
    LevelUpDel<any, AbstractOptions>;

export interface LevelUpChain<K = any, V = any> {
    readonly length: number;
    put(key: K, value: V): this;
    del(key: K): this;
    clear(): this;
    write(callback: ErrorCallback): this;
    write(): Promise<this>;
}

export interface LevelUp<DB = AbstractLevelDOWN> {
    open(): Promise<void>;
    open(callback?: ErrorCallback): void;
    close(): Promise<void>;
    close(callback?: ErrorCallback): void;

    put: InferDBPut<DB>;
    get: InferDBGet<DB>;
    del: InferDBDel<DB>;

    batch(array: AbstractBatch[], options?: any): Promise<void>;
    batch(array: AbstractBatch[], options: any, callback: (err?: any) => any): void;
    batch(array: AbstractBatch[], callback: (err?: any) => any): void;

    batch(): LevelUpChain;

    isOpen(): boolean;
    isClosed(): boolean;

    createReadStream(options?: AbstractIteratorOptions): NodeJS.ReadableStream;
    createKeyStream(options?: AbstractIteratorOptions): NodeJS.ReadableStream;
    createValueStream(options?: AbstractIteratorOptions): NodeJS.ReadableStream;

    /*
    emitted when a new value is 'put'
    */
    on(event: 'put', cb: (key: any, value: any) => void): this;
    /*
    emitted when a value is deleted
    */
    on(event: 'del', cb: (key: any) => void): this;
    /*
    emitted when a batch operation has executed
    */
    on(event: 'batch', cb: (ary: any[]) => void): this;
    /*
    emitted on given event
    */
    on(event: 'open' | 'ready' | 'closed' | 'opening' | 'closing', cb: () => void): this;
}