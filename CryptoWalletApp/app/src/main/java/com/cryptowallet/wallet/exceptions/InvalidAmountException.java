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

package com.cryptowallet.wallet.exceptions;


import com.cryptowallet.wallet.SupportedAssets;

/**
 * Excepción de cantidad invalida, que ocurre cuando una cantidad supera el mínimo o máximo permitido.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class InvalidAmountException extends CryptoAssetException {

    /**
     * Monto que provocó la excepción.
     */
    private final long mAmount;

    /**
     * Crea una instancia la excepción.
     *
     * @param cryptoAsset Activo que lanza la excepción.
     * @param amount      Monto que provocó la excepción.
     */
    public InvalidAmountException(SupportedAssets cryptoAsset, long amount) {
        this("The specified amount is invalid because it exceeds the minimum or maximum allowed "
                + "by the network.", cryptoAsset, amount);
    }


    /**
     * Crea una instancia la excepción.
     *
     * @param message     Mensaje del detalle de la excepción.
     * @param cryptoAsset Activo que lanza la excepción.
     * @param amount      Monto que provocó la excepción.
     */
    public InvalidAmountException(String message, SupportedAssets cryptoAsset, long amount) {
        super(message, cryptoAsset);
        mAmount = amount;
    }

    /**
     * Obtiene el monto que provocó la excepción.
     *
     * @return Monto del activo.
     */
    public long getAmount() {
        return mAmount;
    }
}
