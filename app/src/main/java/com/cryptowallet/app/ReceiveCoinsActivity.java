/*
 * Copyright 2019 InnSy Tech
 * Copyright 2019 Ing. Javier de Jesús Flores Mondragón
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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.DecimalsFilter;
import com.cryptowallet.utils.OnAfterTextChangedListenerBase;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletServiceBase;
import com.cryptowallet.wallet.coinmarket.coins.CoinBase;
import com.cryptowallet.wallet.coinmarket.coins.CoinFactory;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;

import java.util.Objects;

/**
 * Actividad que permite realizar petición de pagos y compartirlo a través de otras aplicaciones.
 */
public class ReceiveCoinsActivity extends ActivityBase {

    /**
     * Tamaño del código QR.
     */
    private static final int QR_CODE_SIZE = 250;

    /**
     * Dirección para la solicitud de pagos.
     */
    private String mAddress;

    /**
     * Monto de solicitar.
     */
    private CoinBase mAmount;

    /**
     * Activo seleccionado para la petición.
     */
    private SupportedAssets mSelectedAsset;

    /**
     * Cuenta regresiva para actualizar el código QR, cuando la cantidad es modificada.
     */
    private CountDownTimer mUpdateQrCountDown
            = new CountDownTimer(350, 1) {

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            handlerRefreshCode(null);
        }

    };

    /**
     * Escucha del evento AfterTextChanged, que actualiza la información de la petición.
     */
    private TextWatcher mChangedAmount = new OnAfterTextChangedListenerBase() {
        /**
         * Este método es llamado cuando se cambió el texto del EditText.
         *
         * @param s Texto final.
         */
        @Override
        public void afterTextChanged(Editable s) {
            mUpdateQrCountDown.cancel();

            EditText mAmountEdit = findViewById(R.id.mAmountToRequest);

            String amountStr = mAmountEdit.getText().toString();

            if (amountStr.isEmpty() || amountStr.contentEquals(".")) {
                mAmount = CoinFactory.getZero(mSelectedAsset);
            } else {
                mAmount = CoinFactory.parse(mSelectedAsset, mAmountEdit.getText().toString());
            }

            mUpdateQrCountDown.start();
        }
    };

    /**
     * Este método es llamado cuando la actividad es creada.
     *
     * @param savedInstanceState Almacena el estado de la instancia.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_coins);

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.receive_text));

        mSelectedAsset =
                SupportedAssets.valueOf(getIntent().getStringExtra(ExtrasKey.SELECTED_COIN));

        mAddress = getAddressRecipient();
        mAmount = CoinFactory.getZero(mSelectedAsset);

        TextView mAddressText = findViewById(R.id.mAddressToReceive);
        mAddressText.setText(mAddress);

        EditText mAmountEdit = findViewById(R.id.mAmountToRequest);
        mAmountEdit.setFilters(new InputFilter[]{new DecimalsFilter(16, 8)});
        mAmountEdit.addTextChangedListener(mChangedAmount);

        handlerRefreshCode(null);
    }

    /**
     * Comparte la petición de un pago a través de otras APPs.
     *
     * @param view Componente que desencadenó el evento Click.
     */
    public void handlerShareRequest(View view) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);

        if (mAmount.isZero())
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    getString(R.string.request_payment_template, mAddress));
        else
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    getString(R.string.request_payment_template_2,
                            mAmount.toStringFriendly(), mAddress));

        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_title)));
    }

    /**
     * Obtiene una cadena que representa la cantidad a solicitar.
     *
     * @param amount Monto a solicitar.
     * @return Una cadena del monto.
     */
    private String cointToStringFriendly(long amount) {
        switch (mSelectedAsset) {
            case BTC:
                return Coin.valueOf(amount).toFriendlyString();
        }
        return null;
    }

    /**
     * Obtiene la dirección de recepción de monedas según el activo especificado.
     *
     * @return Una dirección de recepción.
     */
    private String getAddressRecipient() {
        switch (mSelectedAsset) {
            case BTC:
                return BitcoinService.get().getReceiveAddress();
        }
        return null;
    }

    /**
     * Copia al portapapeles la dirección actual para recibir pagos.
     *
     * @param view Componente que desencadenó el evento Click.
     */
    public void handlerCopyToClipboard(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        ClipData data = ClipData.newPlainText(getString(R.string.request_payment_text),
                mAddress);
        clipboard.setPrimaryClip(data);

        Utils.showSnackbar(view, getString(R.string.copy_to_clipboard_text));
    }

    /**
     * Actualiza de manera manual el código QR para realizar la petición de cobro.
     *
     * @param view Componente que desencadenó el evento Click.
     */
    public void handlerRefreshCode(View view) {
        ImageView mQrCode = findViewById(R.id.mQrCode);
        Bitmap qrCode = Utils.generateQrCode(getUri(), QR_CODE_SIZE);
        mQrCode.setImageBitmap(qrCode);
    }

    /**
     * Obtiene una cadena que representa la petición de pago.
     *
     * @return Una instancia URI.
     */
    @NonNull
    private Uri getUri() {
        switch (mSelectedAsset) {
            case BTC:
                Address address = Address.fromBase58(BitcoinService.NETWORK_PARAMS, mAddress);
                String token = WalletServiceBase.generatePaymentToken(
                        SupportedAssets.BTC, address.toBase58());

                return Uri.parse(BitcoinURI.convertToBitcoinURI(
                        address,
                        Coin.valueOf(mAmount.getValue()),
                        null,
                        null
                ) + "&token=" + token);
        }
        return Uri.EMPTY;
    }
}
