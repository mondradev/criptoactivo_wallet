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
import com.google.common.base.Strings;

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
 * @version 2.2
 */
public enum SupportedAssets {
    /**
     * Cripto-activo Bitcoin.
     */
    BTC(100000000, R.string.bitcoin_text),

    /**
     * Activo fiduciario Dolar estadounidense.
     */
    USD(100, R.string.usd_text, true),

    /**
     * Activo fiduciario Peso mexicano.
     */
    MXN(100, R.string.mxn_text, true);

    /**
     * Tamaño de la unidad en su porción más pequeña.
     */
    private long mUnit;

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
     * @param unit     La unidad expresada en su porción más pequeña. Ej. 100000000 satoshis = 1 BTC.
     * @param name     Nombre utilizado en la IU.
     * @param fiatSign Signo del activo fiat.
     */
    SupportedAssets(long unit, @StringRes int name, char fiatSign) {
        this(unit, name, true);
        mSign = fiatSign;
    }

    /**
     * Crea una nueva instancia del activo.
     *
     * @param unit   La unidad expresada en su porción más pequeña. Ej. 100000000 satoshis = 1 BTC.
     * @param name   Nombre utilizado en la IU.
     * @param isFiat Indica si el activo es fiduciario.
     */
    SupportedAssets(long unit, @StringRes int name, boolean isFiat) {
        mUnit = unit;
        mName = name;
        mFiat = isFiat;

        if (isFiat) mSign = '$';
    }

    /**
     * Crea una nueva instancia del activo.
     *
     * @param unit La unidad expresada en su porción más pequeña. Ej. 100000000 satoshis = 1 BTC.
     * @param name Nombre utilizado en la IU.
     */
    SupportedAssets(long unit, @StringRes int name) {
        this(unit, name, false);
    }

    /**
     * Obtiene el tamaño de la unidad expresada en su porción más pequeña.
     *
     * @return Tamaño de la unidad.
     */
    public long getUnit() {
        return this.mUnit;
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
    public String toStringFriendly(double value) {
        return  toStringFriendly(value, true);
    }

    /**
     * Obtiene la representación de una cantidad en la divisa. Ej: 100.0 -> $100.00
     *
     * @param value Valor a formatear.
     * @param reduce  Indica si se debe abreviar el valor, Ej. 1,000 -> K o 1,000,000 -> M.
     * @return Una cadena que representa la cantidad en la divisa.
     */
    public String toStringFriendly(double value, boolean reduce) {
        final DecimalFormat format = new DecimalFormat();

        int minDigits = (int) Math.log10(mUnit);
        int maxDigits = (int) Math.log10(mUnit);
        double newValue = value;
        String unit = "";

        if (reduce) {

            if (newValue >= 10000) {
                unit = "K";
                newValue /= 1000;
            }

            if (newValue >= 10000) {
                unit = "M";
                newValue /= 1000;
            }
        }

        if (isFiat() || (reduce && !Strings.isNullOrEmpty(unit))) {
            minDigits = 2;
            maxDigits = 2;
        } else
            minDigits = Math.min(minDigits, 4);

        format.setMinimumFractionDigits(minDigits);
        format.setMaximumFractionDigits(maxDigits);

        String money = "";

        if (isFiat())
            money = String.format("%s ", mSign);

        money += String.format("%s%s %s", format.format(newValue), unit, this.name());

        return money;
    }

    /**
     * Obtiene la representación de la cantidad formateada con el número de digitos máximos.
     *
     * @param value Valor a formatear.
     * @return Una cadena que representa la cantidad en la divisa.
     */
    public String toPlainText(double value) {
        return  toPlainText(value, true);
    }

    /**
     * Obtiene la representación de la cantidad formateada con el número de digitos máximos.
     *
     * @param value Valor a formatear.
     * @param reduce  Indica si se debe abreviar el valor, Ej. 1,000 -> K o 1,000,000 -> M.
     * @return Una cadena que representa la cantidad en la divisa.
     */
    public String toPlainText(double value, boolean reduce) {
        final NumberFormat instance = NumberFormat.getInstance();

        int minDigits = (int) Math.log10(mUnit);
        int maxDigits = (int) Math.log10(mUnit);
        double newValue = value;
        String unit = "";

        if (reduce) {
            if (newValue >= 10000) {
                unit = "K";
                newValue /= 1000;
            }

            if (newValue >= 10000) {
                unit = "M";
                newValue /= 1000;
            }
        }

        if (isFiat() || (!Strings.isNullOrEmpty(unit) && reduce)) {
            minDigits = 2;
            maxDigits = 2;
        } else
            minDigits = Math.min(minDigits, 4);

        instance.setMinimumFractionDigits(minDigits);
        instance.setMaximumFractionDigits(maxDigits);

        return String.format("%s%s", instance.format(newValue), unit);
    }
}
