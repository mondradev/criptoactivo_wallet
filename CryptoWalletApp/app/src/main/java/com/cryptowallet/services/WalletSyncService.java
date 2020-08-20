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

package com.cryptowallet.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.cryptowallet.Constants;
import com.cryptowallet.wallet.AbstractWallet;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio que permite la sincronización de las billeteras creadas o restauradas. La sincronización
 * se hace una billetera a la vez.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class WalletSyncService extends Service {

    /**
     * Objeto utilizado para sincronizar el acceso al servicio.
     */
    private static final Object mLockService = new Object();

    /**
     * Ejecutor para actividades en un subproceso.
     */
    private static ExecutorService mExecutor;

    /**
     * Indica si está corriendo el servicio de sincronización.
     */
    private static boolean mSynchronizing;

    /**
     * Este método es invocado cuando se crea el servicio. En esta función se invoca el hilo donde
     * se ejecuta la sincronización de cada billetera.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (mLockService) {
            if (mExecutor == null) {
                mExecutor = Executors.newCachedThreadPool();

                mExecutor.submit(() -> Thread.currentThread()
                        .setName(WalletProvider.class.getSimpleName() + " Thread"));
            }
        }
    }

    /**
     * Este método es invocado después de crear el servicio, cuando se invoca con
     * {@link #startService(Intent)}.
     *
     * @param intent  Intención que invoca el servicio.
     * @param flags   Banderas utilizadas para la ejecución.
     * @param startId Identificador de la llamada.
     * @return Bandera que indica como está siendo ejecutado el servicio.
     */
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (!mSynchronizing)
            mExecutor.submit(() -> {
                stopService(new Intent(this, WalletSyncForegroundService.class));

                mSynchronizing = true;

                Log.d(WalletSyncService.class.getSimpleName(), "Synchronizing each wallet");

                WalletProvider.getInstance().loadWallets();
                WalletProvider.getInstance().forEachWallet(AbstractWallet::syncWallet);
                WalletProvider.getInstance()
                        .updatePushToken(WalletProvider.getInstance().getPushToken());

                mSynchronizing = false;
            });

        return START_STICKY;
    }

    /**
     * Este método es invocado después de crear el servicio, cuando se invoca con
     * {@link #bindService(Intent, ServiceConnection, int)}.
     *
     * @param intent Intención que invoca el servicio.
     * @return Instancia que permite la interacción con el servicio.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Este método es invocado cuando la aplicación es removida de la lista de recientes.
     *
     * @param rootIntent Intención que invocó el servicio.
     */
    @SuppressLint("WrongConstant")
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        Intent intent = new Intent(Constants.START);
        intent.addFlags(Constants.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        sendBroadcast(intent);
    }

}
