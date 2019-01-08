/*
 * Copyright 2019 InnSy Tech
 * Copyright 2019 Ing. Javier de Jesús Flores Mondragón
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

import android.os.Bundle;
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
 * @version 1.1
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

        mSwipeRefreshTx.setOnRefreshListener(() -> Executors.newSingleThreadExecutor()
                .execute(() -> {
                            ExchangeService.get().reloadMarketPrice();
                            loadTransactions();

                            mSwipeRefreshTx.post(() -> mSwipeRefreshTx.setRefreshing(false));
                        }
                ));

        loadTransactions();

    }

    /**
     * Obtiene una lista de todas las transacciones de la billetera.
     */
    private void loadTransactions() {
        mTransactionsAdapter.setSource(getTransactions());
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
