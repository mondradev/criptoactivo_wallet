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

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.coinmarket.coins.CoinFactory;

/**
 * Se encarga de obtener el precio de Bitcoin contra Dolares estadounidenses a través del API Rest
 * de Coinbase.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class CoinbaseBtcUsdService extends PairBase {

    /**
     * URL del API Rest de Coinbase.
     */
    private static final String USD_URL = "https://api.bitfinex.com/v1/pubticker/btcusd";

    /**
     * Crea una nueva petición del precio.
     *
     * @param context Contexto de la aplicación.
     */
    public CoinbaseBtcUsdService(Context context) {
        super(context,
                CoinFactory.getOne(SupportedAssets.BTC),
                CoinFactory.getOne(SupportedAssets.USD)
        );
    }

    /**
     * Crea la petición para solicitar el precio del activo al servidor.
     *
     * @return La petición del precio.
     */
    @Override
    protected JsonObjectRequest createRequest() {
        return new JsonObjectRequest(
                Request.Method.GET, USD_URL, null, response -> {
            try {
                if (response == null)
                    return;

                Double value = response.getDouble("last_price");
                setPrice(value);

                done();
            } catch (Exception ignored) {
            }
        }, Throwable::printStackTrace);
    }
}
