package com.cryptowallet.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.bitcoin.BitcoinTransaction;
import com.cryptowallet.wallet.GenericTransactionBase;
import com.cryptowallet.wallet.SupportedAssets;

import java.util.Locale;
import java.util.Objects;

public class TransactionActivity extends ActivityBase implements GenericTransactionBase.OnUpdateDepthListener {


    private GenericTransactionBase mTransaction;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);
        setTitle(R.string.transaction_title);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        mTransaction = getGenericTransaction();

        if (mTransaction == null)
            finish();

        ((TextView) findViewById(R.id.mTxID)).setText(mTransaction.getID());
        ((TextView) findViewById(R.id.mTxDate)).setText(mTransaction.getTimeToStringFriendly());
        ((EditText) findViewById(R.id.mTxFrom)).setText(mTransaction.getInputsAddress());
        ((EditText) findViewById(R.id.mTxRecipient)).setText(mTransaction.getOutputAddress());
        ((TextView) findViewById(R.id.mTxCommits)).setText(
                String.format(Locale.getDefault(), "%d", mTransaction.getDepth()));
        ((ImageView) findViewById(R.id.mTxIcon)).setImageDrawable(mTransaction.getImage());

        TextView mAmount = findViewById(R.id.mTxAmount);
        TextView mKind = findViewById(R.id.mTxOperationKind);

        TextView mFee = findViewById(R.id.mTxFee);

        mKind.setText(mTransaction.getKindToStringFriendly());

        if (mTransaction.getKind() == GenericTransactionBase.Kind.SEND) {
            mFee.setVisibility(View.VISIBLE);
            mFee.setText(mTransaction.getFeeToStringFriendly());
        } else
            mFee.setVisibility(View.GONE);

        mAmount.setText(mTransaction.getAmountToStringFriendly(SupportedAssets.BTC));

        mAmount.setTextColor(mTransaction.getKind() == GenericTransactionBase.Kind.SEND
                ? getResources().getColor(R.color.send_tx_color)
                : getResources().getColor(R.color.receive_tx_color));

        mKind.setTextColor(mTransaction.getKind() == GenericTransactionBase.Kind.SEND
                ? getResources().getColor(R.color.send_tx_color)
                : getResources().getColor(R.color.receive_tx_color));

        mTransaction.setOnUpdateDepthListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTransaction != null)
            mTransaction.setOnUpdateDepthListener(null);
    }

    private GenericTransactionBase getGenericTransaction() {
        Intent intent = getIntent();
        String txID = intent.getStringExtra(ExtrasKey.TX_ID);
        SupportedAssets asset = SupportedAssets.valueOf(
                intent.getStringExtra(ExtrasKey.SELECTED_COIN));

        switch (asset) {
            case BTC:
                return new BitcoinTransaction(
                        this, BitcoinService.get().getTransaction(txID));
        }

        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 16908332) {
            if (getParent() == null) {
                Intent intent = new Intent(this, WalletAppActivity.class);
                startActivity(intent);
            }
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onUpdate(final GenericTransactionBase tx) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.mTxCommits)).setText(
                        String.format(Locale.getDefault(), "%d", tx.getDepth()));
            }
        });
    }
}
