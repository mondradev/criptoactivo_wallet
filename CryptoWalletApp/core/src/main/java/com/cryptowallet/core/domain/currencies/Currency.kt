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

package com.cryptowallet.core.domain.currencies

import com.cryptowallet.core.domain.Coin
import java.text.DecimalFormat

abstract class Currency(
    val code: String,
    val maxSubdivisions: Long,
    formatPattern: String
) {

    private val _formatter = DecimalFormat(formatPattern)

    private val reducedExpressionSymbols = arrayOf(
        "" to 1,
        "K" to 1000,
        "M" to 1000000
    )

    private fun _getReducedExpression(value: Double): String {
        val (symbol, _) = reducedExpressionSymbols.last { (_, minValue) ->
            value >= minValue
        }

        return "${_formatter.format(value)}$symbol"
    }

    protected open fun formatWithCurrencySymbol(formattedValue: String): String =
        "$formattedValue $code"

    fun format(
        amount: Coin,
        includeSymbol: Boolean,
        reducedExpression: Boolean,
        numberGrouping: Boolean
    ): String {
        val value = amount.units.toDouble() / maxSubdivisions

        _formatter.isGroupingUsed = numberGrouping

        val formattedValue = if (reducedExpression)
            _getReducedExpression(value)
        else
            _formatter.format(value)

        return if (includeSymbol)
            formatWithCurrencySymbol(formattedValue)
        else
            formattedValue
    }

    fun valueOf(units: Long) = Coin(
        units = units,
        currency = this
    )
}