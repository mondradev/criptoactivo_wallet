package com.cryptowallet.wallet.coinmarket.exchangeables;

import android.content.Context;

import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.coinmarket.BitsoBtcMxnService;
import com.cryptowallet.wallet.coinmarket.CoinbaseBtcUsdService;
import com.cryptowallet.wallet.coinmarket.PairBase;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Convierte los montos de otros activos a montos expresados en Dolares estadounidenses.
 *
 * @author Ing. Javier Flores
 * @version 1.2
 */
public final class BtcExchangeable extends ExchangeableBase {

    /**
     * Crea una nueva instancia de un intercambiador.
     */
    public BtcExchangeable(Context context) {
        super(SupportedAssets.BTC, getService(context));
    }

    /**
     * Obtiene los servicios que se encargarán de obtener los precios de BTC.
     *
     * @param context Contexto de la aplicación.
     * @return Un diccionario de servicios.
     */
    private static Dictionary<SupportedAssets, PairBase> getService(Context context) {
        Dictionary<SupportedAssets, PairBase> service = new Hashtable<>();

        service.put(SupportedAssets.USD, new CoinbaseBtcUsdService(context));
        service.put(SupportedAssets.MXN, new BitsoBtcMxnService(context));

        return service;
    }
}
