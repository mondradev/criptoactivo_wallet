package com.cryptowallet.wallet.coinmarket;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.cryptowallet.wallet.SupportedAssets;

import org.json.JSONObject;

/**
 * Se encarga de obtener el precio de Bitcoin contra Pesos mexicanos a través del API Rest de Bitso.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
final class BitsoBtcMxnService extends RequestPriceServiceBase {

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
        super(context, SupportedAssets.MXN);

        setSmallestUnits(100000000, 10000);
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

                    Double value = response.getJSONObject("payload").getDouble("last");

                    setSmallestValue((long) (value * getSmallestUnitBase()));

                    done();
                } catch (Exception ignored) {
                }
            }
        }, null);
    }
}
