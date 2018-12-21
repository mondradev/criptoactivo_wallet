package com.cryptowallet.utils;

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