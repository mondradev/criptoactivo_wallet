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
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.cryptowallet.R;
import com.cryptowallet.app.Preferences;
import com.cryptowallet.app.fragments.TransactionFragment;
import com.cryptowallet.services.WalletProvider;
import com.cryptowallet.utils.Consumer;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.SupportedAssets;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adaptador que permite la visualización de las últimas transacciones de la
 * billetera.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 */
public final class LatestTransactionsAdapter
        extends ListAdapter<ITransaction, LatestTransactionsAdapter.ViewHolder>
        implements View.OnClickListener {

    /**
     * Número máximo de elementos a mostrar.
     */
    private static final int MAX_TRANSACTIONS = 5;

    /**
     * Escuchas de cambio de la divisa en la que se muestra el precio.
     */
    private final CopyOnWriteArrayList<Consumer<Boolean>> mOnCurrencyChangeListener;

    /**
     * Actividad que está utilizando el adaptador.
     */
    private final FragmentActivity mActivity;

    /**
     * Indica si se está mostrando los precios en fiat.
     */
    private Boolean mShowingFiat;

    /**
     * Inicializa la instancia con una colección vacía.
     */
    public LatestTransactionsAdapter(@NonNull FragmentActivity activity) {
        super(R.layout.vh_recent_transaction);

        mActivity = activity;
        mShowingFiat = false;
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
    }

    /**
     * Añade nuevos elementos a la lista.
     *
     * @param item Elemento nuevo.
     */
    @Override
    public void add(ITransaction item) {
        Objects.requireNonNull(item, "Item can't be null");

        if (getItems().contains(item))
            return;

        getItems().add(item);

        if (getItemCount() > 1) {
            Collections.sort(getItems(), (left, right) -> right.compareTo(left));

            if (getItemCount() > MAX_TRANSACTIONS)
                getItems().remove(getItemCount() - 1);
        }

        hideEmptyView();
        notifyChanged();
    }

    /**
     * Establece la fuente de datos del adaptador.
     *
     * @param items Nueva fuente de datos.
     */
    @Override
    public void setSource(List<ITransaction> items) {
        Objects.requireNonNull(items, "Items can't be null");

        if (items.size() > 1)
            Collections.sort(items, (left, right) -> right.compareTo(left));

        getItems().clear();
        getItems().addAll(items.subList(0, Math.min(items.size(), MAX_TRANSACTIONS)));

        if (getItemCount() > 0)
            hideEmptyView();

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
     * Agrega cada uno de los elementos de la colección.
     *
     * @param items Colección de datos a agregar.
     * @see LatestTransactionsAdapter#add(ITransaction)
     */
    @Override
    public void addAll(List<ITransaction> items) {
        Objects.requireNonNull(items, "Items can't be null");

        if (items.size() == 0) return;

        getItems().addAll(items);

        if (getItemCount() > 1) {
            Collections.sort(getItems(), (o1, o2) -> o2.compareTo(o1));

            while (getItemCount() > MAX_TRANSACTIONS)
                getItems().remove(getItemCount() - 1);
        }

        hideEmptyView();
        notifyChanged();
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

            this.itemView.setOnClickListener(this);

            mOnCurrencyChange = isFiat -> {
                final WalletProvider walletProvider = WalletProvider.getInstance();
                final long lastPrice = walletProvider.getLastPrice(mItem.getCryptoAsset());
                final SupportedAssets asset = isFiat ? Preferences.get().getFiat()
                        : mItem.getCryptoAsset();
                final long fiatAmount = Utils
                        .cryptoToFiat(mItem.getAmount(), mItem.getCryptoAsset(), lastPrice, asset);

                itemView.<Button>findViewById(R.id.mRecentTxAmount)
                        .setText(asset.toStringFriendly(isFiat ? fiatAmount : mItem.getAmount()));
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

            final View iconKindBack = itemView.findViewById(R.id.mRecentTxIconBack);
            final View iconKindFore = itemView.findViewById(R.id.mRecentTxIconFore);
            final TextView operationKind = itemView.findViewById(R.id.mRecentTxOperationKind);
            final TextView status = itemView.findViewById(R.id.mRecentTxStatus);
            final TextView time = itemView.findViewById(R.id.mRecentTxTime);
            final Button amout = itemView.findViewById(R.id.mRecentTxAmount);
            final String todayText = itemView.getContext().getString(R.string.today_text);
            final String yesterdayText = itemView.getContext().getString(R.string.yesterday_text);

            iconKindFore.setBackground(itemView.getContext().getDrawable(getKindIcon()));
            iconKindBack.setBackgroundTintList(ColorStateList.valueOf(getKindColor()));
            operationKind.setText(getKindLabel());
            status.setVisibility(mItem.isConfirm() ? View.GONE : View.VISIBLE);

            time.setText(Utils.toLocalDatetimeString(mItem.getTime(), todayText, yesterdayText)
                    .replace("@", "\n@"));

            amout.setBackgroundTintList(ColorStateList.valueOf(getKindColor()));
            amout.setOnClickListener(view -> notifyCurrencyChange());

            mOnCurrencyChange.accept(mShowingFiat);
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

            if (mActivity != null)
                TransactionFragment.show(mActivity, SupportedAssets.BTC, mItem.getID());
        }
    }

}
