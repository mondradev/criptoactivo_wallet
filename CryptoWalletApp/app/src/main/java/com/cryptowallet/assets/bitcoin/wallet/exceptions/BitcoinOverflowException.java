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

package com.cryptowallet.assets.bitcoin.wallet.exceptions;


import android.content.res.Resources;

import com.cryptowallet.R;
import com.cryptowallet.core.domain.SupportedAssets;
import com.cryptowallet.wallet.exceptions.InvalidAmountException;

import org.jetbrains.annotations.NotNull;


/**
 * Excepción de cantidad desborde, que ocurre cuando una cantidad supera el máximo permitido en una
 * una transacción de Bitcoin.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class BitcoinOverflowException extends InvalidAmountException {

    /**
     * Crea una instancia de la excepción.
     */
    public BitcoinOverflowException(long amount) {
        super(SupportedAssets.BTC, amount);
    }

    /**
     * Obtiene el identificador del mensaje de error de la excepción.
     *
     * @return Identificador del recurso.
     */
    @Override
    public String getMessageRes(@NotNull Resources resources) {
        return resources.getString(R.string.max_value_error,
                getCryptoAsset().toStringFriendly(getAmount()));
    }
}
