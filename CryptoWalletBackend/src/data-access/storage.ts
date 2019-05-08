import Utils from "../../libs/utils";
import { MongoClient, Collection } from "mongodb";
import fs from "fs";

export interface IConnectionOptions {
    host?: string,
    port?: number,
    dbName: string,
    poolSize?: number,
    user?: string,
    pwd?: string,
    ssl?: {
        sslCA?: string,
        sslPEM?: string
    }
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
        return this.collectionInternal;
    }

    public get connected() {
        return this.client && this.client.isConnected();
    }


    public async connect() {
        try {
            let uri = `mongodb://${this.connectionProperties.host}:${this.connectionProperties.port}/${this.connectionProperties.dbName}`;
            let sslAuth = Utils.isNotNull(this.connectionProperties.ssl);
            let ca = sslAuth ? fs.readFileSync(this.connectionProperties.ssl.sslCA) : null;
            let cert = sslAuth ? fs.readFileSync(this.connectionProperties.ssl.sslPEM) : null;
            let key = cert;

            this.client = await MongoClient.connect(uri, {
                noDelay: true, socketTimeoutMS: 3600000,
                useNewUrlParser: true, poolSize: this.connectionProperties.poolSize, ssl: sslAuth, sslCA: [ca], sslCert: cert, sslKey: key, auth: {
                    user: this.connectionProperties.user,
                    password: this.connectionProperties.pwd
                },
                authSource: this.connectionProperties.dbName
            });
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