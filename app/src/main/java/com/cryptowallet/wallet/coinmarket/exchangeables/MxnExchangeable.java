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

package com.cryptowallet.wallet.coinmarket.exchangeables;

import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.coinmarket.ExchangeService;

import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;

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
                return "$ " + MonetaryFormat.FIAT.repeatOptionalDecimals(2, 0)
                        .code(0, SupportedAssets.MXN.name())
                        .postfixCode()
                        .format(Fiat.valueOf(SupportedAssets.MXN.name(),
                                ExchangeService.get().btcToMxn(value))).toString();
            default:
                return "";
        }
    }
}
