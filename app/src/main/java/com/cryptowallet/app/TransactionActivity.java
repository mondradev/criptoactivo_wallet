package com.cryptowallet.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.utils.Helper;
import com.cryptowallet.wallet.GenericTransaction;

import java.util.Locale;

public class TransactionActivity extends ActivityBase {

    @SuppressLint("StaticFieldLeak")
    private static GenericTransaction mTransaction;

    public static void putTransaction(GenericTransaction mTransaction) {
        TransactionActivity.mTransaction = mTransaction;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);
        setTitle(R.string.transaction_title);

        if (mTransaction == null)
            finish();

        ((TextView) findViewById(R.id.mTxID)).setText(mTransaction.getTxID());
        ((TextView) findViewById(R.id.mTxDate)).setText(mTransaction.getTimeToStringFriendly());
        ((EditText) findViewById(R.id.mTxFrom)).setText(mTransaction.getFromAddress());
        ((EditText) findViewById(R.id.mTxRecipient)).setText(mTransaction.getToAddress());
        ((TextView) findViewById(R.id.mTxCommits)).setText(
                String.format(Locale.getDefault(), "%d", mTransaction.getCommits()));
        ((ImageView) findViewById(R.id.mTxIcon)).setImageDrawable(mTransaction.getImage());

        TextView mAmount = findViewById(R.id.mTxAmount);
        TextView mKind = findViewById(R.id.mTxOperationKind);

        TextView mFee = findViewById(R.id.mTxFee);

        if (Helper.isNullOrEmpty(mTransaction.getFee()))
            mFee.setText(mTransaction.getFee());

        mAmount.setTextColor(mTransaction.getOperationKind() == GenericTransaction.TxKind.SEND
                ? getResources().getColor(R.color.sendColor)
                : getResources().getColor(R.color.receiveColor));

        mKind.setTextColor(mTransaction.getOperationKind() == GenericTransaction.TxKind.SEND
                ? getResources().getColor(R.color.sendColor)
                : getResources().getColor(R.color.receiveColor));

    }
}
