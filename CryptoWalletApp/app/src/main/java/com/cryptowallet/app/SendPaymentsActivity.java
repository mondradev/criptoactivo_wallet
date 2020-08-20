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

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.cryptowallet.Constants;
import com.cryptowallet.R;
import com.cryptowallet.app.authentication.IAuthenticationSucceededCallback;
import com.cryptowallet.app.authentication.TwoFactorAuthentication;
import com.cryptowallet.app.fragments.FailMessageFragment;
import com.cryptowallet.app.fragments.SuccessfulPaymentFragment;
import com.cryptowallet.app.fragments.TwoFactorAuthenticationFragment;
import com.cryptowallet.utils.Consumer;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.utils.inputfilters.DecimalsFilter;
import com.cryptowallet.utils.textwatchers.IAfterTextChangedListener;
import com.cryptowallet.wallet.AbstractWallet;
import com.cryptowallet.wallet.IFees;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.exceptions.InvalidAmountException;
import com.cryptowallet.wallet.exceptions.InvalidFeeException;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.internal.CheckableImageButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.base.Strings;

import org.checkerframework.common.value.qual.IntVal;

import java.util.Objects;

/**
 * Actividad que permite el envío de pagos, también ofrece leer códigos QR que almacenan un URI
 * válido.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.2
 */
public class SendPaymentsActivity extends LockableActivity {

    /**
     * Comisión de envío promedio.
     */
    private static final int FEE_NORMAL = 0;

    /**
     * Comisión de envío rápido.
     */
    private static final int FEE_FAST = 1;

    /**
     * Comisión personalizada de envío.
     */
    private static final int FEE_CUSTOM = 2;

    /**
     * Petición de escaneo de código QR.
     */
    private static final int SCAN_QR_REQUEST = 3;

    /**
     * Petición de permiso de uso de cámara.
     */
    private static final int PERMISSION_CAMERA_REQUEST = 4;

    /**
     * Permiso de la cámara.
     */
    private static final String[] PERMISSION_CAMERA = {Manifest.permission.CAMERA};

    /**
     * Tipo de comisión seleccionada.
     */
    @IntVal({FEE_NORMAL, FEE_FAST, FEE_CUSTOM})
    private int mFeeType;

    /**
     * Indica si se está visualizando cantidades en dinero fiduciario.
     */
    private boolean mIsFiat;

    /**
     * Indica si hay un error de saldo.
     */
    private boolean mHasEnoughtBalanceError;

    /**
     * Indica si hay un error en la dirección.
     */
    private boolean mHasValidAddressError;

    /**
     * Billetera que realiza el envío.
     */
    private AbstractWallet mWallet;

    /**
     * Handler del proceso principal.
     */
    private Handler mHandler;

    /**
     * Cuadro de texto para cantidad.
     */
    private EditText mSendAmountText;

    /**
     * Cuadro de texto para dirección.
     */
    private EditText mSendAddressText;

    /**
     * Contenedor del cuadro de texto para la dirección.
     */
    private TextInputLayout mSendAmountLayout;

    /**
     * Contenedor del cuadro de texto para la cantidad.
     */
    private TextInputLayout mSendAddressLayout;

    /**
     * Total de comisión a pagar.
     */
    private TextView mSendTotalFeeText;

    /**
     * Saldo actual de la billetera.
     */
    private TextView mSendCurrentBalanceText;

    /**
     * Total a pagar.
     */
    private TextView mSendTotalPayText;

    /**
     * Cuadro de texto para la comisión personalizada.
     */
    private EditText mSendFeeCustomText;

    /**
     * Contenedor del cuadro de texto para la comisión personalizada.
     */
    private TextInputLayout mSendFeeCustomLayout;

    /**
     * Consumidor del evento de saldo ha cambiado.
     */
    private Consumer<AbstractWallet> mOnBalanceChangedConsumer;

