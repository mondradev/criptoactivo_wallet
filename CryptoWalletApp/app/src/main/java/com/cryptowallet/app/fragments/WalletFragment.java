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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.cryptowallet.Constants;
import com.cryptowallet.R;
import com.cryptowallet.app.Preferences;
import com.cryptowallet.services.WalletProvider;
import com.cryptowallet.utils.Consumer;
import com.cryptowallet.wallet.SupportedAssets;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


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
    private Consumer<Long> mOnBalanceUpdateListener;

    /**
     * Handler del hilo principal.
     */
    private Handler mMainHandler;

    /**
     * Ejecutor de procesos de segundo plano.
     */
    private Executor mExecutor;

    /**
     * Receptor del evento {@link Constants#NEW_TRANSACTION}
     */
    private BroadcastReceiver mPriceReceiver = new BroadcastReceiver() {
        /**
         * This method is called when the BroadcastReceiver is receiving an Intent
         * broadcast.
         *
         * @param context The Context in which the receiver is running.
         * @param intent  The Intent being received.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            mExecutor.execute(() -> {
                final long balance = intent.getLongExtra(Constants.EXTRA_FIAT_BALANCE, 0);
                mMainHandler.post(() -> mOnBalanceUpdateListener.accept(balance));
            });
        }
    };

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
        mExecutor = Executors.newCachedThreadPool();
        mMainHandler = new Handler(Looper.getMainLooper());

        final WalletProvider provider = WalletProvider.getInstance();

        provider.forEachAsset((asset) -> {
            String fragmentTag = CryptoAssetFragment.class.getSimpleName() + asset.name();
            CryptoAssetFragment assetView = CryptoAssetFragment.newInstance(asset);

            getParentFragmentManager()
                    .beginTransaction()
                    .add(container.getId(), assetView, fragmentTag)
                    .commit();
        });

        IntentFilter filter = new IntentFilter(Constants.UPDATED_FIAT_BALANCE);
        filter.addAction(Constants.UPDATED_PRICE);

        requireContext().registerReceiver(mPriceReceiver, filter);

        fiatSign.setText(fiat.getSign());
        fiatName.setText(fiat.name());
        fiatBalance.setText(fiat.toPlainText(provider.getFiatBalance()));
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

        requireContext().unregisterReceiver(mPriceReceiver);
    }
}
