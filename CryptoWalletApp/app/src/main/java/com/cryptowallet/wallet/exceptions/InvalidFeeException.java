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

package com.cryptowallet.wallet.exceptions;

import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.cryptowallet.R;
import com.cryptowallet.core.domain.SupportedAssets;


/**
 * Excepción de comisión invalida, que ocurre cuando una cantidad utilizada para la comisión no es
 * válida.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class InvalidFeeException extends InvalidAmountException {

    /**
     * Crea una instancia la excepción.
     *
     * @param cryptoAsset Activo que lanza la excepción.
     * @param amount      Monto que provocó la excepción.
     */
    public InvalidFeeException(SupportedAssets cryptoAsset, long amount) {
        super(cryptoAsset, amount);
    }

    /**
     * Crea una instancia la excepción.
     *
     * @param message     Mensaje del detalle de la excepción.
     * @param cryptoAsset Activo que lanza la excepción.
     * @param amount      Monto que provocó la excepción.
     */
    public InvalidFeeException(String message, SupportedAssets cryptoAsset, long amount) {
        super(message, cryptoAsset, amount);
    }

    /**
     * Obtiene el identificador del mensaje de error de la excepción.
     *
     * @param resources Recursos de la aplicación.
     * @return Identificador del recurso.
     */
    @Override
    public String getMessageRes(@NonNull Resources resources) {
        return resources.getString(R.string.fee_error);
    }
}
