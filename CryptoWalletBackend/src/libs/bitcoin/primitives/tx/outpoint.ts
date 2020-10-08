import ISerializable, { Stream } from "../serializable"
import UInt256 from "../uint256"

/**
 * Esta clase define la estructura de un outpoint, el cual hace referencia a una salida que 
 * puede ser gastada en una nueva transacción.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export default class Outpoint implements ISerializable {

    /**
     * Define el valor de un indice nulo.
     */
    public static NULL_INDEX: number = 0xffffffff

    /**
     * Tamaño de un outpoint.
     */
    public static OUTPOINT_SIZE: number = 36

    /**
     * Crea un outpoint a la salida que será gastada.
     * 
     * @param {UInt256} txid Hash de la transacción.
     * @param {number} index Indice de la salida.
     * @returns {Outpoint} Una nueva referencia de una salida.
     */
    public static create(txid: UInt256, index: number): Outpoint {
        const outpoint = new Outpoint()

        outpoint._txid = txid
        outpoint._index = index

        return outpoint
    }

    /**
     * Indica si los apuntadores de salida son iguales.
     * 
     * @param {Outpoint} left Operando izquierdo.
     * @param {Outpoint} right Operando derecho.
     * @returns {boolean} True si son iguales.
     */
    public static equal(left: Outpoint, right: Outpoint): boolean {
        if (left == null || right == null) return false
        if (left == null && right == null) return true

        return UInt256.equal(left._txid, right._txid)
            && left._index == right._index
    }

    /**
     * Indica si los apuntadores de salidas son distintos
     * 
     * @param {Outpoint} left Operando izquierdo
     * @param {Outpoint} right Operando derecho
     * @returns {boolean} True en caso que las salidas sean distintas.
     */
    public static notEqual(left: Outpoint, right: Outpoint): boolean {
        return !this.equal(left, right)
    }

    /**
     * Indica que la salida izquierda es menor que la derecha.
     * 
     * @param {Outpoint} left Operando izquierdo
     * @param {Outpoint} right Operando derecho
     * @returns {boolean} True si la salida izquierda es menor que la derecha.
     */
    public static lessThan(left: Outpoint, right: Outpoint): boolean {
        const cmp = left._txid.compare(right._txid)

        return cmp < 0 || (cmp == 0 && left._index < right._index)
    }

    /**
     * Crea una instancia a partir de un vector de bytes.
     * 
     * @param {Stream} stream Vector de bytes.
     * @returns {Outpoint} Una referencia a una salida.
     */
    public static deserialize(stream: Stream): Outpoint {
        if (!stream) throw new TypeError("data is null")
        if (stream.size < Outpoint.OUTPOINT_SIZE) throw new TypeError("data is corrupted")

        return Outpoint.create(stream.deappendUInt256(), stream.deappendUInt32())
    }

    /**
     * Hash de la transacción que contiene la salida a gastar.
     */
    private _txid: UInt256

    /**
     * Indice de la salida a gastar.
     */
    private _index: number

    /**
     * Crea una nueva instancia vacía.
     */
    public constructor() {
        this.setNull()
    }

    /**
     * Obtiene el hash de la salida apuntada.
     * 
     * @returns {UInt256} Hash de la transacción.
     */
    public get txid(): UInt256 {
        return this._txid
    }

    /**
     * Obtiene el index de la salida en la transacción.
     * 
     * @returns {number} Index de la salida.
     */
    public get index(): number {
        return this._index
    }

    /**
     * Indica si el punto de referencia a la salida es nulo.
     * 
     * @returns {boolean} True si la referencia no apunta a nada.
     */
    public isNull(): boolean {
        return this._txid.isNull() && this._index == Outpoint.NULL_INDEX
    }

    /**
     * Serializa la instancia dentro del flujo de datos especificado.
     * 
     * @param stream Flujo de datos de donde se extra la información.
     */
    public serialize(stream: Stream): void {
        stream.appendUInt256(this._txid)
            .appendUInt32(this._index)
    }

    /**
     * Establece como un outpoint nulo.
     */
    public setNull(): void {
        this._txid = UInt256.null()
        this._index = Outpoint.NULL_INDEX
    }

    /**
     * Obtiene una cadena de caracteres que representa a la instancia.
     * 
     * @returns {string} Una cadena que representa la referencia de salida.
     */
    public toString(): string {
        return `Outpoint (${this._txid.toString().substring(0, 10)}, ${this.index})`
    }
}