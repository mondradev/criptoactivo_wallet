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

package com.cryptowallet.app;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.cryptowallet.R;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletManager;
import com.google.common.base.Joiner;

import java.util.Objects;

/**
 * Esta actividad permite mostrar la información de la transacción especificada a través de los
 * extras.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.2
 */
public class TransactionActivity extends LockableActivity {

    /**
     * Clave de extra para identificador de transacción.
     */
    public static final String TX_ID_EXTRA =
            String.format("%s.TxIdKey", TransactionActivity.class.getName());

    /**
     * Clave de extra para tipo de activo.
     */
    public static final String ASSET_EXTRA =
            String.format("%s.AssetKey", TransactionActivity.class.getName());

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);
        setTitle(R.string.transaction_title);

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);

        SupportedAssets asset = SupportedAssets.valueOf(getIntent().getStringExtra(ASSET_EXTRA));
        String id = getIntent().getStringExtra(TX_ID_EXTRA);

        ITransaction tx = WalletManager.get(asset).findTransaction(id);

        if (tx == null) {
            finish();
            return;
        }

        final String fromAddresses = Joiner.on("\n").join(tx.getFromAddress());
        final String toAddresses = Joiner.on("\n").join(tx.getToAddress());
        final int colorTxKind = Utils.resolveColor(this, tx.isPay()
                ? R.attr.colorSentTx : R.attr.colorReceivedTx);
    }

}
