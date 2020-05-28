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
 * Excepción de saldo insuficiente, que ocurre cuando se trata de enviar un pago sin tener saldo
 * disponible en la billetera.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.1
 */
public final class InSufficientBalanceException extends RuntimeException {

    /**
     * Activo del envío.
     */
    private final SupportedAssets mAsset;

    /**
     * Saldo requerido.
     */
    private final Float mRequireAmount;

    /**
     * Crea una nueva excepción de saldo insuficiente.
     *
     * @param asset          Activo que se trató de enviar.
     * @param requireAmount  Saldo requerido.
     */
    public InSufficientBalanceException(SupportedAssets asset, Float requireAmount) {
        this(asset, requireAmount, null);
    }


    /**
     * Crea una nueva excepción de saldo insuficiente.
     *
     * @param asset          Activo que se trató de enviar.
     * @param requireAmount  Saldo requerido.
     * @param innerException Excepción que causó este error.
     */
    public InSufficientBalanceException(SupportedAssets asset, Float requireAmount,
                                        Exception innerException) {
        super("The wallet doesn't have enough balance", innerException);
        mAsset = asset;
        mRequireAmount = requireAmount;
    }

    /**
     * Obtiene el saldo requerido para realizar el pago.
     *
     * @return Saldo requerido.
     */
    public Float getRequireAmount() {
        return mRequireAmount;
    }

    /**
     * Obtiene el activo de la billetera que presenta la excepción..
     *
     * @return Activo de la billetera.
     */
    public SupportedAssets getAsset() {
        return mAsset;
    }
}
