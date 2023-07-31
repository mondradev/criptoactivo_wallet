/*
 * Copyright &copy; 2023. Criptoactivo
 * Copyright &copy; 2023. InnSy Tech
 * Copyright &copy; 2023. Ing. Javier de Jesús Flores Mondragón
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.cryptowallet.Constants;
import com.cryptowallet.R;
import com.cryptowallet.app.fragments.TransactionFragment;
import com.cryptowallet.core.domain.SupportedAssets;
import com.cryptowallet.services.WalletProvider;
import com.cryptowallet.wallet.ITransaction;

import java.util.Objects;

/**
 * Permite la visualización de la información de una transacción. Es invocado cuando se acceder a la
 * información de una transacción por medio de una notificación.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class TxBottomSheetDialogActivity extends LockableActivity {

    /**
     * Crea la intención para invocar la actividad.
     *
     * @param context Contexto de la aplicación.
     * @return Una intención que permite lanzar la actividad.
     */
    public static Intent createIntent(Context context, ITransaction tx) {
        Intent intent = new Intent(context, TxBottomSheetDialogActivity.class);
        intent.putExtra(Constants.EXTRA_TXID, tx.getID());
        intent.putExtra(Constants.EXTRA_CRYPTO_ASSET, tx.getCryptoAsset().name());

        return intent;
    }

    /**
     * Este método es llamado cuando se crea la actividad, en el se invoca el fragmento para
     * visualizar la información de la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transparent);
        LockableActivity.registerMainActivityClass(SplashActivity.class);
        WalletProvider.initialize(this);

        final String txid = getIntent().getStringExtra(Constants.EXTRA_TXID);
        final String assetName = getIntent().getStringExtra(Constants.EXTRA_CRYPTO_ASSET);

        Objects.requireNonNull(txid);
        Objects.requireNonNull(assetName);

        final SupportedAssets asset = SupportedAssets.valueOf(assetName);

        TransactionFragment.show(this, asset, txid, (dialog) -> finishAndRemoveTask());
    }

}
