package com.cryptowallet.app;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.bitcoin.BitcoinTransaction;
import com.cryptowallet.wallet.GenericTransactionBase;
import com.cryptowallet.wallet.TransactionHistoryAdapter;

import org.bitcoinj.core.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


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

        for (GenericTransactionBase tx : getBtcTransactions())
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
    private List<GenericTransactionBase> getBtcTransactions() {
        List<Transaction> transactions = BitcoinService.get().getTransactionsByTime();
        List<GenericTransactionBase> genericTransactions = new ArrayList<>();

        for (Transaction tx : transactions) {
            genericTransactions.add(new BitcoinTransaction(this, tx));
        }

        return genericTransactions;
    }


}
