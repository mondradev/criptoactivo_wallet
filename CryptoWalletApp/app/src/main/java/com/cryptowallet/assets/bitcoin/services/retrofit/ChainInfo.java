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

/**
 * Define una estructura para las respuestas con información de la blockchain de Bitcoin.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 * @see BitcoinApi
 * @see com.cryptowallet.assets.bitcoin.services.BitcoinProvider
 */
@SuppressWarnings("unused")
public class ChainInfo {

    /**
     * Altura del último bloque.
     */
    @SerializedName("height")
    @Expose
    private int mHeight;

    /**
     * Hash del último bloque.
     */
    @SerializedName("hash")
    @Expose
    private String mHash;

    /**
     * Tiempo del último bloque.
     */
    @SerializedName("time")
    @Expose
    private int mTime;

    /**
     * Número de transacciones en la blockchain.
     */
    @SerializedName("txn")
    @Expose
    private int mTxn;

    /**
     * Estado actual de la blockchain.
     */
    @SerializedName("status")
    @Expose
    private String mStatus;

    /**
     * Tipo de red de la blockchain.
     */
    @SerializedName("network")
    @Expose
    private String mNetwork;

    /**
     * Obtiene la altura actual de la blockchain.
     *
     * @return Altura de la blockchain.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Obtiene el hash del bloque más reciente de la blockchain.
     *
     * @return Hash del último bloque.
     */
    public String getHash() {
        return mHash;
    }

    /**
     * Obtiene la fecha y hora del último bloque generado en la blockchain.
     *
     * @return Fecha y hora expresado en segundos.
     */
    public int getTime() {
        return mTime;
    }

    /**
     * Obtiene el total de las transacciones almacenadas en la blockchain.
     *
     * @return Total de transacciones.
     */
    public int getTxn() {
        return mTxn;
    }

    /**
     * Obtiene el estado actual de la blockchain.
     *
     * @return Estado de la blockchain.
     */
    public String getStatus() {
        return mStatus;
    }

    /**
     * Obtiene el tipo de red de la blockchain. Ej. mainnet o testnet.
     *
     * @return Tipo de red.
     */
    public String getNetwork() {
        return mNetwork;
    }
}
