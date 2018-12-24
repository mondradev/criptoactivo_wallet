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
import com.cryptowallet.wallet.SupportedAssets;

/**
 * Una clase base para solicitar el precio de un activo y permitir realizar las conversiones con los
 * activos compatibles.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public abstract class RequestPriceServiceBase {

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
     * Activo que se solicita el precio.
     */
    private final SupportedAssets mAsset;

    /**
     * Precio del activo.
     */
    private Long mSmallestValue = 1L;

    /**
     * Indica si ya se realizó la petición.
     */
    private boolean mDone = false;

    /**
     * La unidad expresada en su porción más pequeña.
     */
    private Long mSmallestUnit;

    /**
     * La unidad base expresada en su porcion más pequeña.
     */
    private Long mSmallestUnitBase;

    /**
     * Crea una nueva petición del precio.
     *
     * @param context Contexto de la aplicación.
     * @param asset   Activo base que se solicitará el precio.
     */
    protected RequestPriceServiceBase(Context context, SupportedAssets asset) {
        mContext = context;
        mAsset = asset;
        mRequest = createRequest();
    }

    /**
     * Crea la petición para solicitar el precio del activo al servidor.
     *
     * @return La petición del precio.
     */
    protected abstract JsonObjectRequest createRequest();


    /**
     * Establece la unidad del activo y su base expresada en su porción más pequeña.
     *
     * @param asset Unidad del activo expresada en su porción más pequeña.
     * @param base  Unidad del activo base expresada en su porción más pequeña.
     */
    public final void setSmallestUnits(long asset, long base) {
        mSmallestUnit = asset;
        mSmallestUnitBase = base;
    }

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
     * Obtiene el precio del activo expresado en su porción más pequeña.
     *
     * @return Precio del activo.
     */
    public final Long getSmallestValue() {
        synchronized (this) {
            try {
                if (!mDone)
                    wait();

                return mSmallestValue;
            } catch (InterruptedException ignored) {
            }

            return 1L;
        }
    }

    /**
     * Establece el precio del activo expresado en su porción más pequeña.
     *
     * @param smallestValue Precio del activo.
     */
    public final void setSmallestValue(Long smallestValue) {
        synchronized (this) {
            mSmallestValue = smallestValue;
        }
    }

    /**
     * Obtiene la unidad del activo base expresada en su porción más pequeña.
     *
     * @return La unidad en su porción más pequeña.
     */
    public final Long getSmallestUnitBase() {
        return mSmallestUnitBase;
    }

    /**
     * Realiza la conversión de un monto del activo utilizando el último precio consultado.
     *
     * @param smallestValue Monto del activo.
     * @return El precio del monto del activo.
     */
    public final Long exchange(Long smallestValue) {

        double asset = (double) mSmallestValue;
        double smallestUnit = (double) mSmallestUnit;
        double amount = (double) smallestValue;

        // TODO: Corregir conversión
        return (long) (amount / smallestUnit * asset);
    }

    /**
     * Notifica a los escuchas de la clase {@link ExchangeService}.
     */
    public void notifyIfDone() {
        if (mDone)
            ExchangeService.get().notifyListeners(mAsset, getSmallestValue());
    }

    /**
     * Indica que ya se completó la petición.
     */
    protected final void done() {
        synchronized (this) {
            mDone = true;
            notify();

            ExchangeService.get().notifyListeners(mAsset, getSmallestValue());
        }
    }
}
