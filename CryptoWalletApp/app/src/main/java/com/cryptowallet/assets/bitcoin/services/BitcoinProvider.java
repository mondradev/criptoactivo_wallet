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
import com.cryptowallet.wallet.ChainTipInfo;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.TransactionState;
import com.google.common.util.concurrent.ListenableFutureTask;

import org.bitcoinj.wallet.WalletTransaction;
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
public class BitcoinProvider {

    /**
     * Etiqueta de log.
     */
    private static final String LOG_TAG = "Bitcoin Provider";

    /**
     * URL de la api.
     */
    private static final String WEBSERVICE_URL = "http://innsytech.com:5000/api/v1/";

    /**
     * Instancia del singleton.
     */
    private static BitcoinProvider mInstance;

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
    public static BitcoinProvider get(Wallet wallet) {
        if (mInstance == null)
            mInstance = new BitcoinProvider(wallet);

        return mInstance;
    }

    /**
     * Obtiene el historial de una dirección de forma asíncrona a través de un
     * {@link ListenableFutureTask}.
     *
     * @param address Dirección en bytes.
     * @return Una tarea encargada de gestionar la petición.
     */
    public ListenableFutureTask<List<WalletTransaction>> getHistoryByAddress(byte[] address) {
        String addressHex = Hex.toHexString(address);

        ListenableFutureTask<List<WalletTransaction>> task = ListenableFutureTask.create(() -> {
            List<WalletTransaction> history = new ArrayList<>();
            try {
                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<List<TxData>> response = mApi.getTxHistory(networkName, addressHex)
                        .execute();

                if (!response.isSuccessful() || response.body() == null)
                    return history;

                List<TxData> historyData = response.body();

                for (TxData data : historyData)
                    history.add(new WalletTransaction(parsePool(data.getState()),
                            Transaction.fromTxData(data, mWallet)));

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
    public ListenableFutureTask<WalletTransaction> getTransactionByTxID(byte[] txid) {
        String txidHex = Hex.toHexString(txid);

        ListenableFutureTask<WalletTransaction> task = ListenableFutureTask.create(() -> {
            try {
                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<TxData> response = mApi.getTx(networkName, txidHex).execute();

                if (!response.isSuccessful() || response.body() == null)
                    return null;

                return new WalletTransaction(parsePool(response.body().getState()),
                        Transaction.fromTxData(response.body(), mWallet));
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
    public ListenableFutureTask<Boolean> broadcastTx(ITransaction transaction) {
        return null;
    }

    /**
     * Obtiene las transacciones dependencia de la indicada por el identificador.
     *
     * @param txid Identificador de la transacción.
     * @return Una tarea encargada de gestionar la petición.
     */
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
    public ListenableFutureTask<List<WalletTransaction>> getHistory(byte[] addresses) {
        final String addressesHex = Hex.toHexString(addresses);

        ListenableFutureTask<List<WalletTransaction>> task = ListenableFutureTask.create(() -> {
            List<WalletTransaction> transactions = new ArrayList<>();
            try {
                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<List<TxData>> response = mApi.getHistory(networkName, addressesHex)
                        .execute();

                if (!response.isSuccessful() || response.body() == null)
                    return transactions;

                List<TxData> txData = response.body();

                for (TxData data : txData)
                    transactions.add(new WalletTransaction(parsePool(data.getState()),
                            Transaction.fromTxData(data, mWallet)));

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

    /**
     * Parsea el estado de la transacción respecto a su dirección.
     *
     * @param stateValue Representación en texto del estado de la transacción.
     * @return Estado de la transacción.
     */
    private static WalletTransaction.Pool parsePool(String stateValue) {
        TransactionState state = Enum.valueOf(TransactionState.class, stateValue);

        switch (state) {
            case SPENT:
                return WalletTransaction.Pool.SPENT;
            case PENDING:
                return WalletTransaction.Pool.PENDING;
            case UNSPENT:
                return WalletTransaction.Pool.UNSPENT;
        }

        return WalletTransaction.Pool.DEAD;
    }
}
