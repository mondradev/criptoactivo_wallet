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
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.cryptowallet.R;

/**
 *
 */
public class WalletSyncForegroundService extends Service {

    private static final int ONGOING_NOTIFICATION_ID = 1;

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
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

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        startService(new Intent(this, WalletSyncService.class));
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