    /**
     * Receptor del evento {@link Constants#NEW_TRANSACTION}
     */
    private BroadcastReceiver mPriceReceiver = new BroadcastReceiver() {
        /**
         * This method is called when the BroadcastReceiver is receiving an Intent
         * broadcast.
         *
         * @param context The Context in which the receiver is running.
         * @param intent  The Intent being received.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.post(() -> mOnBalanceChangedConsumer.accept(mWallet));
        }
    };

    /**
     * Indica si existe una petición de permisos.
     */
    private boolean mOnRequestPermission;

    /**
     * Este método se ejecuta cuando se crea la actividad.
     *
     * @param savedInstanceState Instancia almacena el estado de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_send_payments);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.send_text));

        final String assetName = getIntent().getStringExtra(Constants.EXTRA_CRYPTO_ASSET);
        final SupportedAssets asset = SupportedAssets.valueOf(assetName);
        final int size = (int) Math.log10(asset.getUnit());

        mOnBalanceChangedConsumer = value -> updateInfo();
        mFeeType = FEE_NORMAL;
        mHandler = new Handler(Looper.getMainLooper());
        mWallet = getWalletService().get(asset);

        mSendAmountLayout = this.requireView(R.id.mSendAmount);
        mSendAmountText = Objects.requireNonNull(mSendAmountLayout.getEditText());
        mSendAddressLayout = this.requireView(R.id.mSendToAddress);
        mSendAddressText = Objects.requireNonNull(mSendAddressLayout.getEditText());
        mSendFeeCustomLayout = this.requireView(R.id.mSendFeeCustom);
        mSendFeeCustomText = Objects.requireNonNull(mSendFeeCustomLayout.getEditText());
        mSendCurrentBalanceText = this.requireView(R.id.mSendCurrentBalance);
        mSendTotalPayText = this.requireView(R.id.mSendTotalPay);
        mSendTotalFeeText = this.requireView(R.id.mSendTotalFee);

        mSendFeeCustomLayout.setHint(getString(R.string.fee_by_kb_pattern, asset.name()));
        mSendAmountLayout.setHint(getString(R.string.amount_pattern, asset.name()));

        mSendAmountLayout.setEndIconOnClickListener(this::onCurrencyChanged);
        mSendAddressLayout.setEndIconOnClickListener(this::onScan);

        mWallet.addBalanceChangedListener(mHandler::post, mOnBalanceChangedConsumer);
        mSendAddressText.addTextChangedListener((IAfterTextChangedListener) this::onAddressChanged);
        mSendAmountText.addTextChangedListener((IAfterTextChangedListener) this::onAmountChanged);
        mSendFeeCustomText.addTextChangedListener((IAfterTextChangedListener) this::onAmountChanged);

        this.<MaterialButtonToggleGroup>requireView(R.id.mSendFeeSelector)
                .addOnButtonCheckedListener(this::onFeeChanged);

        InputFilter[] feeCustomFilters = {new DecimalsFilter(22, size)};
        mSendFeeCustomText.setFilters(feeCustomFilters);

        IntentFilter filter = new IntentFilter(Constants.UPDATED_FIAT_BALANCE);
        filter.addAction(Constants.UPDATED_PRICE);

        registerReceiver(mPriceReceiver, filter);

        updateFilters();
        updateInfo();
    }

    /**
     * Llama la actividad principal requiriendo que esta sea bloqueada.
     */
    @Override
    protected void onResumeApp() {
        if (!mOnRequestPermission)
            super.onResumeApp();

        mOnRequestPermission = false;
    }

    /**
     * Este método es llamado cuando se sale de la aplicación.
     */
    @Override
    public void onLeaveApp() {
        super.onLeaveApp();

        if (!mOnRequestPermission) return;

        stopTimer();
        unlockApp();
    }

    /**
     * Este método es llamado cuando la actividad es destruída.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        mWallet.removeBalanceChangedListener(mOnBalanceChangedConsumer);
        unregisterReceiver(mPriceReceiver);
    }

    /**
     * Este método es llamado cuando se busca escanear un código QR, esto debido a presionar el
     * botón "qr_scan".
     *
     * @param view Botón que invoca el método.
     */
    public void onScan(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
            Snackbar.make(requireView(R.id.mSendPayButton),
                    getString(R.string.camera_permission_error),
                    Snackbar.LENGTH_SHORT
            ).setAction(R.string.request_text,
                    v -> {
                        mOnRequestPermission = true;
                        ActivityCompat.requestPermissions(this,
                                PERMISSION_CAMERA, PERMISSION_CAMERA_REQUEST);
                    }
            ).show();
        else
            startActivityForResult(new Intent(this, ScanQrActivity.class),
                    SCAN_QR_REQUEST);
    }

    /**
     * Obtiene la divisa que se está visualizando en el formulario de envío.
     *
     * @return Divisa criptoactivo o fiat.
     */
    private SupportedAssets getCurrency() {
        return mIsFiat ? Preferences.get().getFiat() : mWallet.getCryptoAsset();
    }

    /**
     * Obtiene el saldo de la billetera expresado en la divisa visualizada en el formulario.
     *
     * @return Saldo de la billetera.
     */
    private long getBalance() {
        return mIsFiat ? getWalletService().getFiatBalance(mWallet.getCryptoAsset())
                : mWallet.getBalance();
    }

    /**
     * Actualiza los valores del saldo y la divisa del envío.
     */
    private void updateInfo() {
        SupportedAssets asset = getCurrency();
        long balance = getBalance();

        mSendCurrentBalanceText.setText(asset.toStringFriendly(balance, false));
        checkEnoughtBalance();
    }

    /**
     * Parsea el valor del texto y si está activa la bandera {@link #mIsFiat}, hace la conversión a
     * el criptoactivo.
     *
     * @param value Valor con un formato válido para convertir a número.
     * @return Valor en criptoactivo.
     */
    private long parseToCrypto(String value) {
        final long amount = (long) (Utils.parseDouble(value) * mWallet.getCryptoAsset().getUnit());

        long lastPrice = getWalletService().getLastPrice(mWallet.getCryptoAsset());

        if (mIsFiat && lastPrice == 0) return 0;

        return mIsFiat
                ? amount / lastPrice : amount;
    }

    /**
     * Verifica si el saldo es suficiente para realizar el envío.
     */
    private void checkEnoughtBalance() {
        final SupportedAssets asset = getCurrency();
        final long cryptoAmount = parseToCrypto(mSendAmountText.getText().toString());
        final String address = mSendAddressText.getText().toString();

        try {
            long fee = 0;
            long total = 0;

            if (cryptoAmount > 0 && mWallet.isValidAddress(address)) {
                mWallet.isValidAmount(cryptoAmount, true);

                fee = calculateTotalFee(address, cryptoAmount, getFee());
                total = cryptoAmount + fee;
            }

            mSendAmountLayout.setError(null);
            mSendFeeCustomLayout.setError(null);
            mHasEnoughtBalanceError = false;

            final long lastPrice = getWalletService().getLastPrice(mWallet.getCryptoAsset());

            mSendTotalFeeText.setText(asset.toStringFriendly(
                    mIsFiat ? fee * lastPrice : fee, false));

            mSendTotalPayText.setText(asset.toStringFriendly(
                    mIsFiat ? total * lastPrice : total, false));

        } catch (InvalidFeeException ex) {
            mSendFeeCustomLayout.setError(ex.getMessageRes(getResources()));
            mHasEnoughtBalanceError = true;
        } catch (InvalidAmountException ex) {
            setEnoughtBalanceError(ex.getMessageRes(getResources()));
        }

        canPay();
    }

    /**
     * Indica que existe un error en el saldo utilizado por la transacción.
     *
     * @param error Error a mostrar en el cuadro de texto.
     */
    private void setEnoughtBalanceError(String error) {
        mSendAmountLayout.setError(error);
        mHasEnoughtBalanceError = true;
    }

    /**
     * Determina si se cumple con todos los criterios para realizar el pago.
     */
    private void canPay() {
        this.requireView(R.id.mSendPayButton)
                .setEnabled(!mHasEnoughtBalanceError && !mHasValidAddressError);
    }

    /**
     * Este método es invocado cuando cambia la divisa visualiza en el formulario, esto debido que
     * fue presionado el botón de "cash".
     *
     * @param view Botón que invoca el método.
     */
    private void onCurrencyChanged(View view) {
        Utils.tryNotThrow(() -> CheckableImageButton.class
                .getMethod("setChecked", boolean.class).invoke(view, !mIsFiat));

        SupportedAssets prevCurrency = getCurrency();

        mIsFiat = !mIsFiat;

        final double amountValue = Utils.parseDouble(mSendAmountText.getText().toString())
                * prevCurrency.getUnit();
        final long amount = normalizeValue(Math.round(amountValue));

        String amountText = amount > 0
                ? getCurrency().toPlainText(amount, false, false)
                : "";

        updateFilters();

        mSendAmountLayout.setHint(getString(R.string.amount_pattern, getCurrency().name()));
        mSendAmountText.setText(amountText);
        mSendAmountText.setSelection(amountText.length());

        updateInfo();
    }

    /**
     * Convierte el valor expresado en fiat a criptomoneda o viceversa dependiendo de
     * {@link #mIsFiat}.
     *
     * @param amount Cantidad a expresar.
     * @return Cantidad convertida.
     */
    private long normalizeValue(long amount) {
        final long lastPrice = getWalletService().getLastPrice(mWallet.getCryptoAsset());

        if (amount == 0 || (lastPrice == 0 && mIsFiat)) return 0;

        return mIsFiat ? amount * lastPrice / mWallet.getCryptoAsset().getUnit()
                : mWallet.getCryptoAsset().getUnit() * amount / lastPrice;
    }

    /**
     * Cambia el filtrado de la entrada respecto al tipo de divisa visualizada.
     */
    private void updateFilters() {
        final SupportedAssets asset = mIsFiat ? Preferences.get().getFiat() : mWallet.getCryptoAsset();
        final int size = (int) Math.log10(asset.getUnit());

        InputFilter[] filters = {new DecimalsFilter(20, size)};

        mSendAmountText.setFilters(filters);
    }

    /**
     * Este método es invocado cuando la dirección de envío es modificada.
     *
     * @param address Dirección nueva.
     */
    private void onAddressChanged(Editable address) {
        mHasValidAddressError = !mWallet.isValidAddress(address.toString());

        if (mHasValidAddressError && !Strings.isNullOrEmpty(address.toString()))
            mSendAddressLayout.setError(getString(R.string.error_invalid_address));
        else
            mSendAddressLayout.setError(null);

        canPay();
    }


    /**
     * Este método es invocado cuando la cantidad a enviar es modificada.
     *
     * @param ignored Cantidad nueva (ignorada en el método).
     */
    private void onAmountChanged(Editable ignored) {
        checkEnoughtBalance();
    }

    /**
     * Este método es invoca cuando la opción del tipo de comisión es cambiada.
     *
     * @param group     Grupo de botones.
     * @param checkedId Identificador del botón que esta cambiando su estado "checked".
     * @param isChecked Indica si el botón está seleccionado ("checked").
     */
    private void onFeeChanged(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
        if (!isChecked) return;

        mSendFeeCustomLayout.setVisibility(checkedId == R.id.mSendFeeCustomButton
                ? View.VISIBLE : View.GONE);

        switch (checkedId) {
            case R.id.mSendFeeNormalButton:
                mFeeType = FEE_NORMAL;
                break;
            case R.id.mSendFeeFastButton:
                mFeeType = FEE_FAST;
                break;
            case R.id.mSendFeeCustomButton:
                mFeeType = FEE_CUSTOM;
                break;
        }

        checkEnoughtBalance();
    }

    /**
     * Calcula la comisión de la transacción según el tipo especificado y el peso en KB de los datos.
     *
     * @param address Dirección destino del envío.
     * @param amount  Cantidad a enviar.
     * @param feeByKB Comisión por kilobyte.
     * @return Comisión por realizar la transacción.
     */
    private long calculateTotalFee(String address, long amount, long feeByKB) {
        if (Strings.isNullOrEmpty(address) || amount == 0)
            return 0;

        ITransaction tx = mWallet.createTx(address, amount, feeByKB);

        if (tx == null)
            return 0;

        return tx.getFee();
    }

    /**
     * Obtiene la comisión a pagar por la transacción.
     *
     * @return Comisión por kilobyte.
     */
    private long getFee() {
        IFees fees = mWallet.getCurrentFees();

        switch (mFeeType) {
            case FEE_NORMAL:
                return fees.getAverage();
            case FEE_FAST:
                return fees.getFaster();
            case FEE_CUSTOM:
                return Utils.parseInt(mSendFeeCustomText.getText().toString());
        }

        return 0;
    }

    /**
     * Se llama a este método cada vez que se selecciona un elemento en su menú de opciones.
     *
     * @param item El elemento del menú que fue seleccionado.
     * @return boolean Un valor true si fue procesado.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        setResult(RESULT_CANCELED);
        finish();

        return true;
    }

    /**
     * Este método es invocado después de responder a una petición de solicitud de permisos
     * permitiendo capturar la respuesta de cada permiso.
     *
     * @param requestCode  Código de la solicitud que ha respondido.
     * @param permissions  Permisos que fueron solicitados.
     * @param grantResults Autorización de cada permiso solicitado.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CAMERA_REQUEST) {
            if (permissions.length != grantResults.length)
                return;

            for (int i = 0; i < permissions.length; i++)
                if (permissions[i].equals(Manifest.permission.CAMERA))
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        onScan(null);
                        return;
                    }
        }
    }

    /**
     * Este método es llamado cuando se hace clic sobre el botón enviar.
     *
     * @param view Componente el cual desencadeno el evento Click.
     */
    public void onPay(View view) {
        final String address = mSendAddressText.getText().toString();
        final long amount = parseToCrypto(mSendAmountText.getText().toString());
        final ITransaction tx = mWallet.createTx(address, amount, getFee());

        Preferences.get().authenticate(this, mHandler::post,
                (IAuthenticationSucceededCallback) authenticationToken -> {
                    if (TwoFactorAuthentication.get(getApplicationContext()).isEnabled())
                        TwoFactorAuthenticationFragment
                                .show(this, () -> sendCoins(tx, authenticationToken));
                    else
                        sendCoins(tx, authenticationToken);
                });
    }

    /**
     * Envía la transacción especificada.
     *
     * @param tx                  Transacción a enviar.
     * @param authenticationToken Token de autenticación de la billetera.
     */
    public void sendCoins(ITransaction tx, byte[] authenticationToken) {
        ProgressDialog.show(SendPaymentsActivity.this);

        final boolean sent = mWallet.sendTx(tx, authenticationToken);

        ProgressDialog.hide();

        if (sent)
            SuccessfulPaymentFragment.show(this, tx,
                    getWalletService().getLastPrice(mWallet.getCryptoAsset()));
        else
            FailMessageFragment.show(this,
                    getString(R.string.fail_to_send_tx_error));
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
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != SCAN_QR_REQUEST || resultCode != RESULT_OK || data == null)
            return;

        mSendAddressText.setText(mWallet.getAddressFromUri(data.getData()));
        mSendAmountText.requestFocus();
    }

}
