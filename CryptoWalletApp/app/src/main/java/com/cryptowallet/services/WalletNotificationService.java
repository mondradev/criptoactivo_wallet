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

import com.cryptowallet.wallet.WalletManager;
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
        Log.i(LOG_TAG, "Received message from FCM");
        // TODO recepción de notificaciones
    }
}
