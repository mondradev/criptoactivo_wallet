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
     * Tiempo de espera para el bloqueo.
     */
    private static final int LOCK_TIME = 60000;

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
     *
     */
    private boolean mUserLeaved;

    /**
     * Cuenta regresiva para bloquear la interfaz.
     */
    private CountDownTimer mLockTimer
            = new CountDownTimer(LOCK_TIME, 1) {


        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            if (!mCanLock)
                return;

            if (mRequireLock)
                return;

            Log.v(TAG, "Inactividad de la aplicación por " + (LOCK_TIME / 1000) + "s.");

            lockApp();

            if (!mUserLeaved)
                callMainActivity();
        }

    };

    private void callMainActivity() {
        Intent intent = new Intent(ActivityBase.this,
                WalletAppActivity.class);

        intent.putExtra(ExtrasKey.REQ_AUTH, true);
        ActivityBase.this.startActivity(intent);
        ActivityBase.this.finish();
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
     * Called whenever a key, touch, or trackball event is dispatched to the
     * activity.  Implement this method if you wish to know that the user has
     * interacted with the device in some way while your activity is running.
     * This callback and {@link #onUserLeaveHint} are intended to help
     * activities manage status bar notifications intelligently; specifically,
     * for helping activities determine the proper time to cancel a notfication.
     *
     * <p>All calls to your activity's {@link #onUserLeaveHint} callback will
     * be accompanied by calls to {@link #onUserInteraction}.  This
     * ensures that your activity will be told of relevant user activity such
     * as pulling down the notification pane and touching an item there.
     *
     * <p>Note that this callback will be invoked for the touch down action
     * that begins a touch gesture, but may not be invoked for the touch-moved
     * and touch-up actions that follow.
     *
     * @see #onUserLeaveHint()
     */
    @Override
    public void onUserInteraction() {
        if (mCanLock)
            if (mRequireLock)
                callMainActivity();
            else {
                mLockTimer.cancel();
                mLockTimer.start();

                Log.d(TAG, "Reiniciando contador de inactividad");
            }
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

        mUserLeaved = false;

        super.onResume();
        if (!mCurrentTheme.contentEquals(AppPreference.getThemeName()))
            AppPreference.reloadTheme(this);

        if (mCanLock) {
            Log.v(TAG, "Validando si requiere bloqueo de aplicación.");
            if (mRequireLock)
                callMainActivity();
            else
                mLockTimer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.v(TAG, "Desactivando el contador de inactividad.");

        mLockTimer.cancel();
    }

    /**
     * Establece que la actividad puede ser bloqueada.
     */
    protected void setCanLock(boolean canLock) {
        mCanLock = canLock;

        if (!canLock)
            mLockTimer.cancel();
        else {
            mLockTimer.cancel();
            mLockTimer.start();
        }
    }

    /**
     *
     */
    protected void lockApp() {
        mLockTimer.cancel();
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


    @Override
    protected void onUserLeaveHint() {
        lockApp();

        mUserLeaved = true;
    }
}
