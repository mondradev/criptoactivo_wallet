import DSha256 from "../dsha256"
import BlockHeader from "./blockheader"
import Block from "./block"

/**
 * Define una estructura para almacenar en memoria un bloque. Esto permite consumir
 * pocos recursos de memoria.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export default class MemBlock {

    /**
     * Datos en crudo del bloque.
     */
    private _raw: Buffer

    /**
     * Crea una instancia de un bloque de memoría.
     * 
     * @param data Datos en crudo del bloque.
     */
    public constructor(data: Buffer) {
        this._raw = data
    }

    /**
     * Obtiene el buffer de los datos en crudo del bloque.
     */
    public serialize(): Buffer {
        return this._raw
    }

    /**
     * Obtiene el hash del bloque.
     */
    public getHash(): Buffer {
        return new DSha256(this._raw.slice(0, 80)).get().value
    }

    /**
     * Indica si el bloque contiene las transacciones. Si el bloque contiene las transacciones se 
     * podrá hacer la llamada a #getBlock, de lo contrario se deberá llamar a #getHeader.
     * 
     * @returns {boolean} True en caso de tener las transacciones.
     */
    public hasTransactions(): boolean {
        return this._raw.length > 80
    }

    /**
     * Crea una instancia de bloque en memoria a partir de los datos en crudo.
     * 
     * @param data Datos en crudo del bloque.
     */
    public static deserialize(data: Buffer): MemBlock {
        if (!data) throw new TypeError("data is null")
        if (data.length < 80) throw new TypeError("data is corrupted")

        return new MemBlock(data)
    }

    /**
     * Obtiene el encabezado del bloque.
     */
    public getHeader(): BlockHeader {
        return BlockHeader.deserialize(this._raw)
    }

    /**
     * Obtiene el bloque.
     */
    public getBlock(): Block {
        return Block.deserialize(this._raw)
    }

    /**
     * Crea una instancia a partir de un buffer.
     * 
     * @param data Buffer con datos en crudo.
     */
    public static fromBuffer(data: Buffer): MemBlock {
        return this.deserialize(data)
    }

    /**
     * Obtiene el buffer de la instancia.
     */
    public toBuffer(): Buffer {
        return this.serialize()
    }

}