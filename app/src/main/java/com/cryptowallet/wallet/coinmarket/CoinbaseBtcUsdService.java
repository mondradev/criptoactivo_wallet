package com.cryptowallet.wallet.coinmarket;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.cryptowallet.wallet.SupportedAssets;

import org.json.JSONObject;

/**
 * Se encarga de obtener el precio de Bitcoin contra Dolares estadounidenses a través del API Rest
 * de Coinbase.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
final class CoinbaseBtcUsdService extends RequestPriceServiceBase {

    /**
     * URL del API Rest de Coinbase.
     */
    private static final String USD_URL = "https://api.bitfinex.com/v1/pubticker/btcusd";

    /**
     * Crea una nueva petición del precio.
     *
     * @param context Contexto de la aplicación.
     */
    protected CoinbaseBtcUsdService(Context context) {
        super(context, SupportedAssets.USD);

        setSmallestUnits(100000000, 10000);
    }

    /**
     * Crea la petición para solicitar el precio del activo al servidor.
     *
     * @return La petición del precio.
     */
    @Override
    protected JsonObjectRequest createRequest() {
        return new JsonObjectRequest(
                Request.Method.GET, USD_URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response == null)
                        return;

                    Double value = response.getDouble("last_price");

                    setSmallestValue((long) (value * getSmallestUnitBase()));

                    done();
                } catch (Exception ignored) {
                }
            }
        }, null);
    }
}
