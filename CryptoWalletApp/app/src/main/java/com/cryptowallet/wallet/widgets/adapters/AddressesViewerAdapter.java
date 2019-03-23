package com.cryptowallet.wallet.widgets.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.wallet.widgets.IAddressBalance;

import java.util.List;

/**
 * Adaptador que permite la representaci?n de una lista de direcciones de billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class AddressesViewerAdapter
        extends AdapterBase<IAddressBalance, AddressesViewerAdapter.AddressViewHolder> {

    /**
     * Crea una nueva instancia del adaptador especificando la colecci?n de las direcciones
     * a mostrar.
     *
     * @param addressBalances Colecci?n de las direcciones.
     */
    public AddressesViewerAdapter(List<IAddressBalance> addressBalances) {
        super(R.layout.viewholder_address_balance, addressBalances);
    }

    /**
     * Crea una instancia de la implementaci?n de {@link RecyclerView.ViewHolder}.
     *
     * @param view Vista que contiene el layout del elemento a representar.
     * @return Una instancia de {@link AddressViewHolder}.
     */
    @Override
    protected AddressViewHolder createViewHolder(View view) {
        return new AddressViewHolder(view);
    }

    /**
     * Vincula el {@link RecyclerView.ViewHolder} a los elementos de la colecci?n.
     *
     * @param viewHolder ViewHolder a vincular.
     * @param position   Posici?n en la colecci?n del elemento a vincular.
     */
    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder viewHolder, int position) {
        IAddressBalance addressBalance = getItems().get(position);
        viewHolder.reBind(addressBalance);

        if (position == getItemCount() - 1)
            viewHolder.hideDivider();
        else
            viewHolder.showDivider();
    }

    /**
     * Implementa {@link RecyclerView.ViewHolder} para controla los datos que se visualizan
     * de cada elemento de la colecci?n del adaptador.
     *
     * @author Ing. Javier Flores
     * @version 1.0
     */
    final class AddressViewHolder extends ViewHolderBase<IAddressBalance> {

        /**
         * Crea una nueva instancia especificando la vista del elemento.
         *
         * @param itemView Vista del elemento.
         */
        private AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            setDivider(itemView.findViewById(R.id.mAddressDivider));
        }

        /**
         * Actualiza los datos de las vistas relacionadas con el elemento.
         *
         * @param addressBalance Direcci?n de la billetera a visualizar.
         */
        @Override
        protected void reBind(IAddressBalance addressBalance) {
            ((TextView) itemView.findViewById(R.id.mDisplayedAddress))
                    .setText(addressBalance.getAddress());
            ((TextView) itemView.findViewById(R.id.mAddressBalance))
                    .setText(addressBalance.getBalanceToStringFriendly());
        }
    }


}
