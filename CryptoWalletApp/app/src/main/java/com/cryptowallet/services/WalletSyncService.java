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

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.cryptowallet.wallet.AbstractWallet;
import com.cryptowallet.wallet.WalletProvider;

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
     * Indica si está corriendo el servicio de sincronización.
     */
    private static boolean mRunning;

    /**
     * Ejecutor para actividades en un subproceso.
     */
    private static ExecutorService mExecutor;

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

            WalletProvider.initialize(getApplicationContext());
            WalletProvider walletProvider = WalletProvider.getInstance();

            if (!mRunning && walletProvider.anyCreated())
                mExecutor.submit(() -> {
                    mRunning = true;

                    Log.d(WalletSyncService.class.getSimpleName(), "Synchronizing each wallet");

                    walletProvider.loadWallets();
                    walletProvider.forEachWallet(AbstractWallet::syncWallet);
                    walletProvider.updatePushToken(walletProvider.getPushToken());

                    stopService(new Intent(this, WalletSyncForegroundService.class));
                    stopSelf();

                    mRunning = false;
                });
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
}
