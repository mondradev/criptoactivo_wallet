/*
 * Copyright 2018 InnSy Tech
 * Copyright 2018 Ing. Javier de Jesús Flores Mondragón
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

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Provee una base para las actividades que soporten los cambios de temas desde la aplicación.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public abstract class ActivityBase extends AppCompatActivity {

    /**
     *
     */
    private static final String TAG = "ActivityBase";

    /**
     *
     */
    private static boolean mRequireLock = false;

    /**
     * Tema actual de la aplicación.
     */
    private String mCurrentTheme = "";

    /**
     * Indica si la aplicación puede bloquearse.
     */
    private boolean mCanLock = true;

    /**
     * Cuenta regresiva para bloquear la interfaz.
     */
    private CountDownTimer mLockTimer
            = new CountDownTimer(60000, 1) {


        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            if (!mCanLock)
                return;

            Log.v(TAG, "Inactividad de la aplicación por 30s.");

            Intent intent = new Intent(ActivityBase.this,
                    WalletAppActivity.class);

            intent.putExtra(ExtrasKey.REQ_AUTH, true);
            ActivityBase.this.startActivity(intent);
            ActivityBase.this.finish();
        }

    };

    @Override
    protected void onPause() {
        Log.v(TAG, "Pausando la actividad.");
        super.onPause();

        mLockTimer.cancel();

        lockApp();
    }

    /**
     * Obtiene el temporizador de la vista de autenticación.
     *
     * @return Temporizador de autenticación.
     */
    protected CountDownTimer getLockTimer() {
        return mLockTimer;
    }

    /**
     * Called when a touch screen event was not handled by any of the views
     * under it.  This is most useful to process touch events that happen
     * outside of your window bounds, where there is no view to receive it.
     *
     * @param event The touch screen event being processed.
     * @return Return true if you have consumed the event, false if you haven't.
     * The default implementation always returns false.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);

        if (mCanLock) {
            mLockTimer.cancel();
            mLockTimer.start();

            Log.d(TAG, "Reiniciando contador de inactividad");
        }

        return result;
    }

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

        unlockApp();
    }

    /**
     * Este método es llamado cuando se recupera la aplicación al haber cambiado a otra.
     */
    @Override
    protected void onResume() {
        Log.v(TAG, "Retomando la actividad.");

        super.onResume();
        if (!mCurrentTheme.contentEquals(AppPreference.getThemeName()))
            AppPreference.reloadTheme(this);

        if (mCanLock) {
            Log.v(TAG, "Bloqueando aplicación.");
            if (mRequireLock)
                mLockTimer.onFinish();
            else
                mLockTimer.start();
        }
    }

    /**
     * Establece que la actividad puede ser bloqueada.
     */
    protected void setCanLock(boolean canLock) {
        mCanLock = canLock;

        if (!canLock)
            mLockTimer.cancel();
    }

    /**
     *
     */
    protected void lockApp() {
        Log.v(TAG, "Bloqueando aplicación.");
        mRequireLock = true;
    }

    /**
     *
     */
    protected void unlockApp() {
        Log.v(TAG, "Desbloqueando aplicación.");
        mRequireLock = false;
    }

    /**
     * @return
     */
    protected boolean isLockApp() {
        return mRequireLock;
    }

}
