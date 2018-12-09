package com.cryptowallet.utils;

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
import android.media.RingtoneManager;
import android.net.Uri;
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
import com.cryptowallet.bitcoin.BitcoinService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/***
 * Provee de funciones auxiliares que pueden usarse en diferentes clases.
 */
public final class Helper {

    public static final org.bitcoinj.core.Context BITCOIN_CONTEXT
            = new org.bitcoinj.core.Context(BitcoinService.getNetwork());

    /**
     * Identificador de la aplicación.
     */
    private static final String CHANNEL_ID = "CryptoWallet";
    /**
     *
     */
    private static int notifyID = 0;


    public static boolean isNullOrEmpty(String text) {
        if (text == null)
            return true;
        return text.isEmpty();
    }

    /**
     * Obtiene una cadena que muestra una fecha/hora.
     *
     * @param context Aplicación de ejecución.
     * @param txTime  Fecha/hora de la transacción
     * @return Una cadena que representa la fecha/hora.
     */
    public static String getDateTime(Context context, Date txTime) {
        String pattern = isToday(txTime)
                ? "'" + context.getString(R.string.today_text) + "' '@' hh:mm aa"
                : isYesterday(txTime)
                ? "'" + context.getString(R.string.yesterday_text) + "' '@' hh:mm aa"
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
                .setSound(soundUri);


        if (onTap != null) {
            onTap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent
                    = PendingIntent.getActivity(context, 0, onTap,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(pendingIntent);
        }

        NotificationManager notificationCompat
                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationCompat.notify(notifyID, builder.build());

        notifyID++;
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
        notificationCompat.notify(notifyID, builder.build());

        notifyID++;
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

    public static CharSequence getString(String password) {
        return null;
    }

    public static boolean between(int digit, int min, int max) {
        return min <= digit && digit <= max;
    }

    public static String concatAll(String[] pin) {
        StringBuilder builder = new StringBuilder();
        for (String aPin : pin) builder.append(aPin);

        return builder.toString();
    }

    public static @ColorInt
    int getColorFromTheme(Context context, @AttrRes int attr) {
        Objects.requireNonNull(context);
        TypedValue value = new TypedValue();

        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, value, true);

        return value.data;
    }

    public static String hexToString(byte[] mKey) {
        return String.format("%0" + (mKey.length * 2) + "X",
                new BigInteger(1, mKey));
    }
}
