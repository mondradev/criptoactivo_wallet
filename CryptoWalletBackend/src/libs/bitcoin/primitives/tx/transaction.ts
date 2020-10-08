import DSha256 from "../dsha256";
import Script from "../script";
import ISerializable, { Stream } from "../serializable";
import UInt256 from "../uint256";
import TxIn from "./input";
import TxOut from "./output";

/**
 * Version del protocolo sin soporte Segwit.
 */
const SERIALIZE_TRANSACTION_NO_WITNESS = 0x40000000

/**
 * Versión del protocolo de red actual.
 */
const PROTOCOL_VERSION = 70016

/**
 *  Define la estructura de una transacción contenida en un bloque.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export default class Transaction implements ISerializable {

    /**
     * Versión actual de las transacciones de Bitcoin
     */
    public static CURRENT_VERSION: number = 2

    /**
     * Lee un flujo de bytes y trata de obtener la transacción que ahí se almacena.
     * 
     * @param {Stream} stream Flujo de datos de donde se extraerá la Transacción.
     * @param {number} version Versión del protocolo.
     * @returns {Transaction} La transacción deserializada.
     */
    public static deserialize(stream: Stream, version: number = PROTOCOL_VERSION): Transaction {
        const allowWitness = !(version & SERIALIZE_TRANSACTION_NO_WITNESS)

        let flags = 0
        const transaction = new Transaction()
        transaction._version = stream.deappendUInt32()
        transaction._txi = stream.deappendVector(TxIn.deserialize)

        if (transaction._txi.length == 0 && allowWitness) {
            flags = stream.deappendUInt8()

            if (flags != 0) {
                transaction._txi = stream.deappendVector(TxIn.deserialize)
                transaction._txo = stream.deappendVector(TxOut.deserialize)
            }
        } else
            transaction._txo = stream.deappendVector(TxOut.deserialize)

        if (flags & 1 && allowWitness) {
            flags ^= 1
            for (let i = 0; i < transaction._txi.length; i++)
                transaction._txi[i].scriptWitness.stack
                    = stream.deappendVector((s: Stream) => s.deappendBuffer())

            if (!transaction.hasWitness())
                throw new Error("Superfluos witness record")
        }

        if (flags)
            throw new Error("Unknown transaction optional data")

        transaction._locktime = stream.deappendUInt32()
        transaction._computeHash()
        transaction._computeWitnessHash()

        return transaction
    }

    /**
     * Indica si ambas transacciones son iguales.
     * 
     * @param {Transaction} left Operando izquierdo
     * @param {Transaction} right Operando derecho
     * @returns {boolean} True si ambas transacciones son iguales.
     */
    public static equal(left: Transaction, right: Transaction): boolean {
        return UInt256.equal(left._hash, right._hash)
    }

    /**
     * Indica si ambas transacciones son distintas.
     * 
     * @param {Transaction} left Operando izquierdo
     * @param {Transaction} right Operando derecho
     * @returns {boolean}  True si las transacciones son diferentes.
     */
    public static notEqual(left: Transaction, right: Transaction): boolean {
        return !this.equal(left, right)
    }

    /**
     * Lista de entradas.
     */
    protected _txi: Array<TxIn>

    /**
     * Lista de salidas.
     */
    protected _txo: Array<TxOut>

    /**
     * Versión de la transacción.
     */
    protected _version: number

    /**
     * Es el tiempo o el numero de bloques necesarios para que pueda ser 
     * confirmada la transacción.
     */
    protected _locktime: number

    /**
     * Identificador unico de la transacción.
     */
    private _hash: UInt256

    /**
     * Identificador unico de la transacción segwit.
     */
    private _witnessHash: UInt256

    /**
     * Crea una transacción vacía.
     * 
     * @constructor
     */
    public constructor() {
        this._txi = new Array()
        this._txo = new Array()
        this._version = Transaction.CURRENT_VERSION
        this._locktime = 0
        this._hash = UInt256.null()
        this._witnessHash = UInt256.null()
    }

    /**
     * Calcula el hash de la transacción.
     */
    protected _computeHash() {
        const stream = Stream.empty()
        this.serialize(stream, SERIALIZE_TRANSACTION_NO_WITNESS)

        this._hash = new DSha256(stream.data).get()
    }

    /**
     * Calcula el hash de la transacción segwit.
     */
    protected _computeWitnessHash() {
        const stream = Stream.empty()
        this.serialize(stream)

        this._witnessHash = new DSha256(stream.data).get()
    }

    /**
     * Obtiene el hash de la transacción.
     * 
     * @returns {UInt256} Hash de la transacción.
     */
    public get hash(): UInt256 {
        return this._hash
    }

    /**
     * Obtiene el hash de la transacción segwit.
     * 
     * @returns {UInt256} Hash de la transacción segwit.
     */
    public get witnessHash(): UInt256 {
        if (!this.hasWitness())
            return this._hash

        return this._witnessHash
    }

    /**
     * Obtiene las entradas de la transacción.
     * 
     * @returns {ReadonlyArray<TxIn>} Lista de entradas.
     */
    public get inputs(): ReadonlyArray<TxIn> {
        return Object.freeze(this._txi)
    }

    /**
     * Obtiene las salidas de la transacción.
     * 
     * @returns {ReadonlyArray<TxOut>} Lista de salidas.
     */
    public get outputs(): ReadonlyArray<TxOut> {
        return Object.freeze(this._txo)
    }

    /**
     * Obtiene la versión de la transacción.
     * 
     * @returns {number} Versión de la transacción.
     */
    public get version(): number {
        return this._version
    }

    /**
     * Obtiene el tiempo de bloqueo de la transacción.
     * 
     * @returns {number} Tiempo de espera de la transacción.
     */
    public get locktime(): number {
        return this._locktime
    }

    /**
     * Indica si la transacción es nula.
     * 
     * @returns {boolean} True si la transacción es nula.
     */
    public isNull(): boolean {
        return this._txi.length == 0 && this._txo.length == 0
    }

    /**
     * Indica si la transacción es de monedas nuevas.
     * 
     * @returns {boolean} True si la transacción es de monedas nuevas..
     */
    public isCoinbase(): boolean {
        return this._txi.length == 1 && this._txi.first().prevOut.isNull()
    }

    /**
     * Indica si la transacción es segwit.
     * 
     * @returns {boolean} True si la transacción es segwit.
     */
    public hasWitness(): boolean {
        for (let i = 0; i < this._txi.length; i++)
            if (!this._txi[i].scriptWitness.isNull())
                return true

        return false
    }

    /**
     * Convierte en bytes la transacción actual y la escribe en el flujo de datos.
     * 
     * @param {Stream} stream Flujo de datos donde escribirá la información de la transacción.
     * @param {boolean} supportedWitness Indica si se soporta Segwit
     */
    public serialize(stream: Stream, version: number = PROTOCOL_VERSION): void {
        const allowWitness = !(version & SERIALIZE_TRANSACTION_NO_WITNESS)

        stream.appendUInt32(this._version)

        let flags = 0

        if (this.hasWitness() && allowWitness)
            flags |= 1

        if (flags) {
            stream.appendVector(new Array<TxIn>())
            stream.appendUInt8(flags)
        }

        stream.appendVector(this._txi)
        stream.appendVector(this._txo)

        if (flags & 1)
            for (let i = 0; i < this._txi.length; i++)
                stream.appendVector(this._txi[i].scriptWitness.stack, (s: Stream, value: Buffer) => s.appendBuffer(value))

        stream.appendUInt32(this._locktime)
    }

    /**
     * Clona la instancia en una nueva.
     * 
     * @returns {Transaction} Nueva instancia.
     */
    public clone(): Transaction {
        const cloned = new Transaction()
        cloned._locktime = this._locktime
        cloned._version = this._version

        for (const txi of this._txi) {
            const newTxi = TxIn.create(
                new UInt256(txi.prevOut.txid.value),
                txi.prevOut.index,
                Script.empty().append(txi.scriptSig),
                txi.sequence)

            if (!txi.scriptWitness.isNull())
                newTxi.scriptWitness.stack.push(...txi.scriptWitness.stack.map(chuck => Buffer.from(chuck)))

            cloned._txi.push(newTxi)
        }

        for (const txo of this._txo)
            cloned._txo.push(TxOut.create(txo.amount, Script.empty().append(txo.scriptPubKey)))

        return cloned
    }

}

