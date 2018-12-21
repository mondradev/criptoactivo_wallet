package com.cryptowallet.app;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.widgets.IAddressBalance;
import com.cryptowallet.wallet.widgets.adapters.AddressesViewerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Esta actividad permite la visualización de todas las direcciones han recibido pagos.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class AddressViewerActivity extends ActivityBase {

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_viewer);

        setTitle(R.string.address_title);

        AddressesViewerAdapter mAddress = new AddressesViewerAdapter(loadAddress());
        RecyclerView recyclerView = findViewById(R.id.mAddressesRecycler);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAddress);
    }

    /**
     * Carga las direcciones de todas los activos soportados de la billetera.
     *
     * @return Lista de direcciones.
     */
    private List<IAddressBalance> loadAddress() {
        List<IAddressBalance> addressBalances = new ArrayList<>();

        for (SupportedAssets assets : SupportedAssets.values()) {
            switch (assets) {
                case BTC:
                    addressBalances.addAll(BitcoinService.get().getAddresses());
                    break;
            }
        }

        return addressBalances;
    }
}
