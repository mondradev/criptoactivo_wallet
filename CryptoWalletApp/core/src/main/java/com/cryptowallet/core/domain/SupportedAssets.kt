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
package com.cryptowallet.core.domain

import java.text.DecimalFormat
import java.text.NumberFormat
import kotlin.math.log10

/**
 * Enumera los activos que maneja la billetera. Cada activo contiene datos unicos que los definen
 * y son utilizados para formatear valores numéricos en cadenas que representen un cantidad del
 * activo.
 *
 * <pre>
 * Ejemplo:
 *
 * 1 BTC expresado en su unidad más pequeña sería 100,000,000 satoshis.
 * 1 USD expresado en su unidad más pequeña sería 100 centavos.
</pre> *
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.2
 */
sealed class SupportedAssets @JvmOverloads constructor(
    /**
     * Tamaño de la unidad en su porción más pequeña.
     */
    val unit: Long,

    /**
     * Nombre utilizado en la IU.
     */
    val name: String,

    /**
     * Bandera que indica si es un activo FIAT.
     */
    val isFiat: Boolean = false
) {
    /**
     * Signo de la divisa si es un activo fiat.
     */
    val sign: String
        get() = if (isFiat) "$" else ""

    /**
     * Obtiene la representación de una cantidad en la divisa. Ej: 100.0 -> $100.00
     *
     * @param value  Valor a formatear.
     * @param reduce Indica si se debe abreviar el valor, Ej. 1,000 -> K o 1,000,000 -> M.
     * @return Una cadena que representa la cantidad en la divisa.
     */
    @JvmOverloads
    fun toStringFriendly(value: Long, reduce: Boolean = true): String {
        val format = DecimalFormat()
        var minDigits = log10(unit.toDouble()).toInt()
        var maxDigits = log10(unit.toDouble()).toInt()
        var newValue = value.toDouble() / unit
        var unit = ""

        if (reduce) {
            if (newValue >= 10000) {
                unit = KILO_SYMBOL
                newValue /= 1000.0
            }
            if (newValue >= 1000 && unit == KILO_SYMBOL) {
                unit = MEGA_SYMBOL
                newValue /= 1000.0
            }
        }

        if (isFiat || reduce && !unit.isEmpty()) {
            minDigits = 2
            maxDigits = 2
        } else minDigits = minDigits.coerceAtMost(4)

        format.minimumFractionDigits = minDigits
        format.maximumFractionDigits = maxDigits

        return "$sign${format.format(newValue)}$unit $name"
    }

    /**
     * Obtiene la representación de la cantidad formateada con el número de digitos máximos.
     *
     * @param value        Valor a formatear.
     * @param reduce       Indica si se debe abreviar el valor. Ej. 1,000 -> K o 1,000,000 -> M.
     * @param groupingUsed Indica si se agrupan las cifras. Ej. 1000000 -> 1,000,000
     * @return Una cadena que representa la cantidad en la divisa.
     */
    @JvmOverloads
    fun toPlainText(value: Long, reduce: Boolean = true, groupingUsed: Boolean = true): String {
        val instance = NumberFormat.getInstance()
        var minDigits = log10(unit.toDouble()).toInt()
        var maxDigits = log10(unit.toDouble()).toInt()
        var newValue = value.toDouble() / unit
        var unit = ""
        if (reduce) {
            if (newValue >= 10000) {
                unit = KILO_SYMBOL
                newValue /= 1000.0
            }
            if (newValue >= 1000 && unit == KILO_SYMBOL) {
                unit = MEGA_SYMBOL
                newValue /= 1000.0
            }
        }
        if (isFiat || unit.isNotEmpty() && reduce) {
            minDigits = 2
            maxDigits = 2
        } else minDigits = minDigits.coerceAtMost(4)
        instance.isGroupingUsed = groupingUsed
        instance.minimumFractionDigits = minDigits
        instance.maximumFractionDigits = maxDigits
        return String.format("%s%s", instance.format(newValue), unit)
    }

    companion object {
        /**
         * Indica que la cantidad está expresando miles.
         */
        private const val KILO_SYMBOL = "K"

        /**
         * Indica que la cantidad está expresando millones.
         */
        private const val MEGA_SYMBOL = "M"

        /**
         * Cripto-activo Bitcoin.
         */
        data object BTC : SupportedAssets(100000000, "Bitcon")

        /**
         * Activo fiduciario Dolar estadounidense.
         */
        data object USD : SupportedAssets(100, "Dolar", true)

        /**
         * Activo fiduciario Peso mexicano.
         */
        data object MXN : SupportedAssets(100, "Pesos", true)

        val supportedFiatAssets: List<SupportedAssets>
            /**
             * Obtiene la lista de los activos fiduciarios soportados.
             *
             * @return Lista de activos.
             */
            get() = arrayListOf(
                BTC,
                USD,
                MXN
            )
    }
}