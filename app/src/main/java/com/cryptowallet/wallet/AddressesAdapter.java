package com.cryptowallet.wallet;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cryptowallet.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddressesAdapter extends Adapter<AddressesAdapter.AddressViewHolder> {

    private final List<AddressBalance> mAddresses = new ArrayList<>();

    public AddressesAdapter(AddressBalance[] source) {
        mAddresses.addAll(Arrays.asList(source));
    }

    public void replaceSource(AddressBalance[] source) {
        mAddresses.clear();
        mAddresses.addAll(Arrays.asList(source));

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.viewholder_address_balance, viewGroup, false);

        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder addressViewHolder, int i) {
        AddressBalance addressBalance = mAddresses.get(i);
        addressViewHolder.update(addressBalance);

        if (i == getItemCount() - 1)
            addressViewHolder.hideDivider();
        else
            addressViewHolder.showDivider();
    }

    @Override
    public int getItemCount() {
        return mAddresses.size();
    }

    static class AddressViewHolder extends RecyclerView.ViewHolder {

        private final View mItem;

        AddressViewHolder(@NonNull View itemView) {
            super(itemView);

            mItem = itemView;
        }


        void showDivider() {
            mItem.findViewById(R.id.mAddressDivider).setVisibility(View.VISIBLE);
        }

        void hideDivider() {
            mItem.findViewById(R.id.mAddressDivider).setVisibility(View.INVISIBLE);
        }

        void update(AddressBalance addressBalance) {
            ((TextView) mItem.findViewById(R.id.mDisplayedAddress))
                    .setText(addressBalance.getAddress());
            ((TextView) mItem.findViewById(R.id.mAddressBalance))
                    .setText(addressBalance.getBalanceToStringFriendly());
        }
    }

}
