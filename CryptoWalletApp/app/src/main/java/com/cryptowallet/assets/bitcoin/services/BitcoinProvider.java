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

package com.cryptowallet.assets.bitcoin.services;

import android.util.Log;

import com.cryptowallet.assets.bitcoin.services.retrofit.BitcoinApi;
import com.cryptowallet.assets.bitcoin.services.retrofit.ChainInfo;
import com.cryptowallet.assets.bitcoin.services.retrofit.TxData;
import com.cryptowallet.assets.bitcoin.wallet.Transaction;
import com.cryptowallet.assets.bitcoin.wallet.Wallet;
import com.cryptowallet.services.IWalletProvider;
import com.cryptowallet.wallet.ChainTipInfo;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.SupportedAssets;
import com.google.common.util.concurrent.ListenableFutureTask;

import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Proveedor de datos para la billetera de Bitcoin.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 * @see BitcoinApi
 */
public class BitcoinProvider implements IWalletProvider {

    /**
     * Etiqueta de log.
     */
    private static final String LOG_TAG = "Bitcoin Provider";

    /**
     * URL de la api.
     */
    private static final String WEBSERVICE_URL = "http://innsytech.com:5000/api/v1/";

    /**
     * Mapeo de proveedores según los parametros de red.
     */
    private static Map<Wallet, IWalletProvider> mMap;

    /**
     * Instancia de los servicios de la api.
     */
    private final BitcoinApi mApi;

    /**
     * Ejecutor de los servicios.
     */
    private final ExecutorService mExecutor;

    /**
     * Parametros de la red.
     */
    private final Wallet mWallet;

    /**
     * Crea una nueva instancia del proveedor de datos.
     *
     * @param wallet Parametros de red.
     */
    private BitcoinProvider(Wallet wallet) {
        mWallet = wallet;
        mExecutor = Executors.newSingleThreadExecutor();
        mApi = new Retrofit.Builder()
                .baseUrl(WEBSERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BitcoinApi.class);
    }

    /**
     * Obtiene el proveedor correspondiente a los parametros de red especificados.
     *
     * @param wallet Instancia de la billetera que contendrá las transacciones.
     * @return Un proveedor de billetera.
     */
    public static IWalletProvider get(Wallet wallet) {
        if (mMap == null)
            mMap = new HashMap<>();

        if (!mMap.containsKey(wallet))
            mMap.put(wallet, new BitcoinProvider(wallet));

        return mMap.get(wallet);
    }

    /**
     * Obtiene el cripto-activo soportado por este proveedor.
     *
     * @return El cripto-activo del proveedor.
     */
    @Override
    public SupportedAssets getCriptoAsset() {
        return SupportedAssets.BTC;
    }

    /**
     * Obtiene el historial de una dirección de forma asíncrona a través de un
     * {@link ListenableFutureTask}.
     *
     * @param address Dirección en bytes.
     * @return Una tarea encargada de gestionar la petición.
     */
    @Override
    public ListenableFutureTask<List<ITransaction>> getHistoryByAddress(byte[] address) {
        String addressHex = Hex.toHexString(address);

        ListenableFutureTask<List<ITransaction>> task = ListenableFutureTask.create(() -> {
            List<ITransaction> history = new ArrayList<>();
            try {
                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<List<TxData>> response = mApi.getTxHistory(networkName, addressHex)
                        .execute();

                if (!response.isSuccessful() || response.body() == null)
                    return history;

                List<TxData> historyData = response.body();

                for (TxData data : historyData)
                    history.add(Transaction.fromTxData(data, mWallet));

                return history;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Ocurrió un error al realizar la petición al servidor: "
                        + e.getMessage());
            }

            return history;
        });

        mExecutor.execute(task);

