package com.cryptowallet.app;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.coinmarket.ExchangeService;
import com.cryptowallet.wallet.widgets.GenericTransactionBase;
import com.cryptowallet.wallet.widgets.adapters.TransactionHistoryAdapter;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;


/**
 * Actividad que permite visualizar todas las transacciones de la billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public class TransactionHistoryActivity extends ActivityBase {

    /**
     * Adaptador de transacciones de la vista.
     */
    private TransactionHistoryAdapter mTransactionsAdapter;

    /**
     * Handler que permite ejecutar funciones en el hilo principal.
     */
    private Handler mHandler = new Handler();

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
        mTransactionsAdapter.setEmptyView(findViewById(R.id.mEmptyHistory));

        RecyclerView mTransactionsHistory = findViewById(R.id.mTransactionsHistory);

        if (mTransactionsHistory == null)
            return;

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

        mTransactionsHistory.setAdapter(mTransactionsAdapter);
        mTransactionsHistory.setHasFixedSize(true);
        mTransactionsHistory.setLayoutManager(layoutManager);

        final SwipeRefreshLayout mSwipeRefreshTx = findViewById(R.id.mSwipeRefreshTx);

        mSwipeRefreshTx.setColorSchemeColors(
                Utils.getColorFromTheme(this, R.attr.textIconsColor));

        mSwipeRefreshTx.setProgressBackgroundColorSchemeColor(
                Utils.getColorFromTheme(this, R.attr.colorAccent));

        mSwipeRefreshTx.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Executors.newSingleThreadExecutor()
                        .execute(new Runnable() {
                                     @Override
                                     public void run() {
                                         ExchangeService.get().reloadMarketPrice();

                                         mHandler.post(new Runnable() {
                                             @Override
                                             public void run() {
                                                 mTransactionsAdapter.clear();
                                                 mTransactionsAdapter.addAll(getTransactions());
                                                 mSwipeRefreshTx.setRefreshing(false);
                                             }
                                         });
                                     }
                                 }
                        );
            }
        });

        loadTransactions();

    }

    /**
     * Obtiene una lista de todas las transacciones de la billetera.
     */
    private void loadTransactions() {

        for (GenericTransactionBase tx : getTransactions())
            mTransactionsAdapter.add(tx);
    }

    /**
     * Obtiene las transacciones de BTC.
     *
     * @return Lista de transaciones.
     */
    private List<GenericTransactionBase> getTransactions() {
        return BitcoinService.get().getTransactionsByTime();
    }


}
