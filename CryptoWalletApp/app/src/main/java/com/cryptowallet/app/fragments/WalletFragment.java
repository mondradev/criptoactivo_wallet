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
import android.os.Handler;
import android.os.Looper;
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
import com.cryptowallet.assets.bitcoin.wallet.Wallet;
import com.cryptowallet.utils.Consumer;
import com.cryptowallet.wallet.IWallet;
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
     * Escucha del cambio del saldo en alguna billetera.
     */
    private Consumer<Double> mOnBalanceUpdateListener;

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

        final SupportedAssets fiat = Preferences.get().getFiat();
        final LinearLayout container = view.findViewById(R.id.mWalletAssetsContainer);
        final TextView fiatBalance = view.findViewById(R.id.mWalletFiatBalance);
        final TextView fiatSign = view.findViewById(R.id.mWalletFiatSign);
        final TextView fiatName = view.findViewById(R.id.mWalletFiatName);

        container.removeAllViews();

        mOnBalanceUpdateListener = (balance) -> fiatBalance.setText(fiat.toPlainText(balance));

        WalletManager.forEachAsset((asset) -> {
            String fragmentTag = CryptoAssetFragment.class.getSimpleName() + asset.name();
            CryptoAssetFragment assetView = CryptoAssetFragment.newInstance(asset);

            getParentFragmentManager()
                    .beginTransaction()
                    .add(container.getId(), assetView, fragmentTag)
                    .commit();
        });

        WalletManager.addChangedBalanceListener(
                new Handler(Looper.getMainLooper())::post, mOnBalanceUpdateListener);

        fiatSign.setText(fiat.getSign());
        fiatName.setText(fiat.name());
        fiatBalance.setText(fiat.toPlainText(WalletManager.getBalance()));
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.  The next time the fragment needs
     * to be displayed, a new view will be created.  This is called
     * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
     * <em>regardless</em> of whether {@link #onCreateView} returned a
     * non-null view.  Internally it is called after the view's state has
     * been saved but before it has been removed from its parent.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        WalletManager.removeChangedBalanceListener(mOnBalanceUpdateListener);
    }
}
