package com.cryptowallet.wallet;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.app.ExtrasKey;
import com.cryptowallet.app.TransactionActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adaptador del historial de transacciones de la billetera.
 */
public final class TransactionHistoryAdapter
        extends RecyclerView.Adapter<TransactionHistoryAdapter.TransactionHistoryViewHolder>
        implements View.OnClickListener {

    private static final int PAGE_SIZE = 20;
    /**
     * Lista de elementos.
     */
    private List<GenericTransactionBase> mItemList = new ArrayList<>();
    /**
     * Cantidad de elementos a mostrar.
     */
    private int mCurrentLimit = 20;
    /**
     * Permite enviar procesos al hilo principal.
     */
    private Handler mHandler = new Handler();
    private SupportedAssets mCurrentCurrency = SupportedAssets.BTC;
    private CopyOnWriteArrayList<View.OnClickListener> mUpdateAmountListeners
            = new CopyOnWriteArrayList<>();

    /**
     * Crea cada elemento visual que representa a un <code>{@link GenericTransactionBase}</code>.
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

        final TransactionHistoryViewHolder holder = new TransactionHistoryViewHolder(view);
        mUpdateAmountListeners.add(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.updateAmount();
            }
        });

        return holder;
    }

    /**
     * Vincula la vista de los elementos con cada uno <code>{@link GenericTransactionBase}</code> que se encuentra de
     * en la lista.
     *
     * @param viewHolder ViewHolder que se va a vincular.
     * @param i          Posición del elemento en la lista que se va a vincular.
     */
    @Override
    public void onBindViewHolder(@NonNull TransactionHistoryViewHolder viewHolder, int i) {
        GenericTransactionBase transaction = mItemList.get(i);
        viewHolder.update(transaction);

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
    public void addItem(GenericTransactionBase item) {
        mItemList.add(item);
        Collections.sort(mItemList, new Comparator<GenericTransactionBase>() {
            @Override
            public int compare(GenericTransactionBase o1, GenericTransactionBase o2) {
                return o2.compareTo(o1);
            }
        });

        notifyDataSetChanged();
    }

    /**
     * Muestra más elementos de la lista.
     */
    private void expandList() {
        if (mCurrentLimit == mItemList.size())
            return;

        mCurrentLimit += PAGE_SIZE;

        if (mCurrentLimit > mItemList.size())
            mCurrentLimit = mItemList.size();

        notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        switch (mCurrentCurrency) {
            case BTC:
                mCurrentCurrency = SupportedAssets.USD;
                break;
            case USD:
                mCurrentCurrency = SupportedAssets.MXN;
                break;
            case MXN:
                mCurrentCurrency = SupportedAssets.BTC;
                break;
        }

        updateAmount();
    }

    private void updateAmount() {
        for (View.OnClickListener listener : mUpdateAmountListeners)
            listener.onClick(null);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        mUpdateAmountListeners.clear();
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
    final class TransactionHistoryViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        /**
         * Contenedor del elemento de la lista.
         */
        private View mItemView;

        private GenericTransactionBase mItem;

        /**
         * Crea una nueva instancia.
         *
         * @param itemView Elemento que representa a la transacción en la lista.
         */
        TransactionHistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            mItemView = itemView;

            itemView.setOnClickListener(this);
        }


        /**
         * Establece el elemento de este <code>ViewHolder</code>.
         *
         * @param item Elemento de la lista.
         */
        void update(final GenericTransactionBase item) {
            Objects.requireNonNull(item);

            if (mItem != null && item.getID().contentEquals(mItem.getID()))
                return;

            mItem = item;

            final TextView mStatus = mItemView.findViewById(R.id.mStatus);
            TextView mOperKind = mItemView.findViewById(R.id.mOperationKind);
            Button mAmount = mItemView.findViewById(R.id.mAmount);
            TextView mTime = mItemView.findViewById(R.id.mTime);
            ImageView mIcon = mItemView.findViewById(R.id.mIcon);

            mAmount.setText(item.getAmountToStringFriendly(mCurrentCurrency));

            mOperKind.setText(item.getKindToStringFriendly());
            mAmount.setBackground(item.getKind() == GenericTransactionBase.Kind.SEND
                    ? mItemView.getResources().getDrawable(R.drawable.bg_tx_send)
                    : mItemView.getResources().getDrawable(R.drawable.bg_tx_receive)
            );
            mAmount.setOnClickListener(TransactionHistoryAdapter.this);

            mTime.setText(item.getTimeToStringFriendly());
            mIcon.setImageDrawable(item.getImage());

            item.setOnUpdateDepthListener(new GenericTransactionBase.OnUpdateDepthListener() {
                @Override
                public void onUpdate(GenericTransactionBase tx) {
                    setCommitColor(mStatus, tx.getDepth());
                }
            });
            setCommitColor(mStatus, item.getDepth());
        }

        /**
         * Establece el color del texto de las confirmaciones por transacción.
         *
         * @param mCommits Componente de texto.
         * @param commits  Confirmaciones de la transacciones.
         */
        void setCommitColor(final TextView mCommits, final int commits) {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCommits.setText(commits > 6 ? "6+" : Integer.toString(commits));

                    Resources a = mCommits.getContext().getResources();

                    int uncommit = a.getColor(R.color.unCommitColor);
                    int commitedPlus = a.getColor(R.color.plusCommitColor);
                    int commited = a.getColor(R.color.commitColor);


                    mCommits.setTextColor(
                            commits == 0 ? uncommit : commits > 6 ? commitedPlus : commited);
                }
            });
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


        @Override
        public void onClick(View v) {
            if (mItem == null)
                return;

            Intent intent = new Intent(v.getContext(), TransactionActivity.class);
            intent.putExtra(ExtrasKey.TX_ID, mItem.getID());
            intent.putExtra(ExtrasKey.SELECTED_COIN, SupportedAssets.BTC.name());
            v.getContext().startActivity(intent);
        }

        void updateAmount() {
            Button mAmount = mItemView.findViewById(R.id.mAmount);
            mAmount.setText(mItem.getAmountToStringFriendly(mCurrentCurrency));
        }
    }

}
