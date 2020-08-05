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

package com.cryptowallet;

import android.content.Intent;

/**
 * Esta clase contiene las constantes de los extras y acciones usados para establecer los parametros
 * dentro de las {@link Intent}.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public abstract class Constants {

    /**
     * Clave del parametro activo.
     */
    public static final String EXTRA_MESSAGE
            = String.format("%s.extra.EXTRA_MESSAGE", BuildConfig.APPLICATION_ID);

    /**
     * Clave de extra que representa el saldo del evento {@link Constants#UPDATED_FIAT_BALANCE}
     */
    public static final String EXTRA_FIAT_BALANCE
            = String.format("%s.extra.EXTRA_FIAT_BALANCE", BuildConfig.APPLICATION_ID);

    /**
     * Clave de extra que representa el precio de un activo del evento
     * {@link Constants#UPDATED_PRICE}.
     */
    public static final String EXTRA_FIAT_PRICE
            = String.format("%s.extra.EXTRA_FIAT_PRICE", BuildConfig.APPLICATION_ID);

    /**
     * Clave del parametro cripto-activo.
     */
    public static final String EXTRA_CRYPTO_ASSET
            = String.format("%s.extra.EXTRA_CRYPTO_ASSET", BuildConfig.APPLICATION_ID);

    /**
     * Clave del parametro activo fiduciario.
     */
    public static final String EXTRA_FIAT_ASSET
            = String.format("%s.extra.EXTRA_FIAT_ASSET", BuildConfig.APPLICATION_ID);

    /**
     * Clave del parametro identificador de transacción.
     */
    public static final String EXTRA_TXID
            = String.format("%s.extra.EXTRA_TXID", BuildConfig.APPLICATION_ID);

    /**
     * Clave del parametro monto de la transacción.
     */
    public static final String EXTRA_AMOUNT
            = String.format("%s.extra.EXTRA_AMOUNT", BuildConfig.APPLICATION_ID);

    /**
     * Clave del parametro monto en fiat de la transacción.
     */
    public static final String EXTRA_FIAT_AMOUNT
            = String.format("%s.extra.EXTRA_FIAT_AMOUNT", BuildConfig.APPLICATION_ID);

    /**
     * Clave del parametro comisión de la transacción.
     */
    public static final String EXTRA_FEE
            = String.format("%s.extra.EXTRA_FEE", BuildConfig.APPLICATION_ID);

    /**
     * Clave del parametro destino de la transacción.
     */
    public static final String EXTRA_TO_ADDRESSES
            = String.format("%s.extra.EXTRA_TO_ADDRESSES", BuildConfig.APPLICATION_ID);

    /**
     * Clave de extra que representa la altura de un bloque.
     */
    public static final String EXTRA_HEIGHT
            = String.format("%s.extra.EXTRA_HEIGHT", BuildConfig.APPLICATION_ID);

    /**
     * Clave de extra que representa el hash de un bloque.
     */
    public static final String EXTRA_HASH
            = String.format("%s.extra.EXTRA_HASH", BuildConfig.APPLICATION_ID);

    /**
     * Clave de extra que representa el tiempo de un bloque.
     */
    public static final String EXTRA_TIME
            = String.format("%s.extra.EXTRA_TIME", BuildConfig.APPLICATION_ID);

    /**
     * Clave de extra que representa el tipo de red de un activo.
     */
    public static final String EXTRA_NETWORK
            = String.format("%s.extra.EXTRA_NETWORK", BuildConfig.APPLICATION_ID);

    /**
     * Clave de extra que representa un vector con los hashes de las transacciones del bloque.
     */
    public static final String EXTRA_TXS
            = String.format("%s.extra.EXTRA_TXS", BuildConfig.APPLICATION_ID);

    /**
     * Clave de extra que representa una dirección de criptoactivo.
     */
    public static final String EXTRA_ADDRESS
            = String.format("%s.extra.EXTRA_ADDRESS", BuildConfig.APPLICATION_ID);

    /**
     * Clave de extra que representa una URI con datos para solicitar pagos.
     */
    public static final String EXTRA_RECEIVER_URI
            = String.format("%s.extra.EXTRA_RECEIVER_URI", BuildConfig.APPLICATION_ID);

    /**
     * Clave de extra que representa el identificador del recurso de icono.
     */
    public static final String EXTRA_ICON_RES_ID
            = String.format("%s.extra.EXTRA_ICON_RES_ID", BuildConfig.APPLICATION_ID);

    /**
     * Acción del evento saldo actualizado.
     */
    public static final String UPDATED_FIAT_BALANCE
            = String.format("%s.action.UPDATED_FIAT_BALANCE", BuildConfig.APPLICATION_ID);

    /**
     * Acción del evento nueva transacción.
     */
    public static final String NEW_TRANSACTION
            = String.format("%s.action.NEW_TRANSACTION", BuildConfig.APPLICATION_ID);

    /**
     * Acción del evento nuevo bloque.
     */
    public static final String NEW_BLOCK
            = String.format("%s.action.NEW_BLOCK", BuildConfig.APPLICATION_ID);

    /**
     * Acción del evento precio actualizado.
     */
    public static final String UPDATED_PRICE
            = String.format("%s.action.UPDATED_PRICE", BuildConfig.APPLICATION_ID);

    /**
     * Esta clase es estática, no está soportada las instanciacion.
     *
     * @throws UnsupportedOperationException Al intentar crear una instancia.
     */
    public Constants() {
        throw new UnsupportedOperationException("This is a static class, cannot be instantiated.");
    }
}
