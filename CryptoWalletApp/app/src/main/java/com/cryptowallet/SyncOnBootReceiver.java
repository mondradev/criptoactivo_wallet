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

package com.cryptowallet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cryptowallet.wallet.WalletProvider;

/**
 * Crea un receptor de la acción {@link Intent#ACTION_BOOT_COMPLETED}. Esta clase invoca el servicio
 * de sincronización, permitiendo poner al día la billetera.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class SyncOnBootReceiver extends BroadcastReceiver {

    /**
     * Este método es llamado cuando se recibe una petición a este receptor.
     *
     * @param context Contexto de la aplicación.
     * @param intent  Intención de la recepción.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            WalletProvider.getInstance(context).loadWallets();
            WalletProvider.getInstance(context).syncWalletsForeground();
        }
    }
}
