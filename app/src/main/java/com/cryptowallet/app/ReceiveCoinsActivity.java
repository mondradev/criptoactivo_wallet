package com.cryptowallet.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.AfterTextWatcher;
import com.cryptowallet.utils.DecimalsFilter;
import com.cryptowallet.utils.Helper;
import com.cryptowallet.wallet.SupportedAssets;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;

import java.util.Objects;

/**
 * Actividad que permite realizar petición de pagos y compartirlo a través de otras aplicaciones.
 */
public class ReceiveCoinsActivity extends AppCompatActivity {

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
    private long mAmount;

    /**
     * Activo seleccionado para la petición.
     */
    private SupportedAssets mSelectedAsset;

    /**
     * Cuenta regresiva para actualizar el código QR, cuando la cantidad es modificada.
     */
    private CountDownTimer mUpdateQrCountDown
            = new CountDownTimer(250, 1) {

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
    private TextWatcher mChangedAmount = new AfterTextWatcher() {
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

            if (amountStr.isEmpty() || amountStr.contentEquals(".")
                    || Coin.parseCoin(amountStr).equals(Coin.ZERO)) {
                mAmount = 0;
            } else {
                mAmount = getMaxFraction(mAmountEdit.getText().toString());
            }

            mUpdateQrCountDown.start();
        }
    };

    /**
     * Convierte la cantidad especificada en la fracción más pequeña de la moneda o token.
     *
     * @param amountStr Cantidad actual a convertir.
     * @return La cantidad expresada en la unidad más pequeña.
     */
    private long getMaxFraction(@NonNull String amountStr) {
        double amount = amountStr.isEmpty() ? 0 : Double.parseDouble(amountStr);

        switch (mSelectedAsset) {
            case BTC:
                return (long) (amount * BitcoinService.BTC_IN_SATOSHIS);
        }

        return 0;
    }

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
        mAmount = 0;

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

        if (mAmount == 0)
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    String.format(getString(R.string.request_payment_template),
                            mAddress));
        else
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    String.format(getString(R.string.request_payment_template_2),
                            cointToStringFriendly(mAmount), mAddress));

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
                return BitcoinService.get().getAddressRecipient();
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

        Helper.showSnackbar(view, getString(R.string.copy_to_clipboard_text));
    }

    /**
     * Actualiza de manera manual el código QR para realizar la petición de cobro.
     *
     * @param view Componente que desencadenó el evento Click.
     */
    public void handlerRefreshCode(View view) {
        ImageView mQrCode = findViewById(R.id.mQrCode);
        Bitmap qrCode = Helper.generateQrCode(getUri(), QR_CODE_SIZE);
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
                return Uri.parse(BitcoinURI.convertToBitcoinURI(
                        Address.fromBase58(BitcoinService.get().getNetwork(), mAddress),
                        Coin.valueOf(mAmount),
                        null,
                        null
                ));
        }
        return Uri.EMPTY;
    }
}
