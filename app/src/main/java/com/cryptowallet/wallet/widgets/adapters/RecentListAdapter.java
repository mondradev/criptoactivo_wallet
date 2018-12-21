package com.cryptowallet.wallet.widgets.adapters;

import android.content.Context;
import android.content.Intent;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
     * Activo seleccionado para visualizar los montos.
     */
    private SupportedAssets mSelectedAsset = SupportedAssets.BTC;

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
        return new RecentItemHolder(view);
    }

    /**
     * Añade nuevos elementos a la lista.
     *
     * @param collection Colección de elementos a agregar.
     */
    public void addAll(List<GenericTransactionBase> collection) {
        getItems().addAll(collection);

        Collections.sort(getItems(), new Comparator<GenericTransactionBase>() {
            @Override
            public int compare(GenericTransactionBase o1, GenericTransactionBase o2) {
                return o2.compareTo(o1);
            }
        });

        notifyDataSetChanged();

        if (getItems().size() > 0)
            hideEmptyView();
    }

    /**
     * Añade nuevos elementos a la lista.
     *
     * @param item Elemento nuevo.
     */
    public void add(GenericTransactionBase item) {
        getItems().add(item);
        Collections.sort(getItems(), new Comparator<GenericTransactionBase>() {
            @Override
            public int compare(GenericTransactionBase o1, GenericTransactionBase o2) {
                return o2.compareTo(o1);
            }
        });

        notifyDataSetChanged();

        hideEmptyView();
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
        String assetName = AppPreference.getSelectedCurrency(view.getContext());
        SupportedAssets asset = SupportedAssets.valueOf(assetName);

        mSelectedAsset = mSelectedAsset == asset ? SupportedAssets.BTC : asset;

        notifyDataSetChanged();
    }

    /**
     * Esta clase representa un ViewHolder para mostrar los elementos de transacciones recientes.
     *
     * @author Ing. Javier Flores
     * @version 1.0
     */
    public final class RecentItemHolder extends ViewHolderBase<GenericTransactionBase>
            implements View.OnClickListener {

        /**
         * Transacción contenida en el ViewHolder.
         */
        private GenericTransactionBase mItem;

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

            mAmount.setText(ExchangeService.get().getExchange(mSelectedAsset)
                    .ToStringFriendly(item.getAsset(), item.getUsignedAmount()));

            mOperKind.setText(item.getAmount() < 0
                    ? itemView.getContext().getString(R.string.sent_text)
                    : itemView.getContext().getString(R.string.received_text));

            mAmount.setBackground(item.getAmount() < 0
                    ? itemView.getResources().getDrawable(R.drawable.bg_tx_send)
                    : itemView.getResources().getDrawable(R.drawable.bg_tx_receive)
            );
            mAmount.setOnClickListener(RecentListAdapter.this);

            Context context = mTime.getContext();
            mTime.setText(Utils.getDateTime(item.getTime(), context.getString(R.string.today_text),
                    context.getString(R.string.yesterday_text)));

            mIcon.setImageDrawable(mIcon.getContext().getDrawable(item.getImage()));

            mStatus.setVisibility(View.VISIBLE);
            item.setOnUpdateDepthListener(new GenericTransactionBase.IOnUpdateDepthListener() {
                @Override
                public void onUpdate(GenericTransactionBase tx) {
                    itemView.post(new Runnable() {
                        @Override
                        public void run() {
                            mStatus.setVisibility(View.GONE);
                            item.setOnUpdateDepthListener(null);
                        }
                    });
                }
            });
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
    }

}
