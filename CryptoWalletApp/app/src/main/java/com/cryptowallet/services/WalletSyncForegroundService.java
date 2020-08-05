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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.cryptowallet.R;

/**
 * Servicio de primer plano que permite la invocación del servicio de segundo plano para realizar
 * la sincronización de las billeteras. Este servicio es invocado desde
 * {@link com.cryptowallet.SyncOnBootReceiver}.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class WalletSyncForegroundService extends Service {

    /**
     * Identificador de la notificación del servicio.
     */
    private static final int ONGOING_NOTIFICATION_ID = 1;

    /**
     * Este método es invocado cuando se crea el servicio.
     */
    @Override
    public void onCreate() {
        Notification notification =
                new NotificationCompat.Builder(this,
                        getString(R.string.default_notification_channel_id))
                        .setContentTitle(getText(R.string.sync_notification_title))
                        .setContentText(getText(R.string.sync_notification_message))
                        .setSmallIcon(R.drawable.ic_sync)
                        .build();

        NotificationManager notificationCompat
                = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationCompat == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    getString(R.string.default_notification_channel_id),
                    getString(R.string.default_notification_channel_name),
                    NotificationManager.IMPORTANCE_NONE
            );

            notificationCompat.createNotificationChannel(channel);
        }

        startForeground(ONGOING_NOTIFICATION_ID, notification);
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
        startService(new Intent(this, WalletSyncService.class));
        return START_NOT_STICKY;
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
