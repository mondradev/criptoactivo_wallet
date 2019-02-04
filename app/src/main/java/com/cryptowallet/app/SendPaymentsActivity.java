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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.security.Security;
import com.cryptowallet.security.TimeBasedOneTimePassword;
import com.cryptowallet.utils.DecimalsFilter;
import com.cryptowallet.utils.OnAfterTextChangedListenerBase;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.InSufficientBalanceException;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletServiceBase;
import com.cryptowallet.wallet.coinmarket.ExchangeService;
import com.cryptowallet.wallet.coinmarket.coins.CoinBase;
import com.cryptowallet.wallet.coinmarket.coins.CoinFactory;
import com.cryptowallet.wallet.coinmarket.exchangeables.ExchangeableBase;
import com.google.common.base.Strings;
import com.squareup.okhttp.internal.NamedRunnable;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.security.GeneralSecurityException;
import java.security.KeyException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Actividad que permite el envío de pagos, también ofrece leer códigos QR que almacenan un URI
 * válido.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class SendPaymentsActivity extends ActivityBase
        implements AdapterView.OnItemSelectedListener {

    /**
     * Etiqueta de la clase.
     */
    private static final String TAG = "SendPayment";

    /**
     * Opciones disponibles de comisión.
     */
    private String[] mFeeOptions;

    /**
     * Indica la cuota que se paga por kilobyte al realizar la transacción.
     */
    private CoinBase mFeePerKb;

    /**
     * Indica el monto que se enviará.
     */
    private CoinBase mAmount;

    /**
     * Indica la moneda a enviar.
     */
    private SupportedAssets mSelectCoin;

    /**
     * Dirección de destino.
     */
    private String mAddress;
    /**
     * Indica si el pago es fuera de la aplicación.
     */
    private boolean mOutOfTheApp = true;
    /**
     * La dirección que se reconoció como perteneciente a la aplicación.
     */
    private String mAddressOnApp;

    private boolean mDisableListener;

    /**
     * Permite actualizar los campos de mRemainingBalance y mAmount.
     */
    private final OnAfterTextChangedListenerBase mUpdateContent = new OnAfterTextChangedListenerBase() {

        /**
         * Este método es llamado cuando mAmount es modificado.
         * @param s Cadena que representa la cantidad a enviar.
         */
        @Override
        public void afterTextChanged(Editable s) {
            fillFiat(s.toString());
            updateFeeAndRemaining();
            validateFundsAndAddress(s.toString());
        }

        private void fillFiat(String src) {

            if (mDisableListener) {
                mDisableListener = false;
                return;
            }

            ExchangeableBase service = ExchangeService.get().getExchange(mSelectCoin);

            SupportedAssets asset =
                    AppPreference.getSelectedCurrency(SendPaymentsActivity.this);

            String value = service.convertTo(asset, CoinFactory.parse(mSelectCoin, src))
                    .toPlainString();

            mDisableListener = true;
            ((EditText) findViewById(R.id.mAmountSendPaymentFiatEdit)).setText(value);
        }
    };


    /**
     * Permite actualizar los campos de mRemainingBalance y mAmount.
     */
    private final OnAfterTextChangedListenerBase mUpdateFiatContent
            = new OnAfterTextChangedListenerBase() {

        /**
         * Este método es llamado cuando mAmount es modificado.
         *
         * @param s Cadena que representa la cantidad a enviar.
         */
        @Override
        public void afterTextChanged(Editable s) {
            fillCrypto(s.toString());
        }

        /**
         * @param src
         */
        private void fillCrypto(String src) {
            if (mDisableListener) {
                mDisableListener = false;
                return;
            }

            ExchangeableBase service = ExchangeService.get().getExchange(mSelectCoin);

            SupportedAssets asset =
                    AppPreference.getSelectedCurrency(SendPaymentsActivity.this);

            String value = service.convertFrom(CoinFactory.parse(asset, src)).toPlainString();

            mDisableListener = true;
            ((EditText) findViewById(R.id.mAmountSendPaymentEdit)).setText(value);
        }

    };

    /**
     * Obtiene un valor que indica si el pago se hará fuera de la aplicación.
     *
     * @param token   Etiqueta con token a validar.
     * @param asset   Activo a la cual pertenece la dirección.
     * @param address Dirección destino.
     * @return Un valor true si el pago es fuera de la billetera.
     */
    private static boolean isOutOfTheApp(String token, SupportedAssets asset, String address) {

        if (Strings.isNullOrEmpty(token))
            return true;

        String sha256Token = WalletServiceBase.generatePaymentToken(asset, address);

        return !sha256Token.contentEquals(token);
    }

    /**
     * Valida los fondos y la dirección especificada.
     */
    private void validateFundsAndAddress(String amountStr) {
        TextView mAddressRecipient = findViewById(R.id.mAddressRecipientText);
        TextView mAmountText = findViewById(R.id.mAmountSendPaymentEdit);

        Button mSendPayment = findViewById(R.id.mSendPayment);

        mSendPayment.setEnabled(false);

        boolean canSend = true;

        if (!validateAddress(mAddressRecipient.getText().toString())) {
            if (!mAddressRecipient.getText().toString().isEmpty())
                mAddressRecipient.setError(getString(R.string.address_error));
            canSend = false;
        } else {
            mAddressRecipient.setError(null);
            mAddress = mAddressRecipient.getText().toString();

            Log.v(TAG, "Dirección elegida: " + mAddress);

            if (!Strings.isNullOrEmpty(mAddressOnApp))
                mOutOfTheApp = !mAddress.contentEquals(mAddressOnApp);
        }

        if (Strings.isNullOrEmpty(amountStr))
            amountStr = "0";

        CoinBase amount = CoinFactory.parse(mSelectCoin, amountStr);
        CoinBase total = getBalance();

        if (total.substract(amount).substract(mFeePerKb).isNegative()) {
            mAmountText.setError(getString(R.string.no_enought_funds));
            canSend = false;
        } else
            mAmountText.setError(null);

        if (canSend)
            mSendPayment.setEnabled(true);

    }

    /**
     * Valida la dirección especificada para el envío.
     *
     * @param addressBase58 Dirección de destino.
     */
    private boolean validateAddress(String addressBase58) {
        switch (mSelectCoin) {
            case BTC:
                return BitcoinService.get().isValidAddress(addressBase58);
        }

        return false;
    }

    /**
     * Este método se ejecuta cuando se crea la actividad.
     *
     * @param savedInstanceState Instancia almacena el estado de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_payments);

        mSelectCoin = SupportedAssets.valueOf(getIntent()
                .getStringExtra(ExtrasKey.SELECTED_COIN));

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.send_text));

        mFeeOptions = new String[]{
                getString(R.string.regular_fee_text),
                getString(R.string.priority_fee_text)
        };

        Spinner mFeeSelector = findViewById(R.id.mFeeSelector);
        mFeeSelector.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                mFeeOptions
        ));

        SupportedAssets asset = AppPreference.getSelectedCurrency(this);

        mFeeSelector.setOnItemSelectedListener(this);
        onSelectedFee(getString(R.string.regular_fee_text));

        TextView mRemainingText = findViewById(R.id.mRemainingBalanceText);
        TextView mBalanceText = findViewById(R.id.mBalanceTextSendPayments);
        TextView mBalanceFiatText = findViewById(R.id.mBalanceFiatTextSendPayments);

        mRemainingText.setText(getBalance().substract(mFeePerKb).toStringFriendly());
        mBalanceText.setText(getBalance().toStringFriendly());
        mBalanceFiatText.setText(ExchangeService.get().getExchange(mSelectCoin)
                .convertTo(asset, getBalance()).toStringFriendly());

        EditText mAddressRecipient = findViewById(R.id.mAddressRecipientText);
        EditText mAmount = findViewById(R.id.mAmountSendPaymentEdit);
        EditText mAmountFiat = findViewById(R.id.mAmountSendPaymentFiatEdit);

        mAddressRecipient.addTextChangedListener(mUpdateContent);
        mAmount.addTextChangedListener(mUpdateContent);
        mAmount.setFilters(new InputFilter[]{new DecimalsFilter(16, 8)});
        mAmountFiat.addTextChangedListener(mUpdateFiatContent);
        mAmountFiat.setFilters(new InputFilter[]{new DecimalsFilter(16, 2)});


        mAmountFiat.setHint(getString(R.string.fiat_sample_text, asset));

        ((TextView) findViewById(R.id.mAmountFiatCaptionText))
                .setText(getString(R.string.amount_text, asset));
        ((TextView) findViewById(R.id.mAmountCaptionText))
                .setText(getString(R.string.amount_text, mSelectCoin));

        if (Strings.isNullOrEmpty(AppPreference.getSecretPhrase(this))) {
            findViewById(R.id.mSend2faCodeCaption).setVisibility(View.GONE);
            findViewById(R.id.mSend2faCode).setVisibility(View.GONE);
        }
    }

    /**
     * Obtiene el saldo actual de la billetera.
     *
     * @return El saldo actual.
     */
    private CoinBase getBalance() {
        switch (mSelectCoin) {
            case BTC:
                return BitcoinService.get().getBalance();
        }
        return CoinFactory.getZero(mSelectCoin);
    }

    /**
     * Este método es llamado cuando se selecciona un tipo de comisión.
     *
     * @param fee Comisión seleccionada.
     */
    public void onSelectedFee(String fee) {
        String regular = getString(R.string.regular_fee_text);
        String priority = getString(R.string.priority_fee_text);

        if (fee.contentEquals(regular))
            mFeePerKb = CoinFactory.valueOf(mSelectCoin, 10000);
        else if (fee.contentEquals(priority))
            mFeePerKb = CoinFactory.valueOf(mSelectCoin, 43000);

        updateFeeAndRemaining();
        validateFundsAndAddress(
                ((EditText) findViewById(R.id.mAmountSendPaymentEdit)).getText().toString());
    }

    /**
     * Actualiza los valores por comisión y saldo restante.
     */
    public void updateFeeAndRemaining() {
        TextView mAmountText = findViewById(R.id.mAmountSendPaymentEdit);
        CharSequence amountStr = mAmountText.getText();

        mAmount = CoinFactory.parse(mSelectCoin, amountStr.toString());

        String feePerKb = getFeeForSendOutApp().add(mFeePerKb).toStringFriendly();
        String remainingBalance =
                getBalance().substract(mAmount).substract(mFeePerKb).toStringFriendly();

        TextView mRemainingBalance = findViewById(R.id.mRemainingBalanceText);
        TextView mFeePerKb = findViewById(R.id.mCurrentSelectedFeeText);

        mRemainingBalance.setText(remainingBalance);
        mFeePerKb.setText(feePerKb);
    }

    /**
     * Obtiene la comisión si se hace un envío fuera de la aplicación.
     *
     * @return Una comisión de envío.
     */
    private CoinBase getFeeForSendOutApp() {
        if (!Strings.isNullOrEmpty(mAddressOnApp)
                && !Strings.isNullOrEmpty(mAddress)
                && mAddress.contentEquals(mAddressOnApp))
            return CoinFactory.getZero(mSelectCoin);

        return WalletServiceBase.getFeeForSendOutApp(mSelectCoin);
    }

    /**
     * Este método es llamado cuando se hace clic al componente mQrReaderIcon.
     *
     * @param view Componente que desencadena el evento Click.
     */
    public void handlerScanQr(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Utils.showSnackbar(findViewById(R.id.mSendPayment),
                    getString(R.string.require_camera_permission));
            return;
        }
        Intent intent = new Intent(this, ScanQRActivity.class);
        startActivityForResult(intent, 0);
    }

    /**
     * Este método es llamado cuando se selecciona un elemento del <code>Spinner</code>
     *
     * @param parent   AdapterView en el cual se hizo clic.
     * @param view     Componente que contiene el AdapterView.
     * @param position Posición del componente en el adaptador.
     * @param id       El ID fila del elemento al cual se hizo clic.
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position >= 0 && position < mFeeOptions.length)
            onSelectedFee(mFeeOptions[position]);
    }

    /**
     * Este método es llamado cuando no se hace selección del elemento.
     *
     * @param parent AdapterView en el cual se hizo clic.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        onSelectedFee(getString(R.string.regular_fee_text));
    }

    /**
     * Este método es llamado cuando se hace clic sobre el botón enviar.
     *
     * @param view Componente el cual desencadeno el evento Click.
     */
    public void handlerSendPayment(View view) {
        doPay();
    }

    /**
     * Efectua el pago a realizar y finaliza la actividad.
     */
    private void doPay() {
        EditText code2Fa = findViewById(R.id.mSend2faCode);

        if (code2Fa.getVisibility() == View.VISIBLE) {
            String codeStr = code2Fa.getText().toString();

            if (Strings.isNullOrEmpty(codeStr)) {
                code2Fa.setError(getString(R.string.error_2fa_code));
                return;
            }

            int code = Integer.parseInt(codeStr);

            try {
                String secret = AppPreference.getSecretPhrase(this);
                secret = Security.decryptAES(secret);

                boolean valid = TimeBasedOneTimePassword.validateCurrentNumber(
                        secret,
                        code,
                        0
                );

                if (!valid) {
                    code2Fa.setError(getString(R.string.error_2fa_code));
                    return;
                }

            } catch (GeneralSecurityException ignored) {
            }
        }

        final AuthenticateDialog dialog = new AuthenticateDialog()
                .setMode(AuthenticateDialog.AUTH)
                .setWallet(BitcoinService.get());

        Executor executor = Executors.newSingleThreadExecutor();

        executor.execute(new NamedRunnable("AuthenticateToPayment") {
            @Override
            protected void execute() {
                try {
                    switch (mSelectCoin) {
                        case BTC:
                            BitcoinService.get().sendPayment(mAddress, mAmount, mFeePerKb,
                                    mOutOfTheApp, () -> {
                                        dialog.show(SendPaymentsActivity.this);

                                        byte[] authData = null;
                                        try {
                                            authData = dialog.getAuthData();
                                        } catch (InterruptedException ignored) {
                                        }
                                        if (!Utils.isNull(authData))
                                            dialog.showUIProgress(
                                                    getString(R.string.sending_payment));

                                        return authData;
                                    });
                            break;
                    }

                    runOnUiThread(() -> {
                        Intent intent = new Intent();
                        intent.putExtra(
                                ExtrasKey.OP_ACTIVITY, ActivitiesOperation.SEND_PAYMENT.name());
                        intent.putExtra(ExtrasKey.SELECTED_COIN, mSelectCoin.name());
                        intent.putExtra(ExtrasKey.SEND_AMOUNT, mAmount.getValue());
                        setResult(Activity.RESULT_OK, intent);

                        finish();
                    });
                } catch (InSufficientBalanceException ex) {
                    String require = ex.getRequireAmount().toStringFriendly();

                    Utils.showSnackbar(findViewById(R.id.mSendPayment),
                            getString(R.string.insufficient_money, require));
                } catch (KeyException ex) {
                    Utils.showSnackbar(findViewById(R.id.mSendPayment),
                            getString(R.string.error_pin));
                } finally {
                    if (dialog.isShowing())
                        dialog.dismiss();
                }
            }
        });


    }

    /**
     * Este método es llamado cuando se invoca una actividad y está devuelve un valor. Se captura la
     * información leída por el lector de código QR y se interpreta para generar el pago.
     *
     * @param requestCode Código de peticón.
     * @param resultCode  Código de resultado.
     * @param data        Información devuelta.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Button mSendPayment = findViewById(R.id.mSendPayment);
        TextView mAddressRecipient = findViewById(R.id.mAddressRecipientText);
        TextView mAmountText = findViewById(R.id.mAmountSendPaymentEdit);

        if (data != null) {

            if (!data.hasExtra(ExtrasKey.ASSET_DATA_TO_SEND)) {
                Utils.showSnackbar(mSendPayment, getString(R.string.address_error));
            } else {
                try {
                    String uri = data.getStringExtra(ExtrasKey.ASSET_DATA_TO_SEND);

                    BitcoinURI uriParsed = new BitcoinURI(uri);

                    if (uriParsed.getAddress() != null
                            && validateAddress(uriParsed.getAddress().toBase58()))
                        mAddressRecipient.setText(uriParsed.getAddress().toBase58());

                    if (uriParsed.getAmount() != null)
                        mAmountText.setText(uriParsed.getAmount().toPlainString());

                    if (uriParsed.getParameterByName("token") != null) {
                        mOutOfTheApp = isOutOfTheApp(
                                uriParsed.getParameterByName("token").toString(),
                                mSelectCoin,
                                uriParsed.getAddress().toBase58()
                        );

                        if (!mOutOfTheApp) {
                            mAddressOnApp = uriParsed.getAddress().toBase58();

                            Log.d(TAG, "Token encontrado: "
                                    + uriParsed.getParameterByName("token").toString());
                        }
                    }

                } catch (BitcoinURIParseException e) {

                    try {
                        Address addressRead = Address.fromBase58(
                                BitcoinService.NETWORK_PARAMS,
                                data.getStringExtra(ExtrasKey.ASSET_DATA_TO_SEND)
                        );

                        mAddressRecipient.setText(addressRead.toBase58());
                    } catch (AddressFormatException ignored) {
                        Utils.showSnackbar(mSendPayment, getString(R.string.qr_reader_error)
                                + ": " + data.getStringExtra(ExtrasKey.ASSET_DATA_TO_SEND));
                    }
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
