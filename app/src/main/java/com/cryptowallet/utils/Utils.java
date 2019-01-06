/*
 * Copyright 2019 InnSy Tech
 * Copyright 2019 Ing. Javier de Jesús Flores Mondragón
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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewCompat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cryptowallet.R;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.bitcoinj.crypto.KeyCrypterScrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/***
 * Provee de funciones auxiliares que pueden usarse en diferentes clases.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public final class Utils {

    /**
     * Identificador de la aplicación.
     */
    private static final String CHANNEL_ID = "CryptoWallet";
    /**
     * Algoritmo de cifrado.
     */
    private static final String DIGEST_SHA256 = "SHA-256";
    /**
     * Identificador de la notificación actual.
     */
    private static int notifyID = 0;

    /**
     * Obtiene una cadena que muestra una fecha/hora.
     *
     * @param txTime          Fecha/hora de la transacción
     * @param todayString     Texto que hace referencia al día actual.
     * @param yesterdayString Texto que hace referencia al día anterior al actual.
     * @return Una cadena que representa la fecha/hora.
     */
    public static String getDateTime(Date txTime, String todayString, String yesterdayString) {

        String pattern = isToday(txTime)
                ? "'" + todayString + "' '@' hh:mm aa"
                : isYesterday(txTime)
                ? "'" + yesterdayString + "' '@' hh:mm aa"
                : "MMMM dd, yyyy '@' hh:mm aa";

        return new SimpleDateFormat(pattern, Locale.getDefault()).format(txTime);
    }

    /**
     * Obtiene un valor que indica si la fecha/hora corresponde al día de ayer.
     *
     * @param time Fecha/hora a evaluar.
     * @return Un valor que indica que corresponde al día de ayer.
     */
    private static boolean isYesterday(Date time) {
        Calendar calendar = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();

        calendar.setTime(time);

        return calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR)
                && calendar.get(Calendar.MONTH) == yesterday.get(Calendar.MONTH)
                && calendar.get(Calendar.DAY_OF_MONTH) == (yesterday.get(Calendar.DAY_OF_MONTH) - 1);
    }

    /**
     * Obtiene un valor que indica si la fecha/hora corresponde al día de hoy.
     *
     * @param time Fecha/hora a evaluar.
     * @return Un valor que indica que corresponde al día de hoy.
     */
    private static boolean isToday(Date time) {
        Calendar calendar = Calendar.getInstance();
        Calendar today = Calendar.getInstance();

        calendar.setTime(time);

        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Válida si el valor es nulo y en caso de serlo devuelve el segundo parametro.
     *
     * @param value  Valor a válidar.
     * @param ifNull Valor en caso de ser nulo.
     * @param <T>    Tipo de dato de la validación.
     * @return El valor del segundo parametro en caso que el primero sea nulo.
     */
    public static <T> T coalesce(T value, T ifNull) {
        Objects.requireNonNull(ifNull, "ifNull no puede ser un valor null");

        if (value == null)
            return ifNull;

        return value;
    }

    /**
     * Muestra una notificación en la actividad contenedora del componente de referencia.
     *
     * @param reference Componente de referencia.
     * @param message   Mensaje a mostrar.
     */
    public static void showSnackbar(View reference, String message) {
        showSnackbar(reference, message, null, null);
    }

    /**
     * Muestra una notificación en la actividad contenedora del componente de referencia.
     *
     * @param reference Componente de referencia.
     * @param message   Mensaje a mostrar.
     * @param caption   Etiqueta del botón.
     * @param listener  Escucha del evento clic para el Snackbar.
     */
    public static void showSnackbar(View reference, String message, String caption,
                                    View.OnClickListener listener) {

        Snackbar snackbar = Snackbar.make(reference, message, Snackbar.LENGTH_LONG);

        View snackView = snackbar.getView();
        ViewGroup.MarginLayoutParams layoutParams
                = (ViewGroup.MarginLayoutParams) snackView.getLayoutParams();
        layoutParams.setMargins(16, 16, 16, 16);

        snackView.setBackground(reference.getContext().getDrawable(R.drawable.bg_snackbar));
        ((TextView) snackView.findViewById(android.support.design.R.id.snackbar_text))
                .setTextColor(getColorFromTheme(reference.getContext(), R.attr.textIconsColor));

        ViewCompat.setElevation(snackView, 6);

        if (listener != null && caption != null && !caption.isEmpty())
            snackbar.setAction(caption, listener);

        snackbar.show();
    }

    /**
     * Envía una notificación al sistema operativo.
     *
     * @param context Contexto de la aplicación.
     * @param title   Título de la aplicación.
     * @param message Mensaje de la aplicación.
     */
    public static void sendNotificationOs(Context context,
                                          String title,
                                          String message,
                                          Intent onTap, int staticNotify) {
        if (message == null || message.isEmpty())
            return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cryptowallet)
                .setContentTitle(title)
                .setContentText(message)
                .setVibrate(new long[]{500, 500, 500, 500, 500, 500, 500, 500, 500})
                .setColor(context.getResources().getColor(R.color.light_primary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager
                        .getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION));

        if (onTap != null) {
            onTap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent
                    = PendingIntent.getActivity(context, 0, onTap, 0);

            builder.setContentIntent(pendingIntent);
        }

        NotificationManager notificationCompat
                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationCompat.createNotificationChannel(
                    new NotificationChannel(
                            CHANNEL_ID,
                            context.getString(R.string.app_name),
                            NotificationManager.IMPORTANCE_HIGH
                    )
            );

        notificationCompat.notify(staticNotify == 0 ? notifyID : staticNotify, builder.build());

        if (notifyID > 9999) notifyID = 0;
        else notifyID++;
    }


    /**
     * Envía una notificación al sistema operativo.
     *
     * @param context   Contexto de la aplicación.
     * @param assetIcon Icono del activo involucrádo.
     */
    public static void sendReceiveMoneyNotification(Context context,
                                                    @DrawableRes int assetIcon,
                                                    String amount,
                                                    String txID,
                                                    Intent onTap) {
        String message = String.format(context.getString(R.string.notify_receive), amount);
        String template = "%s\nTxID: %s";

        Uri soundUri = Uri.parse(String.format(Locale.getDefault(), "%s://%s/%d",
                ContentResolver.SCHEME_ANDROID_RESOURCE,
                context.getPackageName(),
                R.raw.receive_money));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(assetIcon)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(message)
                .setVibrate(new long[]{500, 500, 500, 500, 500, 500, 500, 500, 500})
                .setColor(context.getResources().getColor(R.color.light_primary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(String.format(template,
                        message, txID
                )))
                .setSound(soundUri)
                .setAutoCancel(true);


        if (onTap != null) {
            onTap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent
                    = PendingIntent.getActivity(context, 0, onTap,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(pendingIntent);
        }

        NotificationManager notificationCompat
                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setSound(soundUri, new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build());

            notificationCompat.createNotificationChannel(channel);
        }

        notificationCompat.notify(notifyID, builder.build());

        if (notifyID > 9999) notifyID = 0;
        else notifyID++;
    }

    /**
     * Envía una notificación al sistema operativo.
     *
     * @param context   Contexto de la aplicación.
     * @param assetIcon Icono del activo involucrádo.
     * @param title     Título de la aplicación.
     * @param message   Mensaje de la aplicación.
     * @param largeText Resto del mensaje de la notificación.
     */
    public static void sendLargeTextNotificationOs(Context context,
                                                   @DrawableRes int assetIcon,
                                                   String title,
                                                   String message,
                                                   String largeText,
                                                   Intent onTap) {
        if (message == null || message.isEmpty())
            return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(assetIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setVibrate(new long[]{500, 500, 500, 500, 500, 500, 500, 500, 500})
                .setColor(context.getResources().getColor(R.color.light_primary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(largeText))
                .setSound(RingtoneManager
                        .getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION));


        if (onTap != null) {
            onTap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent
                    = PendingIntent.getActivity(context, 0, onTap,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(pendingIntent);
        }

        NotificationManager notificationCompat
                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationCompat.createNotificationChannel(
                    new NotificationChannel(
                            CHANNEL_ID,
                            context.getString(R.string.app_name),
                            NotificationManager.IMPORTANCE_HIGH
                    )
            );

        notificationCompat.notify(notifyID, builder.build());

        if (notifyID > 9999) notifyID = 0;
        else notifyID++;
    }

    /**
     * Genera un código QR a partir de la URI especificada.
     *
     * @param uri  Información del código QR.
     * @param size Tamaño del código QR.
     * @return Mapa de bits del código QR.
     */
    public static Bitmap generateQrCode(Uri uri, int size) {
        try {
            Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);

            hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hintMap.put(EncodeHintType.MARGIN, 1);
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix byteMatrix = qrCodeWriter.encode(uri.toString(), BarcodeFormat.QR_CODE, size,
                    size, hintMap);
            int matrixWidth = byteMatrix.getWidth();
            int matrixHeight = byteMatrix.getHeight();

            Bitmap codeImage =
                    Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888);
            Canvas imageRaw = new Canvas(codeImage);
            Paint whitePaint = new Paint();

            whitePaint.setColor(Color.parseColor("#FFFFFF"));
            imageRaw.drawRect(new Rect(0, 0, matrixWidth, matrixHeight),
                    whitePaint);

            Paint blackPaint = new Paint(Color.parseColor("#000000"));

            for (int i = 0; i < matrixWidth; i++) {
                for (int j = 0; j < matrixWidth; j++) {
                    if (byteMatrix.get(i, j)) {
                        imageRaw.drawRect(i, j, i + 1f, j + 1f, blackPaint);
                    }
                }
            }

            return codeImage;

        } catch (WriterException ignored) {
        }

        return null;
    }

    /**
     * Determina un valor que indica si un número se encuentra dentro del rango especificado.
     *
     * @param digit Valor a comparar.
     * @param min   Limite inferior.
     * @param max   Limite superior.
     * @return Un valor true que indica que el número está especificado entre los limites.
     */
    public static boolean between(int digit, int min, int max) {
        return min <= digit && digit <= max;
    }

    /**
     * Concatena todas las cadenas contenida dentro de la matriz unidimensional especificada.
     *
     * @param strings Matriz unidimensional que contiene las cadenas a concatenar.
     * @return Un cadena formada a partir de las cadenas de la matriz.
     */
    public static String concatAll(String[] strings) {
        StringBuilder builder = new StringBuilder();
        for (String aPin : strings) builder.append(aPin);

        return builder.toString();
    }

    /**
     * Obtiene el color a partir de un atributo que es definido por el tema actual de la aplicación.
     *
     * @param context Contexto de la aplicación Android.
     * @param attr    Atributo compatible con valores de color.
     * @return Un color especificado por el tema.
     */
    public static @ColorInt
    int getColorFromTheme(Context context, @AttrRes int attr) {
        Objects.requireNonNull(context);
        TypedValue value = new TypedValue();

        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, value, true);

        return value.data;
    }

    /**
     * Obtiene el número de iteraciones que se requieren para realizar la encriptación de una clave.
     *
     * @param password Clave a analizar.
     * @return El número de iteraciones.
     */
    public static int calculateIterations(String password) {
        final int targetTimeMsec = 2000;

        int iterations = 16384;
        KeyCrypterScrypt scrypt = new KeyCrypterScrypt(iterations);
        long now = System.currentTimeMillis();
        scrypt.deriveKey(password);
        long time = System.currentTimeMillis() - now;

        while (time > targetTimeMsec) {
            iterations >>= 1;
            time /= 2;
        }

        return iterations;
    }

    /**
     * Lanza una excepción si la cadena especificada tiene un valor null o está vacía.
     *
     * @param text         Texto a evaluar.
     * @param errorMessage Mensaje de error de la excepción.
     */
    public static void throwIfNullOrEmpty(String text, String errorMessage) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(text), errorMessage);
    }

    /**
     * Obtiene un valor nulo si el primer argumento es igual al segundo, si no se devuelve el valor
     * del primer argumento.
     *
     * @param left  El valor a comparar.
     * @param right El valor que se requiere para devolver null.
     * @param <T>   Tipo de dato a maneja.
     * @return Un valor null si ambos parametros son iguales.
     */
    public static <T> T nullIf(T left, T right) {
        if (isNull(left))
            return null;

        if (isNull(right))
            return left;

        if (left.equals(right))
            return null;
        return left;
    }

    /**
     * Obtiene un valor que indica si el argumento es un valor null.
     *
     * @param value Valor a comparar.
     * @return Un valor true si el argumento es null.
     */
    public static boolean isNull(Object value) {
        return value == null;
    }

    /**
     * Obtiene el SHA256 de una secuencia de bytes especificada.
     *
     * @param data La información del cual se deriva el hash.
     * @return Un hash SHA256 de la secuencia de bytes.
     */
    public static byte[] toSha256(byte[] data) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(DIGEST_SHA256);
        } catch (NoSuchAlgorithmException ignored) {
        }

        if (digest == null)
            return null;

        digest.reset();

        return digest.digest(data);
    }

}
