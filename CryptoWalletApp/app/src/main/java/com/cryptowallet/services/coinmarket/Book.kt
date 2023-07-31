/*
 * Copyright &copy; 2023. Criptoactivo
 * Copyright &copy; 2023. InnSy Tech
 * Copyright &copy; 2023. Ing. Javier de Jesús Flores Mondragón
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
package com.cryptowallet.services.coinmarket

import com.cryptowallet.core.domain.SupportedAssets

/**
 * Define los atributos requeridos para los libros de un seguidor de precio. En este se especifica
 * el activo tradeado en el libro y su activo en el cual se representa el precio. También se
 * define la clave utilizada por el servicio para identificar este libro y así poder realizar las
 * peticiones al servidor del exchange.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 * @see PriceTracker
 *
 * @see SupportedAssets
 */
data class Book
/**
 * Crea una instancia de un libro de comercio.
 *
 * @param crypto     Activo comerciado.
 * @param priceAsset Activo de precio.
 * @param key        Clave del libro.
 */(
    /**
     * Activo comerciado.
     */
    val cryptoAsset: SupportedAssets,
    /**
     * Activo usado para el precio.
     */
    val priceAsset: SupportedAssets,
    /**
     * Clave utilizada para identificar el libro de comercio.
     */
    val key: String
)