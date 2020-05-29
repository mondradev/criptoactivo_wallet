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
import android.content.Intent;
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

import com.cryptowallet.R;
import com.cryptowallet.app.authentication.IAuthenticationSucceededCallback;
import com.cryptowallet.app.fragments.CryptoAssetFragment;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.utils.inputfilters.DecimalsFilter;
import com.cryptowallet.utils.textwatchers.IAfterTextChangedListener;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.ITransactionFee;
import com.cryptowallet.wallet.IWallet;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletManager;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.internal.CheckableImageButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.base.Strings;

import org.checkerframework.common.value.qual.IntVal;

import java.text.NumberFormat;
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
     * Bytes contenidos en un KB.
     */
    private static final float KB_SIZE = 1024f;

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
    private IWallet mWallet;

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

        String assetName = getIntent().getStringExtra(CryptoAssetFragment.ASSET_KEY);
        SupportedAssets asset = SupportedAssets.valueOf(assetName);

        mFeeType = FEE_NORMAL;
        mHandler = new Handler(Looper.getMainLooper());
        mWallet = WalletManager.get(asset);

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

        mSendAmountLayout.setEndIconOnClickListener(this::onCurrencyChange);
        mSendAddressLayout.setEndIconOnClickListener(this::onScan);

        mWallet.addBalanceChangeListener(mHandler::post, this::onBalanceChange);
        mSendAddressText.addTextChangedListener((IAfterTextChangedListener) this::onAddressChange);
        mSendAmountText.addTextChangedListener((IAfterTextChangedListener) this::onAmountChange);

        this.<MaterialButtonToggleGroup>requireView(R.id.mSendFeeSelector)
                .addOnButtonCheckedListener(this::onFeeChange);

        updateFilters();
        checkEnoughtBalance();
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
                    v -> ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.CAMERA
                    }, PERMISSION_CAMERA_REQUEST)
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
        return mIsFiat ? Preferences.get().getFiat() : mWallet.getAsset();
    }

    /**
     * Obtiene el saldo de la billetera expresado en la divisa visualizada en el formulario.
     *
     * @return Saldo de la billetera.
     */
    private Float getBalance() {
        return mIsFiat ? mWallet.getFiatBalance() : mWallet.getBalance();
    }

    /**
     * Este método es invocado cuando el saldo de la billetera cambia.
     *
     * @param ignored El saldo de la billetera (ignorado en este método).
     */
    private void onBalanceChange(Float ignored) {
        updateInfo();
    }

    /**
     * Actualiza los valores del saldo y la divisa del envío.
     */
    private void updateInfo() {
        SupportedAssets asset = getCurrency();
        Float balance = getBalance();

        mSendCurrentBalanceText.setText(asset.toStringFriendly(balance));
        checkEnoughtBalance();
    }

    /**
     * Verifica si el saldo es suficiente para realizar el envío.
     */
    private void checkEnoughtBalance() {
        SupportedAssets asset = getCurrency();
        Float balance = getBalance();
        Float amount = Utils.parseFloat(mSendAmountText.getText().toString());
        Float fee = calculateTotalFee(mSendAddressText.getText().toString(), amount);
        Float total = amount + fee;

        if (mWallet.isDust(toCryptoAsset(amount)) && toCryptoAsset(amount) > 0)
            mSendAmountLayout.setError(getString(R.string.is_dust_error));
        else if (mWallet.getMaxValue() < toCryptoAsset(amount))
            mSendAmountLayout.setError(getString(R.string.max_value_error,
                    mWallet.getAsset().toStringFriendly(mWallet.getMaxValue())));
        else
            mSendAmountLayout.setError(total > balance
                    ? getString(R.string.no_enought_funds_error) : null);

        mSendTotalFeeText.setText(asset.toStringFriendly(fee));
        mSendTotalPayText.setText(asset.toStringFriendly(total));

        mHasEnoughtBalanceError = total > balance;

        canPay();
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
    private void onCurrencyChange(View view) {
        Utils.tryNotThrow(() -> CheckableImageButton.class
                .getMethod("setChecked", boolean.class).invoke(view, !mIsFiat));

        mSendAmountLayout.setHint(getString(R.string.amount_pattern, getCurrency().name()));

        if (!Strings.isNullOrEmpty(mSendAmountText.getText().toString())) {
            Float amount = Utils.parseFloat(mSendAmountText.getText().toString());
            amount = toFiat(toCryptoAsset(amount));

            String amountText = NumberFormat.getNumberInstance().format(amount);

            mSendAmountText.setText(amountText);
            mSendAmountText.setSelection(amountText.length());
        }

        mIsFiat = !mIsFiat;

        updateFilters();
        updateInfo();
    }

    /**
     * Convierte el valor expresado en fiat a su valor en criptoactivo.
     *
     * @param amount Valor en fiat.
     * @return Valor en criptoactivo.
     */
    private Float toCryptoAsset(Float amount) {
        if (!mIsFiat)
            return amount;

        return amount / mWallet.getLastPrice();
    }

    /**
     * Convierte el valor expresado en criptoactivo a su valor en fiat.
     *
     * @param amount Valor en criptoactivo.
     * @return Valor en fiat.
     */
    private Float toFiat(Float amount) {
        if (mIsFiat)
            return amount;

        return amount * mWallet.getLastPrice();
    }

    /**
     * Cambia el filtrado de la entrada respecto al tipo de divisa visualizada.
     */
    private void updateFilters() {
        final SupportedAssets asset = mIsFiat ? Preferences.get().getFiat() : mWallet.getAsset();
        final int size = asset.getSize();

        InputFilter[] filters = {new DecimalsFilter(20, size)};
        InputFilter[] feeCustomFilters = {new DecimalsFilter(22, size + 2)};

        mSendAmountText.setFilters(filters);
        mSendFeeCustomText.setFilters(feeCustomFilters);
    }

    /**
     * Este método es invocado cuando la dirección de envío es modificada.
     *
     * @param address Dirección nueva.
     */
    private void onAddressChange(Editable address) {
        mHasValidAddressError = !mWallet.validateAddress(address.toString());

        if (mHasValidAddressError)
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
    private void onAmountChange(Editable ignored) {
        checkEnoughtBalance();
    }

    /**
     * Este método es invoca cuando la opción del tipo de comisión es cambiada.
     *
     * @param group     Grupo de botones.
     * @param checkedId Identificador del botón que esta cambiando su estado "checked".
     * @param isChecked Indica si el botón está seleccionado ("checked").
     */
    private void onFeeChange(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
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
     * @return Comisión por realizar la transacción.
     */
    private Float calculateTotalFee(String address, Float amount) {
        if (Strings.isNullOrEmpty(address))
            return 0f;

        ITransaction tx = mWallet.createTx(address, amount, getFee());

        if (tx == null)
            return 0f;

        return (tx.getSize() / KB_SIZE) * getFee();
    }

    /**
     * Obtiene la comisión a pagar por la transacción.
     *
     * @return Comisión por kilobyte.
     */
    private Float getFee() {
        ITransactionFee fees = mWallet.getFees();

        switch (mFeeType) {
            case FEE_NORMAL:
                return fees.getAverage();
            case FEE_FAST:
                return fees.getFaster();
            case FEE_CUSTOM:
                return Utils.parseFloat(mSendFeeCustomText.toString());
        }

        return 0f;
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
        // TODO Validate can pay
        final String address = mSendAddressText.getText().toString();
        Float amount = Float.parseFloat(mSendAmountText.getText().toString());
        Float fee = calculateTotalFee(address, amount);

        amount = toCryptoAsset(amount);
        fee = toCryptoAsset(fee);

        Preferences.get().authenticate(this, mHandler::post,
                (IAuthenticationSucceededCallback) authenticationToken -> {
                    // TODO Show Dialog 2FA and Send Pay {address, amount, fee}
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
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != SCAN_QR_REQUEST || resultCode != RESULT_OK || data == null)
            return;

        mSendAddressText.setText(mWallet.getAddressFromUri(data.getData()));
        mSendAmountText.requestFocus();
    }

}