        return task;
    }

    /**
     * Obtiene la transacción de forma asíncrona especificando su identificador único.
     *
     * @param txid Identificador de la transacción en bytes.
     * @return Una tarea encargada de gestionar la petición.
     */
    @Override
    public ListenableFutureTask<ITransaction> getTransactionByTxID(byte[] txid) {
        String txidHex = Hex.toHexString(txid);

        ListenableFutureTask<ITransaction> task = ListenableFutureTask.create(() -> {
            try {
                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<TxData> response = mApi.getTx(networkName, txidHex).execute();

                if (!response.isSuccessful() || response.body() == null)
                    return null;

                return Transaction.fromTxData(response.body(), mWallet);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Ocurrió un error al realizar la petición al servidor: "
                        + e.getMessage());
            }

            return null;
        });

        mExecutor.execute(task);

        return task;
    }

    /**
     * Obtiene la información de la punta de la blockchain.
     *
     * @return Una tarea encargada de gestionar la petición.
     */
    @Override
    public ListenableFutureTask<ChainTipInfo> getChainTipInfo() {
        ListenableFutureTask<ChainTipInfo> task = ListenableFutureTask.create(() -> {
            try {
                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<ChainInfo> response = mApi.getChainInfo(networkName).execute();

                if (!response.isSuccessful() || response.body() == null)
                    return null;

                ChainInfo info = response.body();

                return new ChainTipInfo.Builder()
                        .setHash(info.getHash())
                        .setHeight(info.getHeight())
                        .setTxn(info.getTxn())
                        .setTime(info.getTime())
                        .setNetwork(info.getNetwork())
                        .setStatus(info.getStatus())
                        .build();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Ocurrió un error al realizar la petición al servidor: "
                        + e.getMessage());
            }

            return null;
        });

        mExecutor.execute(task);

        return task;
    }

    /**
     * Propaga una nueva transacción por la red del cripto-activo.
     *
     * @param transaction Transacción a propagar.
     * @return Una tarea encargada de gestionar la petición.
     */
    @Override
    public ListenableFutureTask<Boolean> broadcastTx(ITransaction transaction) {
        return null;
    }

    /**
     * Obtiene las transacciones dependencia de la indicada por el identificador.
     *
     * @param txid Identificador de la transacción.
     * @return Una tarea encargada de gestionar la petición.
     */
    @Override
    public ListenableFutureTask<Map<String, ITransaction>> getDependencies(byte[] txid) {
        String txidHex = Hex.toHexString(txid);

        ListenableFutureTask<Map<String, ITransaction>> task = ListenableFutureTask.create(() -> {
            Map<String, ITransaction> deps = new HashMap<>();
            try {
                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<List<TxData>> response = mApi.getTxDeps(networkName, txidHex)
                        .execute();

                if (!response.isSuccessful() || response.body() == null)
                    return deps;

                List<TxData> depsData = response.body();

                for (TxData data : depsData) {
                    Transaction transaction = Transaction.fromTxData(data, mWallet);
                    deps.put(transaction.getTxId().toString(), transaction);
                }

                return deps;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Ocurrió un error al realizar la petición al servidor: "
                        + e.getMessage());
            }

            return deps;
        });

        mExecutor.execute(task);

        return task;
    }

    /**
     * Obtiene el historial de transacciones de multiples direcciones.
     *
     * @param addresses Direcciones a consultar.
     * @return Un tarea encargada de gestionar la petición.
     */
    @Override
    public ListenableFutureTask<List<ITransaction>> getHistory(byte[] addresses) {
        final String addressesHex = Hex.toHexString(addresses);

        ListenableFutureTask<List<ITransaction>> task = ListenableFutureTask.create(() -> {
            List<ITransaction> transactions = new ArrayList<>();
            try {
                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<List<TxData>> response = mApi.getHistory(networkName, addressesHex)
                        .execute();

                if (!response.isSuccessful() || response.body() == null)
                    return transactions;

                List<TxData> txData = response.body();

                for (TxData data : txData)
                    transactions.add(Transaction.fromTxData(data, mWallet));

                return transactions;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Ocurrió un error al realizar la petición al servidor: "
                        + e.getMessage());
            }

            return transactions;
        });

        mExecutor.execute(task);

        return task;
    }
}
