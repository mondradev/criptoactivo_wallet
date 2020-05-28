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

import androidx.annotation.StringRes;

import com.cryptowallet.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Enumera los activos que maneja la billetera. Cada activo contiene datos unicos que los definen
 * y son utilizados para formatear valores numéricos en cadenas que representen un cantidad del
 * activo.
 *
 * <pre>
 *     Ejemplo:
 *
 *          1 BTC expresado en su unidad más pequeña sería 100,000,000 satoshis.
 *          1 USD expresado en su unidad más pequeña sería 100 centavos.
 * </pre>
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.1
 */
public enum SupportedAssets {
    /**
     * Cripto-activo Bitcoin.
     */
    BTC(8, R.string.bitcoin_text),

    /**
     * Activo fiduciario Dolar estadounidense.
     */
    USD(2, R.string.usd_text, true),

    /**
     * Activo fiduciario Peso mexicano.
     */
    MXN(2, R.string.mxn_text, true);

    /**
     * Tamaño de la unidad en su porción más pequeña.
     */
    private int mSize;

    /**
     * Bandera que indica si es un activo FIAT.
     */
    private boolean mFiat;

    /**
     * Nombre utilizado en la IU.
     */
    @StringRes
    private int mName;

    /**
     * Signo de la divisa fiat.
     */
    private char mSign;

    /**
     * Crea una nueva instancia del activo.
     *
     * @param size     Tamaño de la unidad expresada en su porción más pequeña.
     * @param name     Nombre utilizado en la IU.
     * @param fiatSign Signo del activo fiat.
     */
    SupportedAssets(int size, @StringRes int name, char fiatSign) {
        this(size, name, true);
        mSign = fiatSign;
    }

    /**
     * Crea una nueva instancia del activo.
     *
     * @param size   Tamaño de la unidad expresada en su porción más pequeña.
     * @param name   Nombre utilizado en la IU.
     * @param isFiat Indica si el activo es fiduciario.
     */
    SupportedAssets(int size, @StringRes int name, boolean isFiat) {
        mSize = size;
        mName = name;
        mFiat = isFiat;

        if (isFiat) mSign = '$';
    }

    /**
     * Crea una nueva instancia del activo.
     *
     * @param size Tamaño de la unidad expresada en su porción más pequeña.
     * @param name Nombre utilizado en la IU.
     */
    SupportedAssets(int size, @StringRes int name) {
        this(size, name, false);
    }

    /**
     * Obtiene el tamaño de la unidad expresada en su porción más pequeña.
     *
     * @return Tamaño de la unidad.
     */
    public int getSize() {
        return this.mSize;
    }

    /**
     * Determina si es un activo fiduciario.
     *
     * @return Un true en caso de ser fiat.
     */
    public boolean isFiat() {
        return mFiat;
    }

    /**
     * Obtiene el nombre del activo utilizado en IU.
     *
     * @return Nombre del activo.
     */
    public int getName() {
        return mName;
    }


    /**
     * Obtiene el signo de la divisa si es un activo fiat.
     *
     * @return Signo del activo.
     */
    public String getSign() {
        return isFiat() ? Character.toString(mSign) : "";
    }

    /**
     * Obtiene la representación de una cantidad en la divisa. Ej: 100.0 -> $100.00
     *
     * @param value Valor a formatear.
     * @return Una cadena que representa la cantidad en la divisa.
     */
    public String toStringFriendly(float value) {
        DecimalFormat format = new DecimalFormat();

        format.setMinimumFractionDigits(isFiat() ? mSize : Math.min(mSize, 4));
        format.setMaximumFractionDigits(mSize);

        String money = "";

        if (isFiat())
            money = String.format("%s ", mSign);

        money += String.format("%s %s", format.format(value), this.name());

        return money;
    }

    /**
     * Obtiene la representación de la cantidad formateada con el número de digitos máximos.
     *
     * @param value Valor a formatear.
     * @return Una cadena que representa la cantidad en la divisa.
     */
    public String toPlainText(float value) {
        NumberFormat instance = NumberFormat.getInstance();

        instance.setMinimumFractionDigits(isFiat() ? mSize : Math.min(mSize, 4));
        instance.setMaximumFractionDigits(mSize);

        return instance.format(value);
    }
}
