import { Transaction } from "bitcore-lib"
import { TxindexEntry } from "./txindexentry"
import { Enconding } from "./enconding"
import { getDirectory } from "../../../../utils"
import { getKey } from './dbutils'

import level, { LevelUp, AbstractLevelDOWN, CodecOptions } from "level"
import LoggerFactory, { Logger } from 'log4js'
import { Blockchain } from "../../chain/blockchain"

/**
 * Ruta del indice de transacciones
 */
const TXINDEX_PATH: string = "db/bitcoin/:network/txindex"

/**
 * Tipo de codificado para los campos del indice.
 */
const TXINDEX_DB_TYPE: CodecOptions = { keyEncoding: 'binary', valueEncoding: 'binary' }

/**
 * Esta clase administra el indice de Transacciones de Bitcoin. Ofrece funciones para almanacenar 
 * y extraer el indice deseado.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 2.0
 * @see TxindexEntry
 */
export class TxIndex {

    /**
     * Instancia de la base de datos.
     */
    private _db: LevelUp<AbstractLevelDOWN<Buffer, Buffer>>

    /**
     * Instancia de la bitacora de la clase.
     */
    private _logger: Logger

    /**
     * Cadena de bloques de Bitcoin.
     */
    private _chain : Blockchain

    /**
     * Crea una nueva instancia del indice.
     */
    public constructor(chain: Blockchain) {
        this._db = level(getDirectory(TXINDEX_PATH), TXINDEX_DB_TYPE)
        this._chain = chain
    }

    /**
     * Obtiene el indice de la transacción especificada.
     * 
     * @param txid Hash de la transacciones.
     * @returns Una promesa de un indice de transacción.
     */
    public async getIndexByHash(txid: Buffer): Promise<TxindexEntry> {
        return await getKey(this._db, txid, Enconding.Txindex)
    }

    /**
     * Indexa todas las transacciones.
     * 
     * @param transactions Conjunto de transacciones a indexar.
     * @param blockHash Hash del bloque al que pertenecen.
     * @returns Una promesa vacía.
     */
    public async indexing(transactions: Transaction[], blockHash: Buffer) {
        const dbTransaction = this._db.batch()

        for (const [index, transaction] of transactions.entries()) {
            const hash = transaction._getHash()

            dbTransaction.put(Enconding.Txindex.key(hash),
                Enconding.Txindex.encode(new TxindexEntry(blockHash, index)))
        }

        await dbTransaction.write()

        this._logger.debug("Indexed %d txs from %s", transactions.length, blockHash.toReverseHex())
    }
}