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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.cryptowallet.R;
import com.cryptowallet.app.Preferences;
import com.cryptowallet.app.SendPaymentsActivity;
import com.cryptowallet.wallet.IWallet;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletManager;

import java.util.Objects;

/**
 * Este fragmento representa una tarjeta (Material Design) donde se muestra la información de un
 * criptoactivo. Para funcionar se require el registro del activo
 * {@link WalletManager#registerWallet(IWallet)}.
 * <p></p>
 * Esta tarjeta muestra la siguiente información:
 * <ul>
 *     <li>Nombre del activo con su logo.</li>
 *     <li>Precio en el mercado.</li>
 *     <li>Cantidad en la billetera.</li>
 *     <li>Valor total al precio del mercado.</li>
 * </ul>
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 * @see WalletManager
 * @see IWallet
 * @see SupportedAssets
 */
public class CryptoAssetFragment extends Fragment {

    /**
     * Simbolo de valor aproximado.
     */
    private static final String ALMOST_EQUAL_TO = "≈";

    /**
     * Clave del parametro activo.
     */
    public static final String ASSET_KEY
            = String.format("%s.AssetKey", CryptoAssetFragment.class.getName());

    /**
     * Petición de envío de pago.
     */
    private static final int SEND_PAYMENTS_REQUEST = 2;

    /**
     * Controlador de la billetera.
     */
    private IWallet mWallet;

    /**
     * Vista de la tarjeta.
     */
    private CardView mRoot;

    /**
     * Vista del valor total.
     */
    private TextView mFiatValueView;

    /**
     * Vista de la cantidad total.
     */
    private TextView mBalanceView;

    /**
     * Vista del precio en el mercado.
     */
    private TextView mPriceView;

    /**
     * Último precio del activo en el mercado.
     */
    private double mLastPrice;

    /**
     * Último saldo de la billetera.
     */
    private double mBalance;

    /**
     * Handler para manejo de la IU.
     */
    private Handler mHandler;

    /**
     * Establece la función utilizada para notificar algún cambio en el saldo fiat o cripto.
     *
     * @param listener Función de escucha.
     */
    public void setOnBalanceUpdate(IOnBalanceUpdate listener) {
        this.mOnBalanceUpdate = listener;
    }

    /***
     * Función que permite notificar que el saldo (fiat y cripto) ha cambiado.
     */
    private IOnBalanceUpdate mOnBalanceUpdate;

