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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cryptowallet.R;
import com.cryptowallet.app.adapters.TransactionHistoryAdapter;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.WalletManager;

import java.util.ArrayList;
import java.util.List;
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
        return inflater.inflate(R.layout.fragment_transactions_history,
                container, false);
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

        mExecutor = Executors.newSingleThreadExecutor();
        mAdapter = new TransactionHistoryAdapter();
        mAdapter.setEmptyView(requireView().findViewById(R.id.mTxHistEmptyLayout));

        RecyclerView txList = requireView().findViewById(R.id.mTxHistList);
        txList.setAdapter(mAdapter);
        txList.setHasFixedSize(true);
        txList.setLayoutManager(new LinearLayoutManager(requireContext()));

        mSwipeRefresh = requireView().findViewById(R.id.mTxHistSwipeRefreshTx);
        mSwipeRefresh.setColorSchemeColors(requireContext().getResources().getColor(
                R.color.sl_color_onsurface));
        mSwipeRefresh.setProgressBackgroundColorSchemeColor(Utils.resolveColor(requireContext(),
                R.attr.colorAccent));
        mSwipeRefresh.setOnRefreshListener(this::onRefresh);
        mSwipeRefresh.setRefreshing(true);

        onRefresh();
    }

    /**
     * Obtiene la lista de transacciones de todas las billeteras.
     * @return Lista de transacciones.
     */
    private List<ITransaction> getTransactions() {
        List<ITransaction> txList = new ArrayList<>();
        WalletManager.forEachAsset(asset ->
                txList.addAll(WalletManager.get(asset).getTransactions()));
        return txList;
    }

    /**
     * Refresca la lista de transacciones.
     */
    private void onRefresh() {
        mExecutor.execute(() -> {
            mAdapter.setSource(getTransactions());
            mSwipeRefresh.post(() -> mSwipeRefresh.setRefreshing(false));
        });
    }
}
