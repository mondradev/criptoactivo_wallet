import '../../../utils/bufferhelper'
import '../../../utils/extensions'

import BufferHelper from "../../../utils/bufferhelper"
import { isBuffer, isString } from "../../../utils/preconditions"
import { number } from 'yargs'

/**
 * Entero sin signo de 32 bytes.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export default class UInt256 {

    /**
     * Tamaño máximo del Buffer que puede utilizarse para crear el entero.
     */
    public static MAX_SIZE: number = 32

    /**
    * Crea una instancia con valor 0 o nulo.
    * 
    * @returns {UInt256} Nueva instancia.
    */
    public static null(): UInt256 {
        return new UInt256(Buffer.alloc(this.MAX_SIZE, 0x00))
    }

    /**
     * Crea una nueva instancia a partir de un número.
     * 
     * @param value Número que inicializa la instancia.
     * @returns {UInt256} Nueva instancia.
     */
    public static fromNumber(value: number): UInt256 {
        const hex = value.toHex(UInt256.MAX_SIZE)
        return new UInt256(hex)
    }

    /**
    * Determina si ambos elementos son iguales byte por byte.
    * 
    * @param left Operando izquierdo.
    * @param right Operando derecho.
    * @returns {boolean} True si son iguales.
    */
    public static equal(left: UInt256, right: UInt256): boolean {
        if (left == null || right == null)
            return false

        return left.value.equals(right.value)
    }

    /**
     * Valor del entero.
     */
    private _value: Buffer

    /**
     * Crea una nueva instancia.
     * 
     * @param value Valor del entero a asignar.
     * @constructor
     */
    public constructor(value: string | Buffer) {
        if (isString(value))
            this._value = this._normalize(BufferHelper.fromHex(value as string))
        else if (isBuffer(value))
            this._value = this._normalize(Buffer.from(value as Buffer))
        else
            throw new TypeError("Value require be a string or Buffer instance");
    }

    /**
     * Obtiene el valor en Buffer.
     * @returns {Buffer} Buffer del flujo de datos.
     * @property
     */
    public get value(): Buffer {
        return this._value
    }

    /**
     * Obtiene la representación hexadecimal del valor de esta instancia.
     * 
     * @returns {string} Valor en hexadecimal.
     */
    public toHex(): string {
        return this._value.toHex()
    }

     /**
     * Obtiene la representación hexadecimal re-ordenando los bytes al contrario.
     * 
     * @returns {string} Valor en hexadecimal.
     */
    public toReverseHex(): string {
        return this._value.toReverseHex()
    }

    /**
     * Obtiene una cadena que representa el valor numérico de la instancia.
     * 
     * @returns {string} Valor númerico de 32 bytes.
     */
    public toString(): string {
        return this.toNumber().toString()
    }

    /**
     * Obtiene el valor numérico que representa.
     * 
     * @returns {number} Valor numérico de 32 bytes.
     */
    public toNumber(): number {
        return parseInt(Buffer.from(this._value).toHex(), 16)
    }

    /**
     * Compara con otra instancia UInt256 y devuelve negativo si es menor, 0 si son iguales o 
     * positivo si es mayor.
     * 
     * @param other Valor a comparar. 
     * @returns Un negativo si es menor, un positivo si es mayor o 0 si son iguales.
     */
    public compare(other: UInt256): number {
        return this.toNumber() - other.toNumber()
    }

    /**
     * Indica si el valor es nulo, esto es cuando es igual a 0x00000000.
     * 
     * @returns {boolean} True si es nulo. 
     */
    public isNull(): boolean {
        return UInt256.equal(this, UInt256.null())
    }

    /**
     * Valida y devuelve el vector en con la longitud necesaria para la instancia.
     * 
     * @param value Valor a validar.
     * @returns {Buffer} Un buffer que representa el entero de 256 bits.
     */
    private _normalize(value: Buffer): Buffer {
        if (value.length > UInt256.MAX_SIZE)
            throw new TypeError("Value cannot be more than 32 bytes")

        const data = Buffer.alloc(UInt256.MAX_SIZE, 0x00)
        data.write(value.toHex(), UInt256.MAX_SIZE - value.length, 'hex')

        return data
    }

}