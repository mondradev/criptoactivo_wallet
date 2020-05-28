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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.cryptowallet.R;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletManager;

import java.text.NumberFormat;
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
     * Simbolo de valor aproximado.
     */
    private static final String ALMOST_EQUAL_TO = "≈";

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

        final SupportedAssets asset = SupportedAssets.valueOf(getIntent().getStringExtra(ASSET_EXTRA));
        final String id = getIntent().getStringExtra(TX_ID_EXTRA);

        ITransaction tx = WalletManager.get(asset).findTransaction(id);

        if (tx == null) {
            finish();
            return;
        }

        final NumberFormat formatter = NumberFormat.getIntegerInstance();
        final SupportedAssets fiatAsset = Preferences.get().getFiat();
        final int colorTxKind = Utils.resolveColor(this, tx.isPay()
                ? R.attr.colorSentTx : R.attr.colorReceivedTx);
        final int status = tx.getBlockHeight() >= 0 ? tx.isConfirm()
                ? R.string.confirmed_status_text
                : R.string.unconfirmed_status_text
                : R.string.mempool_status_text;

        this.<TextView>requireView(R.id.mTxId).setText(tx.getID());
        this.<TextView>requireView(R.id.mTxAmount).setText(asset.toStringFriendly(tx.getAmount()));
        this.<TextView>requireView(R.id.mTxAmount).setTextColor(colorTxKind);
        this.<TextView>requireView(R.id.mTxFiatAmount).setText(String.format("%s %s",
                ALMOST_EQUAL_TO, fiatAsset.toStringFriendly(tx.getFiatAmount())));
        this.<ImageView>requireView(R.id.mTxIcon).setImageResource(tx.getWallet().getIcon());
        this.<TextView>requireView(R.id.mTxDatetime).setText(Utils.toLocalDatetimeString(
                tx.getTime(), getString(R.string.today_text), getString(R.string.yesterday_text)));
        this.<TextView>requireView(R.id.mTxFee).setText(asset.toStringFriendly(tx.getNetworkFee()));
        this.<TextView>requireView(R.id.mTxSize).setText(Utils.toSizeFriendlyString(tx.getSize()));
        this.<TextView>requireView(R.id.mTxStatus).setText(status);

        if (tx.getBlockHeight() < 0)
            this.requireView(R.id.mTxBlockInfo).setVisibility(View.GONE);
        else {

            this.<TextView>requireView(R.id.mTxBlockHash).setText(tx.getBlockHash());
            this.<TextView>requireView(R.id.mTxBlockHeight).setText(
                    formatter.format(tx.getBlockHeight()));
            this.<TextView>requireView(R.id.mTxConfirmations).setText(
                    formatter.format(tx.getConfirmations()));
        }
    }

    /**
     * Este método es llamado cuando se presiona el botón "Entradas". Se muestra un cuadro de
     * diálogo que permite visualizar las direcciones origen de donde procede el monto de la
     * transacción.
     *
     * @param view Botón que invoca.
     */
    public void onClickedInputs(View view) {

    }

    /**
     * Este método es llamdo cuando se presiona el botón "Salidas". Se muestra un cuadro de dialogo
     * que permite visualizar las direcciones destino a donde se envía el monto de la transacción.
     *
     * @param view Botón que invoca.
     */
    public void onClickedOutputs(View view) {

    }
}
