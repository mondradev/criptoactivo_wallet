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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.cryptowallet.utils.Timeout;

import java.util.Objects;

/**
 * Provee una base para las actividades que soporten los cambios de temas desde la aplicación.
 * <p></p>
 * Al comienzo de la aplicación se deberá establecer la actividad principal que herede de esta misma,
 * que será invocada después del bloqueo de la aplicación de la siguiente manera:
 *
 * <pre>
 *
 *     LockableActivity.registerMainActivityClass(MainActivity.class)
 *
 * </pre>
 * Las otras actividades que requieran bloquear después del tiempo especificado por
 * {@link Preferences#getLockTimeout()} deberá heredar igualmente de esta clase. El tiempo comenzará
 * a correr una vez que la aplicación desencadena el evento {@link Lifecycle.Event#ON_PAUSE}. Si el
 * tiempo es agotado, al desencadenar el evento {@link Lifecycle.Event#ON_RESUME} la actividad
 * invocará la clase de la actividad registrada previamente.
 * <p></p>
 * En la actividad principal deberá implementar la manera de desbloquear la aplicación, y como
 * resultado invocar la función {@link #unlockApp()}. De lo contrario, las actividades estarán
 * enviando a la actividad principal cada vez que se suspenda la aplicación.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.1
 */
// TODO Fix lifecycle, don't recovery state
public abstract class LockableActivity extends AppCompatActivity implements LifecycleObserver {

    /**
     * Etiqueta de la clase.
     */
    private static final String TAG = "LockableActivity";

    /**
     * Clase principal de la billetera.
     */
    private static Class<? extends FragmentActivity> mMainActivity;

    /**
     * Indica que la aplicación requiere ser bloqueda.
     */
    private static boolean mRequireLock = false;

    /**
     * Cuenta regresiva para bloquear la interfaz.
     */
    private static Timeout mLockTimer;

    /**
     * Indica que la aplicación está bloqueada.
     */
    private static boolean mLocked;

    /**
     * Tema actual de la aplicación.
     */
    private String mCurrentTheme;

    /**
     * Registra la clase de la actividad prinpal de la aplicación.
     *
     * @param clazz Clase de la actividad.
     */
    public static void registerMainActivityClass(@NonNull Class<? extends FragmentActivity> clazz) {
        mMainActivity = clazz;
    }

    /**
     * Bloquea las actividades desde el exterior.
     */
    public static void requireUnlock() {
        if (mRequireLock)
            return;

        stopTimer();

        Log.v(TAG, "Locking app");

        mRequireLock = true;
    }

    /**
     * Detiene el temporizador de bloqueo.
     */
    public static void stopTimer() {
        if (mLockTimer != null)
            mLockTimer.cancel();
    }

    /**
     * Llama la actividad principal requiriendo que esta sea bloqueada.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private void callLockScreen() {
        if (mMainActivity == null)
            throw new UnsupportedOperationException(
                    "Requires register the class by #registerMainActivityClass(Class)");

        stopTimer();

        if (!mRequireLock)
            return;

        if (mMainActivity.isInstance(this) && mLocked)
            return;

        Log.d(TAG, "Locking screen by timeout and calling main activity");

        Intent intent = new Intent(LockableActivity.this, mMainActivity);

        mLocked = true;

        LockableActivity.this.startActivity(intent);
        LockableActivity.this.finishAfterTransition();
    }

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Preferences.get().loadTheme(this);

        mCurrentTheme = Preferences.get().getTheme().getName();

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    /**
     * Este método es llamado cuando la actividad es destruída.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
    }

    /**
     * Este método es llamado cuando se sale de la aplicación.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onLeaveApp() {
        createLockTimer();
    }

    /**
     * Este método es llamado cuando se recupera la aplicación al haber cambiado a otra.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (!mCurrentTheme.contentEquals(Preferences.get().getTheme().getName()))
            recreate();
    }

    /**
     * Borra la bandera de bloqueo.
     */
    public void unlockApp() {
        if (!mRequireLock)
            return;

        Log.v(TAG, "Unlocking app");

        mRequireLock = false;
        mLocked = false;
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
        if (mRequireLock)
            return;

        int time = Preferences.get().getLockTimeout() * 1000;

        Log.v(TAG, "LockTimeout " + time + " ms");

        if (time == 0) {
            requireUnlock();

            return;
        }

        if (time < 0)
            return;

        if (mLockTimer != null)
            mLockTimer.restart();
        else
            mLockTimer = new Timeout(time) {

                /**
                 * Este método es llamado cuando el temporizador se agota. Este método configura la
                 * actividad para que esta se bloquee al momento de interactuar con ella.
                 */
                @Override
                public void onFinish() {
                    requireUnlock();
                }

            };
    }

    /**
     * Obtiene el elemento especificado por el ID, en caso de no encontrarlo se lanza un excepción.
     *
     * @param id  Identificador del elemento.
     * @param <T> Tipo del elemento.
     * @return Un elemento.
     */
    @NonNull
    protected <T extends View> T requireView(int id) {
        View view = findViewById(id);
        Objects.requireNonNull(view);

        //noinspection unchecked
        return (T) view;
    }
}
