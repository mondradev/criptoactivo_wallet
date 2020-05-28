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

package com.cryptowallet.utils.inputfilters;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Esta clase provee de una restrinción para evitar que se ingresen caracteres no numéricos.
 * Se puede especificar la cantidad de digitos y cuantos son decimales del total.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public class DecimalsFilter implements InputFilter {

    /**
     * Expresión regular de la clase.
     */
    private Pattern mPattern;

    /**
     * Crea una instancia nueva.
     *
     * @param digits   Número total de digitos.
     * @param decimals Número total de decimales.
     */
    public DecimalsFilter(int digits, int decimals) {
        mPattern = Pattern.compile("^(0|[1-9][0-9]{0," + (digits - decimals - 1)
                + "})?(\\.[0-9]{0," + decimals + "})?");
    }

    /**
     * Este método es llamado cuando el buffer del editor de texto va a reemplazar el contenido del
     * EditText
     *
     * @param source Secuencia de caracteres que provienen del buffer.
     * @param start  Posición inicial de la secuencia.
     * @param end    Posición final de la secuencia.
     * @param dest   Objeto destino que almacena el texto del EditText
     * @param dstart Posición inicial del rango a reemplazar.
     * @param dend   Posición final del rango a reemplazar.
     * @return Secuencia final que será utilizada para reemplazar.
     */
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                               int dstart, int dend) {

        StringBuilder destText = new StringBuilder(dest.toString());
        String sourceText = source.toString();

        String notMatch = destText.toString().substring(dstart, dend);
        String newSourceText = sourceText.isEmpty() ? sourceText
                : sourceText.substring(start, end == start ? end + 1 : end);

        if (dstart == dend)
            destText.insert(dstart, newSourceText);
        else
            destText.replace(dstart, dend, sourceText.isEmpty() ? sourceText : newSourceText);

        Matcher matcher = mPattern.matcher(destText);

        if (!matcher.matches())
            if (sourceText.isEmpty())
                return notMatch;
            else
                return "";

        return source;
    }


}