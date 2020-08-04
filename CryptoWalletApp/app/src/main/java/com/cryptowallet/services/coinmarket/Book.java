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

package com.cryptowallet.services.coinmarket;

import com.cryptowallet.wallet.SupportedAssets;

/**
 * Define los atributos requeridos para los libros de un seguidor de precio. En este se especifica
 * el activo tradeado en el libro y su activo en el cual se representa el precio. También se
 * define la clave utilizada por el servicio para identificar este libro y así poder realizar las
 * peticiones al servidor del exchange.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 * @see PriceTracker
 * @see SupportedAssets
 */
public class Book {

    /**
     * Activo comerciado.
     */
    private SupportedAssets mCryptoAsset;

    /**
     * Activo usado para el precio.
     */
    private SupportedAssets mPriceAsset;

    /**
     * Clave utilizada para identificar el libro de comercio.
     */
    private String mKey;

    /**
     * Crea una instancia de un libro de comercio.
     *
     * @param crypto     Activo comerciado.
     * @param priceAsset Activo de precio.
     * @param key        Clave del libro.
     */
    public Book(SupportedAssets crypto, SupportedAssets priceAsset, String key) {
        this.mCryptoAsset = crypto;
        this.mPriceAsset = priceAsset;
        this.mKey = key;
    }

    /**
     * Obtiene el activo comerciado en el libro.
     *
     * @return Activo.
     */
    public SupportedAssets getCryptoAsset() {
        return mCryptoAsset;
    }

    /**
     * Obtiene el activo utilizado para dar precio.
     *
     * @return Activo de precio.
     */
    public SupportedAssets getPriceAsset() {
        return mPriceAsset;
    }

    /**
     * Obtiene la clave utilizada para identificar el libro en el intercambio.
     *
     * @return Clave del libro.
     */
    public String getKey() {
        return mKey;
    }
}
