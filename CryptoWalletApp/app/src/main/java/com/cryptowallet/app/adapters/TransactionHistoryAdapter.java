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

package com.cryptowallet.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cryptowallet.R;
import com.cryptowallet.app.Preferences;
import com.cryptowallet.app.TransactionActivity;
import com.cryptowallet.utils.Consumer;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.SupportedAssets;
import com.google.android.material.button.MaterialButton;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adaptador que permite la visualización de un historial de transacciones de la
 * billetera.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 */
public final class TransactionHistoryAdapter
        extends ListAdapter<ITransaction, TransactionHistoryAdapter.ViewHolder>
        implements View.OnClickListener {

    /**
     * Número máximo de elementos que puede tener el historial en cada carga.
     */
    private static final int PAGE_SIZE = 10;

    /**
     * Escuchas de cambio de la divisa en la que se muestra el precio.
     */
    private final CopyOnWriteArrayList<Consumer<Boolean>> mOnCurrencyChangeListener;

    /**
     * Cantidad de elementos a mostrándose.
     */
    private int mSize = PAGE_SIZE;

    /**
     * Indica si se está mostrando los precios en fiat.
     */
    private Boolean mShowingFiat;

    /**
     * Inicializa la instancia con una colección vacía.
     */
    public TransactionHistoryAdapter() {
        super(R.layout.vh_transactions_history);

        mOnCurrencyChangeListener = new CopyOnWriteArrayList<>();
    }

    /**
     * Crea una instancia de la implementación de {@link RecyclerView.ViewHolder}.
     *
     * @param view Vista que contiene el layout del elemento a representar.
     * @return Una instancia de {@link ViewHolder}.
     */
    @Override
    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    /**
     * Vincula el {@link RecyclerView.ViewHolder} a los elementos de la colección.
     *
     * @param viewHolder ViewHolder a vincular.
     * @param position   Posición en la colección del elemento a vincular.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        ITransaction transaction = getItems().get(position);
        viewHolder.rebind(transaction);
        mOnCurrencyChangeListener.add(viewHolder.mOnCurrencyChange);

        if (position == getItemCount() - 1)
            viewHolder.itemView.post(this::loadNextPage);
    }

    /**
     * Añade nuevos elementos a la lista.
     *
     * @param item Elemento nuevo.
     */
    @Override
    public void add(ITransaction item) {
        Objects.requireNonNull(item, "Item can't be null");

        getItems().add(item);
        Collections.sort(getItems(), (left, right) -> right.compareTo(left));

        notifyChanged();
        hideEmptyView();
    }

    /**
     * Vacía la colección del adaptador.
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
    public void setSource(List<ITransaction> items) {
        Objects.requireNonNull(items, "Items can't be null");

        getItems().clear();
        getItems().addAll(items);
        Collections.sort(getItems(), (left, right) -> right.compareTo(left));

        mSize = PAGE_SIZE;

        if (getItemCount() > 0)
            hideEmptyView();

        notifyChanged();
    }

    /**
     * Muestra más elementos de la lista.
     */
    private void loadNextPage() {
        if (mSize == super.getItemCount())
            return;

        mSize = Math.min(mSize + PAGE_SIZE, super.getItemCount());

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
        notifyCurrencyChange();
    }

    /**
     * Called by RecyclerView when it stops observing this Adapter.
     *
     * @param recyclerView The RecyclerView instance which stopped observing this adapter.
     * @see #onAttachedToRecyclerView(RecyclerView)
     */
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        mOnCurrencyChangeListener.clear();
    }

    /**
     * Notifica a todos los viewHolders actualizar su divisa.
     */
    private void notifyCurrencyChange() {
        mShowingFiat = !mShowingFiat;

        for (Consumer<Boolean> listener : mOnCurrencyChangeListener)
            listener.accept(mShowingFiat);
    }

    /**
     * Obtiene el total de elementos mostrandose.
     *
     * @return Cantidad total de elementos.
     */
    @Override
    public int getItemCount() {
        return Math.min(mSize, super.getItemCount());
    }

    /**
     * Agrega cada uno de los elementos de la colección.
     *
     * @param items Colección de datos a agregar.
     * @see TransactionHistoryAdapter#add(ITransaction)
     */
    @Override
    public void addAll(List<ITransaction> items) {
        Objects.requireNonNull(items, "Items can't be null");

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
    final class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        /**
         * Transacción enlazada al {@link RecyclerView.ViewHolder}.
         */
        private ITransaction mItem;

        /**
         * Función de consumo utilizada para actualizar la divisa en la que se muestra el precio.
         */
        private Consumer<Boolean> mOnCurrencyChange;

        /**
         * Crea una nueva instancia.
         *
         * @param itemView Elemento que representa a la transacción en la lista.
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            mOnCurrencyChange = isFiat -> {
                SupportedAssets asset = isFiat ? Preferences.get().getFiat()
                        : mItem.getCriptoAsset();

                itemView.<Button>findViewById(R.id.mTxHistAmount)
                        .setText(asset.toPlainText(isFiat ? mItem.getFiatAmount() : mItem.getAmount()));
            };
        }

        /**
         * Re-enlaza la transacción al {@link RecyclerView.ViewHolder} y actualiza los valores
         * mostrados.
         *
         * @param item Elemento de la lista.
         */
        void rebind(final ITransaction item) {
            Objects.requireNonNull(item, "Item can't be null");

            if (mItem != null && item.getID().contentEquals(mItem.getID()))
                return;

            mItem = item;

            final ImageView icon = itemView.findViewById(R.id.mTxHistIcon);
            final MaterialButton iconKind = itemView.findViewById(R.id.mTxHistIconMov);
            final TextView operationKind = itemView.findViewById(R.id.mTxHistOperationKind);
            final TextView status = itemView.findViewById(R.id.mTxHistStatus);
            final TextView from = itemView.findViewById(R.id.mTxHistId);
            final TextView time = itemView.findViewById(R.id.mTxHistTime);
            final Button amout = itemView.findViewById(R.id.mTxHistAmount);

            icon.setImageResource(mItem.getWallet().getIcon());
            iconKind.setIcon(itemView.getContext().getDrawable(getKindIcon()));
            operationKind.setText(getKindLabel());
            status.setVisibility(mItem.isConfirm() ? View.GONE : View.VISIBLE);
            from.setText(mItem.isPay() ? getToLabel() : getFromLabel());
            time.setText(Utils.toLocalTimeString(mItem.getTime()));

            amout.setBackgroundTintList(ColorStateList.valueOf(getKindColor()));
            amout.setOnClickListener(view -> notifyCurrencyChange());

            mOnCurrencyChange.accept(false);
        }

        /**
         * Obtiene el color del tipo de la transacción.
         *
         * @return Color del tipo.
         */
        private int getKindColor() {
            Context context = itemView.getContext();

            return Utils.resolveColor(context, mItem.isPay() ? R.attr.colorSentTx
                    : R.attr.colorReceivedTx);
        }

        /**
         * Obtiene la etiqueta de las direcciones de destino de la transacción.
         *
         * @return Etiqueta de direcciones de destino.
         */
        private String getToLabel() {
            if (mItem.getToAddress().size() == 0)
                throw new IllegalArgumentException("The destination address list is empty");

            return mItem.getToAddress().size() > 1
                    ? itemView.getContext().getString(R.string.multi_addresses_text)
                    : mItem.getToAddress().get(0);
        }

        /**
         * Obtiene la etiqueta de las direcciones que envía la transacción.
         *
         * @return Etiqueta de direcciones de origen.
         */
        private String getFromLabel() {
            if (mItem.isCoinbase())
                return itemView.getContext().getString(R.string.coinbase_address);

            return mItem.getFromAddress().size() > 1
                    ? itemView.getContext().getString(R.string.multi_addresses_text)
                    : mItem.getFromAddress().get(0);
        }

        /**
         * Obtiene la etiqueta del tipo de movimiento.
         *
         * @return Etiqueta del movimiento.
         */
        private int getKindLabel() {
            return mItem.isPay() ? R.string.sent_text : R.string.received_text;
        }

        /**
         * Obtiene el icono del tipo de movimiento de la transacción.
         *
         * @return Icono de tipo.
         */
        private int getKindIcon() {
            return mItem.isPay() ? R.drawable.ic_send : R.drawable.ic_receive;
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

            // TODO Cambiar la referencia de la actividad.
            view.getContext().startActivity(new Intent(view.getContext(), TransactionActivity.class)
                    .putExtra(TransactionActivity.TX_ID_EXTRA, mItem.getID())
                    .putExtra(TransactionActivity.ASSET_EXTRA, SupportedAssets.BTC.name()));
        }
    }

}