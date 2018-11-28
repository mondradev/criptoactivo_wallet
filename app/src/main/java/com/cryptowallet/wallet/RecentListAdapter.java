package com.cryptowallet.wallet;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.app.TransactionActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Adaptador de la lista de transacciones recientes.
 */
public final class RecentListAdapter
        extends RecyclerView.Adapter<RecentListAdapter.RecentItemHolder>
        implements View.OnClickListener {

    /**
     * Lista de elementos.
     */
    private List<GenericTransaction> mItemList = new ArrayList<>();

    /**
     *
     */
    private View mEmptyView;

    private ExchangeService.Currencies mCurrentCurrency = ExchangeService.Currencies.BTC;

    /**
     * Crea cada elemento visual que representa a un <code>GenericTransaction</code>.
     *
     * @param viewGroup Contenedor principal de transacciones.
     * @param i         Posición del elemento a crear.
     * @return El elemento visual nuevo.
     */
    @NonNull
    @Override
    public RecentItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.viewholder_generic_transaction, viewGroup, false);

        return new RecentItemHolder(view);
    }

    /**
     * Vincula la vista de los elementos con cada uno <code>GenericTransaction</code> que se encuentra de
     * en la lista.
     *
     * @param recentItemHolder ViewHolder que se va a vincular.
     * @param i                Posición del elemento en la lista que se va a vincular.
     */
    @Override
    public void onBindViewHolder(@NonNull RecentItemHolder recentItemHolder, int i) {
        GenericTransaction item = mItemList.get(i);
        recentItemHolder.update(item);

        if (i == getItemCount() - 1)
            recentItemHolder.hideDivider();
        else
            recentItemHolder.showDivider();
    }

    /**
     * Añade nuevos elementos a la lista.
     *
     * @param item Elemento nuevo.
     */
    public void addItem(GenericTransaction item) {
        mItemList.add(item);
        Collections.sort(mItemList, new Comparator<GenericTransaction>() {
            @Override
            public int compare(GenericTransaction o1, GenericTransaction o2) {
                return o2.getTime().compareTo(o1.getTime());
            }
        });

        notifyDataSetChanged();

        if (mEmptyView != null)
            mEmptyView.setVisibility(View.GONE);
    }

    /**
     * @return
     */
    public View getEmptyView() {
        return mEmptyView;
    }

    /**
     * @param mEmptyView
     */
    public void setEmptyView(View mEmptyView) {
        this.mEmptyView = mEmptyView;
    }

    /**
     * Obtiene el total de elementos de la lista.
     *
     * @return Cantidad total de elementos.
     */
    @Override
    public int getItemCount() {
        return mItemList.size() > 5 ? 5 : mItemList.size();
    }

    @Override
    public void onClick(View v) {
        switch (mCurrentCurrency) {
            case BTC:
                mCurrentCurrency = ExchangeService.Currencies.USD;
                break;
            case USD:
                mCurrentCurrency = ExchangeService.Currencies.MXN;
                break;
            case MXN:
                mCurrentCurrency = ExchangeService.Currencies.BTC;
                break;
        }

        notifyDataSetChanged();
    }

    /**
     * Esta clase representa un ViewHolder para mostrar los elementos de transacciones recientes.
     */
    public final class RecentItemHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        /**
         * Contenedor del elemento de la lista.
         */
        private View mItemView;

        private GenericTransaction mItem;

        /**
         * Crea una nueva instancia.
         *
         * @param itemView Contenedor del elemento.
         */
        RecentItemHolder(@NonNull View itemView) {
            super(itemView);
            mItemView = itemView;

            itemView.setOnClickListener(this);
        }

        /**
         * Muestra el divisor de elementos.
         */
        void showDivider() {
            View mDivider = mItemView.findViewById(R.id.mDivider);
            mDivider.setVisibility(View.VISIBLE);
        }

        /**
         * Oculta el divisor de elementos.
         */
        void hideDivider() {
            View mDivider = mItemView.findViewById(R.id.mDivider);
            mDivider.setVisibility(View.GONE);
        }

        void update(GenericTransaction item) {
            mItem = item;
            final TextView mStatus = mItemView.findViewById(R.id.mStatus);

            Button mAmount = mItemView.findViewById(R.id.mAmount);
            TextView mOperKind = mItemView.findViewById(R.id.mOperationKind);
            TextView mTime = mItemView.findViewById(R.id.mTime);
            ImageView mIcon = mItemView.findViewById(R.id.mIcon);

            mAmount.setText(item.getAmount(mCurrentCurrency));
            mOperKind.setText(item.getOperationKind() == GenericTransaction.TxKind.RECEIVE
                    ? R.string.received_text : R.string.sent_text);

            mAmount.setBackground(item.getOperationKind() == GenericTransaction.TxKind.SEND
                    ? mItemView.getResources().getDrawable(R.drawable.bg_tx_send)
                    : mItemView.getResources().getDrawable(R.drawable.bg_tx_receive)
            );
            mAmount.setOnClickListener(RecentListAdapter.this);

            mTime.setText(item.getTimeToStringFriendly());
            mIcon.setImageDrawable(item.getImage());

            item.setOnCommited(new Runnable() {
                @Override
                public void run() {
                    mStatus.setVisibility(View.GONE);
                }
            });
            if (item.isCommited())
                mStatus.setVisibility(View.GONE);
            else
                mStatus.setVisibility(View.VISIBLE);
        }

        @Override
        public void onClick(View v) {
            if (mItem == null)
                return;

            Intent intent = new Intent(v.getContext(), TransactionActivity.class);
            TransactionActivity.putTransaction(mItem);
            v.getContext().startActivity(intent);
        }
    }

}
