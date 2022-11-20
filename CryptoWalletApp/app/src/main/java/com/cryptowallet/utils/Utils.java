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

package com.cryptowallet.utils;

import static android.graphics.Bitmap.Config.ARGB_8888;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.cryptowallet.wallet.SupportedAssets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Clase con funciones de utilería para la aplicación.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.2
 */
public final class Utils {

    /**
     * Conjunto de caracteres del código QR.
     */
    private static final String UTF8 = "UTF-8";

    /**
     * Algoritmo de cifrado SHA-256.
     */
    private static final String DIGEST_SHA256 = "SHA-256";

    /**
     * No es posible crear instancias.
     */
    private Utils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Obtiene el hash SHA256 de una secuencia de bytes especificada.
     *
     * @param data La información del cual se deriva el hash.
     * @return Un hash SHA256 de la secuencia de bytes.
     */
    @NonNull
    public static byte[] sha256(byte[] data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(DIGEST_SHA256);
        } catch (NoSuchAlgorithmException cause) {
            throw new UnsupportedOperationException(cause);
        }

        digest.reset();

        return digest.digest(data);
    }

    /**
     * Obtiene el color del valor del atributo provisto por el tema del contexto.
     *
     * @param context Contexto de la aplicación Android.
     * @param attr    Atributo de tema.
     * @return Un recurso tipo color.
     */
    public static @ColorInt
    int resolveColor(Context context, @AttrRes int attr) {
        return resolveAttr(context, attr).data;
    }

    /**
     * Obtiene el estilo del valor del atributo provisto por el tema del contexto.
     *
     * @param context Contexto de la aplicación Android.
     * @param attr    Atributo de tema.
     * @return Un recurso tipo style.
     */
    public static @StyleRes
    int resolveStyle(Context context, @AttrRes int attr) {
        return resolveAttr(context, attr).resourceId;
    }

    /**
     * Obtiene el TypedValue de un atributo del tema.
     *
     * @param context Contexto de la aplicación.
     * @param attr    Atributo a resolver.
     * @return TypedValue del atributo.
     */
    private static TypedValue resolveAttr(Context context, int attr) {
        Objects.requireNonNull(context);
        TypedValue value = new TypedValue();

        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, value, true);

        return value;
    }


    /**
     * Genera un código QR.
     *
     * @return Mapa de bits del código QR.
     */
    public static Bitmap getQrCode(String data, int qrSize) {
        try {
            final Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);

            hintMap.put(EncodeHintType.CHARACTER_SET, UTF8);
            hintMap.put(EncodeHintType.MARGIN, 1);
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

            final BitMatrix matrix = new QRCodeWriter().encode(
                    data,
                    BarcodeFormat.QR_CODE,
                    qrSize,
                    qrSize,
                    hintMap
            );

            final Bitmap qrCode = Bitmap.createBitmap(qrSize, qrSize, ARGB_8888);
            final Canvas canvas = new Canvas(qrCode);
            final Paint fill = new Paint();

            fill.setColor(Color.WHITE);
            canvas.drawRect(new Rect(0, 0, qrSize, qrSize), fill);

            for (int i = 0; i < qrSize; i++)
                for (int j = 0; j < qrSize; j++)
                    if (matrix.get(i, j))
                        canvas.drawRect(i, j, i + 1f, j + 1f, new Paint());

            return qrCode;

        } catch (WriterException ignored) {
        }

        return null;
    }

    /**
     * Obtiene un nuevo <code>float</code> con el valor representado por la cadena <code>text</code>.
     *
     * @param text Cadena que representa el valor flotante a convertir.
     * @return Un valor float.
     */
    public static Float parseFloat(@NonNull String text) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException ignored) {
            return 0f;
        }
    }

    /**
     * Ejecuta una comando ignorando las excepciones lanzadas. Si existe una excepción se retorna
     * un valor false.
     *
     * @param command Comando a ejecutar.
     * @return True si se ejecutó sin excepciones.
     */
    @CanIgnoreReturnValue
    public static boolean tryNotThrow(TryNotThrowCommand command) {
        try {
            command.run();

            return true;
        } catch (Exception e) {
            if (e.getStackTrace().length > 1)
                Log.e("Utils", "Method: " + e.getStackTrace()[1], e);
            Log.e("Utils", String.format("Exception avoided: %s", e.getMessage()));
            return false;
        }
    }

    /**
     * Ejecuta una comando que devuelve un booleano ignorando las excepciones lanzadas.
     *
     * @param command Comando a ejecutar.
     */
    public static boolean tryReturnBoolean(TryReturnCommand<Boolean> command, boolean defaultValue) {
        try {
            return command.execute();
        } catch (Exception e) {
            Log.d("Utils", String.format("Exception avoided: %s", e.getMessage()));
        }

        return defaultValue;
    }

    /**
     * Obtiene una cadena que representa la hora de la instancia de tiempo.
     *
     * @param time Instancia de tiempo.
     * @return Cadena de tiempo.
     */
    public static String toLocalTimeString(Date time) {
        return new SimpleDateFormat("hh:mm aa", Locale.getDefault()).format(time);
    }

    /**
     * Obtiene una cadena que muestra una fecha/hora.
     *
     * @param datetime       Fecha/hora de la transacción
     * @param todayLabel     Texto que hace referencia al día actual.
     * @param yesterdayLabel Texto que hace referencia al día anterior al actual.
     * @return Una cadena que representa la fecha/hora.
     */
    public static String toLocalDatetimeString(Date datetime, String todayLabel,
                                               String yesterdayLabel) {
        String pattern = isToday(datetime)
                ? "'" + todayLabel + "' '@' hh:mm aa"
                : isYesterday(datetime)
                ? "'" + yesterdayLabel + "' '@' hh:mm aa"
                : "MMMM dd, yyyy '@' hh:mm aa";

        return new SimpleDateFormat(pattern, Locale.getDefault()).format(datetime);
    }

    /**
     * Obtiene un valor que indica si la fecha/hora corresponde al día de ayer.
     *
     * @param datetime Fecha/hora a evaluar.
     * @return Un valor que indica que corresponde al día de ayer.
     */
    private static boolean isYesterday(Date datetime) {
        Calendar calendar = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();

        calendar.setTime(datetime);
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        return calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR)
                && calendar.get(Calendar.MONTH) == yesterday.get(Calendar.MONTH)
                && calendar.get(Calendar.DAY_OF_MONTH) == yesterday.get(Calendar.DAY_OF_MONTH);

    }

    /**
     * Obtiene un valor que indica si la fecha/hora corresponde al día de hoy.
     *
     * @param datetime Fecha/hora a evaluar.
     * @return Un valor que indica que corresponde al día de hoy.
     */
    private static boolean isToday(Date datetime) {
        Calendar calendar = Calendar.getInstance();
        Calendar today = Calendar.getInstance();

        calendar.setTime(datetime);

        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);

    }

    /**
     * Obtiene la cadena que representa una cantidad de bytes, utilizando sus equivalentes.
     *
     * @param size Tamaño a convertir.
     * @return Una cadena que representa el tamaño.
     */
    public static String toSizeFriendlyString(long size) {
        int index = 0;
        float newSize = size;
        final int maxSize = 1024;
        final List<String> units = Arrays.asList("B", "KB", "MB", "GB", "TB");

        while (newSize > maxSize) {
            newSize = newSize / maxSize;
            index++;
        }

        return String.format("%s %s", NumberFormat.getNumberInstance().format(newSize),
                units.get(index));
    }

    /**
     * Obtiene un nuevo <code>double</code> con el valor representado por la cadena <code>text</code>.
     *
     * @param text Cadena que representa el valor de doble precisión a convertir.
     * @return Un valor double.
     */
    public static double parseDouble(@NonNull String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
            return 0f;
        }
    }

    public static <T> T coalesce(T value, T defaultValue) {
        if (value != null)
            return value;

        return defaultValue;
    }

    /**
     * Obtiene un nuevo <code>int</code> con el valor representado por la cadena <code>text</code>.
     *
     * @param text Cadena que representa el valor de entero a convertir.
     * @return Un valor entero.
     */
    public static int parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * Convierte una cantidad expresada en un cripto-activo en una cantidad expresada en un activo
     * fiduciario a partir de su equivalencia del entero.
     *
     * @param amount      Monto a convertir.
     * @param cryptoAsset Cripto-activo en el cual se expresa el monto.
     * @param price       Precio del cripto-activo.
     * @param fiatAsset   Activo en el cual se expresa el precio del cripto-activo.
     * @return Un monto equivalente al monto en cripto-activo.
     */
    public static long cryptoToFiat(long amount, SupportedAssets cryptoAsset,
                                    long price, SupportedAssets fiatAsset) {
        return (amount * price) / cryptoAsset.getUnit();
    }

    /**
     * Provee de una función que puede lanzar excepciones.
     */
    public interface TryNotThrowCommand {

        /**
         * Ejecuta la función.
         *
         * @throws Exception Si un error surge.
         */
        void run() throws Exception;

    }


    /**
     * Provee de una función con retorno que puede lanzar excepciones.
     */
    public interface TryReturnCommand<T> {

        /**
         * Ejecuta la función.
         *
         * @throws Exception Si un error surge.
         */
        T execute() throws Exception;
    }

    /**
     * Funciones de utilidad para las listas.
     *
     * @author Ing. Javier Flores (jjflores@innsytech.com)
     * @version 1.0
     */
    public static class Lists {

        /**
         * Función para realizar un recorrido de toda la colección y acumula el valor devuelto por
         * la función acumuladora.
         *
         * @param list         Colección de datos.
         * @param aggregator   Función acumuladora.
         * @param initialValue Valor inicial del acumulador.
         * @param <T>          Tipo de la colección.
         * @param <R>          Valor del acumulador.
         * @return Total acumulado.
         */
        public static <T, R> R aggregate(List<T> list, BiFunction<T, R, R> aggregator,
                                         R initialValue) {
            R aggregate = initialValue;
            for (T item : list)
                aggregate = aggregator.accept(item, aggregate);

            return aggregate;
        }

        /**
         * Permite realizar un mapeo a una lista de valores.
         *
         * @param list   Lista de valores.
         * @param mapper Función de mapeo.
         * @param <T>    Tipo de la lista.
         * @param <R>    Tipo de la nueva lista.
         * @return Nueva lista.
         */
        public static <T, R> List<R> map(List<T> list, Function<T, R> mapper) {
            List<R> newList = new ArrayList<>();

            for (T item : list)
                newList.add(mapper.accept(item));

            return newList;
        }

    }
}
