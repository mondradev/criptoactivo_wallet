import Utils from "../utils/utils";
import { MongoClient, Collection, Db } from "mongodb";

export interface IConnectionOptions {
    host?: string,
    port?: number,
    dbname: string,
    schema: string,
    poolSize?: number
};

export abstract class Storage<TModel> {

    private collectionInternal: Collection<TModel>;
    protected client: MongoClient;

    protected constructor(
        private _collectionName: string,
        private _connectionProperties: IConnectionOptions) {
        Utils.requireNotNull("[_connectionProperties] can't be null", _connectionProperties);

        _connectionProperties.host = Utils.coalesce(_connectionProperties.host, 'localhost');
        _connectionProperties.port = Utils.coalesce(_connectionProperties.port, 27017);

    }

    protected get collection() {
        if (!this.connected)
            this.connect();

        return this.collectionInternal;
    }

    protected get connected() {
        return this.client.isConnected();
    }


    public async connect() {
        try {
            let uri = `mongodb://${this._connectionProperties.host}:${this._connectionProperties.port}/${this._connectionProperties.dbname}?socketTimeoutMS=3600000&noDelay=true`;

            this.client = await MongoClient.connect(uri, { useNewUrlParser: true, poolSize: this._connectionProperties.poolSize });
            let db = await this.client.db(this._connectionProperties.dbname);

            this.collectionInternal = await db.collection(this._connectionProperties.schema + '.' + this._collectionName);

            await this.createIndexes();

            return this;
        } catch (ex) {
            throw new Error(`Verify the options used to connect to ${this._connectionProperties.host}:${this._connectionProperties.port}/${this._connectionProperties.dbname} => ${ex}`);
        }
    }

    protected abstract async  createIndexes(): Promise<void>;

}