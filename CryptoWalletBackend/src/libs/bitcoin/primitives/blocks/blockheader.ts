import DSha256 from "../dsha256"
import ISerializable, { Stream } from "../serializable"
import UInt256 from "../uint256"

/**
 *  Define la estructura de la cabecera de un bloque.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export default class BlockHeader implements ISerializable {

    /**
     * Crea una instancia a partir un flujo de bytes.
     * 
     * @param stream Datos en crudo del bloque.
     * @returns Nueva instancia de encabezado.
     */
    public static deserialize(stream: Stream): BlockHeader {
        if (!stream) throw new TypeError("data is null")
        if (stream.size < this.BLOCK_HEADER_SIZE) throw new TypeError("data is corrupted")

        const header = new BlockHeader()

        header._version = stream.deappendUInt32()
        header._hashPrevBlock = stream.deappendUInt256()
        header._hashMerkleRoot = stream.deappendUInt256()
        header._time = stream.deappendUInt32()
        header._bits = stream.deappendUInt32()
        header._nonce = stream.deappendUInt32()

        return header
    }

    /**
     * Tamaño de un encabezado de bloque.
     */
    public static BLOCK_HEADER_SIZE: number = 80

    /**
     * Versión del bloque.
     */
    private _version: number

    /**
     * Hash del bloque anterior
     */
    private _hashPrevBlock: UInt256

    /**
     * Hash del arbol merkle de transacciones.
     */
    private _hashMerkleRoot: UInt256

    /**
     * Tiempo en el que se generó el bloque.
     */
    private _time: number

    /**
     * Dificultad del bloque.
     */
    private _bits: number

    /**
     * Nonce del bloque.
     */
    private _nonce: number

    /**
     * Indica si el hash ha sido cacheado.
     */
    protected _cachedHash: boolean

    /**
     * Hash del bloque, utilizando para almacenar el hash cacheado.
     */
    private _hash: UInt256

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
        this._hashMerkleRoot = UInt256.null()
        this._hashPrevBlock = UInt256.null()
        this._cachedHash = true
        this._hash = UInt256.null()
    }

    /**
     * Establece el valor de la versión.
     * 
     * @param {number} value Nueva versión del bloque.
     * @property
     */
    public set version(value: number) {
        if (this._version != value)
            this._cachedHash = false

        this._version = value
    }

    /**
     * Obtiene la versión del bloque.
     * 
     * @returns {number} Versión del bloque.
     * @property
     */
    public get version(): number {
        return this._version
    }

    /**
     * Establece el valor del tiempo del bloque.
     * 
     * @param {number} value Nuevo tiempo del bloque.
     * @property
     */
    public set time(value: number) {
        if (this._time != value)
            this._cachedHash = false

        this._time = value
    }

    /**
     * Obtiene el tiempo del bloque.
     * 
     * @returns {number} Tiempo del bloque.
     * @property
     */
    public get time(): number {
        return this._time
    }

    /**
     * Establece el valor de la dificultad del bloque.
     * 
     * @param {number} value Nueva dificultad del bloque.
     * @property
     */
    public set bits(value: number) {
        if (this._bits != value)
            this._cachedHash = false

        this._bits = value
    }

    /**
     * Obtiene la dificultad del bloque.
     * 
     * @returns {number} Dificultad del bloque.
     * @property
     */
    public get bits(): number {
        return this._bits
    }

    /**
     * Obtiene el nonce del bloque.
     * 
     * @returns {number} Nonce del bloque.
     * @property
     */
    public get nonce(): number {
        return this._nonce
    }

    /**
    * Establece el valor de la dificultad del bloque.
    * 
    * @param {number} value Nueva dificultad del bloque.
    * @property
    */
    public set nonce(value: number) {
        if (this._nonce != value)
            this._cachedHash = false

        this._nonce = value
    }

    /**
     * Establce el hash del bloque antecesor.
     * 
     * @param {UInt256} value Hash del bloque anterior.
     * @property
     */
    public set hashPrevBlock(value: UInt256) {
        this._hashPrevBlock = value
    }

    /**
    * Obtiene el hash del bloque antecesor.
    * 
    * @returns {UInt256} Hash del bloque anterior.
    * @property
    */
    public get hashPrevBlock(): UInt256 {
        return this._hashPrevBlock
    }

    /**
     * Establece el hash del árbol merkle de transacciones.
     * 
     * @param {UInt256} value Hash del árbol merkle de transacciones.
     * @property
     */
    public set hashMerkleRoot(value: UInt256) {
        this._hashMerkleRoot = value
    }

    /**
     * Obtiene el hash del árbol merkle de transacciones.
     * 
     * @returns {UInt256} Hash del árbol merkle de transacciones.
     * @property
     */
    public get hashMerkleRoot(): UInt256 {
        return this._hashMerkleRoot
    }

    /**
     * Obtiene el hash del bloque.
     * 
     * @returns {UInt256} Hash del bloque.
     * @property
     */
    public get hash(): UInt256 {
        if (this._cachedHash)
            return this._hash

        const stream = Stream.empty()

        this._serializeHeader(stream)

        this._hash = new DSha256(stream.data.slice(0, BlockHeader.BLOCK_HEADER_SIZE)).get()
        this._cachedHash = true

        return this._hash
    }

    /**
     * Serializa el encabezado y obtiene el vector de bytes.
     * 
     * @param {Stream} stream Flujo de datos a donde se realizará 
     *                          el serializado.
     */
    public serialize(stream: Stream): void {
        this._serializeHeader(stream)
    }

    /**
    * Serializa el encabezado y obtiene el vector de bytes. Esto se hace para separar
    * el encabezado de la serialización de la instancia.
    * 
    * @param {Stream} stream Flujo de datos a donde se realizará 
    *                          el serializado.
    */
    private _serializeHeader(stream: Stream): void {
        stream
            .appendUInt32(this.version)
            .appendUInt256(this.hashPrevBlock)
            .appendUInt256(this.hashMerkleRoot)
            .appendUInt32(this._time)
            .appendUInt32(this._bits)
            .appendUInt32(this._nonce)
    }
}