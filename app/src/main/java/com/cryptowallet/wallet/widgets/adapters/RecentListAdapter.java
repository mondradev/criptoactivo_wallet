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

package com.cryptowallet.wallet.widgets.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.app.AppPreference;
import com.cryptowallet.app.ExtrasKey;
import com.cryptowallet.app.TransactionActivity;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.coinmarket.ExchangeService;
import com.cryptowallet.wallet.widgets.GenericTransactionBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adaptador de la lista de transacciones recientes.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public final class RecentListAdapter
        extends AdapterBase<GenericTransactionBase, RecentListAdapter.RecentItemHolder>
        implements View.OnClickListener {

    /**
     * Activo utilizado para visualizar el monto de la transacción.
     */
    private SupportedAssets mDisplayedAsset = SupportedAssets.BTC;

    /**
     * Listado de escuchas de los eventos de actualización de montos.
     */
    private CopyOnWriteArrayList<IOnUpdateAmountListener> mUpdateAmountListeners
            = new CopyOnWriteArrayList<>();

    /**
     * Inicializa la instancia.
     */
    public RecentListAdapter() {
        super(R.layout.viewholder_generic_transaction);
    }

    /**
     * Vincula el {@link RecyclerView.ViewHolder} a los elementos de la colección.
     *
     * @param viewHolder ViewHolder a vincular.
     * @param position   Posición en la colección del elemento a vincular.
     */
    @Override
    public void onBindViewHolder(@NonNull RecentItemHolder viewHolder, int position) {
        GenericTransactionBase item = getItems().get(position);
        viewHolder.reBind(item);

        if (position == getItemCount() - 1)
            viewHolder.hideDivider();
        else
            viewHolder.showDivider();
    }

    /**
     * Crea una instancia de la implementación de {@link RecyclerView.ViewHolder}.
     *
     * @param view Vista que contiene el layout del elemento a representar.
     * @return Una instancia de {@link RecentItemHolder}.
     */
    @Override
    protected RecentItemHolder createViewHolder(View view) {
        RecentItemHolder holder = new RecentItemHolder(view);

        mUpdateAmountListeners.add(holder);

        return holder;
    }

    /**
     * Establece la fuente de datos del adaptador.
     *
     * @param items Nueva fuente de datos.
     */
    @Override
    public void setSource(List<GenericTransactionBase> items) {
        Objects.requireNonNull(items);

        getItems().clear();
        getItems().addAll(items);

        Collections.sort(getItems(), (o1, o2) -> o2.compareTo(o1));

        if (getItemCount() > 0)
            hideEmptyView();

        notifyChanged();
    }

    /**
     * Añade nuevos elementos a la lista.
     *
     * @param collection Colección de elementos a agregar.
     */
    public void addAll(List<GenericTransactionBase> collection) {
        if (collection.size() == 0)
            return;

        List<GenericTransactionBase> uniqueList = new ArrayList<>();

        for (GenericTransactionBase item : collection) {
            if (!getItems().contains(item))
                uniqueList.add(item);
        }

        getItems().addAll(uniqueList);

        Collections.sort(getItems(), (o1, o2) -> o2.compareTo(o1));

        hideEmptyView();

        notifyChanged();
    }

    /**
     * Añade nuevos elementos a la lista.
     *
     * @param item Elemento nuevo.
     */
    public void add(GenericTransactionBase item) {
        if (getItems().contains(item))
            return;

        getItems().add(item);
        Collections.sort(getItems(), (o1, o2) -> o2.compareTo(o1));

        hideEmptyView();

        notifyChanged();
    }

    /**
     * Obtiene el total de elementos de la lista.
     *
     * @return Cantidad total de elementos.
     */
    @Override
    public int getItemCount() {
        return getItems().size() > 5 ? 5 : getItems().size();
    }

    /**
     * Este método se desencadena cuando se hace clic en el precio visualizado en las transacciones
     * recientes.
     *
     * @param view Vista que desencadena el evento.
     */
    @Override
    public void onClick(View view) {
        String assetName = AppPreference.getSelectedCurrency(view.getContext()).name();
        SupportedAssets asset = SupportedAssets.valueOf(assetName);

        mDisplayedAsset = mDisplayedAsset == asset ? SupportedAssets.BTC : asset;

        notifyListeners();
    }

    /**
     * Este método se desencadena cuando se desacopla del {@link RecyclerView}, lo que
     * permite limpiar los escucha de eventos.
     *
     * @param recyclerView Vista a la cual se desacopla el adaptador.
     */
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        mUpdateAmountListeners.clear();
    }

    /**
     * Notifica a todos los escuchas del evento {@link IOnUpdateAmountListener }.
     */
    private void notifyListeners() {
        for (IOnUpdateAmountListener listener : mUpdateAmountListeners)
            listener.onUpdate();
    }

    /**
     * Esta clase representa un ViewHolder para mostrar los elementos de transacciones recientes.
     *
     * @author Ing. Javier Flores
     * @version 1.0
     */
    public final class RecentItemHolder extends ViewHolderBase<GenericTransactionBase>
            implements View.OnClickListener, IOnUpdateAmountListener {

        /**
         * Transacción contenida en el ViewHolder.
         */
        private GenericTransactionBase mItem;

        /**
         * Ejecuta las funciones en el hilo principal.
         */
        private Handler mHandler = new Handler();

        /**
         * Crea una nueva instancia.
         *
         * @param itemView Contenedor del elemento.
         */
        RecentItemHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            setDivider(itemView.findViewById(R.id.mDivider));
        }

        /**
         * Enlaza una instancia compatible con la vista que se gestiona por el ViewHolder.
         *
         * @param item Instancia a enlazar.
         */
        @Override
        protected void reBind(final GenericTransactionBase item) {
            Objects.requireNonNull(item);

            mItem = item;
            final TextView mStatus = itemView.findViewById(R.id.mStatus);

            Button mAmount = itemView.findViewById(R.id.mAmount);
            TextView mOperKind = itemView.findViewById(R.id.mOperationKind);
            TextView mTime = itemView.findViewById(R.id.mTime);
            ImageView mIcon = itemView.findViewById(R.id.mIcon);

            mAmount.setText(ExchangeService.get().getExchange(item.getAmount().getAsset())
                    .convertTo(mDisplayedAsset, item.getAmount().getUnsigned())
                    .toStringFriendly());

            mOperKind.setText(item.getAmount().isNegative()
                    ? itemView.getContext().getString(R.string.sent_text)
                    : item.getAmount().isPositive()
                    ? itemView.getContext().getString(R.string.received_text)
                    : itemView.getContext().getString(R.string.transferred_text));

            mAmount.setBackground(item.getAmount().isNegative()
                    ? itemView.getResources().getDrawable(R.drawable.bg_tx_send)
                    : item.getAmount().isPositive()
                    ? itemView.getResources().getDrawable(R.drawable.bg_tx_receive)
                    : itemView.getResources().getDrawable(R.drawable.bg_tx_transferred)
            );

            mAmount.setOnClickListener(RecentListAdapter.this);

            Context context = mTime.getContext();
            mTime.setText(Utils.getDateTime(item.getTime(), context.getString(R.string.today_text),
                    context.getString(R.string.yesterday_text)));

            mIcon.setImageDrawable(mIcon.getContext().getDrawable(item.getImage()));

            if (item.getDepth() == 0)
                mStatus.setVisibility(View.VISIBLE);

            item.setOnUpdateDepthListener(tx -> mHandler.post(() -> {
                mStatus.setVisibility(View.GONE);
                item.setOnUpdateDepthListener(null);
            }));
        }

        /**
         * Este método es desencadenado cuando se hace clic en la vista del registro que representa
         * a la transacción en el adaptador.
         *
         * @param view Vista a la cual se hace clic.
         */
        @Override
        public void onClick(View view) {
            if (mItem == null)
                return;

            Intent intent = new Intent(view.getContext(), TransactionActivity.class);
            intent.putExtra(ExtrasKey.TX_ID, mItem.getID());
            intent.putExtra(ExtrasKey.SELECTED_COIN, SupportedAssets.BTC.name());
            view.getContext().startActivity(intent);
        }

        /**
         * Este método se desencadena cuando activo del precio es reemplazado.
         */
        @Override
        public void onUpdate() {
            Button mAmount = itemView.findViewById(R.id.mAmount);
            mAmount.setText(ExchangeService.get().getExchange(mItem.getAmount().getAsset())
                    .convertTo(mDisplayedAsset, mItem.getAmount().getUnsigned()).toStringFriendly());
        }
    }

}
