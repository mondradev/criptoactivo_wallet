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

package com.cryptowallet.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.cryptowallet.R;
import com.cryptowallet.app.authentication.Authenticator;
import com.cryptowallet.app.authentication.IAuthenticationCallback;
import com.cryptowallet.wallet.SupportedAssets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Provee el control de las configuraciones de la aplicación.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.1
 */
public final class Preferences {

    /**
     * Tema claro/día.
     */
    private static final String LIGHT_THEME = "AppTheme";

    /**
     * Tema oscuro/nocturno.
     */
    private static final String DARK_THEME = "AppTheme.Dark";

    /**
     * Tema especial azul.
     */
    private static final String BLUE_THEME = "AppTheme.Blue";

    /**
     * Tema predeterminado.
     */
    private static final String DEFAULT_THEME = LIGHT_THEME;

    /**
     * Etiqueta de la clase.
     */
    private final static String TAG = "Preference";

    /**
     * Etiqueta del campo de tiempo de bloqueo.
     */
    private static final String LOCK_TIMEOUT_KEY = "lockTimeout";

    /**
     * Etiqueta del campo de lenguaje de la aplicación.
     */
    private static final String LANGUAGE_KEY = "language";

    /**
     * Etiqueta del campo de activo fiat para visualizar los precios.
     */
    private static final String FIAT_KEY = "fiat";

    /**
     * Etiqueta del campo de tema.
     */
    private static final String THEME_KEY = "theme";

    /**
     * Etiqueta del campo de biometrico activado.
     */
    private static final String ENABLED_BIOMETRIC = "biometric";

    /**
     * Idioma español.
     */
    private static final String SPANISH_TAG = "es";

    /**
     * Idioma ingles.
     */
    private static final String ENGLISH_TAG = "en";

    /**
     * Instancia del singleton.
     */
    private static Preferences mInstance;

    /**
     * Mapa de temas.
     */
    private static Map<String, ThemeValue> mThemeMap;

    /**
     * Mapa de idiomas soportados.
     */
    private static Map<String, SupportedLanguage> mLanguageMap;

    /**
     * Contexto de la aplicación Android.
     */
    private final Context mAppContext;

    /**
     * Tema actual en ejecución.
     */
    private String mCurrentTheme = LIGHT_THEME;

    /**
     * Crea una nueva instancia.
     *
     * @param context Contexto de la aplicación.
     */
    private Preferences(Context context) {
        this.mAppContext = context.getApplicationContext();

        mThemeMap = new HashMap<>();
        mLanguageMap = new HashMap<>();

        registerTheme(LIGHT_THEME, R.style.AppTheme, R.string.lightMode);
        registerTheme(DARK_THEME, R.style.AppTheme_Dark, R.string.darkMode);
        registerTheme(BLUE_THEME, R.style.AppTheme_Blue, R.string.blueMode);

        registerLanguage(SPANISH_TAG, R.string.spanish_text);
        registerLanguage(ENGLISH_TAG, R.string.english_text);
    }

    /**
     * Obtiene la instancia del singleton.
     *
     * @param context Contexto de la aplicación.
     * @return Una instancia nueva de Preferencias.
     */
    public static Preferences get(Context context) {
        if (mInstance != null)
            return get();

        mInstance = new Preferences(context);

        return mInstance;
    }

    /**
     * Obtiene la instancia del singleton.
     *
     * @return La instancia de la clase.
     */
    public static Preferences get() {
        if (mInstance == null)
            throw new IllegalStateException("Require call to Preferences#get(Context)");

        return mInstance;
    }

