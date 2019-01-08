/*
 * Copyright 2019 InnSy Tech
 * Copyright 2019 Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.wallet.coinmarket;


import android.content.Context;

import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.coinmarket.exchangeables.BtcExchangeable;
import com.cryptowallet.wallet.coinmarket.exchangeables.IExchangeable;
import com.cryptowallet.wallet.coinmarket.exchangeables.MxnExchangeable;
import com.cryptowallet.wallet.coinmarket.exchangeables.UsdExchangeable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * Ofrece servicio para realizar las conversiones o solictudes de precio en el mercado de las
 * cryptomonedas.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class ExchangeService {

    /**
     * Instancia de la clase.
     */
    private static ExchangeService mInstance;

    /**
     * Colección de intercambiadores de activos.
     */
    private Map<SupportedAssets, IExchangeable> mCurrencies = new HashMap<>();

    /**
     * Colección de escuchas de eventos.
     */
    private CopyOnWriteArrayList<IListener> mListeners
            = new CopyOnWriteArrayList<>();

    /**
     * Petición de precio a Coinbase de BTCUSD.
     */
    private RequestPriceServiceBase usdPriceRequest;

    /**
     * Petición de precio a Bitso de BTCMXN.
     */
    private RequestPriceServiceBase mxnPriceRequest;

    /**
     * Crea una nueva instancia.
     *
     * @param context Contexto de la aplicación Android.
     */
    private ExchangeService(Context context) {
        usdPriceRequest = new CoinbaseBtcUsdService(context);
        mxnPriceRequest = new BitsoBtcMxnService(context);

        mCurrencies.put(SupportedAssets.BTC, new BtcExchangeable());
        mCurrencies.put(SupportedAssets.USD, new UsdExchangeable());
        mCurrencies.put(SupportedAssets.MXN, new MxnExchangeable());
    }

    /**
     * Obtiene la instancia generada a través de la función {@link ExchangeService#init(Context)}.
     *
     * @return La instancia de {@link ExchangeService}.
     * @throws IllegalStateException En caso de no haber inicializado la clase.
     */
    public static ExchangeService get() {
        if (mInstance == null)
            throw new IllegalStateException("Se requiere llamar ExchangeService#init(Context)");

        return mInstance;
    }

    /**
     * Inicializa la clase para permitir generar las peticiones a los servicios.
     *
     * @param context Contexto de la aplicación Android.
     * @throws IllegalStateException En caso de haber inicializado la clase anteriormente.
     */
    public static void init(Context context) {
        if (mInstance != null)
            throw new IllegalStateException("Solo se puede llamar una vez por ejecución.");

        mInstance = new ExchangeService(context);
    }

    /**
     * Obtiene un valor que indica si la clase fue inicializada.
     *
     * @return Indica si la clase fue inicializada.
     */
    public static boolean isInitialized() {
        return mInstance != null;
    }

    /**
     * Notifica a todos los escuchas de una actualización de precio.
     *
     * @param assets       Activo que actualizó su precio.
     * @param smallestUnit El precio expresado en su unidad más pequeña.
     */
    void notifyListeners(final SupportedAssets assets, final long smallestUnit) {
        Executors.newSingleThreadExecutor().execute(() -> {
            for (IListener listener : mListeners)
                listener.onUpdatePrice(assets, smallestUnit);
        });
    }

    /**
     * Obtiene el precio en USD de un monto especificado en BTC.
     *
     * @param smallestUnit Monto de BTC expresado en su unidad más pequeña.
     * @return El precio en USD del monto.
     */
    public long btcToUsd(long smallestUnit) {
        return mInstance.usdPriceRequest.exchange(smallestUnit);
    }

    /**
     * Obtiene el precio en MXN de un monto especificado en BTC.
     *
     * @param smallestUnit Monto de BTC expresado en su unidad más pequeña.
     * @return El precio en MXN del monto.
     */
    public long btcToMxn(long smallestUnit) {
        return mInstance.mxnPriceRequest.exchange(smallestUnit);
    }

    /**
     * Obtiene la función de intercambio para calcular el precio del monto especificado del activo.
     *
     * @param symbol Activo de la función de intercambio.
     * @return Una función de intercambio.
     */
    public IExchangeable getExchange(SupportedAssets symbol) {
        if (mCurrencies.containsKey(symbol))
            return mCurrencies.get(symbol);
        else
            throw new UnsupportedOperationException("No se soporta el activo");
    }

    /**
     * Agrega un escucha de eventos para los cambios del precio.
     *
     * @param listener Escucha del precio.
     */
    public void addEventListener(IListener listener) {
        if (!mListeners.contains(listener))
            mListeners.add(listener);

        completedTasks();
    }

    /**
     * Verifica si las peticiones fueron completadas, y activa los escuchas.
     */
    private void completedTasks() {
        mInstance.mxnPriceRequest.notifyIfDone();
        mInstance.usdPriceRequest.notifyIfDone();
    }

    /**
     * Remueve un escucha de eventos para los cambios del precio.
     *
     * @param listener Escucha del precio.
     */
    public void removeEventListener(IListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Re-inicia las peticiones de precio.
     */
    public void reloadMarketPrice() {
        usdPriceRequest.sendRequest();
        mxnPriceRequest.sendRequest();
    }

    /**
     * Define el escucha de la actualización del precio de algún activo.
     *
     * @author Ing. Javier Flores
     * @version 1.0
     */
    public interface IListener {

        /**
         * Este método se ejecuta cuando el precio de un activo es actualizado.
         *
         * @param asset        Activo que fue actualizado.
         * @param smallestUnit Precio del activo expresado en su unidad más pequeña.
         */
        void onUpdatePrice(SupportedAssets asset, long smallestUnit);
    }

}
