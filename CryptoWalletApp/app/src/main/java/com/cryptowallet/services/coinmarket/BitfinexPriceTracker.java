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

package com.cryptowallet.services.coinmarket;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Fabrica que permite crear un seguidor de precio para un par manejado en el intercambio de
 * Bitfinex.
 * Una vez que es creado el seguidor, puede ser utilizado en toda la aplicación ya que son
 * almacenados.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 * @see <a href="https://docs.bitfinex.com/reference#rest-public-ticker">Bitfinex API v2</a>
 * @see <a href="https://api-pub.bitfinex.com/v2/tickers?symbols=ALL">Pares disponibles en Bitfinex</a>
 */
@SuppressWarnings("unused")
public class BitfinexPriceTracker extends PriceTracker {

    /**
     * URL de la API Rest de Bitfinex.
     */
    private static final String API_URL = "https://api-pub.bitfinex.com/v2/";

    /**
     * Instancia de la clase singleton.
     */
    private static Map<String, PriceTracker> mBooks;

    /**
     * Servicio Bitfinex para Retrofit
     */
    private final BitfinexService mService;

    /**
     * Par del seguimiento.
     */
    private String mBook;

    /**
     * Crea una nueva instancia del seguimiento de precio en Bitfinex.
     */
    private BitfinexPriceTracker(String book) {
        this.mService = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BitfinexService.class);

        this.mBook = book;
    }

    /**
     * Obtiene la instancia del singletón para el seguimiento del precio.
     *
     * @return Instancia del singletón.
     */
    public static PriceTracker get(String book) {
        if (mBooks == null)
            mBooks = new HashMap<>();

        if (!mBooks.containsKey(book))
            mBooks.put(book, new BitfinexPriceTracker(book));

        return mBooks.get(book);
    }

    /**
     * Realiza una petición para obtener el precio actual.
     */
    @Override
    protected void request() {
        mService.getTicker(this.mBook)
                .clone()
                .enqueue(new Callback<Float[]>() {
                    @Override
                    public void onResponse(@NotNull Call<Float[]> call,
                                           @NotNull Response<Float[]> response) {
                        if (!response.isSuccessful() || response.body() == null)
                            return;

                        setPrice(response.body()[6]);
                    }

                    @Override
                    public void onFailure(@NotNull Call<Float[]> call,
                                          @NotNull Throwable t) {
                        retryRequest();
                    }
                });

    }

    /**
     * Estructura del servicio de API Rest de Bitfinex.
     *
     * @author Ing. Javier Flores (jjflores@innsytech.com)
     * @version 1.0
     */
    private interface BitfinexService {

        /**
         * Obtiene el precio de un par utilizado en el intercambio de Bitfinex.
         *
         * @param ticker Nombre del par.
         * @return Una instancia que permite realizar la petición.
         */
        @GET("ticker/{book}")
        Call<Float[]> getTicker(@Path("book") String ticker);
    }
}
