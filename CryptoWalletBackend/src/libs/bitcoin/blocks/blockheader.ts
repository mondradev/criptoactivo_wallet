import DSha256 from "../dsha256"
import UInt256 from "../uint256"

/**
 *  Define la estructura de la cabecera de un bloque.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export default class BlockHeader {

    /**
     * Versión del bloque.
     */
    protected _version: number

    /**
     * Hash del bloque anterior
     */
    protected _hashPrevBlock: UInt256

    /**
     * Hash del arbol merkle de transacciones.
     */
    protected _hashMerkleRoot: UInt256

    /**
     * Tiempo en el que se generó el bloque.
     */
    protected _time: number

    /**
     * Dificultad del bloque.
     */
    protected _bits: number

    /**
     * Nonce del bloque.
     */
    protected _nonce: number

    /**
     * Crea una instancia 
     */
    public constructor() {
        this.setNull()
    }

    /**
     * Establece el encabezado como nulo.
     */
    public setNull(): void {
        this._version = 0
        this._time = 0
        this._bits = 0
        this._nonce = 0
        this._hashMerkleRoot = UInt256.zero()
        this._hashPrevBlock = UInt256.zero();
    }

    /**
     * Obtiene la versión del bloque.
     */
    public get version(): number {
        return this._version
    }

    /**
     * Obtiene el tiempo del bloque.
     */
    public get time(): number {
        return this._time
    }

    /**
     * Obtiene la dificultad del bloque.
     */
    public get bits(): number {
        return this._bits
    }

    /**
     * Obtiene el nonce del bloque.
     */
    public get nonce(): number {
        return this._nonce
    }

    /**
     * Obtiene el hash del bloque antecesor.
     */
    public get hashPrevBlock(): UInt256 {
        return this._hashPrevBlock
    }

    /**
     * Obtiene el hash del árbol merkle de transacciones.
     */
    public get hashMerkleRoot(): UInt256 {
        return this._hashMerkleRoot
    }

    /**
     * Obtiene el hash del bloque.
     */
    public getHash() {
        return new DSha256(this.serialize()).get()
    }

    /**
     * Serializa el encabezado y obtiene el vector de bytes.
     */
    public serialize(): Buffer {
        return Buffer.alloc(0)
            .appendUInt32LE(this.version)
            .append(this.hashPrevBlock.value)
            .append(this.hashMerkleRoot.value)
            .appendUInt32LE(this._time)
            .appendUInt32LE(this._bits)
            .appendUInt32LE(this._nonce)
    }

    /**
     * Crea una instancia a partir un vector de bytes.
     * 
     * @param data Datos en crudo del bloque.
     */
    public static deserialize(data: Uint8Array): BlockHeader {
        if (!data) throw new TypeError("data is null")
        if (data.length < 80) throw new TypeError("data is corrupted")

        const raw = Buffer.from(data)
        const header = new BlockHeader()

        header._version = raw.readUInt32LE(0)
        header._hashPrevBlock = new UInt256(raw.read(32, 4))
        header._hashMerkleRoot = new UInt256(raw.read(32, 36))
        header._time = raw.readUInt32LE(68)
        header._bits = raw.readUInt32LE(72)
        header._nonce = raw.readUInt32LE(76)

        return header
    }
}