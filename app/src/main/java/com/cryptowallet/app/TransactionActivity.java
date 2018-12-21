package com.cryptowallet.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletServiceBase;
import com.cryptowallet.wallet.widgets.GenericTransactionBase;
import com.google.common.base.Joiner;

import java.util.Locale;
import java.util.Objects;

/**
 * Esta actividad permite mostrar la información de la transacción especificada a través de los
 * extras.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class TransactionActivity extends ActivityBase
        implements GenericTransactionBase.IOnUpdateDepthListener {


    /**
     * Transacción de la vista.
     */
    private GenericTransactionBase mTransaction;


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

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        mTransaction = getGenericTransaction();

        if (mTransaction == null)
            finish();

        ((TextView) findViewById(R.id.mTxID)).setText(mTransaction.getID());
        ((TextView) findViewById(R.id.mTxDate))
                .setText(Utils.getDateTime(mTransaction.getTime(), getString(R.string.today_text),
                        getString(R.string.yesterday_text)));
        ((EditText) findViewById(R.id.mTxFrom))
                .setText(Joiner.on("\n").join(mTransaction.getInputsAddress()));
        ((EditText) findViewById(R.id.mTxRecipient))
                .setText(Joiner.on("\n").join(mTransaction.getOutputAddress()));
        ((TextView) findViewById(R.id.mTxCommits)).setText(
                String.format(Locale.getDefault(), "%d", mTransaction.getDepth()));
        ((ImageView) findViewById(R.id.mTxIcon))
                .setImageDrawable(getDrawable(mTransaction.getImage()));

        TextView mAmount = findViewById(R.id.mTxAmount);
        TextView mKind = findViewById(R.id.mTxOperationKind);

        TextView mFee = findViewById(R.id.mTxFee);

        mKind.setText(mTransaction.getAmount() < 0
                ? getString(R.string.sent_text) : getString(R.string.received_text));

        if (mTransaction.getAmount() < 0) {
            mFee.setVisibility(View.VISIBLE);
            mFee.setText(WalletServiceBase.get(mTransaction.getAsset())
                    .getFormatter().format(mTransaction.getFee()));
        } else
            mFee.setVisibility(View.GONE);

        mAmount.setText(WalletServiceBase.get(mTransaction.getAsset())
                .getFormatter().format(mTransaction.getUsignedAmount()));

        mAmount.setTextColor(mTransaction.getAmount() < 0
                ? getResources().getColor(R.color.send_tx_color)
                : getResources().getColor(R.color.receive_tx_color));

        mKind.setTextColor(mTransaction.getAmount() < 0
                ? getResources().getColor(R.color.send_tx_color)
                : getResources().getColor(R.color.receive_tx_color));

        mTransaction.setOnUpdateDepthListener(this);
    }


    /**
     * Este método es llamado cuando actividad es destruida.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTransaction != null)
            mTransaction.setOnUpdateDepthListener(null);
    }

    /**
     * Obtiene la transacción a través de su ID, la cual se visualizará su información en esta
     * actividad.
     *
     * @return Transacción a visualizar.
     */
    private GenericTransactionBase getGenericTransaction() {
        Intent intent = getIntent();
        String txID = intent.getStringExtra(ExtrasKey.TX_ID);
        SupportedAssets asset = SupportedAssets.valueOf(
                intent.getStringExtra(ExtrasKey.SELECTED_COIN));

        switch (asset) {
            case BTC:
                return BitcoinService.get().findTransaction(txID);
        }

        return null;
    }

    /**
     * Este método es llamado cuando se hace clic sobre un botón de la barra de soporte de la
     * actividad.
     *
     * @param item Botón que fue presionado.
     * @return Un valor true si la opción fue realizada.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 16908332) {
            if (getParent() == null) {
                Intent intent = new Intent(this, WalletAppActivity.class);
                startActivity(intent);
            }
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Este método se desencadena cuando se actualiza la profundidad del bloque en la cadena.
     *
     * @param tx Transacción que cambia su profundidad.
     */
    @Override
    public void onUpdate(final GenericTransactionBase tx) {
        final TextView commits = findViewById(R.id.mTxCommits);
        commits.post(new Runnable() {
            @Override
            public void run() {
                commits.setText(
                        String.format(Locale.getDefault(), "%d", tx.getDepth()));
            }
        });
    }
}
