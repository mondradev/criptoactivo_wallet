import '../../utils/bufferhelper'

import BufferHelper from "../../utils/bufferhelper";
import { isBuffer, isString } from "../../utils/preconditions";

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
     * Valor del entero.
     */
    private _value: Buffer

    /**
     * Crea una nueva instancia.
     * 
     * @param value Valor del entero a asignar.
     */
    constructor(value: string | Buffer) {
        if (isString(value))
            this._value = this._validateSize(BufferHelper.fromHex(value as string))
        else if (isBuffer(value))
            this._value = this._validateSize(Buffer.from(value as Buffer))
        else
            throw new TypeError("Value require be a string or Buffer instance");
    }

    /**
     * Crea una instancia con valor 0.
     */
    public static zero(): UInt256 {
        return new UInt256(Buffer.alloc(this.MAX_SIZE, 0x00))
    }

    /**
     * Obtiene el valor en Buffer.
     */
    public get value() {
        return this._value;
    }

    /**
     * Valida y devuelve el vector en con la longitud necesaria para la instancia.
     * 
     * @param value Valor a validar.
     */
    private _validateSize(value: Buffer): Buffer {
        if (value.length > UInt256.MAX_SIZE)
            throw new TypeError("Value cannot be more than 32 bytes")

        const data = Buffer.alloc(UInt256.MAX_SIZE, 0x00);
        data.write(value.toHex(), UInt256.MAX_SIZE - value.length, 'hex');

        return data;
    }

}