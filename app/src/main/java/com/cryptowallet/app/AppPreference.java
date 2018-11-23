package com.cryptowallet.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.cryptowallet.R;
import com.cryptowallet.utils.Helper;

/**
 *
 */
final class AppPreference {

    private static final String LIGHT_THEME = "AppTheme";
    private static final String DARK_THEME = "AppTheme.Dark";

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
        }
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

    static void enableTheme(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        mCurrentTheme = LIGHT_THEME;

        context.setTheme(R.style.AppTheme);
        preferences
                .edit()
                .putString("current_theme", LIGHT_THEME)
                .apply();
    }


}
