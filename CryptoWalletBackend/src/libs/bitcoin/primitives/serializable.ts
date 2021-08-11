import "../../../utils/bufferhelper"

import BufferHelper from "../../../utils/bufferhelper"
import UInt256 from "./uint256"

/**
 * Define las funciones de un objeto que puede ser serializado.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export default interface ISerializable {

    /**
     * Serializa la instancia y obtiene el buffer de la misma.
     * 
     * @param {Stream } Flujo de datos a donde se va a serializar.
     */
    serialize(stream: Stream): void
}

/**
 * Define la firma de una función utilizada para realizar la deserialización 
 * desde un flujo de datos.
 * 
 * @template T Tipo de dato de la instancia
 * @param {Stream} Flujo de datos
 * @returns {T} Una instancia leída desde el flujo de datos.
 */
type DeserializeFunc<T> = (stream: Stream) => T


/**
 * Define la firma de una función utilizada para realizar la serialización 
 * de un tipo de dato.
 * 
 * @template T Tipo de dato de la instancia
 * @param {Stream} Flujo de datos
 * @param {T} Dato a serializar.
 */
type SerializeFunc<T> = (stream: Stream, value: T) => void


/**
 * Una clase que representa un flujo de datos, a la cual se puede agregar o quitar
 * ya sea en valores numericos, vectores o Buffers.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0 
 */
export class Stream {

    /**
     * Instancia del buffer de datos del flujo.
     */
    private _buffer: Buffer

    /**
     * Crea una nueva instancia de datos.
     * 
     * @param {Buffer} buffer Buffer con el cual se crea la instancia.
     */
    private constructor(buffer: Buffer) {
        this._buffer = buffer;
    }

    /**
     * Crea un flujo de datos vacío.
     * 
     * @returns {Stream} Una nuevo flujo vacío.
     */
    public static empty(): Stream {
        return new Stream(BufferHelper.zero())
    }

    /**
     * Crea un flujo de datos a partir de un buffer.
     * 
     * @param {Buffer} buffer Un buffer con el cual se crea la instancia.
     * @returns {Stream} Nuevo flujo de datos.
     */
    public static fromBuffer(buffer: Buffer): Stream {
        return new Stream(Buffer.from(buffer))
    }


    /**
     * Agrega al flujo de datos un valor númerico de tamaño variable.
     * 
     * @param {number} value Valor numérico que se agregará.
     * @returns {Stream} Flujo de datos actual.
     */
    public appendVarInt(value: number): Stream {
        this._buffer = this._buffer.appendVarNum(value)
        return this
    }

    /**
     * Agrega al flujo de datos un valor numérico de 8 bits sin signo.
     * 
     * @param {number} value Valor numérico en 8 bits sin signo.
     * @returns {Stream} Flujo de datos actual.
     */
    public appendUInt8(value: number): Stream {
        this._buffer = this._buffer.appendUInt8(value)
        return this
    }

    /**
     * Agrega al flujo de datos un valor numérico de 32 bits sin signo, el 
     * cual será almacenado en formato little edian 0x00000001 => 0x01000000
     * 
     * @param {number} value Valor numérico en 32 bits sin signo.
     * @returns {Stream} Flujo de datos actual.
     */
    public appendUInt32(value: number): Stream {
        this._buffer = this._buffer.appendUInt32LE(value)
        return this
    }

    /**
     * Agrega al flujo de datos un valor numérico de 64 bits sin signo, el 
     * cual será almacenado en formato little edian
     * 0x0000000000000001 => 0x0100000000000000
     * 
     * @param {number} value Valor numérico en 64 bits sin signo.
     * @returns {Stream} Flujo de datos actual.
     */
    public appendUInt64(value: number): Stream {
        this._buffer = this._buffer.appendUInt64LE(value)
        return this
    }

    /**
     * Agrega al flujo de datos un valor numérico de 256 bits sin signo, el 
     * cual será almacenado en formato little edian.
     * 
     * @param {number} value Valor numérico en 256 bits sin signo.
     * @returns {Stream} Flujo de datos actual.
     */
    public appendUInt256(value: UInt256): Stream {
        this._buffer = this._buffer.append(value.value)
        return this
    }


    /**
    * Agrega al flujo de datos un vector.
    * 
    * @template T Tipo de dato.
    * @param {Array<T>} data Vector a serializar.
    * @param {SerializeFunc<T>} serializeFunc Función utilizada para la serialización.
    * @returns {Stream} Flujo de datos actual.
    */
    public appendVector<T>(data: Array<T>, serializeFunc: SerializeFunc<T>): Stream

    /**
    * Agrega al flujo de datos un vector de objetos serializables.
    * 
    * @template T Tipo de dato que implemente a ISerializable.
    * @param {Array<T>} data Vector a serializar.
    * @returns {Stream} Flujo de datos actual.
    */
    public appendVector<T extends ISerializable>(data: Array<T>): Stream

