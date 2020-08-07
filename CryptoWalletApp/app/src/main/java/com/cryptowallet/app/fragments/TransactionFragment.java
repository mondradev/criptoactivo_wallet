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

package com.cryptowallet.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.cryptowallet.Constants;
import com.cryptowallet.R;
import com.cryptowallet.app.Preferences;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.AbstractWallet;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import java.text.NumberFormat;

/**
 * Este fragmento provee de un cuadro de dialogo inferior que permite visualizar los datos de una
 * transacción.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class TransactionFragment extends BottomSheetDialogFragment {

    /**
     * TAG del fragmento.
     */
    private static final String TAG_FRAGMENT = "TransactionFragment";

    /**
     * Simbolo de valor aproximado.
     */
    private static final String ALMOST_EQUAL_TO = "≈";

    /**
     * Muestra un cuadro de diálogo inferior con los datos de recepción de la billetera.
     *
     * @param activity Actividad que invoca.
     * @param asset    Activo de la billetera.
     */
    public static void show(@NonNull FragmentActivity activity, @NonNull SupportedAssets asset,
                            @NonNull String txid) {
        if (Strings.isNullOrEmpty(txid))
            throw new IllegalArgumentException("TxId must be not null or empty");

        Bundle parameters = new Bundle();
        parameters.putCharSequence(Constants.EXTRA_CRYPTO_ASSET, asset.name());
        parameters.putCharSequence(Constants.EXTRA_TXID, txid);

        TransactionFragment fragment = new TransactionFragment();
        fragment.setArguments(parameters);
        fragment.show(activity.getSupportFragmentManager(), TAG_FRAGMENT);
    }

    /**
     * Este método es llamado se crea una nueva instancia de la vista del fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.bsd_transaction, container, false);

        if (root == null)
            throw new UnsupportedOperationException();

        final WalletProvider walletService = WalletProvider.getInstance();
        final SupportedAssets cryptoAsset
                = SupportedAssets.valueOf(requireArguments().getString(Constants.EXTRA_CRYPTO_ASSET));
        final AbstractWallet wallet = walletService.get(cryptoAsset);

        if (!wallet.isInitialized())
            wallet.loadWallet();

        final long lastPrice = walletService.getLastPrice(cryptoAsset);
        final String txid = requireArguments().getString(Constants.EXTRA_TXID);
        final ITransaction tx = wallet.findTransaction(txid);

        if (tx == null)
            throw new IllegalArgumentException("Transaction not found: " + txid);

        final NumberFormat formatter = NumberFormat.getIntegerInstance();
        final SupportedAssets criptoAsset = tx.getCryptoAsset();
        final SupportedAssets fiatAsset = Preferences.get().getFiat();

        final long fiatAmount
                = Utils.cryptoToFiat(tx.getAmount(), criptoAsset, lastPrice, fiatAsset);

        final int colorTxKind = Utils.resolveColor(requireContext(), tx.isPay()
                ? R.attr.colorSentTx : R.attr.colorReceivedTx);
        final int status = tx.getBlockHeight() >= 0 ? tx.isConfirm()
                ? R.string.confirmed_status_text
                : R.string.unconfirmed_status_text
                : R.string.mempool_status_text;

        root.<ImageView>findViewById(R.id.mTxIcon).setImageResource(tx.getWallet().getIcon());
        root.<TextView>findViewById(R.id.mTxAmount)
                .setText(criptoAsset.toStringFriendly(tx.getAmount(), false));
        root.<TextView>findViewById(R.id.mTxAmount).setTextColor(colorTxKind);
        root.<TextView>findViewById(R.id.mTxFiatAmount)
                .setText(String.format("%s %s", ALMOST_EQUAL_TO,
                        fiatAsset.toStringFriendly(fiatAmount, false)));
        root.<TextView>findViewById(R.id.mTxId).setText(tx.getID());
        root.<TextView>findViewById(R.id.mTxDatetime)
                .setText(Utils.toLocalDatetimeString(
                        tx.getTime(),
                        getString(R.string.today_text),
                        getString(R.string.yesterday_text)
                ));
        root.<TextView>findViewById(R.id.mTxFee)
                .setText(criptoAsset.toStringFriendly(tx.getFee()));
        root.<TextView>findViewById(R.id.mTxSize)
                .setText(Utils.toSizeFriendlyString(tx.getSize()));
        root.<TextView>findViewById(R.id.mTxStatus).setText(status);

        String fromAddress = tx.isCoinbase()
                ? getString(R.string.coinbase_address)
                : Joiner.on("\n").join(tx.getFromAddress());

        root.<TextView>findViewById(R.id.mTxFrom)
                .setText(fromAddress);
        root.<TextView>findViewById(R.id.mTxTo)
                .setText(Joiner.on("\n").join(tx.getToAddress()));

        if (tx.getBlockHeight() < 0)
            root.findViewById(R.id.mTxBlockInfo).setVisibility(View.GONE);
        else {
            root.<TextView>findViewById(R.id.mTxBlockHash).setText(tx.getBlockHash());
            root.<TextView>findViewById(R.id.mTxBlockHeight).setText(
                    formatter.format(tx.getBlockHeight()));
            root.<TextView>findViewById(R.id.mTxConfirmations).setText(
                    formatter.format(tx.getConfirmations()));
        }

        return root;
    }

}