    /**
     * Obtiene la versión de la aplicación.
     *
     * @return Versión de la aplicación.
     */
    public CharSequence getVesion() {
        String version;
        try {
            PackageInfo pInfo = mAppContext.getPackageManager()
                    .getPackageInfo(mAppContext.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = mAppContext.getString(R.string.unknown_version);
        }

        return version;
    }

    /**
     * Registra el tema de colores en la aplicación.
     *
     * @param name  Nombre del tema.
     * @param theme Identificador del tema.
     */
    private void registerTheme(String name, @StyleRes int theme, @StringRes int caption) {
        if (!mThemeMap.containsKey(name))
            mThemeMap.put(name, new ThemeValue(name, theme, caption));
    }

    /**
     * Registra el idioma en la aplicación.
     *
     * @param tag     Etiqueta del idioma.
     * @param caption Identificador del nombre del idioma.
     */
    private void registerLanguage(String tag, @StringRes int caption) {
        if (!mLanguageMap.containsKey(tag))
            mLanguageMap.put(tag, new SupportedLanguage(tag, caption));
    }

    /**
     * Obtiene el nombre del tema actual.
     *
     * @return Nombre de tema.
     */
    public ThemeValue getTheme() {
        return mThemeMap.get(mCurrentTheme);
    }

    /**
     * Obtiene un valor que indica si la opción "Usar lector de huellas" está activa.
     *
     * @return Un valor true que indica si la opción está activada.
     */
    public boolean isEnabledBiometric() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        return preferences.getBoolean(ENABLED_BIOMETRIC, false);
    }

