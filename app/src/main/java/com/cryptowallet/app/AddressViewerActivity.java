package com.cryptowallet.app;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinAddress;
import com.cryptowallet.wallet.AddressBalance;
import com.cryptowallet.wallet.AddressesAdapter;
import com.cryptowallet.wallet.SupportedAssets;

import java.util.ArrayList;
import java.util.List;

public class AddressViewerActivity extends ActivityBase {

    private AddressesAdapter mAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_viewer);

        setTitle(R.string.address_title);

        mAddress = new AddressesAdapter(loadAddress());
        RecyclerView recyclerView = findViewById(R.id.mAddressesRecycler);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAddress);
    }

    private AddressBalance[] loadAddress() {
        List<AddressBalance> addressBalances = new ArrayList<>();

        for (SupportedAssets assets : SupportedAssets.values()) {
            switch (assets) {
                case BTC:
                    addressBalances.addAll(BitcoinAddress.getAll());
                    break;
            }
        }

        return addressBalances.toArray(new AddressBalance[0]);
    }
}
