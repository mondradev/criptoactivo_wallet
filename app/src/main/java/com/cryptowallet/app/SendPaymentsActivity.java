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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.cryptowallet.utils.DecimalsFilter;
import com.cryptowallet.utils.OnAfterTextChangedListenerBase;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.InSufficientBalanceException;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletServiceBase;
import com.google.common.base.Strings;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.squareup.okhttp.internal.NamedRunnable;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

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
     * Instancia del lector de códigos QR.
     */
    private IntentIntegrator mQrReader;

    /**
     * Opciones disponibles de comisión.
     */
    private String[] mFeeOptions;

    /**
     * Indica la cuota que se paga por kilobyte al realizar la transacción.
     */
    private long mFeePerKb;

    /**
     * Indica el monto que se enviará.
     */
    private long mAmount;

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
            updateFeeAndRemaining();
            validateFundsAndAddress();
        }
    };

    /**
     * Indica que se está utilizando el lector de códigos QR.
     */
    private boolean mUseQrReader;

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
    private void validateFundsAndAddress() {
        TextView mAddressRecipient = findViewById(R.id.mAddressRecipientText);
        TextView mAmountText = findViewById(R.id.mAmountSendPaymentEdit);

        Button mSendPayment = findViewById(R.id.mSendPayment);

        mSendPayment.setEnabled(false);

        boolean canSend = true;

        String amountStr = mAmountText.getText().toString();

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

        double amount = Double.parseDouble(amountStr.length() == 0
                || amountStr.charAt(amountStr.length() - 1) == '.' ? "0" :
                Utils.coalesce(amountStr, "0"));

        if ((getBalance() - getMaxFraction(amount) - mFeePerKb) < 0) {
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

        mQrReader = new IntentIntegrator(this);
        mQrReader.setPrompt(getString(R.string.qr_reader_indications));
        mQrReader.setBarcodeImageEnabled(false);

        Spinner mFeeSelector = findViewById(R.id.mFeeSelector);
        mFeeSelector.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                mFeeOptions
        ));

        mFeeSelector.setOnItemSelectedListener(this);
        onSelectedFee(getString(R.string.regular_fee_text));

        TextView mRemainingText = findViewById(R.id.mRemainingBalanceText);
        TextView mBalanceText = findViewById(R.id.mBalanceTextSendPayments);

        mRemainingText.setText(coinToStringFriendly(getBalance() - mFeePerKb));
        mBalanceText.setText(coinToStringFriendly(getBalance()));

        EditText mAddressRecipient = findViewById(R.id.mAddressRecipientText);
        EditText mAmount = findViewById(R.id.mAmountSendPaymentEdit);

        mAddressRecipient.addTextChangedListener(mUpdateContent);
        mAmount.addTextChangedListener(mUpdateContent);
        mAmount.setFilters(new InputFilter[]{new DecimalsFilter(16, 8)});
    }

    @Override
    protected void onResume() {
        if (mUseQrReader) {
            unlockApp();
            setCanLock(true);

            mUseQrReader = false;
        }

        super.onResume();
    }

    /**
     * Obtiene el saldo actual de la billetera.
     *
     * @return El saldo actual.
     */
    private long getBalance() {

        switch (mSelectCoin) {
            case BTC:
                return BitcoinService.get().getBalance();
        }
        return 0;
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
            mFeePerKb = 10000;
        else if (fee.contentEquals(priority))
            mFeePerKb = 43000;

        updateFeeAndRemaining();
        validateFundsAndAddress();
    }

    /**
     * Actualiza los valores por comisión y saldo restante.
     */
    public void updateFeeAndRemaining() {
        TextView mAmountText = findViewById(R.id.mAmountSendPaymentEdit);
        CharSequence amountStr = mAmountText.getText();
        double amount = Double.parseDouble(amountStr.length() == 0
                || amountStr.charAt(amountStr.length() - 1) == '.' ? "0" :
                Utils.coalesce(amountStr.toString(), "0"));

        mAmount = getMaxFraction(amount);

        String feePerKb = coinToStringFriendly(mFeePerKb + getFeeForSendOutApp());
        String remainingBalance = coinToStringFriendly(
                getBalance() - mAmount - mFeePerKb - getChangingConfigurations());

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
    private long getFeeForSendOutApp() {
        if (!Strings.isNullOrEmpty(mAddressOnApp)
                && !Strings.isNullOrEmpty(mAddress)
                && mAddress.contentEquals(mAddressOnApp))
            return 0;

        return WalletServiceBase.getFeeForSendOutApp(mSelectCoin);
    }

    /**
     * Este método es llamado cuando se hace clic al componente mQrReaderIcon.
     *
     * @param view Componente que desencadena el evento Click.
     */
    public void handlerScanQr(View view) {
        mQrReader.initiateScan();
        setCanLock(false);
        mUseQrReader = true;
    }

    /**
     * Convierte la cantidad especificada en la fracción más pequeña de la moneda o token.
     *
     * @param amount Cantidad actual a convertir.
     * @return La cantidad expresada en la unidad más pequeña.
     */
    private long getMaxFraction(double amount) {
        switch (mSelectCoin) {
            case BTC:
                return (long) (amount * BitcoinService.BTC_IN_SATOSHIS);
        }

        return 0;
    }

    /**
     * Obtiene una cadena que representa la cantidad especificada en la fracción más pequeña de la
     * moneda.
     *
     * @param amount Cantidad a representar.
     * @return Una cadena que muestra la cantidad a representar.
     */
    private String coinToStringFriendly(long amount) {

        switch (mSelectCoin) {
            case BTC:
                return Coin.valueOf(amount).toFriendlyString();
        }

        return String.format("(Moneda no disponible: %s)", amount);
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
        setCanLock(false);

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
                        intent.putExtra(ExtrasKey.SEND_AMOUNT, mAmount);
                        setResult(Activity.RESULT_OK, intent);

                        finish();
                    });
                } catch (InSufficientBalanceException ex) {
                    String require = WalletServiceBase.get(mSelectCoin).getFormatter()
                            .format(ex.getRequireAmount());
                    Utils.showSnackbar(findViewById(R.id.mSendPayment),
                            getString(R.string.insufficient_money, require));
                } catch (KeyException ex) {
                    Utils.showSnackbar(findViewById(R.id.mSendPayment),
                            getString(R.string.error_pin));
                } finally {
                    if (dialog.isShowing())
                        dialog.dismiss();

                    setCanLock(true);
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
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        Button mSendPayment = findViewById(R.id.mSendPayment);
        TextView mAddressRecipient = findViewById(R.id.mAddressRecipientText);
        TextView mAmountText = findViewById(R.id.mAmountSendPaymentEdit);

        if (result != null) {

            if (result.getContents() == null) {
                Utils.showSnackbar(mSendPayment, getString(R.string.address_error));
            } else {
                try {
                    String uri = result.getContents();

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
                                BitcoinService.NETWORK_PARAMS, result.getContents());

                        mAddressRecipient.setText(addressRead.toBase58());
                    } catch (AddressFormatException ignored) {
                        Utils.showSnackbar(mSendPayment, getString(R.string.qr_reader_error)
                                + ": " + result.getContents());
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
