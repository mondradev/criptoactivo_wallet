/*
 * Copyright © 2020. Criptoactivo
 * Copyright © 2020. InnSy Tech
 * Copyright © 2020. Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.app.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cryptowallet.Constants;
import com.cryptowallet.R;
import com.cryptowallet.app.adapters.TransactionHistoryAdapter;
import com.cryptowallet.services.WalletProvider;
import com.cryptowallet.utils.Consumer;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.SupportedAssets;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Este fragmento muestra el historial de las transacciones de la billetera.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 */
public class TransactionHistoryFragment extends Fragment {

    /**
     * Handler del hilo principal.
     */
    private Handler mMainHandler;

    /**
     * Adaptador de la lista de transacciones.
     */
    private TransactionHistoryAdapter mAdapter;

    /**
     * Ejecutor de procesos de segundo plano.
     */
    private Executor mExecutor;

    /**
     * Instancia del layout de refresco por desliz.
     */
    private SwipeRefreshLayout mSwipeRefresh;

    /**
     * Escucha de nuevas transacciones.
     */
    private Consumer<ITransaction> mOnNewTransactionListener;

    /**
     * Indica que el esuchucha ya puede recibir transacciones.
     */
    private boolean mIsReady;

    /**
     * Receptor del evento {@link Constants#NEW_TRANSACTION}
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /**
         * This method is called when the BroadcastReceiver is receiving an Intent
         * broadcast.
         *
         * @param context The Context in which the receiver is running.
         * @param intent  The Intent being received.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            mExecutor.execute(() -> {
                final String assetName = intent.getStringExtra(Constants.EXTRA_CRYPTO_ASSET);
                final String txid = intent.getStringExtra(Constants.EXTRA_TXID);
                final SupportedAssets asset = SupportedAssets.valueOf(assetName);
                final WalletProvider provider = WalletProvider.getInstance();

                ITransaction tx = provider.get(asset).findTransaction(txid);

                mMainHandler.post(() -> mOnNewTransactionListener.accept(tx));
            });
        }
    };

    /**
     * Este método es llamado cuando se requiere crear la vista del fragmento.
     *
     * @param inflater           Inflador de XML.
     * @param container          Contenedor de la vista del fragmento.
     * @param savedInstanceState Datos de estado de la aplicación.
     * @return Vista del fragmento.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_transactions_history,
                container, false);

        mExecutor = Executors.newCachedThreadPool();
        mMainHandler = new Handler(Looper.getMainLooper());
        mAdapter = new TransactionHistoryAdapter(requireActivity());
        mAdapter.setEmptyView(root.findViewById(R.id.mTxHistEmptyLayout));

        RecyclerView txList = root.findViewById(R.id.mTxHistList);
        txList.setAdapter(mAdapter);
        txList.setHasFixedSize(true);
        txList.setLayoutManager(new LinearLayoutManager(requireContext()));

        mSwipeRefresh = root.findViewById(R.id.mTxHistSwipeRefreshTx);
        mSwipeRefresh.setOnRefreshListener(this::onRefresh);
        mSwipeRefresh.setRefreshing(true);
        mSwipeRefresh.setColorSchemeColors(
                Utils.resolveColor(requireContext(), R.attr.colorOnPrimary));
        mSwipeRefresh.setProgressBackgroundColorSchemeColor(Utils.resolveColor(requireContext(),
                R.attr.colorAccent));

        mOnNewTransactionListener = (tx) -> {
            if (!mIsReady) return;

            mAdapter.add(tx);
        };

        return root;
    }

    /**
     * Este método es invocado cuando la vista del fragmento es creado. En este método se crean las
     * tarjetas de cada criptoactivo habilidado.
     *
     * @param view               Vista del fragmento.
     * @param savedInstanceState Datos de estado de la aplicación.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        onRefresh();

        requireActivity().registerReceiver(mReceiver, new IntentFilter(Constants.NEW_TRANSACTION));
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.  The next time the fragment needs
     * to be displayed, a new view will be created.  This is called
     * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
     * <em>regardless</em> of whether {@link #onCreateView} returned a
     * non-null view.  Internally it is called after the view's state has
     * been saved but before it has been removed from its parent.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        requireActivity().unregisterReceiver(mReceiver);
    }

    /**
     * Refresca la lista de transacciones.
     */
    private void onRefresh() {
        mExecutor.execute(() -> {
            final WalletProvider provider = WalletProvider.getInstance();

            mAdapter.setSource(provider.getTransactions());
            mSwipeRefresh.post(() -> mSwipeRefresh.setRefreshing(false));

            if (!mIsReady)
                mIsReady = true;
        });
    }
}
