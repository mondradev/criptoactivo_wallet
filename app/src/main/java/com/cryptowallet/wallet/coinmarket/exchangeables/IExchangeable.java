package com.cryptowallet.wallet.coinmarket.exchangeables;

import com.cryptowallet.wallet.SupportedAssets;

/**
 * Define la estructura base de un conversor de valores.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public interface IExchangeable {

    /**
     * Convierte el monto expresado en su unidad más pequeña al activo manejado por la instancia.
     *
     * @param currencyBase Activo en el cual se expresa el monto.
     * @param value        Monto a convertir.
     * @return Monto convertido.
     */
    String ToStringFriendly(SupportedAssets currencyBase, long value);
}