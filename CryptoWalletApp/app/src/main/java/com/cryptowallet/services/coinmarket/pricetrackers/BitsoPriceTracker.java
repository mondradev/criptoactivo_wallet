/*
 * Copyright &copy; 2023. Criptoactivo
 * Copyright &copy; 2023. InnSy Tech
 * Copyright &copy; 2023. Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.services.coinmarket.pricetrackers;

import com.cryptowallet.core.domain.Book;
import com.cryptowallet.core.domain.SupportedAssets;
import com.cryptowallet.services.coinmarket.PriceTracker;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Fabrica que permite crear un seguidor de precio para un par manejado en el intercambio de Bitso.
 * Una vez que es creado el seguidor, puede ser utilizado en toda la aplicación ya que son
 * almacenados.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 * @see <a href="https://bitso.com/api_info?java#ticker">Bitso API v3</a>
 * @see <a href="https://api.bitso.com/v3/available_books/">Pares disponibles en Bitso</a>
 */
public class BitsoPriceTracker extends PriceTracker {

    /**
     * Libro de Bitcoin-PesosMxn Bitso
     */
    public static final Book BTCMXN
            = new Book(SupportedAssets.BTC.INSTANCE, SupportedAssets.MXN.INSTANCE, "btc_mxn");

    /**
     * URL de la API Rest de Bitso.
     */
    private static final String API_URL = "https://api.bitso.com/v3/";

    /**
     * Instancia de la clase singleton.
     */
    private static Map<Book, PriceTracker> mBooks;

    /**
     * Servicio Bitso para Retrofit
     */
    private final BitsoService mService;

    /**
     * Crea una nueva instancia del seguimiento de precio en Bitso.
     */
    private BitsoPriceTracker(Book book) {
        super(book);
        this.mService = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BitsoService.class);
    }

    /**
     * Obtiene la instancia del singletón para el seguimiento del precio.
     *
     * @return Instancia del singletón.
     */
    public static PriceTracker get(Book book) {
        if (mBooks == null)
            mBooks = new HashMap<>();

        if (!mBooks.containsKey(book))
            mBooks.put(book, new BitsoPriceTracker(book));

        return mBooks.get(book);
    }

    /**
     * Realiza una petición para obtener el precio actual.
     */
    @Override
    protected void requestPrice() {
        mService.getTicker(this.getBook().getKey())
                .clone()
                .enqueue(new Callback<TickerResponse>() {
                    @Override
                    public void onResponse(@NotNull Call<TickerResponse> call,
                                           @NotNull Response<TickerResponse> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || !response.body().mSuccess)
                            return;

                        setPrice(Math.round(response.body().mPayload.mLast
                                * getBook().getPriceAsset().getUnit()));
                    }

                    @Override
                    public void onFailure(@NotNull Call<TickerResponse> call,
                                          @NotNull Throwable t) {
                        requestPrice();
                    }
                });

    }

    /**
     * Estructura del servicio de API Rest de Bitso
     *
     * @author Ing. Javier Flores (jjflores@innsytech.com)
     * @version 1.0
     */
    private interface BitsoService {
        /**
         * Obtiene el precio de un par utilizado en el intercambio de Bitso.
         *
         * @param ticker Nombre del par.
         * @return Una instancia que permite realizar la petición.
         */
        @GET("ticker")
        Call<TickerResponse> getTicker(@Query("book") String ticker);
    }

    /**
     * Estructura de la respuesta de ticker en Bitso.
     *
     * @author Ing. Javier Flores (jjflores@innsytech.com)
     * @version 1.0
     */
    @SuppressWarnings("unused")
    private static class TickerResponse {

        /**
         * Indica si se completó la petición.
         */
        @SerializedName("success")
        @Expose
        private boolean mSuccess;

        /**
         * Datos obtenidos de la petición.
         */
        @SerializedName("payload")
        @Expose
        private Ticker mPayload;
    }

    /**
     * Estructura parcial de un ticker de Bitso para obtener el último precio.
     *
     * @author Ing. Javier Flores (jjflores@innsytech.com)
     * @version 1.0
     */
    @SuppressWarnings("unused")
    private static class Ticker {

        /**
         * Último precio del par.
         */
        @SerializedName("last")
        @Expose
        private float mLast;
    }
}
