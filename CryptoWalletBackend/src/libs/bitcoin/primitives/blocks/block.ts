import DSha256 from "../dsha256"
import { Stream } from "../serializable"
import Transaction from "../tx/transaction"
import UInt256 from "../uint256"
import BlockHeader from "./blockheader"

/**
 *  Define la estructura de un bloque.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export default class Block extends BlockHeader {

    /**
     * Crea un bloque a partir de su encabezado.
     * 
     * @param {BlockHeader} header Encabezado del bloque.
     * @returns {Block} Un bloque con sin transacciones.
     */
    public static fromHeader(header: BlockHeader): Block {
        const block = new Block()

        block.version = header.version
        block.hashPrevBlock = header.hashPrevBlock
        block.hashMerkleRoot = header.hashMerkleRoot
        block.time = header.time
        block.bits = header.bits
        block.nonce = header.nonce

        return block
    }

    /**
     * Crea una instancia a partir un vector de bytes.
     * 
     * @param {Stream} stream Datos en crudo del bloque.
     * @returns {Block} Un bloque deserializado desde el flujo de datos.
     */
    public static deserialize(stream: Stream): Block {
        const block = Block.fromHeader(BlockHeader.deserialize(stream))
        block._txs = stream.deappendVector(Transaction.deserialize)

        return block
    }

    /**
     * Lista de transacciones.
     */
    private _txs: Array<Transaction>

    /**
     * Crea una instancia 
     */
    public constructor() {
        super()
        this._txs = new Array()
    }
    
    /**
     * Serializa el encabezado y el listado de transacciones contenidas en el
     * bloque.
     * @argument
     */
    public serialize(stream: Stream): void {
        super.serialize(stream);
        stream.appendVector(this._txs)
    }

    /**
     * Obtiene el listado de transacciones.
     * 
     * @returns {Array<Transaction>} Transacciones.
     */
    public get txs(): Array<Transaction> {
        return this._txs
    }
}