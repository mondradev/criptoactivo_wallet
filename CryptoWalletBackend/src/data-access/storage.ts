import Utils from "../utils/utils";
import { MongoClient, Collection } from "mongodb";

export interface IConnectionOptions {
    host?: string,
    port?: number,
    dbName: string,
    poolSize?: number
};

export type BulkUpdate<T> = {
    updateOne: {
        filter: Partial<T>,
        update: { $set: Partial<T> },
        upsert: boolean
    }
};

export type BulkReplace<T> = {
    replaceOne: {
        filter: Partial<T>,
        replacement: Partial<T>,
        upsert: boolean
    }
};

export abstract class Storage<TModel> {

    private collectionInternal: Collection<TModel>;
    protected client: MongoClient;

    protected constructor(
        private collectionName: string,
        private connectionProperties: IConnectionOptions) {
        Utils.requireNotNull("[_connectionProperties] can't be null", connectionProperties);

        connectionProperties.host = Utils.coalesce(connectionProperties.host, 'localhost');
        connectionProperties.port = Utils.coalesce(connectionProperties.port, 27017);

    }

    public get collection() {
        if (!this.connected)
            this.connect();

        return this.collectionInternal;
    }

    public get connected() {
        return this.client.isConnected();
    }


    public async connect() {
        try {
            let uri = `mongodb://${this.connectionProperties.host}:${this.connectionProperties.port}/${this.connectionProperties.dbName}?socketTimeoutMS=3600000&noDelay=true`;

            this.client = await MongoClient.connect(uri, { useNewUrlParser: true, poolSize: this.connectionProperties.poolSize });
            let db = await this.client.db(this.connectionProperties.dbName);

            this.collectionInternal = await db.collection(this.collectionName);

            await this.createIndexes();

            return this;
        } catch (ex) {
            throw new Error(`Verify the options used to connect to ${this.connectionProperties.host}:${this.connectionProperties.port}/${this.connectionProperties.dbName} => ${ex}`);
        }
    }

    protected abstract async  createIndexes(): Promise<void>;

}