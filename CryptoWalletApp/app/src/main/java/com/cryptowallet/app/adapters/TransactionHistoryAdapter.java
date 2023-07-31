/*
 * Copyright &copy; 2023. Criptoactivo
 * Copyright &copy; 2023. InnSy Tech
 * Copyright &copy; 2023. Ing. Javier de Jesús Flores Mondragón
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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.cryptowallet.R;
import com.cryptowallet.app.Preferences;
import com.cryptowallet.app.fragments.TransactionFragment;
import com.cryptowallet.core.domain.SupportedAssets;
import com.cryptowallet.services.WalletProvider;
import com.cryptowallet.utils.Consumer;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.ITransaction;

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
     * Actividad que está utilizando el adaptador.
     */
    private final FragmentActivity mActivity;

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
    public TransactionHistoryAdapter(@NonNull FragmentActivity activity) {
        super(R.layout.vh_transactions_history);

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

        if (getItems().contains(item))
            return;

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
        else
            showEmptyView();

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
        private final Consumer<Boolean> mOnCurrencyChange;

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

                itemView.<Button>findViewById(R.id.mTxHistAmount)
                        .setText(asset.toStringFriendly(isFiat
                                ? fiatAmount : mItem.getAmount()));
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
            final View iconKindBack = itemView.findViewById(R.id.mTxHistIconBack);
            final View iconKindFore = itemView.findViewById(R.id.mTxHistIconFore);
            final TextView operationKind = itemView.findViewById(R.id.mTxHistOperationKind);
            final TextView status = itemView.findViewById(R.id.mTxHistStatus);
            final TextView id = itemView.findViewById(R.id.mTxHistId);
            final TextView time = itemView.findViewById(R.id.mTxHistTime);
            final Button amout = itemView.findViewById(R.id.mTxHistAmount);
            final String todayText = itemView.getContext().getString(R.string.today_text);
            final String yesterdayText = itemView.getContext().getString(R.string.yesterday_text);

            icon.setImageResource(mItem.getWallet().getIcon());
            iconKindFore.setBackground(ContextCompat.getDrawable(itemView.getContext(), getKindIcon()));
            iconKindBack.setBackgroundTintList(ColorStateList.valueOf(getKindColor()));
            operationKind.setText(getKindLabel());
            status.setVisibility(mItem.isConfirm() ? View.GONE : View.VISIBLE);
            id.setText(mItem.getID());
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
