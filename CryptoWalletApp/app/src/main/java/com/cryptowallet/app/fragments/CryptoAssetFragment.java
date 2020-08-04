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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cryptowallet.Constants;
import com.cryptowallet.R;
import com.cryptowallet.app.Preferences;
import com.cryptowallet.app.SendPaymentsActivity;
import com.cryptowallet.app.adapters.LatestTransactionsAdapter;
import com.cryptowallet.utils.Consumer;
import com.cryptowallet.wallet.AbstractWallet;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletProvider;

import java.util.Objects;

/**
 * Este fragmento representa una tarjeta (Material Design) donde se muestra la información de un
 * criptoactivo. Para funcionar se require el registro del activo dentro de la clase
 * {@link WalletProvider}.
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
 * @see WalletProvider
 * @see AbstractWallet
 * @see SupportedAssets
 */
public class CryptoAssetFragment extends Fragment {

    /**
     * Simbolo de valor aproximado.
     */
    private static final String ALMOST_EQUAL_TO = "≈";
    /**
     * Petición de envío de pago.
     */
    private static final int SEND_PAYMENTS_REQUEST = 2;

    /**
     * Controlador de la billetera.
     */
    private AbstractWallet mWallet;

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
    private long mLastPrice;

    /**
     * Último saldo de la billetera.
     */
    private long mLastBalance;

    /**
     * Handler para manejo de la IU.
     */
    private Handler mHandler;

    /**
     * Escucha del saldo de la billetera.
     */
    private Consumer<Long> mOnBalanceChangeListener;

    /**
     * Escucha de nuevas transacciones.
     */
    private Consumer<ITransaction> mOnNewTransactionListener;

    /**
     * Adaptador de las transacciones recientes.
     */
    private LatestTransactionsAdapter mAdapter;

    /**
     * Crea una nueva instancia del fragmento.
     */
    public CryptoAssetFragment() {
        mLastBalance = 0;
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
        parameters.putCharSequence(Constants.EXTRA_ASSET, walletAsset.name());

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
        final WalletProvider walletService = WalletProvider.getInstance(this.requireContext());
        final SupportedAssets asset = SupportedAssets
                .valueOf(requireArguments().getString(Constants.EXTRA_ASSET));

        mOnBalanceChangeListener = this::onBalanceChange;
        mOnNewTransactionListener = this::onNewTransaction;
        mWallet = walletService.get(asset);
        mRoot = (CardView) inflater.inflate(R.layout.layout_cryptoasset, container,
                false);

        mRoot.findViewById(R.id.mCryptoAssetExpandButton).setOnClickListener(this::toggleCard);
        mRoot.findViewById(R.id.mCryptoAssetReceive).setOnClickListener(this::showReceiveDialog);
        mRoot.findViewById(R.id.mCryptoAssetSend).setOnClickListener(this::callSendFragment);

        ((ImageView) mRoot.findViewById(R.id.mCryptoAssetIcon))
                .setImageDrawable(requireActivity().getDrawable(mWallet.getIcon()));
        ((TextView) mRoot.findViewById(R.id.mCryptoAssetName))
                .setText(mWallet.getCryptoAsset().getName());

        mBalanceView = mRoot.findViewById(R.id.mCryptoAssetBalance);
        mFiatValueView = mRoot.findViewById(R.id.mCryptoAssetValue);
        mPriceView = mRoot.findViewById(R.id.mCryptoAssetPrice);

        mWallet.addBalanceChangedListener(mHandler::post, mOnBalanceChangeListener);
        mWallet.addNewTransactionListener(mHandler::post, mOnNewTransactionListener);

        mLastBalance = mWallet.getBalance();
        mAdapter = new LatestTransactionsAdapter(requireActivity());
        mAdapter.setEmptyView(mRoot.findViewById(R.id.mCryptoAssetEmptyRecentsLayout));

        RecyclerView txList = mRoot.findViewById(R.id.mCryptoAssetRecents);
        txList.setAdapter(mAdapter);
        txList.setHasFixedSize(true);
        txList.setLayoutManager(new LinearLayoutManager(requireContext()));

        mAdapter.setSource(mWallet.getTransactions());

        if (mAdapter.getItemCount() > 0 && walletService.getCount() == 1)
            mRoot.findViewById(R.id.mCryptoAssetExpandButton).performClick();

        updateViews();

        return mRoot;
    }

    /**
     * Este método es llamado cuando se agrega una transacción nueva a la billetera.
     *
     * @param tx Transacción nueva.
     */
    private void onNewTransaction(ITransaction tx) {
        if (mAdapter.getItemCount() == 0
                && WalletProvider.getInstance(this.requireContext()).getCount() == 1)
            expandCard();

        mAdapter.add(tx);
    }

    /**
     * Expande la tarjeta para mostrar las transacciones recientes.
     */
    private void expandCard() {
        if (mRoot.findViewById(R.id.mCryptoAssetRecentsLayout).getVisibility() == View.GONE)
            mRoot.findViewById(R.id.mCryptoAssetExpandButton).performClick();
    }

    /**
     * Este método es llamado inmediatamente cuando la vista es creada.
     *
     * @param view               Vista creada en
     *                           {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param savedInstanceState Datos de estado.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateViews();
    }

    /**
     * Invoca el fragmento para enviar una cantidad del activo a otras billeteras.
     *
     * @param view Vista que invoca al fragmento.
     */
    private void callSendFragment(View view) {
        Intent intent = new Intent(requireActivity(), SendPaymentsActivity.class);
        intent.putExtra(Constants.EXTRA_ASSET, mWallet.getCryptoAsset().name());

        startActivityForResult(intent, SEND_PAYMENTS_REQUEST);
    }

    /**
     * Muestra en fragmento con un código QR que permite la recepción de una cantidad de activo
     * desde otra billetera.
     *
     * @param view Vista que invoca al fragmento.
     */
    private void showReceiveDialog(View view) {
        ReceptorInfoFragment.show(requireActivity(), mWallet);
    }

    /**
     * Este método es invocado cuando la vista es destruida.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mWallet.removeBalanceChangedListener(mOnBalanceChangeListener);
        mWallet.removeNewTransactionListener(mOnNewTransactionListener);
    }

    /**
     * Actualiza los datos (precio, saldo y valor) visualizados en la vista.
     */
    private void updateViews() {
        Objects.requireNonNull(mLastBalance);
        Objects.requireNonNull(mLastPrice);

        long total = mLastBalance * mLastPrice;
        SupportedAssets mFiatAsset = Preferences.get().getFiat();
        SupportedAssets asset = mWallet.getCryptoAsset();

        mBalanceView.setText(asset.toStringFriendly(mLastBalance));
        mPriceView.setText(mFiatAsset.toStringFriendly(mLastPrice));
        mFiatValueView.setText(String.format("%s %s", ALMOST_EQUAL_TO,
                mFiatAsset.toStringFriendly(total)));
    }

    /**
     * Este método es invocado cuando el saldo de la billetera ha cambiado.
     *
     * @param balance Nuevo saldo de la billetera.
     */
    private void onBalanceChange(long balance) {
        mLastBalance = balance;

        updateViews();
    }

    /**
     * Expande o contrae la sección de la tarjeta que muestra las transacciones recientes.
     *
     * @param view Vista que invoca.
     */
    private void toggleCard(View view) {
        View layout = mRoot.findViewById(R.id.mCryptoAssetRecentsLayout);
        layout.setVisibility(layout.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }
}
