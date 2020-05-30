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

package com.cryptowallet.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cryptowallet.R;
import com.cryptowallet.app.Preferences;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletManager;


/**
 * Este fragmento muestra la información de todas los criptoactivos habilitados. En esta podemos ver
 * el saldo total de la billetera y cada criptoactivo así como sus respectivas transacciones
 * recientes y las operaciones de solicitar o enviar pagos.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class WalletFragment extends Fragment {

    /**
     * Este método es llamado cuando se requiere crear la vista del fragmento.
     *
     * @param inflater           Inflador de XML.
     * @param container          Contenedor de la vista del fragmento.
     * @param savedInstanceState Datos de estado de la aplicación.
     * @return Vista del fragmento.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet, container, false);
    }

    /**
     * Este método es invocado cuando la vista del fragmento es creado. En este método se crean las
     * tarjetas de cada criptoactivo habilidado.
     *
     * @param view               Vista del fragmento.
     * @param savedInstanceState Datos de estado de la aplicación.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout container = view.findViewById(R.id.mWalletAssetsContainer);
        container.removeAllViews();

        WalletManager.forEachAsset((asset) -> {
            String fragmentTag = CryptoAssetFragment.class.getSimpleName() + asset.name();
            CryptoAssetFragment assetView = CryptoAssetFragment.newInstance(asset);

            getParentFragmentManager()
                    .beginTransaction()
                    .add(container.getId(), assetView, fragmentTag)
                    .commit();
        });

        TextView fiatBalance = view.findViewById(R.id.mWalletFiatBalance);
        TextView fiatSign = view.findViewById(R.id.mWalletFiatSign);
        TextView fiatName = view.findViewById(R.id.mWalletFiatName);
        SupportedAssets fiat = Preferences.get().getFiat();

        fiatSign.setText(fiat.getSign());
        fiatName.setText(fiat.name());
        fiatBalance.setText(fiat.toPlainText(WalletManager.getBalance()));
    }

}