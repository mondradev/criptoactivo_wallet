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

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.cryptowallet.Constants;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.WalletProvider;
import com.google.common.base.Strings;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

/**
 * Esta clase permite la recepción de mensajes provenientes del servidor. Esto permite la generación
 * de notificaciones de la aplicación como la recepción de pagos o envíos desde otras billeteras.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class FirebaseNotificationsService extends FirebaseMessagingService {

    /**
     * Clave para tipo de mensaje.
     */
    private static final String TYPE_KEY = "type";

    /**
     * Clave para la altura del bloque.
     */
    private static final String HEIGHT_KEY = "height";

    /**
     * Clave para el hash del bloque.
     */
    private static final String HASH_KEY = "hash";

    /**
     * Clave para el tiempo del bloque.
     */
    private static final String TIME_KEY = "time";

    /**
     * Clave para el tipo de red.
     */
    private static final String NETWORK_KEY = "network";

    /**
     * Clave para el activo del mensaje.
     */
    private static final String ASSET_KEY = "asset";

    /**
     * Clave para el identificador de la transacción.
     */
    private static final String TXID_KEY = "txid";

    /**
     * Clave para la lista de los identificadores de las transacciones del nuevo bloque.
     */
    private static final String TXS_KEY = "txs";

    /**
     * Etiqueta de log de la clase.
     */
    private static String LOG_TAG = FirebaseNotificationsService.class.getSimpleName();

    /**
     * Este método es llamado cuando si el token es actualizado.
     *
     * @param token Token de la aplicación.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        WalletProvider.getInstance(this)
                .forEachWallet(wallet -> wallet.updatePushToken(token));
    }

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        WalletProvider.getInstance(this).loadWallets();
        WalletProvider.getInstance(this).syncWallets();
    }

    /**
     * Este método es llamado cuando un mensaje es recibido.
     *
     * @param remoteMessage Mensaje recibido.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(LOG_TAG, "Received message from firebase server: " + remoteMessage.getData());

        String type = remoteMessage.getData().get(TYPE_KEY);

        if (Strings.isNullOrEmpty(type)) return;

        MessageType messageType = MessageType.valueOf(type.toUpperCase());

        if (messageType.equals(MessageType.UNKNOWN)) return;

        Utils.tryNotThrow(() -> {
            Intent intent = new Intent(getApplicationContext(), WalletProvider.class)
                    .putExtra(Constants.EXTRA_HEIGHT, Utils.parseInt(remoteMessage.getData().get(HEIGHT_KEY)))
                    .putExtra(Constants.EXTRA_HASH, remoteMessage.getData().get(HASH_KEY))
                    .putExtra(Constants.EXTRA_TIME, Utils.parseInt(remoteMessage.getData().get(TIME_KEY)))
                    .putExtra(Constants.EXTRA_NETWORK, remoteMessage.getData().get(NETWORK_KEY))
                    .putExtra(Constants.EXTRA_ASSET, remoteMessage.getData().get(ASSET_KEY));

            if (messageType.equals(MessageType.NEW_BLOCK)) {
                String[] txs = new Gson()
                        .fromJson(remoteMessage.getData().get(TXS_KEY), String[].class);
                intent.setAction(Constants.NEW_BLOCK)
                        .putExtra(Constants.EXTRA_TXS, txs);
            } else if (messageType.equals(MessageType.NEW_TX)) {
                intent.setAction(Constants.NEW_TRANSACTION)
                        .putExtra(Constants.EXTRA_TXID, remoteMessage.getData().get(TXID_KEY));
            }

            WalletProvider.getInstance(this).sendRequest(intent);
        });
    }

    /**
     * Tipos de mensajes que puede recibirse desde el servidor de Firebase.
     *
     * @author Ing. Javier Flores (jjflores@innsytech.com)
     * @version 1.0
     */
    enum MessageType {

        /**
         * Mensaje desconocido.
         */
        UNKNOWN,

        /**
         * Mensaje de nuevo bloque.
         */
        NEW_BLOCK,

        /**
         * Mensaje de nueva transacción.
         */
        NEW_TX
    }
}
