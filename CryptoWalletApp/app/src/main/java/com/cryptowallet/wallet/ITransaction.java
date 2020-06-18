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

package com.cryptowallet.wallet;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.List;

/**
 * Define la estructura básica de una transacción de algún cripto-activo para ser visualizada en la
 * aplicación.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 */
public interface ITransaction extends Comparable<ITransaction> {

    /**
     * Obtiene el cripto-activo que maneja esta transacción.
     *
     * @return Cripto-activo de la transacción.
     */
    SupportedAssets getCriptoAsset();

    /**
     * Obtiene la comisión gastada en la transacción.
     *
     * @return Comisión de la transacción.
     */
    double getFee();

    /**
     * Obtiene la cantidad gastada en la transacción sin incluir la comisión.
     *
     * @return Cantidad de la transacción.
     */
    double getAmount();

    /**
     * Obtiene la lista de direcciones que envian alguna cantidad en la transacción.
     *
     * @return Lista de direcciones remitentes.
     */
    @NonNull
    List<String> getFromAddress();

    /**
     * Obtiene la lista de direcciones que reciben alguna cantidad en la transacción.
     *
     * @return Lista de direcciones destinatarios.
     */
    @NonNull
    List<String> getToAddress();

    /**
     * Obtiene la fecha y hora de la transacción. Esta corresponde a la fecha en la cual el bloque
     * fue generado.
     *
     * @return Fecha y hora de la transacción.
     */
    @NonNull
    Date getTime();

    /**
     * Obtiene el identificador único de la transacción.
     *
     * @return Identificador de la transacción.
     */
    @NonNull
    String getID();

    /**
     * Indica si la transacción ya corresponde a un bloque y este no será cambiado.
     *
     * @return Un valor true si la transacción fue confirmada.
     */
    boolean isConfirm();

    /**
     * Obtiene el identificador único del bloque padre de esta transacción.
     *
     * @return Un hash del bloque padre.
     */
    String getBlockHash();

    /**
     * Obtiene la altura del bloque padre de esta transacción.
     *
     * @return La altura del bloque padre.
     */
    long getBlockHeight();

    /**
     * Obtiene el tamaño de la transacción en bytes.
     *
     * @return Tamaño en bytes.
     */
    long getSize();

    /**
     * Obtiene la billetera que contiene esta transacción.
     *
     * @return Billetera contenedora.
     */
    IWallet getWallet();

    /**
     * Obtiene la cantidad en su valor fiat.
     *
     * @return Valor fiat.
     */
    double getFiatAmount();

    /**
     * Indica si la transacción es un pago.
     *
     * @return True si es un pago.
     */
    boolean isPay();

    /**
     * Indica si es una transacción con nuevas monedas.
     *
     * @return Un true si la transacción es de nuevas monedas.
     */
    boolean isCoinbase();

    /**
     * Obtiene las confirmaciones de la transacción.
     *
     * @return El número de confirmaciones.
     */
    long getConfirmations();
}
