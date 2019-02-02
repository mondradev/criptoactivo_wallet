/*
 *    Copyright 2018 InnSy Tech
 *    Copyright 2018 Ing. Javier de Jesús Flores Mondragón
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.cryptowallet.wallet.coinmarket;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.coinmarket.coins.CoinFactory;

import org.json.JSONObject;

/**
 * Se encarga de obtener el precio de Bitcoin contra Pesos mexicanos a través del API Rest de Bitso.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class BitsoBtcMxnService extends PairBase {

    /**
     * URL de la API Rest de Bitso.
     */
    private static final String MXN_URL = "https://api.bitso.com/v3/ticker/?book=btc_mxn";

    /**
     * Crea una nueva petición del precio.
     *
     * @param context Contexto de la aplicación.
     */
    public BitsoBtcMxnService(Context context) {
        super(context,
                CoinFactory.getOne(SupportedAssets.BTC),
                CoinFactory.getOne(SupportedAssets.MXN)
        );
    }

    /**
     * Crea la petición para solicitar el precio del Btc en pesos mexicanos en Bitso.
     *
     * @return La petición del precio.
     */
    @Override
    protected JsonObjectRequest createRequest() {
        return new JsonObjectRequest(
                Request.Method.GET, MXN_URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response == null)
                        return;

                    double value = response.getJSONObject("payload").getDouble("last");

                    setPrice(value);

                    done();
                } catch (Exception ignored) {
                }
            }
        }, Throwable::printStackTrace);
    }
}
