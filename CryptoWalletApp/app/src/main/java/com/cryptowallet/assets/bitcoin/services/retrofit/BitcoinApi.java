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

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Define los endpoints de la api Bitcoin que serán utilizados por {@link retrofit2.Retrofit}.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public interface BitcoinApi {
    /**
     * Obtiene el historial de transacciones de una dirección.
     *
     * @param network Tipo de red a la cual pertenece la dirección. Ej: mainnet o testnet.
     * @param address Dirección serializada en formato hexadecimal.
     * @return Una instancia que gestiona la llamada asíncrona a la API.
     */
    @GET("btc/{network}/txhistory/{address}")
    Call<List<TxData>> getTxHistory(@Path("network") String network,
                                    @Path("address") String address);

    /**
     * Obtiene la transacción especificada por el TxID.
     *
     * @param network Tipo de red a la cual pertenece la transacción. Ej: mainnet o testnet.
     * @param txid    Identificador único de la transacción.
     * @return Una instancia que gestiona la llamada asíncrona de la API.
     */
    @GET("btc/{network}/tx/{txid}")
    Call<TxData> getTx(@Path("network") String network, @Path("txid") String txid);

    /**
     * Obtiene la información de la blockchain.
     *
     * @param network Tipo de red de la cual se requiere la información. Ej: mainnet o testnet.
     * @return Una instancia que gestiona la llamana asíncrona de la API.
     */
    @GET("btc/{network}/chaininfo")
    Call<ChainInfo> getChainInfo(@Path("network") String network);

    /**
     * Obtiene las dependencias de la transacción especificada por el TxID.
     *
     * @param network Tipo de red a la cual pertenece la transacción. Ej: mainnet o testnet.
     * @param txid    Identificador único de la transacción.
     * @return Una instancia que gestiona la llamada asíncrona de la API.
     */
    @GET("btc/{network}/txdeps/{txid}")
    Call<List<TxData>> getTxDeps(@Path("network") String network,
                                 @Path("txid") String txid);
}
