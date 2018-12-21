package com.cryptowallet.wallet.coinmarket.exchangeables;

import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.coinmarket.ExchangeService;

import org.bitcoinj.utils.Fiat;

/**
 * Convierte los montos de otros activos a montos expresados en Pesos mexicanos.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class MxnExchangeable implements IExchangeable {

    /**
     * Convierte el monto expresado en su unidad más pequeña al activo manejado por la instancia.
     *
     * @param currencyBase Activo en el cual se expresa el monto.
     * @param value        Monto a convertir.
     * @return Monto convertido.
     */
    @Override
    public String ToStringFriendly(SupportedAssets currencyBase, long value) {
        switch (currencyBase) {
            case BTC:
                return Fiat.valueOf(SupportedAssets.MXN.name(),
                        ExchangeService.get().btcToMxn(value)).toFriendlyString();
            default:
                return "";
        }
    }
}