    /**
     * Crea una nueva instancia del fragmento.
     */
    public CryptoAssetFragment() {
        mBalance = 0;
        mLastPrice = 0;
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Crea una nueva instancia del fragmento especificando el activo a mostrar.
     *
     * @param walletAsset Activo a mostrar.
     * @return Una instancia del fragmento.
     */
    static CryptoAssetFragment newInstance(SupportedAssets walletAsset) {
        Bundle parameters = new Bundle();
        parameters.putCharSequence(ASSET_KEY, walletAsset.name());

        CryptoAssetFragment fragment = new CryptoAssetFragment();
        fragment.setArguments(parameters);
        return fragment;
    }

    /**
     * Este método es llamado cuando se crea la vista del fragmento.
     *
     * @param inflater           Inflador de elementos XML de archivos de layout.
     * @param container          Contenedor de la vista a crear.
     * @param savedInstanceState Datos de estado.
     * @return La vista del fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mWallet = WalletManager.get(
                SupportedAssets.valueOf(requireArguments().getString(ASSET_KEY)));
        mRoot = (CardView) inflater.inflate(R.layout.layout_cryptoasset, container,
                false);

        mRoot.findViewById(R.id.mCryptoAssetExpandButton).setOnClickListener(this::expandCard);
        mRoot.findViewById(R.id.mCryptoAssetReceive).setOnClickListener(this::showReceiveDialog);
        mRoot.findViewById(R.id.mCryptoAssetSend).setOnClickListener(this::callSendFragment);

        ((ImageView) mRoot.findViewById(R.id.mCryptoAssetIcon))
                .setImageDrawable(requireActivity().getDrawable(mWallet.getIcon()));
        ((TextView) mRoot.findViewById(R.id.mCryptoAssetName))
                .setText(mWallet.getAsset().getName());

        mBalanceView = mRoot.findViewById(R.id.mCryptoAssetBalance);
        mFiatValueView = mRoot.findViewById(R.id.mCryptoAssetValue);
        mPriceView = mRoot.findViewById(R.id.mCryptoAssetPrice);

        mWallet.addBalanceChangeListener(mHandler::post, this::onBalanceChange);
        mWallet.addPriceChangeListener(mHandler::post, this::onPriceChange);

        mBalance = mWallet.getBalance();

        updateViews();

        return mRoot;
    }

    /**
     * Este método es llamado inmediatamente cuando
     *
     * @param view               Vista creada en
     *                           {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param savedInstanceState Datos de estado.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mWallet.updatePriceListeners();
        updateViews();
    }

    /**
     * Invoca el fragmento para enviar una cantidad del activo a otras billeteras.
     *
     * @param view Vista que invoca al fragmento.
     */
    private void callSendFragment(View view) {
        Intent intent = new Intent(requireActivity(), SendPaymentsActivity.class);
        intent.putExtra(ASSET_KEY, mWallet.getAsset().name());

        startActivityForResult(intent, SEND_PAYMENTS_REQUEST); // TODO Process result
    }

    /**
     * Muestra en fragmento con un código QR que permite la recepción de una cantidad de activo
     * desde otra billetera.
     *
     * @param view Vista que invoca al fragmento.
     */
    private void showReceiveDialog(View view) {
        ReceptorInfoFragment.show(requireActivity(), mWallet.getAsset());
    }

    /**
     * Este método es invocado cuando la vista es destruida.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mWallet.removeBalanceChangeListener(this::onBalanceChange);
        mWallet.removePriceChangeListener(this::onPriceChange);
    }

    /**
     * Este método es llamado cuando el precio del mercado ha cambiado.
     *
     * @param price Nuevo precio del activo.
     */
    private void onPriceChange(double price) {
        mLastPrice = price;

        updateViews();
    }

    /**
     * Actualiza los datos (precio, saldo y valor) visualizados en la vista.
     */
    private void updateViews() {
        Objects.requireNonNull(mBalance);
        Objects.requireNonNull(mLastPrice);

        double total = mBalance * mLastPrice;
        SupportedAssets mFiatAsset = Preferences.get().getFiat();
        SupportedAssets asset = mWallet.getAsset();

        mBalanceView.setText(asset.toStringFriendly(mBalance));
        mPriceView.setText(mFiatAsset.toStringFriendly(mLastPrice));
        mFiatValueView.setText(String.format("%s %s", ALMOST_EQUAL_TO,
                mFiatAsset.toStringFriendly(total)));

        if (mOnBalanceUpdate != null)
            this.mOnBalanceUpdate.onUpdate();
    }

    /**
     * Este método es invocado cuando el saldo de la billetera ha cambiado.
     *
     * @param balance Nuevo saldo de la billetera.
     */
    private void onBalanceChange(double balance) {
        mBalance = balance;

        updateViews();
    }

    /**
     * Expande o contrae la sección de la tarjeta que muestra las transacciones recientes.
     *
     * @param view Vista que invoca.
     */
    private void expandCard(View view) {
        View layout = mRoot.findViewById(R.id.mCryptoAssetRecentsLayout);
        layout.setVisibility(layout.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }

    /**
     * Provee de una función que notifica que la billetera a cambiado su saldo o precio del activo.
     */
    public interface IOnBalanceUpdate {

        /**
         * Este método es llamado cuando el precio del activo o el saldo ha sido actualizado.
         */
        void onUpdate();
    }
}
