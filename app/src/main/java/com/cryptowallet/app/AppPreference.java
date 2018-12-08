package com.cryptowallet.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import com.cryptowallet.R;
import com.cryptowallet.utils.Helper;

/**
 *
 */
public final class AppPreference {

    private static final String LIGHT_THEME = "AppTheme";
    private static final String DARK_THEME = "AppTheme.Dark";
    private static final String BLUE_THEME = "AppTheme.Blue";

    private static String mCurrentTheme = LIGHT_THEME;


    /**
     * @param context
     */
    static void loadTheme(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String themeName = Helper.coalesce(
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

    static boolean isBlueTheme() {
        return mCurrentTheme.contentEquals(BLUE_THEME);
    }

    static boolean isLightTheme() {
        return mCurrentTheme.contentEquals(LIGHT_THEME);
    }

    static boolean isDarkTheme() {
        return mCurrentTheme.contentEquals(DARK_THEME);
    }

    static void enableDarkTheme(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        mCurrentTheme = DARK_THEME;

        context.setTheme(R.style.AppTheme_Dark);
        preferences
                .edit()
                .putString("current_theme", DARK_THEME)
                .apply();
    }

    static void enableBlueTheme(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        mCurrentTheme = BLUE_THEME;

        context.setTheme(R.style.AppTheme_Dark);
        preferences
                .edit()
                .putString("current_theme", BLUE_THEME)
                .apply();
    }

    static void enableTheme(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        mCurrentTheme = LIGHT_THEME;

        context.setTheme(R.style.AppTheme);
        preferences
                .edit()
                .putString("current_theme", LIGHT_THEME)
                .apply();
    }


    static void reloadTheme(Activity context) {
        context.recreate();
    }

    static String getThemeName() {
        return mCurrentTheme;
    }

    public static boolean useOnlyWifi(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean("only_wifi", false);
    }

    public static void useOnlyWifi(Context context, boolean use) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
                .edit()
                .putBoolean("only_wifi", use)
                .apply();
    }

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

    public static boolean useFingerprint(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean("use_fingerprint", false);
    }


    public static void useFingerprint(Context context, boolean use) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
                .edit()
                .putBoolean("use_fingerprint", use)
                .apply();

    }

    public static String getIV(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("data_2", "");
    }

    public static String getPin(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("data_1", "");
    }

    public static void setIV(Context context, String encryptionIv) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
                .edit()
                .putString("data_2", encryptionIv)
                .apply();
    }

    public static void setPin(Context context, String encryptedPassword) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
                .edit()
                .putString("data_1", encryptedPassword)
                .apply();
    }

    public static void removeData(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
                .edit()
                .remove("data_1")
                .remove("data_2")
                .apply();
    }

    public static void setSelectedCurrency(Context context, @CurrencyFiat String currency) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
                .edit()
                .putString("selected_currency", currency)
                .apply();
    }

    public static String getSelectedCurrency(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("selected_currency", "USD");
    }

    @StringDef(value = {"USD", "MXN"})
    @interface CurrencyFiat {
    }
}
