import Utils from "../utils";
import { MongoClient, Collection } from "mongodb";

export interface IConnectionOptions {
    host?: string,
    port?: number,
    dbname: string,
    schema: string,
    username?: string,
    password?: string
};

export abstract class Storage<TModel> {

    protected collection: Collection<TModel>;
    protected connected: boolean;

    protected constructor(
        private _collectionName: string,
        private _connectionProperties: IConnectionOptions) {
        Utils.requireNotNull("[_connectionProperties] can't be null", _connectionProperties);

        _connectionProperties.host = Utils.coalesce(_connectionProperties.host, 'localhost');
        _connectionProperties.port = Utils.coalesce(_connectionProperties.port, 27017);

    }

    public async connect() {
        try {
            let auth = Utils.isNull(this._connectionProperties.username, this._connectionProperties.password)
                ? ''
                : `${this._connectionProperties.username}:${this._connectionProperties.password}@`;
            let uri = `mongodb://${auth}${this._connectionProperties.host}:${this._connectionProperties.port}/${this._connectionProperties.dbname}`;

            let client = await MongoClient.connect(uri, { useNewUrlParser: true, poolSize: 1000 });
            let db = await client.db(this._connectionProperties.dbname);

            this.collection = await db.collection(this._connectionProperties.schema + '.' + this._collectionName);

            await this.createIndexes();

            this.connected = true;

            return this;
        } catch (ex) {
            throw new Error(`Verify the options used to connect to ${this._connectionProperties.host}:${this._connectionProperties.port}/${this._connectionProperties.dbname} => ${ex}`);
        }
    }

    protected abstract async  createIndexes(): Promise<void>;

}