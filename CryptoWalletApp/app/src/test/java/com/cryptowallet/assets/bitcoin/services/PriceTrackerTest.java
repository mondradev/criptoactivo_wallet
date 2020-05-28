/*
 * Copyright © 2020. Criptoactivo
 * Copyright © 2020. InnSy Tech
 * Copyright © 2020. Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.assets.bitcoin.services;

import com.cryptowallet.services.coinmarket.BitfinexPriceTracker;
import com.cryptowallet.services.coinmarket.BitsoPriceTracker;
import com.cryptowallet.services.coinmarket.PriceTracker;

import org.junit.Test;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Pruebas para los seguidores de precio.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class PriceTrackerTest {

    /**
     * Libro de Bitcoin-PesosMxn Bitso
     */
    private static final String BOOK_BTC_MXN = "btc_mxn";

    /**
     * Libro de Bitcoin-USD Bitfinex
     */
    private static final String BOOK_BTC_USD = "tBTCUSD";

    /**
     * Evalua el seguimiento del precio de Bitcoin en Bitso y es pasada si este es mayor a cero.
     */
    @Test
    public void bitsoPriceTracker() {
        final PriceTracker bitsoBtcMxnTracker = BitsoPriceTracker.get(BOOK_BTC_MXN);
        final PriceTracker.IListener listener = mock(PriceTracker.IListener.class);

        bitsoBtcMxnTracker.addChangeListener(listener);

        verify(listener, timeout(2000))
                .onChange(ArgumentMatchers.floatThat(price -> price > 0));
    }

    /**
     * Evalua el seguimiento del precio de Bitcoin en Bitfinex y es pasada si este es mayor a cero.
     */
    @Test
    public void bitfinexPriceTracker() {
        final PriceTracker bitfinexBtcUsdTracker = BitfinexPriceTracker.get(BOOK_BTC_USD);
        final PriceTracker.IListener listener = mock(PriceTracker.IListener.class);

        bitfinexBtcUsdTracker.addChangeListener(listener);

        verify(listener, timeout(2000))
                .onChange(ArgumentMatchers.floatThat(price -> price > 0));
    }
}