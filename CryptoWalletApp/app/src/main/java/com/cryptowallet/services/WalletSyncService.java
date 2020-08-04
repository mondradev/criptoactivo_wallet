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
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.cryptowallet.wallet.WalletProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class WalletSyncService extends Service {

    private static final Object mLockService = new Object();

    private static boolean mRunning;

    /**
     * Ejecutor para actividades en un subproceso.
     */
    private static ExecutorService mExecutor;

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
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

            WalletProvider walletProvider = WalletProvider.getInstance(getApplicationContext());

            if (!mRunning && walletProvider.anyCreated())
                mExecutor.submit(() -> {
                    mRunning = true;

                    Log.d(WalletSyncService.class.getSimpleName(), "Sync starting");

                    walletProvider.forEachAsset((asset) -> {
                        walletProvider.loadWallets();
                        walletProvider.get(asset).syncWallet();
                    });

                    stopService(new Intent(this, WalletSyncForegroundService.class));
                    stopSelf();

                    mRunning = false;
                });
        }
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(WalletSyncService.class.getSimpleName(), "Service destroyed");
    }
}
