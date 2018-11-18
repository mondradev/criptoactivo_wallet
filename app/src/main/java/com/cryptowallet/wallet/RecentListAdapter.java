package com.cryptowallet.wallet;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptowallet.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Adaptador de la lista de transacciones recientes.
 */
public final class RecentListAdapter
        extends RecyclerView.Adapter<RecentListAdapter.RecentItemHolder> {

    /**
     * Lista de elementos.
     */
    private List<RecentItem> mItemList = new ArrayList<>();

    /**
     * Crea cada elemento visual que representa a un <code>RecentItem</code>.
     *
     * @param viewGroup Contenedor principal de transacciones.
     * @param i         Posición del elemento a crear.
     * @return El elemento visual nuevo.
     */
    @NonNull
    @Override
    public RecentItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.recycler_recents_list, viewGroup, false);

        RecentItemHolder recentItemHolder = new RecentItemHolder(view);

        if (i == getItemCount() - 1)
            recentItemHolder.hideDivider();

        return recentItemHolder;
    }

    /**
     * Vincula la vista de los elementos con cada uno <code>RecentItem</code> que se encuentra de
     * en la lista.
     *
     * @param recentItemHolder ViewHolder que se va a vincular.
     * @param i                Posición del elemento en la lista que se va a vincular.
     */
    @Override
    public void onBindViewHolder(@NonNull RecentItemHolder recentItemHolder, int i) {
        RecentItem item = mItemList.get(i);
        recentItemHolder.setRecentItem(item);
    }

    /**
     * Añade nuevos elementos a la lista.
     *
     * @param item Elemento nuevo.
     */
    public void addItem(RecentItem item) {
        mItemList.add(item);
        Collections.sort(mItemList, new Comparator<RecentItem>() {
            @Override
            public int compare(RecentItem o1, RecentItem o2) {
                return o2.getTime().compareTo(o1.getTime());
            }
        });

        notifyDataSetChanged();
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

    /**
     * Esta clase representa un ViewHolder para mostrar los elementos de transacciones recientes.
     */
    public final class RecentItemHolder extends RecyclerView.ViewHolder {

        /**
         * Contenedor del elemento de la lista.
         */
        private View mItemView;

        /**
         * Crea una nueva instancia.
         *
         * @param itemView Contenedor del elemento.
         */
        public RecentItemHolder(@NonNull View itemView) {
            super(itemView);
            mItemView = itemView;
        }

        /**
         * Establece el elemento de este <code>ViewHolder</code>.
         *
         * @param item Elemento de la lista.
         */
        void setRecentItem(RecentItem item) {
            final TextView mStatus = mItemView.findViewById(R.id.mStatus);

            TextView mOperKind = mItemView.findViewById(R.id.mOperationKind);
            TextView mAmount = mItemView.findViewById(R.id.mAmount);
            TextView mFee = mItemView.findViewById(R.id.mFee);
            TextView mTime = mItemView.findViewById(R.id.mTime);
            ImageView mIcon = mItemView.findViewById(R.id.mIcon);

            mOperKind.setText(item.getOperationKind() == RecentItem.TxKind.RECEIVE
                    ? R.string.received_text : R.string.sent_text);

            mAmount.setText(item.getAmount());
            mAmount.setTextColor(item.getOperationKind() == RecentItem.TxKind.SEND
                    ? mItemView.getResources().getColor(R.color.red_color)
                    : mItemView.getResources().getColor(R.color.green_color)
            );

            mTime.setText(item.getTimeToStringFriendly());
            mIcon.setImageDrawable(item.getImage());

            if (!item.getFee().isEmpty())
                mFee.setText(item.getFee());
            else
                mFee.setVisibility(View.GONE);

            if (item.isCommited())
                mStatus.setVisibility(View.GONE);
            else
                mStatus.setVisibility(View.VISIBLE);

            item.setOnCommited(new Runnable() {
                @Override
                public void run() {
                    mStatus.setVisibility(View.GONE);
                }
            });
        }

        /**
         * Oculta el divisor de elementos.
         */
        void hideDivider() {
            View mDivider = mItemView.findViewById(R.id.mDivider);
            mDivider.setVisibility(View.GONE);
        }
    }

}
