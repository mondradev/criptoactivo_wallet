package com.cryptowallet.wallet;

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
                .inflate(R.layout.recycler_transactions_history, viewGroup, false);

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
        }
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
            final TextView mCommits = mItemView.findViewById(R.id.mCommits);

            TextView mOperKind = mItemView.findViewById(R.id.mTxOperationKind);
            TextView mAmount = mItemView.findViewById(R.id.mTxAmount);
            TextView mFee = mItemView.findViewById(R.id.mTxFee);
            TextView mTime = mItemView.findViewById(R.id.mTxTime);
            ImageView mIcon = mItemView.findViewById(R.id.mAssetImage);
            TextView mAddress = mItemView.findViewById(R.id.mTxAddress);
            TextView mTxID = mItemView.findViewById(R.id.mTxHash);

            mTxID.setText(item.getTxID());
            mOperKind.setText(item.getOperationKind() == GenericTransaction.TxKind.RECEIVE
                    ? R.string.received_text : R.string.sent_text);


            int sendTxColor = mItemView.getResources().getColor(R.color.red_color);
            int receiveTxColor = mItemView.getResources().getColor(R.color.green_color);

            mAddress.setText(item.getAddress());
            mAddress.setTextColor(item.getOperationKind() == GenericTransaction.TxKind.SEND
                    ? sendTxColor : receiveTxColor);

            mAmount.setText(item.getAmount());
            mAmount.setTextColor(item.getOperationKind() == GenericTransaction.TxKind.SEND
                    ? sendTxColor : receiveTxColor
            );

            mTime.setText(item.getTimeToStringFriendly());
            mIcon.setImageDrawable(item.getImage());

            if (!item.getFee().isEmpty()) {
                mFee.setText(item.getFee());
                mFee.setVisibility(View.VISIBLE);
            } else
                mFee.setVisibility(View.GONE);

            setCommitColor(mCommits, item.getCommits());

            item.setOnCommited(new Runnable() {
                @Override
                public void run() {
                    setCommitColor(mCommits, item.getCommits());
                }
            });
        }

        /**
         * Establece el color del texto de las confirmaciones por transacción.
         *
         * @param mCommits Componente de texto.
         * @param commits  Confirmaciones de la transacciones.
         */
        void setCommitColor(TextView mCommits, int commits) {

            mCommits.setText(commits > 6 ? "6+" : Integer.toString(commits));


            int uncommit = mCommits.getResources().getColor(R.color.red_color);
            int commitedPlus = mCommits.getResources().getColor(R.color.green_color);
            int commited = mCommits.getResources().getColor(R.color.yellow_color);

            mCommits.setTextColor(commits == 0 ? uncommit : commits > 6 ? commitedPlus : commited);
        }

        /**
         * Oculta el divisor de elementos.
         */
        void hideDivider() {
            View mDivider = mItemView.findViewById(R.id.mDividerTx);
            mDivider.setVisibility(View.GONE);
        }
    }

}
