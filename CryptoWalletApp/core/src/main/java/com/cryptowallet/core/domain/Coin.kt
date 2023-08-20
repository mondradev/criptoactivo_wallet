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

import com.cryptowallet.core.domain.currencies.Currency
import com.cryptowallet.core.domain.exceptions.DifferentCurrenciesException

data class Coin(val units: Long, val currency: Currency) : Comparable<Coin> {

    operator fun plus(other: Coin): Coin {
        _ensureSameCurrency(other)

        return Coin(
            units = units + other.units,
            currency = currency
        )
    }

    operator fun minus(other: Coin): Coin {
        _ensureSameCurrency(other)

        return Coin(
            units = units - other.units,
            currency = currency
        )
    }

    override fun compareTo(other: Coin): Int {
        _ensureSameCurrency(other)

        return units.compareTo(other.units)
    }

    fun toString(
        includeSymbol: Boolean = true,
        reducedExpression: Boolean = true,
        numberGrouping: Boolean = true
    ) = currency.format(
        amount = this,
        includeSymbol = includeSymbol,
        reducedExpression = reducedExpression,
        numberGrouping = numberGrouping
    )

    private fun _ensureSameCurrency(other: Coin) {
        if (currency != other.currency)
            throw DifferentCurrenciesException(currency, other.currency)
    }

}