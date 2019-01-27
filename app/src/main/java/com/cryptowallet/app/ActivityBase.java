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

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletServiceBase;

/**
 * Provee una base para las actividades que soporten los cambios de temas desde la aplicación.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public abstract class ActivityBase extends AppCompatActivity {

    /**
     * Etiqueta de la clase.
     */
    private static final String TAG = "ActivityBase";

    /**
     * Indica que la aplicación requiere ser bloqueda.
     */
    private static boolean mRequireLock = false;

    /**
     * Tema actual de la aplicación.
     */
    private String mCurrentTheme = "";

    /**
     * Cuenta regresiva para bloquear la interfaz.
     */
    private CountDownTimer mLockTimer;

    /**
     * Llama la actividad principal requiriendo que esta sea bloqueada.
     */
    private void callMainActivity() {
        if (mLockTimer != null)
            mLockTimer.cancel();

        Intent intent = new Intent(ActivityBase.this,
                WalletAppActivity.class);

        intent.putExtra(ExtrasKey.REQ_AUTH, true);
        ActivityBase.this.startActivity(intent);
        ActivityBase.this.finish();
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


        if (mRequireLock)
            callMainActivity();
    }

    /**
     * Establece la bandera de bloqueo de la aplicación y desactiva el temporizador de inactividad.
     */
    protected void lockApp() {
        mLockTimer.cancel();
        Log.v(TAG, "Bloqueando aplicación.");
        mRequireLock = true;
    }

    /**
     * Borra la bandera de bloqueo e inicia el temporizador de inactividad.
     */
    protected void unlockApp() {
        Log.v(TAG, "Desbloqueando aplicación.");
        mRequireLock = false;
    }

    /**
     * Obtiene un valor que indica si la aplicación está bloqueada.
     *
     * @return Un valor true cuando la aplicación está bloqueda.
     */
    protected boolean isLockApp() {
        return mRequireLock;
    }


    /**
     * Crea e inicia el contador de la aplicación.
     */
    private void createLockTimer() {

        if (!WalletServiceBase.isRunning(SupportedAssets.BTC))
            return;

        int time = AppPreference.getLockTime(this.getApplicationContext()) * 1000;

        if (time < 0)
            return;

        Log.d(TAG, "Creando temporizador de seguridad: " + time);

        mLockTimer = new CountDownTimer(time, 1) {


            /**
             * Este método es llamado cuando el temporizador realiza un tick.
             *
             * @param ignored Parametro ignorado.
             */
            @Override
            public void onTick(long ignored) {
            }

            /**
             * Este método es llamado cuando el temporizador se agota. Este método configura la
             * actividad para que esta se bloquee al momento de interactuar con ella.
             */
            @Override
            public void onFinish() {

                if (mRequireLock)
                    return;

                Log.v(TAG, "Inactividad de la aplicación por " + (time / 1000) + "s.");

                lockApp();
            }

        };

        mLockTimer.start();
    }

    /**
     * Este método es llamado cuando el usuario abandona la actividad actual, bloqueando la
     * aplicación.
     */
    @Override
    protected void onUserLeaveHint() {
        createLockTimer();
    }
}
