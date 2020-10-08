import Script from "../script"
import ISerializable, { Stream } from "../serializable"

/**
 * Una salida de una transacción. Indica el valor y un script 
 * del nuevo dueño del saldo.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export default class TxOut implements ISerializable {

    /**
   * Crea una salida de transacción especificando sus valores.
   * 
   * @param {number} amount Monto que representa la salida.
   * @param {Script} scriptPubKey Script de la llave publica.
   * @returns {TxOut} Nueva salida creada.
   */
    public static create(amount: number, scriptPubKey: Script): TxOut {
        const txo = new TxOut()
        txo._amount = amount
        txo._scriptPubKey = scriptPubKey

        return txo
    }

    /**
     * Indica si dos salida de transacciones son iguales.
     * 
     * @param {TxOut} left Salida de transacción a comparar.
     * @param {TxOut} right Salida de transacción a comparar.
     * @returns {boolean} True en caso que las salidas sean iguales.
     */
    public static equal(left: TxOut, right: TxOut): boolean {
        if (left == null || right == null) return false
        if (left == null && right == null) return true

        return left._amount == right._amount
            && Script.equal(left._scriptPubKey, right._scriptPubKey)
    }


    /**
     * Indica si dos salida de transacciones no son iguales.
     * 
     * @param {TxOut} left Salida de transacción a comparar.
     * @param {TxOut} right Salida de transacción a comparar.
     * @returns {boolean} True en caso que las salidas sean distintas.
     */
    public static notEqual(left: TxOut, right: TxOut): boolean {
        return !this.equal(left, right)
    }

    /**
     * Deserializa una salida de transacción.
     * 
     * @param {Stream} stream Flujo de datos que contiene una salida de transacción.
     * @returns {TxOut} Una salida de transacción. 
     */
    public static deserialize(stream: Stream): TxOut {
        if (!stream) throw new TypeError("data is null")
        if (stream.size < 4) throw new TypeError("data is corrupted")

        const txo = new TxOut()
        txo._amount = stream.deappendUInt64()
        txo._scriptPubKey = Script.deserialize(stream)

        return txo
    }

    /**
     * Indica la cantidad que representa la salida.
     */
    private _amount: number

    /**
     * Script de la llave publica del dueño del saldo.
     */
    private _scriptPubKey: Script

    /**
     * Crea una nueva instancia.
     */
    public constructor() {
        this.setNull()
    }

    /**
     * Obtiene la cantidad que representa la salida.
     * 
     * @returns {number} Cantidad de la salida.
     * @property
     */
    public get amount(): number {
        return this._amount
    }

    /**
     * Script de la llave publica del dueño de esta salida.
     * 
     * @returns {Script} Llave publica del dueño.
     * @property
     */
    public get scriptPubKey(): Script {
        return this._scriptPubKey
    }

    /**
     * Serializa la instancia dentro del flujo de datos especificado.
     * 
     * @param {Stream} stream Un flujo de datos en el cual se ingresa la salida serializada.
     */
    public serialize(stream: Stream): void {
        stream.appendUInt64(this._amount)
        this._scriptPubKey.serialize(stream)
    }

    /**
     * Establece la salida como nula.
     */
    public setNull() {
        this._amount = -1
        this._scriptPubKey = Script.empty()
    }

    /**
     * Indica si la salida es nula.
     * 
     * @returns {boolean} True si la salida es nula.
     */
    public isNull(): boolean {
        return this._amount == -1
    }

    /**
     * Obtiene una cadena de caracteres que representa a la instancia.
     * 
     * @returns {string} Una cadena que representa la salida.
     */
    public toString(): string {
        return `TxOut (value=${this._amount}, scriptPubKey=${this._scriptPubKey})`
    }
}
