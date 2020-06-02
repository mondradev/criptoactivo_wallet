import BufferHelper from "../../../../utils/bufferhelper";

/**
 * Provee de un estructura para un indice de Transacción de Bitcoin, permitiendo
 * que este pueda ser serializado y viceversa.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 2.0
 * @see TxIndex
 */
export class TxindexEntry {

    /**
     * Hash del bloque contiene la transacción.
     */
    private _blockHash: Buffer

    /**
     * Posición de la transacción en el bloque.
     */
    private _index: number

    /**
     * Crea una instancia nueva del indice.
     * 
     * @param blockHash Hash del bloque que contiene la transacción.
     * @param index Posición de la transacción en el bloque.
     */
    public constructor(blockHash: Buffer, index: number) {
        this._blockHash = blockHash
        this._index = index
    }

    /**
     * Obtiene el hash del bloque que la contiene.
     * @returns Hash del bloque.
     */
    public get blockHash() { return this._blockHash; }

    /**
     * Obtiene la posición de la transacción en el bloque.
     * @returns Posición de la transacción.
     */
    public get index() { return this._index; }

    /**
     * Serializa el indice.
     * @returns Los bytes que representan el indice.
     */
    public toBuffer(): Buffer {
        return BufferHelper.zero()
            .appendUInt32LE(this._index)
            .append(this._blockHash)
    }

    /**
     * Deserializa el indice a partir de un conjunto de bytes.
     * @param data Bytes que representan un indice de transacción.
     * @returns Un indice de transacción.
     */
    public static fromBuffer(data: Buffer): TxindexEntry {
        if (data.length < 36)
            throw new Error("The data must be 36 bytes")

        return new TxindexEntry(data.read(32, 4), data.readUInt32LE(0))
    }
}
