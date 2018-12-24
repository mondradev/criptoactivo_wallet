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

package com.cryptowallet.wallet.widgets;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.cryptowallet.wallet.SupportedAssets;

import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Esta clase ofrece una estructura básica para generalizar todas las transacciones generadas en la
 * billetera. De esta forma las actividades podrán visualizar de forma correcta cada una de las
 * transacciones.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public abstract class GenericTransactionBase implements Comparable<GenericTransactionBase> {

    /**
     * Escucha de cambio de la profundidad en la blockchain.
     */
    private IOnUpdateDepthListener onUpdateDepthListener;

    /**
     * Icono de la moneda o token.
     */
    @DrawableRes
    private int mImage;

    /**
     * Activo de la transacción.
     */
    private SupportedAssets mAsset;

    /**
     * Crea una nueva instancia de transacción genérica.
     *
     * @param coinIcon ID del icono/logo del activo.
     */
    public GenericTransactionBase(@DrawableRes int coinIcon, SupportedAssets asset) {
        this.mImage = coinIcon;
        this.mAsset = asset;
    }

    /**
     * Obtiene la imagen del logo/icono del activo a la cual pertenece la billetera.
     *
     * @return El logo/icono del activo.
     */
    @DrawableRes
    public final int getImage() {
        return mImage;
    }

    /**
     * Obtiene una cadena que representa la cantidad paga como comisión para realizar la transacción
     * de envío de pago.
     *
     * @return Una cadena que representa la comisión.
     */
    public abstract long getFee();

    /**
     * Obtiene la cantidad movida en la transacción.
     *
     * @return La cantidad de la transacción.
     */
    public abstract long getAmount();

    /**
     * Obtiene las direcciones de las entradas simpre y cuando la salida relaccionada esté
     * almacenada en la blockchain local, no aplica para modo SPV.
     *
     * @return Las direcciones de las entradas.
     */
    public abstract List<String> getInputsAddress();

    /**
     * Obtiene las direcciones de las salidas de la transacción.
     *
     * @return Las direcciones de salidas.
     */
    public abstract List<String> getOutputAddress();

    /**
     * Obtiene la fecha de la transacción.
     *
     * @return Fecha de la transacción.
     */
    public abstract Date getTime();

    /**
     * Obtiene el escucha del evento que surge cuando cambia el número de confirmaciones.
     *
     * @return Escucha del evento.
     */
    public IOnUpdateDepthListener getOnUpdateDepthListener() {
        return onUpdateDepthListener;
    }

    /**
     * Establece el escucha del evento que surge cuando cambia el número de confirmaciones.
     *
     * @param listener Escucha del evento.
     */
    public void setOnUpdateDepthListener(IOnUpdateDepthListener listener) {
        onUpdateDepthListener = listener;

        if (listener != null && isCommited())
            listener.onUpdate(this);
    }

    /**
     * Obtiene un valor que indica que la transacción ha sido confirmada.
     *
     * @return Un valor true si la transacción ha sido confirmada.
     */
    protected abstract boolean isCommited();

    /**
     * Obtiene el identificador de la transacción.
     *
     * @return El identificador de la transacción.
     */
    @NonNull
    public abstract String getID();

    /**
     * Compara para esta transacción con otra y devuelve un número negativo, cero o positivo si la
     * transacción es menor, igual o mayor tomando como referencia la fecha de creación.
     *
     * @param transaction Otra transacción.
     * @return Un valor que indica si la transacción es menor, igual o mayor tomando como
     * referencia la fecha de creación.
     */
    @Override
    public int compareTo(GenericTransactionBase transaction) {
        return this.getTime().compareTo(transaction.getTime());
    }

    /**
     * Obtiene el activo que maneja la transacción.
     *
     * @return Activo de la transacción.
     */
    public final SupportedAssets getAsset() {
        return mAsset;
    }

    /**
     * Obtiene la cantidad movida en la transacción sin signo.
     *
     * @return Una cadena que representa la cantidad de la transacción.
     */
    public abstract long getUsignedAmount();

    /**
     * Obtiene la profundidad en la blockchain.
     *
     * @return La cantidad de bloque por encima al cual pertenece esta transacción.
     */
    public abstract int getDepth();

    /**
     * Determina si es igual a la instancia especificada.
     *
     * @param obj Otra instancia.
     * @return Un valor true si ambas instancias son iguales.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null)
            return false;

        if (obj instanceof GenericTransactionBase) {
            GenericTransactionBase gtb = (GenericTransactionBase) obj;
            return gtb.getAsset() == getAsset() && gtb.getID().equals(getID());
        }

        return false;
    }

    /**
     * Define la estructura de un escucha del evento cuando se actualiza la profundidad del bloque
     * en el cual se contiene esta transacción.
     * <p>
     * Esto es mejor conocido como el número de confirmaciones.
     *
     * @author Ing. Javier Flores
     * @version 1.0
     */
    public interface IOnUpdateDepthListener {

        /**
         * Este método se desencadena cuando se actualiza la profundidad del bloque en la cadena.
         *
         * @param tx Transacción que cambia su profundidad.
         */
        void onUpdate(GenericTransactionBase tx);
    }
}