/**
 *  Define la estructura de una transacción que puede ser modificada.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export class MutableTransaction extends Transaction {

    /**
     * Crea una nueva instancia de transacción mutable.
     * 
     * @constructor
     */
    public constructor()

    /**
     * Crea una nueva instancia de transacción mutable.
     * 
     * @param {Transaction} tx Transacción de bloque.
     * @constructor
     */
    public constructor(tx: Transaction)

    /**
     * Crea una nueva instancia de transacción mutable.
     * 
     * @param {Transaction?} tx Transacción de bloque.
     * @constructor
     */
    public constructor(tx?: Transaction) {
        super()

        if (tx != null) {
            this._version = tx.version
            this._txi = new Array(...tx.inputs)
            this._txo = new Array(...tx.outputs)
            this._locktime = tx.locktime
        }
    }

    /**
     * Establece la versión de la transacción.
     * 
     * @param {number} value Valor de la versión.
     * @property
     */
    public set version(value: number) {
        this._version = value
    }

    /**
     * Establece el tiempo de bloqueo de la transacción.
     * 
     * @param {number} value Valor del tiempo de bloqueo.
     * @property
     */
    public set locktime(value: number) {
        this._locktime = value
    }

    /**
     * Establece el listado de salidas de la transacción.
     * 
     * @param {Array<TxOut>} value Salidas de transacción.
     * @property
     */
    public set outputs(value: Array<TxOut>) {
        this._txo = value
    }

    /**
     * Establece el listado de entradas de la transacción.
     * 
     * @param {Array<TxIn>} value Entradas de transacción.
     * @property
     */
    public set inputs(value: Array<TxIn>) {
        this._txi = value
    }

    /**
     * Obtiene el listado modificable de las entradas de la transacción.
     * 
     * @returns {Array<TxIn>} Lista de entradas.
     * @property
     */
    public get inputs(): Array<TxIn> {
        return this._txi
    }

    /**
     * Obtiene el listado modificable de las salidas de la transacción.
     * 
     * @returns {Array<TxOut} Lista de salidas.
     * @property
     */
    public get outputs(): Array<TxOut> {
        return this._txo
    }

    /**
    * Obtiene el hash de la transacción.
    * 
    * @returns {UInt256} Hash de la transacción.
    */
    public get hash(): UInt256 {
        this._computeHash()

        return super.hash
    }

    /**
     * Obtiene el hash de la transacción segwit.
     * 
     * @returns {UInt256} Hash de la transacción segwit.
     */
    public get witnessHash(): UInt256 {
        if (!this.hasWitness())
            return super.hash

        this._computeWitnessHash()

        return super.witnessHash
    }

    /**
     * Obtiene una transacción que no puede ser modificada, y será contenida en
     * un bloque.
     * 
     * @returns {Transaction} Transacción no modificable.
     */
    public toTransaction(): Transaction {
        return this.clone()
    }
}