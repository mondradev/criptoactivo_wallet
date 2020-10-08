import '../../../utils/bufferhelper'

import crypto from 'crypto'
import UInt256 from './uint256'

/**
 * Hash de 256 bits.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export default class DSha256 {

    /**
     * Tamaño del vector del hash.
     */
    public static readonly OUTPUT_SIZE = 32

    /**
     * Algoritmo utilizado para generar el hash.
     */
    private static readonly ALGORITHM_HASH = "sha256"

    /**
     * Datos en crudo del hash
     */
    private _data: Buffer

    /**
     * Crea una instancia con un hash nulo.
     * 
     * @param data Datos en crudo del hash.
     */
    public constructor(data?: Buffer) {
        this._data = data || Buffer.alloc(DSha256.OUTPUT_SIZE, 0x00)
    }

    /**
     * Calcula y obtiene el hash de los datos.
     * 
     * @returns {UInt256} 32 bytes que representan como hash.
     */
    public get(): UInt256 {
        if (this._data.length == 0)
            return UInt256.null();

        let sha256 = crypto.createHash(DSha256.ALGORITHM_HASH)
        sha256.update(this._data)

        const prevHash = sha256.digest()

        sha256 = crypto.createHash(DSha256.ALGORITHM_HASH)
        sha256.update(prevHash)

        const hash = sha256.digest()

        return new UInt256(hash)
    }

    /**
     * Obtiene la representación hexadecimal del hash.
     * 
     * @returns {string} Representación hexadecimal del hash.
     */
    public toString(): string {
        const hash = this.get()

        return Buffer.from(hash.value).toHex()
    }
}  