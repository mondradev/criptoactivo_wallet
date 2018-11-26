package com.cryptowallet.wallet;

import android.content.res.Resources;
import android.os.Handler;
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
 * Adaptador del historial de transacciones de la billetera.
 */
public final class TransactionHistoryAdapter
        extends RecyclerView.Adapter<TransactionHistoryAdapter.TransactionHistoryViewHolder> {

    /**
     * Lista de elementos.
     */
    private List<GenericTransaction> mItemList = new ArrayList<>();

    /**
     * Cantidad de elementos a mostrar.
     */
    private int mCurrentLimit = 10;

    /**
     * Permite enviar procesos al hilo principal.
     */
    private Handler mHandler = new Handler();

    /**
     * Crea cada elemento visual que representa a un <code>GenericTransaction</code>.
     *
     * @param viewGroup Contenedor principal de transacciones.
     * @param i         Posición del elemento a crear.
     * @return El elemento visual nuevo.
     */
    @NonNull
    @Override
    public TransactionHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.viewholder_generic_transaction, viewGroup, false);

        return new TransactionHistoryViewHolder(view);
    }

    /**
     * Vincula la vista de los elementos con cada uno <code>GenericTransaction</code> que se encuentra de
     * en la lista.
     *
     * @param viewHolder ViewHolder que se va a vincular.
     * @param i          Posición del elemento en la lista que se va a vincular.
     */
    @Override
    public void onBindViewHolder(@NonNull TransactionHistoryViewHolder viewHolder, int i) {
        GenericTransaction transaction = mItemList.get(i);
        viewHolder.setGenericTransaction(transaction);

        if (i == getItemCount() - 1) {
            viewHolder.hideDivider();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    expandList();
                }
            });
        } else
            viewHolder.showDivider();
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
    }

    /**
     * Muestra más elementos de la lista.
     */
    void expandList() {
        if (mCurrentLimit == mItemList.size())
            return;

        mCurrentLimit += 10;

        if (mCurrentLimit > mItemList.size())
            mCurrentLimit = mItemList.size();

        notifyDataSetChanged();
    }

    /**
     * Obtiene el total de elementos de la lista.
     *
     * @return Cantidad total de elementos.
     */
    @Override
    public int getItemCount() {
        return mItemList.size() > mCurrentLimit ? mCurrentLimit : mItemList.size();
    }


    /**
     * Esta clase representa un ViewHolder para mostrar los elementos del historial de
     * transacciones.
     */
    final class TransactionHistoryViewHolder extends RecyclerView.ViewHolder {

        /**
         * Contenedor del elemento de la lista.
         */
        private View mItemView;

        /**
         * Crea una nueva instancia.
         *
         * @param itemView Elemento que representa a la transacción en la lista.
         */
        TransactionHistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            mItemView = itemView;
        }


        /**
         * Establece el elemento de este <code>ViewHolder</code>.
         *
         * @param item Elemento de la lista.
         */
        void setGenericTransaction(final GenericTransaction item) {
            final TextView mStatus = mItemView.findViewById(R.id.mStatus);

            TextView mOperKind = mItemView.findViewById(R.id.mOperationKind);
            TextView mAmount = mItemView.findViewById(R.id.mAmount);
            TextView mTime = mItemView.findViewById(R.id.mTime);
            ImageView mIcon = mItemView.findViewById(R.id.mIcon);

            mOperKind.setText(item.getOperationKind() == GenericTransaction.TxKind.RECEIVE
                    ? R.string.received_text : R.string.sent_text);

            mAmount.setText(item.getAmount());
            mAmount.setBackground(item.getOperationKind() == GenericTransaction.TxKind.SEND
                    ? mItemView.getResources().getDrawable(R.drawable.bg_tx_send)
                    : mItemView.getResources().getDrawable(R.drawable.bg_tx_receive)
            );

            mTime.setText(item.getTimeToStringFriendly());
            mIcon.setImageDrawable(item.getImage());

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
            setCommitColor(mStatus, item.getCommits());
        }

        /**
         * Establece el color del texto de las confirmaciones por transacción.
         *
         * @param mCommits Componente de texto.
         * @param commits  Confirmaciones de la transacciones.
         */
        void setCommitColor(TextView mCommits, int commits) {

            mCommits.setText(commits > 6 ? "6+" : Integer.toString(commits));

            Resources a = mCommits.getContext().getResources();

            int uncommit = a.getColor(R.color.unCommitColor);
            int commitedPlus = a.getColor(R.color.plusCommitColor);
            int commited = a.getColor(R.color.commitColor);


            mCommits.setTextColor(commits == 0 ? uncommit : commits > 6 ? commitedPlus : commited);
        }

        /**
         * Oculta el divisor de elementos.
         */
        void hideDivider() {
            View mDivider = mItemView.findViewById(R.id.mDivider);
            mDivider.setVisibility(View.INVISIBLE);
        }

        /**
         * Muestra el divisor de elementos.
         */
        void showDivider() {
            View mDivider = mItemView.findViewById(R.id.mDivider);
            mDivider.setVisibility(View.VISIBLE);
        }
    }

}
