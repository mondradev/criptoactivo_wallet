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

package com.cryptowallet.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.util.Log;

import com.cryptowallet.R;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.SupportedAssets;

import java.util.Locale;

/**
 * Provee el control de las configuraciones de la aplicación.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class AppPreference {

    /**
     * Tema claro/día.
     */
    static final String LIGHT_THEME = "AppTheme";
    /**
     * Tema oscuro/nocturno.
     */
    static final String DARK_THEME = "AppTheme.Dark";

    /**
     * Tema especial azul.
     */
    static final String BLUE_THEME = "AppTheme.Blue";

    /**
     * Tema actual en ejecución.
     */
    private static String mCurrentTheme = LIGHT_THEME;


    /**
     * Carga el tema recuperado de las preferencias del usuario.
     *
     * @param context Contexto de la aplicación.
     */
    static void loadTheme(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String themeName = Utils.coalesce(
                preferences.getString("current_theme", "AppTheme"), "AppTheme");

        mCurrentTheme = themeName;

        switch (themeName) {
            case LIGHT_THEME:
                context.setTheme(R.style.AppTheme);
                break;

            case DARK_THEME:
                context.setTheme(R.style.AppTheme_Dark);
                break;

            case BLUE_THEME:
                context.setTheme(R.style.AppTheme_Blue);
                break;
        }
    }

    /**
     * Determina si el tema es azul.
     *
     * @return Un valor true para indicar que si es.
     */
    static boolean isBlueTheme() {
        return mCurrentTheme.contentEquals(BLUE_THEME);
    }

    /**
     * Determina si el tema es oscuro.
     *
     * @return Un valor true para indicar que si es.
     */
    static boolean isDarkTheme() {
        return mCurrentTheme.contentEquals(DARK_THEME);
    }

    /**
     * Activa el tema oscuro en la aplicación.
     *
     * @param context Contexto de la aplicación.
     */
    static void enableDarkTheme(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        mCurrentTheme = DARK_THEME;

        context.setTheme(R.style.AppTheme_Dark);
        preferences
                .edit()
                .putString("current_theme", DARK_THEME)
                .apply();
    }

    /**
     * Activa el tema azul en la aplicación.
     *
     * @param context Contexto de la aplicación.
     */
    static void enableBlueTheme(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        mCurrentTheme = BLUE_THEME;

        context.setTheme(R.style.AppTheme_Dark);
        preferences
                .edit()
                .putString("current_theme", BLUE_THEME)
                .apply();
    }

    /**
     * Activa el tema claro en la aplicación.
     *
     * @param context Contexto de la aplicación.
     */
    static void enableLightTheme(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        mCurrentTheme = LIGHT_THEME;

        context.setTheme(R.style.AppTheme);
        preferences
                .edit()
                .putString("current_theme", LIGHT_THEME)
                .apply();
    }


    /**
     * Recarga el tema en el aplicación, es un alias para recrear el layout.
     *
     * @param activity Actividad a recargar.
     */
    static void reloadTheme(Activity activity) {
        activity.recreate();
    }

    /**
     * Obtiene el nombre del tema actual.
     *
     * @return Nombre de tema.
     */
    static String getThemeName() {
        return mCurrentTheme;
    }

    /**
     * Obtiene un valor que indica si la opción "Solo WiFi" está activada.
     *
     * @param context Contexto de la aplicación.
     * @return Un valor true si la opción está activa.
     */
    public static boolean getUseOnlyWifi(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean("only_wifi", false);
    }

    /**
     * Establece si la opción "Solo WiFi" está activa.
     *
     * @param context Contexto de la aplicación.
     * @param use     Un valor true si la opción está activa.
     */
    static void setUseOnlyWifi(Context context, boolean use) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
                .edit()
                .putBoolean("only_wifi", use)
                .apply();
    }

    /**
     * Obtiene la versión de la aplicación.
     *
     * @param context Contexto de la aplicación.
     * @return Versión de la aplicación.
     */
    public static CharSequence getVesion(Context context) {
        String version;
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            version = context.getString(R.string.unknown_version);
        }

        return version;
    }

    /**
     * Obtiene un valor que indica si la opción "Usar lector de huellas" está activa.
     *
     * @param context Contexto de la aplicación.
     * @return Un valor true que indica si la opción está activada.
     */
    static boolean getUseFingerprint(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean("use_fingerprint", false);
    }

    /**
     * Establece un valor que indica si la opción "Usar lector de huellas" está activa.
     *
     * @param context Contexto de la aplicación.
     * @param use     Un valor true que indica si la opción está activada.
     */
    static void setUseFingerprint(Context context, boolean use) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
                .edit()
                .putBoolean("use_fingerprint", use)
                .apply();

    }

    /**
     * Obtiene el vector de inicialización.
     *
     * @param context Contexto de la aplicación.
     * @return Vector de inicialización.
     */
    public static String getIV(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("data_2", "");
    }

    /**
     * Obtiene la clave almacenada.
     *
     * @param context Contexto de la aplicación.
     * @return Clave almacenada.
     */
    public static String getStoredKey(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("data_1", "");
    }

    /**
     * Establece el vector de inicialización.
     *
     * @param context      Contexto de la aplicación.
     * @param encryptionIv Vector de inicialización.
     */
    public static void setIV(Context context, String encryptionIv) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
                .edit()
                .putString("data_2", encryptionIv)
                .apply();
    }

    /**
     * Establece la clave almacenada.
     *
     * @param context           Contexto de la aplicación.
     * @param encryptedPassword Clave almacenada.
     */
    public static void setStoredKey(Context context, String encryptedPassword) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
                .edit()
                .putString("data_1", encryptedPassword)
                .apply();
    }

    /**
     * Elimina la data sencible de las configuraciones guardadas.
     *
     * @param context Contexto de la aplicación.
     */
    static void removeData(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
                .edit()
                .remove("data_1")
                .remove("data_2")
                .apply();
    }

    /**
     * Establece la divisa seleccinada para visualizar el valor de la billetera.
     *
     * @param context  Contexto de la aplicación.
     * @param currency Simbolo de la divisa.
     */
    static void setSelectedCurrency(Context context, @CurrencyFiat String currency) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
                .edit()
                .putString("selected_currency", currency)
                .apply();
    }

    /**
     * Obtiene la divisa seleccinada para visualizar el valor de la billetera.
     *
     * @param context Contexto de la aplicación.
     * @return Simbolo de la divisa.
     */
    public static SupportedAssets getSelectedCurrency(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String assetStr = preferences.getString("selected_currency", "USD");

        return SupportedAssets.valueOf(assetStr);
    }

    private final static String TAG = "AppPreference";

    static void loadLanguage(Context context, @Language String language) {
        Log.d(TAG, "Cargando idioma: " + language);
        String[] lang = language.split("_");
        Locale locale = new Locale(lang[0], lang.length > 1 ? lang[1] : "");
        Locale.setDefault(locale);
        Configuration configEn = new Configuration();
        configEn.locale = locale;
        context.getResources().updateConfiguration(configEn, null);
    }

    static void loadLanguage(Context context) {
        loadLanguage(context, getLanguage(context));
    }

    /**
     * Elimina todas las configuraciones del usuario.
     *
     * @param context Contexto de la aplicación.
     */
    static void clear(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().clear().apply();
    }

    /**
     * Establece el tiempo en el cual se bloquea la aplicación.
     *
     * @param context  Contexto de la aplicación.
     * @param lockTime Tiempo en segundos.
     */
    static void setLockTime(Context context, int lockTime) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putInt("LockTime", lockTime).apply();
    }

    /**
     * Obtiene el tiempo en el cual se bloquea la aplicación.
     *
     * @param context Contexto de la aplicación.
     * @return Tiempo en segudnos.
     */
    static int getLockTime(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt("LockTime", 0);
    }

    static void setLanguage(Context context, @Language String language) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString("selected_language", language).apply();
    }

    static String getLanguage(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("selected_language",
                Locale.getDefault().getLanguage());
    }

    static void setSecretPhrase(Context context, String privatePhraseEncrypted) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString("private_phrase", privatePhraseEncrypted).apply();
    }

    static String getSecretPhrase(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("private_phrase", "");
    }

    /**
     * Define los valores asignables a un String.
     */
    @StringDef(value = {"USD", "MXN"})
    @interface CurrencyFiat {
    }

    /**
     *
     */
    @StringDef(value = {"en_US", "es_MX"})
    @interface Language {
    }
}
