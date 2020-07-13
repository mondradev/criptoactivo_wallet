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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.cryptowallet.wallet.IWallet;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletManager;
import com.google.common.base.Strings;

import static com.cryptowallet.services.WalletNotificationService.ASSET_KEY;
import static com.cryptowallet.services.WalletNotificationService.HASH_KEY;
import static com.cryptowallet.services.WalletNotificationService.HEIGHT_KEY;
import static com.cryptowallet.services.WalletNotificationService.MessageType;
import static com.cryptowallet.services.WalletNotificationService.NETWORK_KEY;
import static com.cryptowallet.services.WalletNotificationService.TIME_KEY;
import static com.cryptowallet.services.WalletNotificationService.TXID_KEY;
import static com.cryptowallet.services.WalletNotificationService.TYPE_KEY;

class NotificationsWorker extends Worker {

    public NotificationsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Override this method to do your actual background processing.  This method is called on a
     * background thread - you are required to <b>synchronously</b> do your work and return the
     * {@link Result} from this method.  Once you return from this
     * method, the Worker is considered to have finished what its doing and will be destroyed.  If
     * you need to do your work asynchronously on a thread of your own choice, see
     * {@link ListenableWorker}.
     * <p>
     * A Worker is given a maximum of ten minutes to finish its execution and return a
     * {@link Result}.  After this time has expired, the Worker will
     * be signalled to stop.
     *
     * @return The {@link Result} of the computation; note that
     * dependent work will not execute if you use
     * {@link Result#failure()}.
     */
    @NonNull
    @Override
    public Result doWork() {
        WalletManager.init(this.getApplicationContext());

        String assetValue = getInputData().getString(ASSET_KEY);
        String networkValue = getInputData().getString(NETWORK_KEY);
        String typeValue = getInputData().getString(TYPE_KEY);

        if (Strings.isNullOrEmpty(assetValue) || Strings.isNullOrEmpty(networkValue)
                || Strings.isNullOrEmpty(typeValue))
            throw new IllegalArgumentException("Requires asset, network and type data.");

        MessageType type = MessageType.valueOf(typeValue.toUpperCase());
        SupportedAssets asset = SupportedAssets.valueOf(assetValue.toUpperCase());

        IWallet wallet = WalletManager.get(asset);

        if (type == MessageType.NEW_TX) {
            String txid = getInputData().getString(TXID_KEY);

            if (Strings.isNullOrEmpty(txid))
                throw new IllegalArgumentException("Requires a TxID");

            wallet.requestNewTransaction(txid);
        } else if (type == MessageType.UPDATE_TIP) {
            String heightValue = getInputData().getString(HEIGHT_KEY);
            String timeValue = getInputData().getString(TIME_KEY);

            String hash = getInputData().getString(HASH_KEY);

            if (Strings.isNullOrEmpty(heightValue) || Strings.isNullOrEmpty(timeValue)
                    || Strings.isNullOrEmpty(hash))
                throw new IllegalArgumentException("Requires height, hash and time from tip");

            long height = Long.parseLong(heightValue);
            long time = Long.parseLong(timeValue);

            wallet.updateLocalTip(height, hash, time);
        }

        return Result.success();
    }
}
