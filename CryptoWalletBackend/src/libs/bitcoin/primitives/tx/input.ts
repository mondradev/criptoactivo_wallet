import Script, { ScriptWitness } from "../script";
import ISerializable, { Stream } from "../serializable";
import UInt256 from "../uint256";
import Outpoint from "./outpoint";


/**
 * Esta clase define la estructura de un entrada de transacción, la cual indica la salida que se 
 * está gastando así como la firma del dueño de la salida.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export default class TxIn implements ISerializable {

    /**
     * Define el valor de una secuencia de TxIn final.
     */
    public static SEQUENCE_FINAL: number = 0xffffffff

    /**
     * Crea una nueva entrada a partir de una referencia de salida de transacción.
     * 
     * @param {Outpoint} prevoutIn Referencia a la salida a gastar.
     * @param {Script} scriptSigIn Firma del dueño de la salida.
     * @param {number} sequenceIn Secuencia de la entradas nueva.
     * @returns {TxIn} Una nueva entrada de transacción.
     */
    public static fromOutpoint(prevoutIn: Outpoint, scriptSigIn?: Script, sequenceIn?: number): TxIn {
        const txi = new TxIn()
        txi._prevout = prevoutIn
        txi._scriptSig = scriptSigIn || new Script()
        txi._sequence = sequenceIn || TxIn.SEQUENCE_FINAL

        return txi
    }

    /**
    * Deserializa una entrada de transacción.
    * 
    * @param {Stream} stream Flujo de datos que contiene una entrada de transacción.
    * @returns {TxIn} Una entrada de transacción. 
    */
    public static deserialize(stream: Stream): TxIn {
        if (!stream) throw new TypeError("data is null")
        if (stream.size < Outpoint.OUTPOINT_SIZE + 4) throw new TypeError("data is corrupted")

        return TxIn.fromOutpoint(
            Outpoint.deserialize(stream),
            Script.deserialize(stream),
            stream.deappendUInt32()
        )
    }

    /**
    * Crea una nueva entrada a partir de una salida de transacción.
    * 
    * @param {UInt256} hashPrevTx Hash de la transacción padre de la salida.
    * @param {number} nOut Indice de la salida en la transacción.
    * @param {Script} scriptSigIn Firma del dueño de la salida.
    * @param {number} sequenceIn Secuencia de la entradas nueva.
    * @returns {TxIn} Una nueva entrada de transacción.
    */
    public static create(hashPrevTx: UInt256, nOut: number, scriptSigIn?: Script, sequenceIn?: number): TxIn {
        return TxIn.fromOutpoint(Outpoint.create(hashPrevTx, nOut), scriptSigIn, sequenceIn)
    }

    /**
     * Indica si las dos entradas de transacción son iguales
     * 
     * @param {TxIn} left Operando izquierdo
     * @param {TxIn} right Operando derecho
     * @returns {boolean} True si las entradas son iguales.
     */
    public static equal(left: TxIn, right: TxIn): boolean {
        if (left == null || right == null) return false
        if (left == null && right == null) return true

        return Outpoint.equal(left._prevout, right._prevout)
            && Script.equal(left._scriptSig, right._scriptSig)
            && left._sequence == right._sequence
    }

    /**
     * Indica si las dos entradas de transacción no son iguales
     * 
     * @param {TxIn} left Operando izquierdo
     * @param {TxIn} right Operando derecho
     * @returns {boolean} True si las entradas no son iguales.
     */
    public static notEqual(left: TxIn, right: TxIn): boolean {
        return !TxIn.equal(left, right)
    }

    /**
     * Referencia a la salida que será gastada.
     */
    private _prevout: Outpoint

    /**
     * Firma que autentica el gasto de la salida.
     */
    private _scriptSig: Script

    /**
     * Secuencia de la entradas en la transacción.
     */
    private _sequence: number

    /**
     * Script de Segwit.
     */
    private _scriptWitness: ScriptWitness

    /**
     * Crea una nueva entrada de transacción.
     */
    public constructor() {
        this._sequence = TxIn.SEQUENCE_FINAL
        this._scriptWitness = ScriptWitness.empty()
    }

    /**
     * Obtiene la salida que fue gastada por esta entrada.
     * 
     * @returns {Outpoint} Referencia a una salida.
     * @property
     */
    public get prevOut(): Outpoint {
        return this._prevout
    }

    /**
     * Obtiene el script de la firma que valida el gasto de la salida.
     * 
     * @returns {Script} Firma de la salida.
     */
    public get scriptSig(): Script {
        return this._scriptSig
    }

    /**
     * Obtiene el número que define la disponibilidad de la entrada.
     * 
     *  @returns {number} El número de secuencia.
     */
    public get sequence(): number {
        return this._sequence
    }

    /**
     * Obtiene el script de segwit de esta entrada.
     * 
     * @returns {ScriptWitness} Script Segwit de la entrada.
     */
    public get scriptWitness(): ScriptWitness {
        return this._scriptWitness
    }

    /**
     * Serializa la entrada dentro del flujo de datos especificado.
     * 
     * @param {Stream} stream Flujo de datos.
     */
    public serialize(stream: Stream): void {
        this._prevout.serialize(stream)
        this._scriptSig.serialize(stream)
        stream.appendUInt32(this._sequence)
    }

    /**
     * Obtiene una cadena de caracteres que representa la entrada
     * de transacción actual.
     * 
     * @returns {string} Una cadena que representa la entrada.
     */
    public toString(): string {
        let txinString = "TxIn ("
            + this._prevout.toString()

        if (this._prevout.isNull())
            txinString += ", coinbase " + this._scriptSig.toHex()
        else
            txinString += ", scriptSig=" + this._scriptSig.toHex()

        if (this._sequence != TxIn.SEQUENCE_FINAL)
            txinString += ", sequence=" + this._sequence

        return txinString + ")"
    }
}