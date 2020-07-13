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

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.cryptowallet.wallet.WalletManager;
import com.google.common.base.Strings;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Esta clase permite la recepción de mensajes provenientes del servidor. Esto permite la generación
 * de notificaciones de la aplicación como la recepción de pagos o envíos desde otras billeteras.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class WalletNotificationService extends FirebaseMessagingService {

    static final String TYPE_KEY = "type";
    static final String HEIGHT_KEY = "height";
    static final String HASH_KEY = "hash";
    static final String TIME_KEY = "time";
    static final String NETWORK_KEY = "network";
    static final String ASSET_KEY = "asset";
    static final String TXID_KEY = "txid";

    /**
     * Etiqueta de log de la clase.
     */
    private static String LOG_TAG = "WalletNotification";

    /**
     * Este método es llamado cuando si el token es actualizado.
     *
     * @param token Token de la aplicación.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(LOG_TAG, "New token detected: " + token);
        WalletManager.forEachWallet(wallet -> wallet.updatePushToken(token));
    }

    /**
     * Este método es llamado cuando un mensaje es recibido.
     *
     * @param remoteMessage Mensaje recibido.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String type = remoteMessage.getData().get(TYPE_KEY);

        if (Strings.isNullOrEmpty(type)) return;

        MessageType messageType = MessageType.valueOf(type.toUpperCase());

        if (messageType.equals(MessageType.UNKNOWN)) return;

        Data parameters;

        if (messageType.equals(MessageType.UPDATE_TIP)) {
            parameters = new Data.Builder()
                    .putString(HEIGHT_KEY, remoteMessage.getData().get(HEIGHT_KEY))
                    .putString(HASH_KEY, remoteMessage.getData().get(HASH_KEY))
                    .putString(TIME_KEY, remoteMessage.getData().get(TIME_KEY))
                    .putString(NETWORK_KEY, remoteMessage.getData().get(NETWORK_KEY))
                    .putString(ASSET_KEY, remoteMessage.getData().get(ASSET_KEY))
                    .build();
        } else if (messageType.equals(MessageType.NEW_TX)) {
            parameters = new Data.Builder()
                    .putString(TXID_KEY, remoteMessage.getData().get(TXID_KEY))
                    .putString(NETWORK_KEY, remoteMessage.getData().get(NETWORK_KEY))
                    .putString(ASSET_KEY, remoteMessage.getData().get(ASSET_KEY))
                    .build();
        } else return;

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(NotificationsWorker.class)
                .setInputData(parameters).build();
        WorkManager.getInstance(getApplicationContext()).beginWith(work).enqueue();
    }

    enum MessageType {
        UNKNOWN,
        UPDATE_TIP,
        NEW_TX
    }
}
