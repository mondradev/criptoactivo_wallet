package com.cryptowallet.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Provee una base para las actividades que soporten los cambios de temas desde la aplicación.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public abstract class ActivityBase extends AppCompatActivity {

    /**
     * Tema actual de la aplicación.
     */
    private String mCurrentTheme = "";

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppPreference.loadTheme(this);
        mCurrentTheme = AppPreference.getThemeName();
    }

    /**
     * Este método es llamado cuando se recupera la aplicación al haber cambiado a otra.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!mCurrentTheme.contentEquals(AppPreference.getThemeName()))
            AppPreference.reloadTheme(this);
    }
}
