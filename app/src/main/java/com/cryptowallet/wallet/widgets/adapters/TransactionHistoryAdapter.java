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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adaptador que permite la visualización de un historial de transacciones de la
 * billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public final class TransactionHistoryAdapter
        extends AdapterBase<GenericTransactionBase, TransactionHistoryAdapter.TransactionHistoryViewHolder>
        implements View.OnClickListener {

    /**
     * Número máximo de elementos que puede tener el historial en cada carga.
     */
    private static final int PAGE_SIZE = 25;

    /**
     * Cantidad de elementos a mostrándose.
     */
    private int mSize = PAGE_SIZE;

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
     * Inicializa la instancia con una colección vacía.
     */
    public TransactionHistoryAdapter() {
        super(R.layout.viewholder_generic_transaction);
    }

    /**
     * Crea una instancia de la implementación de {@link RecyclerView.ViewHolder}.
     *
     * @param view Vista que contiene el layout del elemento a representar.
     * @return Una instancia de {@link TransactionHistoryViewHolder}.
     */
    @Override
    protected TransactionHistoryViewHolder createViewHolder(View view) {
        final TransactionHistoryViewHolder holder = new TransactionHistoryViewHolder(view);

        mUpdateAmountListeners.add(holder);

        return holder;
    }

    /**
     * Vincula el {@link RecyclerView.ViewHolder} a los elementos de la colección.
     *
     * @param viewHolder ViewHolder a vincular.
     * @param position   Posición en la colección del elemento a vincular.
     */
    @Override
    public void onBindViewHolder(@NonNull TransactionHistoryViewHolder viewHolder, int position) {
        GenericTransactionBase transaction = getItems().get(position);
        viewHolder.reBind(transaction);

        if (position == getItemCount() - 1) {
            viewHolder.hideDivider();
            viewHolder.itemView.post(this::expandList);
        } else
            viewHolder.showDivider();
    }

    /**
     * Añade nuevos elementos a la lista.
     *
     * @param item Elemento nuevo.
     */
    @Override
    public void add(GenericTransactionBase item) {
        Objects.requireNonNull(item, "El elemento a agregar no puede ser un valor null");

        getItems().add(item);

        Collections.sort(getItems(), (o1, o2) -> o2.compareTo(o1));

        notifyChanged();

        hideEmptyView();
    }

    /**
     * Vacía la colección del adaptador y visualiza el {@link AdapterBase#mEmptyView} de
     * ser posible para indicar al usuario que no hay datos que mostrar.
     */
    @Override
    public void clear() {
        mSize = PAGE_SIZE;
        super.clear();
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

        mSize = PAGE_SIZE;

        if (getItemCount() > 0)
            hideEmptyView();

        notifyChanged();
    }

    /**
     * Muestra más elementos de la lista.
     */
    private void expandList() {
        if (mSize == super.getItemCount())
            return;

        mSize += PAGE_SIZE;

        if (mSize > super.getItemCount())
            mSize = super.getItemCount();

        notifyChanged();
    }

    /**
     * Este método se desencadena cuando se hace clic en el precio de cada transacción,
     * lo cual permite el cambio de activo utilizado para visualizar el precio.
     *
     * @param view Vista que desencadena el evento.
     */
    @Override
    public void onClick(View view) {

        String assetName = AppPreference.getSelectedCurrency(view.getContext());
        SupportedAssets asset = SupportedAssets.valueOf(assetName);

        mDisplayedAsset = mDisplayedAsset == asset ? SupportedAssets.BTC : asset;

        notifyListeners();
    }

    /**
     * Notifica a todos los escuchas del evento {@link IOnUpdateAmountListener }.
     */
    private void notifyListeners() {
        for (IOnUpdateAmountListener listener : mUpdateAmountListeners)
            listener.onUpdate();
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
     * Obtiene el total de elementos mostrandose.
     *
     * @return Cantidad total de elementos.
     */
    @Override
    public int getItemCount() {
        return super.getItemCount() > mSize
                ? mSize : super.getItemCount();
    }

    /**
     * Agrega cada uno de los elementos de la colección.
     *
     * @param items Colección de datos a agregar.
     * @see TransactionHistoryAdapter#add(GenericTransactionBase)
     */
    @Override
    public void addAll(List<GenericTransactionBase> items) {
        Objects.requireNonNull(items,
                "La colección a agregar no puede ser un valor null");

        if (items.size() == 0) return;

        getItems().addAll(items);

        Collections.sort(getItems(), (o1, o2) -> o2.compareTo(o1));

        notifyChanged();

        hideEmptyView();
    }

    /**
     * Es la implementación de {@link RecyclerView.ViewHolder} para mostrar los elementos
     * del historial de transacciones.
     *
     * @author Ing. Javier Flores
     * @version 1.0
     */
    final class TransactionHistoryViewHolder extends ViewHolderBase<GenericTransactionBase>
            implements IOnUpdateAmountListener, View.OnClickListener {

        /**
         * Transacción enlazada al {@link RecyclerView.ViewHolder}.
         */
        private GenericTransactionBase mItem;

        /**
         * Ejecuta las funciones en el hilo principal.
         */
        private Handler mHandler = new Handler();

        /**
         * Crea una nueva instancia.
         *
         * @param itemView Elemento que representa a la transacción en la lista.
         */
        TransactionHistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            setDivider(itemView.findViewById(R.id.mDivider));
        }

        /**
         * Re-enlaza la transacción al {@link RecyclerView.ViewHolder} y actualiza los valores
         * mostrados.
         *
         * @param item Elemento de la lista.
         */
        @Override
        protected void reBind(final GenericTransactionBase item) {
            Objects.requireNonNull(item, "La transacción no puede ser un valor null");

            if (mItem != null && item.getID().contentEquals(mItem.getID()))
                return;

            mItem = item;

            final TextView mStatus = itemView.findViewById(R.id.mStatus);
            TextView mOperKind = itemView.findViewById(R.id.mOperationKind);
            Button mAmount = itemView.findViewById(R.id.mAmount);
            TextView mTime = itemView.findViewById(R.id.mTime);
            ImageView mIcon = itemView.findViewById(R.id.mIcon);

            mAmount.setText(ExchangeService.get().getExchange(mDisplayedAsset)
                    .ToStringFriendly(mItem.getAsset(), mItem.getUsignedAmount()));

            mOperKind.setText(mItem.getAmount() < 0
                    ? itemView.getContext().getString(R.string.sent_text)
                    : itemView.getContext().getString(R.string.received_text));

            mAmount.setBackground(mItem.getAmount() < 0
                    ? itemView.getResources().getDrawable(R.drawable.bg_tx_send)
                    : itemView.getResources().getDrawable(R.drawable.bg_tx_receive)
            );
            mAmount.setOnClickListener(TransactionHistoryAdapter.this);

            mTime.setText(Utils.getDateTime(mItem.getTime(),
                    itemView.getContext().getString(R.string.today_text),
                    itemView.getContext().getString(R.string.yesterday_text)));

            mIcon.setImageDrawable(itemView.getContext().getDrawable(mItem.getImage()));

            setCommitColor(mStatus, mItem.getDepth());

            mItem.setOnUpdateDepthListener(tx -> setCommitColor(mStatus, tx.getDepth()));

        }

        /**
         * Establece el color del texto de las confirmaciones por transacción.
         *
         * @param mCommits Componente de texto.
         * @param commits  Confirmaciones de la transacciones.
         */
        void setCommitColor(final TextView mCommits, final int commits) {
            int uncommit = Utils.getColorFromTheme(
                    mCommits.getContext(),
                    R.attr.uncommitTxColor
            );

            int commited = Utils.getColorFromTheme(
                    mCommits.getContext(),
                    R.attr.commitTxColor
            );

            int commitedPlus = Utils.getColorFromTheme(
                    mCommits.getContext(),
                    R.attr.plusCommitTxColor
            );

            final int color = commits == 0 ? uncommit : commits > 6 ? commitedPlus : commited;

            mHandler.post(() -> {
                mCommits.setVisibility(View.VISIBLE);
                mCommits.setText(commits > 6 ? "6+" : Integer.toString(commits));
                mCommits.setTextColor(color);
            });
        }

        /**
         * Este método se desencadena cuando se realiza un clic sobre la transacción, lo cual
         * permite mostrar los detalles de la transacción.
         *
         * @param view Vista que desencadena el evento.
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
            mAmount.setText(ExchangeService.get().getExchange(mDisplayedAsset)
                    .ToStringFriendly(mItem.getAsset(), mItem.getUsignedAmount()));
        }
    }

}
