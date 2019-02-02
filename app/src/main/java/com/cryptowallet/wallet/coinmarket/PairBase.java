/*
 * Copyright 2018 InnSy Tech
 * Copyright 2018 Ing. Javier de Jesús Flores Mondragón
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

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.cryptowallet.wallet.coinmarket.coins.CoinBase;
import com.cryptowallet.wallet.coinmarket.coins.CoinFactory;

/**
 * Una clase base para solicitar el precio de un activo y permitir realizar las conversiones con los
 * activos compatibles.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public abstract class PairBase {

    /**
     * Cola de peticiones.
     */
    private static RequestQueue mRequestQueue;

    /**
     * Instancia de la petición Json.
     */
    private final JsonObjectRequest mRequest;

    /**
     * Contexto de la aplicación Android.
     */
    private final Context mContext;

    /**
     * Activo utilizado para los montos.
     */
    private final CoinBase mLeft;

    /**
     * Activo utilizado para los precios.
     */
    private final CoinBase mRight;

    /**
     * Indica si ya se realizó la petición.
     */
    private boolean mDone = false;

    /**
     * Crea una nueva petición del precio.
     *
     * @param context Contexto de la aplicación.
     * @param left    Activo en el que se representan los montos.
     * @param right   Activo en el que se calcula el precio.
     */
    protected PairBase(Context context, CoinBase left, CoinBase right) {
        mContext = context;
        mLeft = left;
        mRight = right;
        mRequest = createRequest();
    }

    /**
     * Crea la petición para solicitar el precio del activo al servidor.
     *
     * @return La petición del precio.
     */
    protected abstract JsonObjectRequest createRequest();

    /**
     * Envía la solicitud al servidor.
     */
    public final void sendRequest() {
        synchronized (this) {
            initializeQueue();

            mDone = false;
            mRequestQueue.add(mRequest);
        }
    }

    /**
     * Inicializa la cola de peticiones.
     */
    private void initializeQueue() {
        if (mRequestQueue != null)
            return;

        Cache cache = new DiskBasedCache(mContext.getCacheDir(), 1024 * 2014);
        Network network = new BasicNetwork(new HurlStack());

        mRequestQueue = new RequestQueue(cache, network);
        mRequestQueue.start();
    }

    /**
     * Realiza la conversión de un monto del activo utilizando el último precio consultado.
     *
     * @return El precio del monto del activo.
     */
    public final CoinBase getPrice(CoinBase amount) {
        double value = reajustDecimals(amount, mLeft);
        value = value / mLeft.getValue() * mRight.getValue();
        return CoinFactory.valueOf(mRight.getAsset(), (long) value);
    }

    private long reajustDecimals(CoinBase amount, CoinBase right) {
        double value = amount.getValue() / Math.pow(10, amount.getMaxDecimals());
        value = value * Math.pow(10, right.getMaxDecimals());

        return (long) value;
    }

    /**
     * Notifica a los escuchas de la clase {@link ExchangeService}.
     */
    public void notifyIfDone() {
        if (mDone)
            ExchangeService.get().notifyListeners(mRight.getAsset(), mRight);
    }

    /**
     * Indica que ya se completó la petición.
     */
    protected final void done() {
        synchronized (this) {
            mDone = true;
            notify();

            ExchangeService.get().notifyListeners(mRight.getAsset(), mRight);
        }
    }

    /**
     * @param price
     */
    public void setPrice(double price) {
        mRight.setValue((long) (price * Math.pow(10, mRight.getMaxDecimals())));
    }

    /**
     * Realiza la conversión de un precio especificado para obtener el monto.
     *
     * @param price Precio del monto del activo.
     * @return Monto del activo.
     */
    public CoinBase getAmount(CoinBase price) {
        double value = price.getValue();
        value = value / mRight.getValue() * mLeft.getValue();
        return CoinFactory.valueOf(mLeft.getAsset(), (long) value);
    }
}
