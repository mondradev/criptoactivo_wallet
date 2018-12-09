package com.cryptowallet.bitcoin;

import android.content.Context;

import com.cryptowallet.R;
import com.cryptowallet.wallet.ExchangeService;
import com.cryptowallet.wallet.GenericTransactionBase;
import com.cryptowallet.wallet.SupportedAssets;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 */
public final class BitcoinTransaction extends GenericTransactionBase {

    private static Logger mLogger = LoggerFactory.getLogger(BitcoinTransaction.class);

    /**
     *
     */
    private final Transaction mTx;

    /**
     */
    public BitcoinTransaction(Context context, Transaction tx) {
        super(context, BitcoinService.isPay(tx) ? Kind.SEND : Kind.RECEIVE,
                context.getDrawable(R.mipmap.img_bitcoin));

        mTx = tx;
        if (getDepth() < 7)
            tx.getConfidence().addEventListener(new ConfidencialListener());
    }

    public static List<GenericTransactionBase> getTransactionsByTime(Context context) {
        if (!BitcoinService.isRunning())
            return new ArrayList<>();

        List<Transaction> transactions = BitcoinService.get().getTransactionsByTime();
        List<GenericTransactionBase> bitcoinTransactions = new ArrayList<>();

        for (Transaction tx : transactions)
            bitcoinTransactions.add(new BitcoinTransaction(context, tx));

        return bitcoinTransactions;
    }

    @Override
    public String getFeeToStringFriendly() {
        Coin fee = mTx.getFee();
        return fee == null ? Coin.ZERO.toFriendlyString() : fee.toFriendlyString();
    }

    @Override
    public String getAmountToStringFriendly(SupportedAssets currency) {
        return ExchangeService.getExchange(currency).ToStringFriendly(
                SupportedAssets.BTC,
                BitcoinService.get().getValueFromTx(mTx)
        );
    }

    @Override
    public String getInputsAddress() {
        return BitcoinService.getFromAddresses(
                mTx,
                getContext().getString(R.string.coinbase_address),
                getContext().getString(R.string.unknown_address)
        );
    }

    @Override
    public String getOutputAddress() {
        return BitcoinService.getToAddresses(mTx);
    }

    @Override
    public Date getTime() {
        return mTx.getUpdateTime();
    }

    @Override
    public int getDepth() {
        return mTx.getConfidence().getDepthInBlocks();
    }

    @Override
    public String getID() {
        return mTx.getHashAsString();
    }

    private final class ConfidencialListener implements TransactionConfidence.Listener {

        @Override
        public void onConfidenceChanged(TransactionConfidence confidence, ChangeReason reason) {
            if (reason == ChangeReason.DEPTH || confidence.getConfidenceType()
                    == TransactionConfidence.ConfidenceType.BUILDING) {

                mLogger.debug("Transacción confirmada, profundidad: {}",
                        confidence.getDepthInBlocks());


                if (confidence.getDepthInBlocks() == 1)
                    BitcoinService.get().handlerWalletChange();

                OnUpdateDepthListener listener = getOnUpdateDepthListener();
                if (listener != null) listener.onUpdate(BitcoinTransaction.this);

                if (getDepth() >= 7)
                    confidence.removeEventListener(this);
            }
        }
    }
}