    /**
    * Agrega al flujo de datos un vector de objetos.
    * 
    * @template T Tipo de dato
    * @param {Array<T|ISerializable>} data Vector a serializar.
    * @param {SerializeFunc<T>} serializeFunc Función utilizada para la serialización.
    * @returns {Stream} Flujo de datos actual.
    */
    public appendVector<T>(data: Array<T | ISerializable>, serializeFunc?: SerializeFunc<T>): Stream {
        this._buffer = this._buffer.appendVarNum(data.length)

        for (let i = 0; i < data.length; i++)
            if (typeof data[i]['serialize'] == "function")
                (data[i] as ISerializable).serialize(this)
            else if (serializeFunc != null && typeof serializeFunc == 'function')
                serializeFunc(this, data[i] as T)

        return this
    }

    /**
     * Agrega un buffer a el flujo de datos.
     * 
     * @param {Buffer} buffer Buffer que se agregará.
     * @returns {Stream} Flujo de datos actual.
     */
    public appendBuffer(buffer: Buffer): Stream {
        this._buffer = this._buffer.appendVarNum(buffer.length)
            .append(buffer)

        return this
    }


    /**
     * Extrae los primeros 4 bytes del flujo y los devuelve como un
     * número.
     * 
     * @returns {number} Un valor numerico sin signo de 4 bytes.
     */
    public deappendUInt32(): number {
        const value = this._buffer.readUInt32LE(0)
        this._buffer = this._buffer.slice(4)

        return value
    }

    /**
     * Extrae los primeros 8 bytes del flujo y los devuelve como un
     * número.
     * 
     * @returns {number} Un valor numerico sin signo de 8 bytes.
     */
    public deappendUInt64(): number {
        const value = this._buffer.readUInt64LE(0)
        this._buffer = this._buffer.slice(8)

        return value
    }

    /**
     * Extrae los primeros 32 bytes del flujo y los devuelve como un
     * número.
     * 
     * @returns {UInt256} Un valor numerico sin signo de 32 bytes.
     */
    public deappendUInt256(): UInt256 {
        const value = this._buffer.read(UInt256.MAX_SIZE, 0)
        this._buffer = this._buffer.slice(UInt256.MAX_SIZE)

        return new UInt256(value)
    }


    /**
     * Extra los bytes requeridos por el vector almacenado en el flujo de datos.
     * Esto requiere de una función especilizada para extrar el elemento del mismo flujo.
     * 
     * @template T Tipo de dato que implemente a ISerializable.
     * @param {DeserializeFunc<T>} deserializeFunc Función deserializadora.
     * @returns {Array<T>} Un vector formado a partir del flujo de datos.
     */
    public deappendVector<T>(deserializeFunc: DeserializeFunc<T>): Array<T> {
        const ref = { size: 0 }
        const lenght = this._buffer.readVarNum(0, ref)
        const vector = new Array<T>()

        this._buffer = this._buffer.slice(ref.size)

        for (let i = 0; i < lenght; i++)
            vector.push(deserializeFunc(this))

        return vector
    }

    /**
    * Extra los bytes almacenado en el flujo de datos con la longitud determinada por su VarInt.
    * 
    * @returns {Buffer} Buffer extraído del flujo de datos.
    */
    public deappendBuffer(): Buffer {
        const ref = { size: 0 }
        const lenght = this._buffer.readVarNum(0, ref)

        this._buffer = this._buffer.slice(ref.size)

        const buffer = this._buffer.read(lenght)

        this._buffer = this._buffer.slice(lenght)

        return buffer
    }



    /**
     * Extra un número de tamaño variable.
     * 
     * @returns {number} Número de tamaño variable.
     */
    public deappendVarInt(): number {
        const ref = { size: 0 }
        const value = this._buffer.readVarNum(0, ref)

        this._buffer = this._buffer.slice(ref.size)

        return value
    }

    /**
     * Extra el primer byte del flujo y los devuelve como un número.
     * 
     * @returns {number} Número de 8 bits.
     */
    public deappendUInt8(): number {
        const value = this._buffer.readUInt8(0)
        this._buffer = this._buffer.slice(1)

        return value
    }

    /**
     * Obtiene el buffer del flujo de datos.
     * 
     * @returns {Buffer} Buffer del flujo.
     * @property
     */
    public get data(): Buffer {
        return this._buffer
    }

    /**
     * Indica la cantidad de bytes contenidos en el flujo de datos.
     * 
     * @returns {number} Cantidad de bytes contenidos en el flujo.
     * @property
     */
    public get size(): number {
        return this._buffer.length
    }

    /**
     * Vacía el flujo de datos.
     */
    public clear(): void {
        this._buffer = BufferHelper.zero()
    }

    /**
     * Indica si el flujo de datos está vacío.
     * 
     * @returns {boolean} True para indicar que el flujo está vacío.
     */
    public isEmpty(): boolean {
        return this._buffer.length == 0
    }

    /**
     * Muestra una cadena de caracteres que representa en hexadecimal 
     * los valores dentro del flujo de datos.
     * 
     * @returns {string} Cadena de caracteres del flujo.
     */
    public toHex(): string {
        return this._buffer.toHex()
    }

    /**
     * Muestra una cadena de caracteres que representa en hexadecimal 
     * los valores dentro del flujo de datos.
     * 
     * @returns {string} Cadena de caracteres del flujo.
     */
    public toString(): string {
        return this.toHex()
    }
}
