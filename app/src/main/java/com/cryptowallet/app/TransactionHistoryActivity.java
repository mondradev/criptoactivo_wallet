package com.cryptowallet.app;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.Helper;
import com.cryptowallet.wallet.ExchangeService;
import com.cryptowallet.wallet.GenericTransaction;
import com.cryptowallet.wallet.TransactionHistoryAdapter;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.cryptowallet.wallet.GenericTransaction.Builder;

/**
 * Actividad que permite visualizar todas las transacciones de la billetera.
 */
public class TransactionHistoryActivity extends ActivityBase {

    /**
     * Adaptador de transacciones de la vista.
     */
    private TransactionHistoryAdapter mTransactionsAdapter;

    /**
     * Este método se ejecuta cuando la actividad se crea.
     *
     * @param savedInstanceState Almacena el estado de la instancia.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_transaction);

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.history_title));

        mTransactionsAdapter = new TransactionHistoryAdapter();

        RecyclerView mTransactionsHistory = findViewById(R.id.mTransactionsHistory);

        if (mTransactionsHistory == null)
            return;

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

        mTransactionsHistory.setAdapter(mTransactionsAdapter);
        mTransactionsHistory.setHasFixedSize(true);
        mTransactionsHistory.setLayoutManager(layoutManager);

        loadTransactions();

    }

    /**
     * Obtiene una lista de todas las transacciones de la billetera.
     *
     * @return Lista de transacciones.
     */
    private void loadTransactions() {

        for (GenericTransaction tx : getBtcTransactions())
            mTransactionsAdapter.addItem(tx);

        TextView historyEmptyLabel = findViewById(R.id.mEmptyHistory);
        historyEmptyLabel.setVisibility(mTransactionsAdapter.getItemCount() > 0
                ? View.GONE : View.VISIBLE);
    }

    /**
     * Obtiene las transacciones de BTC.
     *
     * @return Lista de transaciones.
     */
    private List<GenericTransaction> getBtcTransactions() {
        List<Transaction> transactions = BitcoinService.get().getTransactionsByTime();
        List<GenericTransaction> genericTransactions = new ArrayList<>();

        for (Transaction tx : transactions) {
            boolean isPay = tx.getValue(BitcoinService.get().getWallet()).isNegative();
            long value = BitcoinService.get().getValueFromTx(tx);
            String fee = isPay ? tx.getFee().toFriendlyString() : "";

            final GenericTransaction gTx
                    = new Builder(this, R.mipmap.img_bitcoin,
                    ExchangeService.Currencies.BTC)
                    .setCommits(tx.getConfidence().getDepthInBlocks())
                    .setAmount(value)
                    .setFee(fee)
                    .setKind(Helper.getTxKind(isPay))
                    .setTime(tx.getUpdateTime())
                    .setTxID(tx.getHashAsString())
                    .appendFromAddress(BitcoinService.getFromAddresses(tx,
                            getString(R.string.coinbase_address),
                            getString(R.string.unknown_address)))
                    .appendToAddress(BitcoinService.getToAddresses(tx))
                    .create();

            if (tx.getConfidence().getDepthInBlocks() < 7)
                tx.getConfidence().addEventListener(new TransactionConfidence.Listener() {
                    @Override
                    public void onConfidenceChanged(TransactionConfidence confidence,
                                                    ChangeReason reason) {
                        if (confidence.getDepthInBlocks() > 7)
                            confidence.removeEventListener(this);
                        gTx.setCommits(confidence.getDepthInBlocks());
                    }
                });

            genericTransactions.add(gTx);
        }

        return genericTransactions;
    }


}
