/*
 * Copyright 2018 InnSy Tech
 * Copyright 2018 Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.wallet;

/**
 * Excepción de saldo insuficiente, que ocurre cuando se trata de enviar un pago sin tener saldo
 * disponible en la billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class InSufficientBalanceException extends Exception {

    /**
     * Saldo durante la excepción.
     */
    private final long mBalance;

    /**
     * Activo del envío.
     */
    private final SupportedAssets mAsset;

    /**
     * Saldo requerido.
     */
    private final long mRequireAmount;

    /**
     * Crea una nueva excepción de saldo insuficiente.
     *
     * @param balance       Saldo actual.
     * @param asset         Activo que se trató de enviar.
     * @param requireAmount Saldo requerido.
     */
    public InSufficientBalanceException(long balance, SupportedAssets asset, long requireAmount) {
        this("No hay saldo disponible para ser enviado.", balance, asset, requireAmount);

    }


    /**
     * Crea una nueva excepción de saldo insuficiente.
     *
     * @param balance        Saldo actual.
     * @param asset          Activo que se trató de enviar.
     * @param requireAmount  Saldo requerido.
     * @param innerException Excepción que causó este error.
     */
    public InSufficientBalanceException(long balance, SupportedAssets asset, long requireAmount,
                                        Exception innerException) {
        super("No hay saldo disponible para ser enviado.", innerException);
        mAsset = asset;
        mBalance = balance;
        mRequireAmount = requireAmount;
    }

    /**
     * Crea una nueva instancia especificando la causa de la excepción.
     *
     * @param message       Mensaje que especifica la causa de la excepción.
     * @param balance       Saldo actual.
     * @param asset         Activo que se trató de enviar.
     * @param requireAmount Saldo requerido.
     */
    public InSufficientBalanceException(String message, long balance, SupportedAssets asset,
                                        long requireAmount) {
        super(message);

        mBalance = balance;
        mAsset = asset;
        mRequireAmount = requireAmount;
    }

    /**
     * Obtiene el saldo requerido para realizar el pago.
     *
     * @return Saldo requerido.
     */
    public long getRequireAmount() {
        return mRequireAmount;
    }

    /**
     * Obtiene el saldo de la billetera cuando ocurrió el error.
     *
     * @return Saldo de la billetera.
     */
    public long getBalance() {
        return mBalance;
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