    /**
     * Establece un valor que indica si la opción "Usar lector de huellas" está activa.
     *
     * @param use Un valor true que indica si la opción está activada.
     */
    public void setEnabledBiometric(boolean use) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        preferences.edit().putBoolean(ENABLED_BIOMETRIC, use).apply();

    }

    /**
     * Obtiene la divisa seleccinada para visualizar el valor de la billetera.
     *
     * @return Simbolo de la divisa.
     */
    public SupportedAssets getFiat() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        String assetStr = preferences.getString(FIAT_KEY, SupportedAssets.USD.name());

        return SupportedAssets.valueOf(assetStr);
    }

    /**
     * Establece la divisa seleccinada para visualizar el valor de la billetera.
     *
     * @param asset Activo FIAT.
     */
    public void setFiat(SupportedAssets asset) {
        if (!asset.isFiat())
            throw new IllegalArgumentException("Asset must be fiat");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        preferences.edit().putString(FIAT_KEY, asset.name()).apply();
    }

    /**
     * Habilita el lenguaje especificado.
     *
     * @param context     Actividad que habilita el idioma.
     * @param languageTag Etiqueta del lenguaje a utilizar.
     */
    private Context enableLanguage(Context context, String languageTag) {
        if (!mLanguageMap.containsKey(languageTag))
            throw new IllegalArgumentException(languageTag + " don't supported");

        Log.d(TAG, "Loading language " + languageTag);

        final Locale locale = Locale.forLanguageTag(languageTag);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }

    /**
     * Obtiene una lista de los lenguajes soportados por la aplicación.
     *
     * @return Lista de lenguajes.
     */
    public List<SupportedLanguage> getSupportedLanguages() {
        return new ArrayList<>(mLanguageMap.values());
    }

    /**
     * Carga el lenguaje guardado en la configuración.
     *
     * @param context Contexto de la aplicación.
     */
    Context loadLanguage(Context context) {
        return enableLanguage(context, getLanguage().getTag());
    }

    /**
     * Elimina todas las configuraciones del usuario.
     */
    public void clear() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        preferences.edit().clear().apply();
    }

    /**
     * Obtiene el tiempo en el cual se bloquea la aplicación.
     *
     * @return Tiempo en segundos.
     */
    public int getLockTimeout() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        return preferences.getInt(LOCK_TIMEOUT_KEY, 0);
    }

    /**
     * Establece el tiempo en el cual se bloquea la aplicación.
     *
     * @param lockTimeout Tiempo en segundos.
     */
    public void setLockTimeout(int lockTimeout) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        preferences.edit().putInt(LOCK_TIMEOUT_KEY, lockTimeout).apply();
    }

    /**
     * Obtiene el lenguaje de la aplicación.
     *
     * @return Etiqueta del lenguaje.
     */
    public SupportedLanguage getLanguage() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        String language = preferences.getString(LANGUAGE_KEY, Locale.getDefault().getLanguage());

        return mLanguageMap.get(language);
    }

    /**
     * Establece el lenguaje de la aplicación.
     *
     * @param activity    Actividad que establece el idioma.
     * @param languageTag Etiqueta del lenguaje.
     */
    public void setLanguage(FragmentActivity activity, String languageTag) {
        if (!mLanguageMap.containsKey(languageTag))
            throw new IllegalArgumentException(languageTag + " don't supported");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        preferences.edit().putString(LANGUAGE_KEY, languageTag).apply();

        enableLanguage(activity, languageTag);
    }

    /**
     * Activa el tema especificado en la aplicación.
     *
     * @param themeName Nombre del tema.
     */
    public void enableTheme(FragmentActivity activity, @NonNull String themeName) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);

        if (!mThemeMap.containsKey(themeName))
            throw new IllegalArgumentException(themeName + " theme isn't exists");

        int theme = Objects.requireNonNull(mThemeMap.get(themeName)).mStyle;

        mCurrentTheme = themeName;
        activity.setTheme(theme);
        activity.recreate();
        preferences.edit().putString(THEME_KEY, themeName).apply();
    }

    /**
     * Muestra el autenticador según la configuración de la aplicación ({@link Preferences}).
     *
     * @param activity Actividad que invoca el autenticador.
     * @param executor Ejecutor de las funciones de vuelta.
     * @param callback Funciones de vuelta para aceptar las respuestas del autenticador.
     */
    public void authenticate(FragmentActivity activity, Executor executor,
                             IAuthenticationCallback callback) {
        if (Preferences.get().isEnabledBiometric())
            Authenticator.authenticateWithBiometric(activity, executor, callback);
        else
            Authenticator.authenticateWithPin(activity, executor, callback);
    }

    /**
     * Carga el tema recuperado de las preferencias del usuario.
     */
    void loadTheme(FragmentActivity activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        String themeName = preferences.getString(THEME_KEY, DEFAULT_THEME);

        themeName = !mThemeMap.containsKey(themeName) ? DEFAULT_THEME : themeName;
        int theme = Objects.requireNonNull(mThemeMap.get(themeName)).mStyle;

        mCurrentTheme = themeName;
        activity.setTheme(theme);
    }

    /**
     * Obtiene los temas disponibles de la aplicación.
     *
     * @return Lista de temas.
     */
    public List<ThemeValue> getThemes() {
        return new ArrayList<>(mThemeMap.values());
    }

    /**
     * Define una estructura para los idiomas soportados por la aplicación.
     */
    public static final class SupportedLanguage {

        /**
         * Etiqueta que identifica el idioma.
         */
        private final String mTag;

        /**
         * Identificador del recurso de texto utilizado para identificar el idioma en IU.
         */
        @StringRes
        private final int mCaption;

        /**
         * Crea una nueva instancia.
         *
         * @param tag     Etiqueta del idioma.
         * @param caption Nombre del idioma en IU.
         */
        SupportedLanguage(String tag, int caption) {
            this.mTag = tag;
            this.mCaption = caption;
        }

        /**
         * Obtiene la etiqueta del idioma.
         *
         * @return Etiqueta del idioma.
         */
        public String getTag() {
            return mTag;
        }

        /**
         * Obtiene el nombre del idioma en IU.
         *
         * @return Nombre del idioma.
         */
        public int getCaption() {
            return mCaption;
        }
    }

    /**
     * Define los atributos de un tema de estilo de la aplicación.
     */
    public static final class ThemeValue {

        /**
         * Nombre del tema.
         */
        private final String mName;

        /**
         * Identificador del recurso de estilo.
         */
        @StyleRes
        private final int mStyle;

        /**
         * Identificador del recurso de texto utilizado para identificar el tema en IU.
         */
        @StringRes
        private final int mCaption;

        /**
         * Crea una nueva instancia.
         *
         * @param style   Identificador del estilo.
         * @param caption Identificador del texto.
         */
        ThemeValue(String name, int style, int caption) {
            this.mName = name;
            this.mStyle = style;
            this.mCaption = caption;
        }

        /**
         * Obtiene el nombre del tema.
         *
         * @return Nombre del tema.
         */
        public String getName() {
            return mName;
        }

        /**
         * Obtiene el identificador del estilo del tema.
         *
         * @return Estilo del tema.
         */
        public int getStyle() {
            return mStyle;
        }

        /**
         * Obtiene el identificador del nombre utilizado para visualizarse en IU.
         *
         * @return Nombre del tema.
         */
        public int getCaption() {
            return mCaption;
        }
    }
}
