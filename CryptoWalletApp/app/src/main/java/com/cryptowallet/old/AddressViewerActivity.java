/*
 * Copyright © 2020. Criptoactivo
 * Copyright © 2020. InnSy Tech
 * Copyright © 2020. Ing. Javier de Jesús Flores Mondragón
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cryptowallet.old;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.cryptowallet.R;

/**
 * Esta actividad permite la visualización de todas las direcciones han recibido pagos.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class AddressViewerActivity extends FragmentActivity {

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

        /*

        AddressesViewerAdapter mAddress = new AddressesViewerAdapter(loadAddress());
        RecyclerView recyclerView = findViewById(R.id.mAddressesRecycler);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

        mAddress.setEmptyView(findViewById(R.id.mEmptyAddress));

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAddress);
         */
    }

    /**
     * Carga las direcciones de todas los activos soportados de la billetera.
     *
     * @return Lista de direcciones.
     */
    private void loadAddress() {
        /*
        List<IAddressBalance> addressBalances = new ArrayList<>();

        for (SupportedAssets assets : SupportedAssets.values()) {
            switch (assets) {
                case BTC:
                    addressBalances.addAll(BitcoinApi.get().getAddresses());
                    break;
            }
        }

        return addressBalances;

         */
    }
}
