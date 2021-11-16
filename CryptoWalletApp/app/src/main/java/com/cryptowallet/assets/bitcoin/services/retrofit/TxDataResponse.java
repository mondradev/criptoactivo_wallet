/*
 * Copyright © 2020. Criptoactivo
 * Copyright © 2020. InnSy Tech
 * Copyright © 2020. Ing. Javier de Jesús Flores Mondragón
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cryptowallet.assets.bitcoin.services.retrofit;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.bouncycastle.util.encoders.Hex;

/**
 * Define una estructura para las respuestas con información de transacciones de Bitcoin.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 * @see BitcoinApi
 * @see com.cryptowallet.assets.bitcoin.services.BitcoinProvider
 */
@SuppressWarnings("unused")
public class TxDataResponse {

    /**
     * Identificador de la transacción.
     */
    @SerializedName("txid")
    @Expose
    private String mTxid;

    /**
     * Data serializada de la transacción.
     */
    @SerializedName("data")
    @Expose
    private String mData;

    /**
     * Identificador del bloque.
     */
    @SerializedName("block")
    @Expose
    private String mBlock;

    /**
     * Fecha y hora del bloque o en la que fue recibida en el mempool.
     */
    @SerializedName("time")
    @Expose
    private Integer mTime;

    /**
     * Altura del bloque.
     */
    @SerializedName("height")
    @Expose
    private Integer mHeight;

    /**
     * Posición de la transacción en el bloque.
     */
    @SerializedName("index")
    @Expose
    private Integer mIndex;

    /**
     * Obtiene la posición de la transacción del bloque.
     *
     * @return Posición del bloque.
     */
    public int getBlockIndex() {
        return mIndex;
    }

    /**
     * Obtiene el identificador de la transacción.
     *
     * @return Identificador único de la transacción.
     */
    public String getTxid() {
        return mTxid;
    }

    /**
     * Obtiene la data serializada de la transacción en formato hexadecimal.
     *
     * @return Transacción serializada.
     */
    public String getData() {
        return mData;
    }

    /**
     * Obtiene el identificador del bloque al cual pertenece la transacción, en caso de estar en el
     * pool de memoria, devuelve un valor null.
     *
     * @return Identificador del bloque.
     */
    public String getBlock() {
        return mBlock;
    }

    /**
     * Obtiene la fecha y hora del bloque al cual pertenece o de llegada en el mempool.
     *
     * @return Fecha y hora de la transacción.
     */
    public Integer getTime() {
        return mTime;
    }

    /**
     * Obtiene la data serializada de la transacción.
     *
     * @return Transacción serializada.
     */
    public byte[] getDataAsBuffer() {
        return Hex.decode(this.getData());
    }

    /**
     * Obtiene la altura del bloque al cual pertenece la transacción, en caso de estar en el pool de
     * memoria, devuelve un valor -1.
     *
     * @return Altura del bloque.
     */
    public long getHeight() {
        return mHeight;
    }
}
