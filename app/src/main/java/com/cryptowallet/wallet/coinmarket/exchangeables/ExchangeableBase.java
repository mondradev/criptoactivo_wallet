package com.cryptowallet.wallet.coinmarket.exchangeables;

import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.coinmarket.PairBase;
import com.cryptowallet.wallet.coinmarket.coins.CoinBase;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Define la estructura base de un conversor de valores.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public abstract class ExchangeableBase {

    /**
     * Servicios que actualizan el precio.
     */
    private final Dictionary<SupportedAssets, PairBase> mService;

    /**
     * Crea una nueva instancia de un intercambiador.
     *
     * @param asset    Activo del intercambiador.
     * @param services Servicios de precio.
     */
    ExchangeableBase(SupportedAssets asset, Dictionary<SupportedAssets, PairBase> services) {
        this.mService = services;
    }

    /**
     * Obtiene los servicios de precio.
     *
     * @return Servicios de precio.
     */
    private Dictionary<SupportedAssets, PairBase> getServices() {
        return mService;
    }

    /**
     * Obtiene el precio del monto especificado.
     */
    public CoinBase convertTo(SupportedAssets asset, final CoinBase value) {
        PairBase pair = getServices().get(asset);

        if (!Utils.isNull(pair))
            return pair.getPrice(value);

        return value;
    }

    public CoinBase convertFrom(final CoinBase value) {
        SupportedAssets asset = value.getAsset();
        PairBase pair = getServices().get(asset);

        if (!Utils.isNull(pair))
            return pair.getAmount(value);
        return value;
    }

    /**
     * Notifica si se finalizó la actualización de los precios.
     */
    public void nofityIfUpdated() {
        for (PairBase service
                : ((Hashtable<SupportedAssets, PairBase>) mService).values())
            service.notifyIfDone();
    }

    /**
     * Se intenta actualizar el precio.
     */
    public void updatePrice() {
        for (PairBase service
                : ((Hashtable<SupportedAssets, PairBase>) mService).values())
            service.sendRequest();
    }
}