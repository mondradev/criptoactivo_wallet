package com.cryptowallet.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.AfterTextWatcher;
import com.cryptowallet.utils.DecimalsFilter;
import com.cryptowallet.utils.Helper;
import com.cryptowallet.wallet.BroadcastListener;
import com.cryptowallet.wallet.SupportedAssets;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.util.Objects;

import static com.cryptowallet.app.ExtrasKey.PIN_DATA;

/**
 * Actividad que permite el envío de pagos, también ofrece leer códigos QR que almacenan un URI
 * válido.
 */
public class SendPaymentsActivity extends ActivityBase
        implements AdapterView.OnItemSelectedListener {

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
     * Permite actualizar los campos de mRemainingBalance y mAmount.
     */
    private final AfterTextWatcher mUpdateContent = new AfterTextWatcher() {

        /**
         * Este método es llamado cuando mAmount es modificado.
         * @param s Cadena que representa la cantidad a enviar.
         */
        @Override
        public void afterTextChanged(Editable s) {
            updateFeeAndRemaining();
            validateFoundAndAddress();
        }
    };

    /**
     * Valida los fondos y la dirección especificada.
     */
    private void validateFoundAndAddress() {
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
        }

        CharSequence amountSecuences = mAmountText.getText();
        double amount = Double.parseDouble(amountSecuences.length() == 0
                || amountSecuences.charAt(amountSecuences.length() - 1) == '.' ? "0" :
                Helper.coalesce(amountSecuences.toString(), "0"));

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
                return BitcoinService.get().validateAddress(addressBase58);
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

    /**
     * Obtiene el saldo actual de la billetera.
     *
     * @return El saldo actual.
     */
    private long getBalance() {

        switch (mSelectCoin) {
            case BTC:
                return BitcoinService.get().getBalance().longValue();
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
        validateFoundAndAddress();
    }

    /**
     * Actualiza los valores por comisión y saldo restante.
     */
    public void updateFeeAndRemaining() {

        TextView mAmountText = findViewById(R.id.mAmountSendPaymentEdit);
        CharSequence amountStr = mAmountText.getText();
        double amount = Double.parseDouble(amountStr.length() == 0
                || amountStr.charAt(amountStr.length() - 1) == '.' ? "0" :
                Helper.coalesce(amountStr.toString(), "0"));

        mAmount = getMaxFraction(amount);

        String feePerKb = coinToStringFriendly(mFeePerKb);
        String remainingBalance = coinToStringFriendly(getBalance() - mAmount - mFeePerKb);

        TextView mRemainingBalance = findViewById(R.id.mRemainingBalanceText);
        TextView mFeePerKb = findViewById(R.id.mCurrentSelectedFeeText);

        mRemainingBalance.setText(remainingBalance);
        mFeePerKb.setText(feePerKb);
    }

    /**
     * Este método es llamado cuando se hace clic al componente mQrReaderIcon.
     *
     * @param view Componente que desencadena el evento Click.
     */
    public void handlerScanQr(View view) {
        mQrReader.initiateScan();
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

        if (BitcoinService.get().requireDecrypted()) {
            Intent intent = new Intent(this, LoginWalletActivity.class);
            startActivityForResult(intent, 0);
        } else
            doPay(null);
    }

    private void doPay(byte[] password) {
        try {
            switch (mSelectCoin) {
                case BTC:


                    Transaction tx = BitcoinService.get().sendPay(Coin.valueOf(mAmount),
                            Address.fromBase58(BitcoinService.get().getNetwork(), mAddress),
                            Coin.valueOf(mFeePerKb), password);

                    BitcoinService.get().broadCastTx(Objects.requireNonNull(tx), new BroadcastListener<Transaction>() {
                        @Override
                        public void onCompleted(Transaction tx) {
                            Helper.sendLargeTextNotificationOs(getApplicationContext(),
                                    R.mipmap.img_bitcoin,
                                    getString(R.string.app_name),
                                    getString(R.string.nofity_propagated),
                                    getString(R.string.nofity_propagated).concat("\n")
                                            .concat(String.format(getString(R.string.transaction_id),
                                                    tx.getHash()))
                            );
                        }
                    });
                    Intent result = new Intent();
                    result.putExtra(ExtrasKey.TX_VALUE, BitcoinService.get().getValueFromTx(tx));
                    result.putExtra(ExtrasKey.SELECTED_COIN, SupportedAssets.BTC.name());
                    result.putExtra(ExtrasKey.OP_ACTIVITY, ActivitiesOperation.SEND_PAYMENT.name());
                    setResult(Activity.RESULT_OK, result);
                    finish();
                    break;
            }
        } catch (InsufficientMoneyException ignored) {

        } catch (KeyCrypterException ignored) {

        }
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
                Helper.showSnackbar(mSendPayment, getString(R.string.address_error));
            } else {
                try {
                    String uri = result.getContents();

                    BitcoinURI uriParsed = new BitcoinURI(uri);

                    mAddressRecipient.setText(Objects
                            .requireNonNull(uriParsed.getAddress()).toBase58());

                    if (uriParsed.getAmount() != null)
                        mAmountText.setText(uriParsed.getAmount().toPlainString());

                } catch (BitcoinURIParseException e) {

                    try {
                        Address addressRead = Address.fromBase58(
                                BitcoinService.get().getNetwork(), result.getContents());

                        mAddressRecipient.setText(addressRead.toBase58());
                    } catch (AddressFormatException ignored) {
                        Helper.showSnackbar(mSendPayment, getString(R.string.qr_reader_error)
                                + ": " + result.getContents());
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

        if (data != null && data.hasExtra(PIN_DATA)) {
            doPay(data.getByteArrayExtra(PIN_DATA));
        }
    }
}
