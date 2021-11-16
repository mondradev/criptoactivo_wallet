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
import com.cryptowallet.assets.bitcoin.services.retrofit.ChainInfoResponse;
import com.cryptowallet.assets.bitcoin.services.retrofit.SuccessfulResponse;
import com.cryptowallet.assets.bitcoin.services.retrofit.TxDataResponse;
import com.cryptowallet.assets.bitcoin.wallet.BitcoinTransaction;
import com.cryptowallet.assets.bitcoin.wallet.BitcoinWallet;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.ChainTipInfo;
import com.google.common.util.concurrent.ListenableFutureTask;

import org.bouncycastle.util.encoders.Hex;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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
     * Reintentos máximos para completar una petición.
     */
    private static final int MAX_ATTEMPS = 3;

    /**
     * Etiqueta de log.
     */
    private static final String LOG_TAG = "Bitcoin Provider";

    /**
     * URL de la api.
     */
    private static final String WEBSERVICE_URL = "https://api.criptoactivo.innsytech.com/v1/";

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
    private final BitcoinWallet mWallet;

    /**
     * Crea una nueva instancia del proveedor de datos.
     *
     * @param wallet Parametros de red.
     */
    private BitcoinProvider(BitcoinWallet wallet) {
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
    public static BitcoinProvider get(BitcoinWallet wallet) {
        if (mInstance == null)
            mInstance = new BitcoinProvider(wallet);

        return mInstance;
    }

    /**
     * Intenta completar la acción o lo reintenta en {@link #MAX_ATTEMPS} ocasiones.
     *
     * @param request Acción a realizar.
     * @param <T>     Tipo del valor a retornar.
     * @return Resultado de la acción realiazda o un valor nulo si la operación falló más de
     * {@link #MAX_ATTEMPS} intentos.
     */
    private <T> T tryDo(Callable<T> request) {
        mWallet.propagateBitcoinJ();

        int maxAttemps = MAX_ATTEMPS;

        for (int attemp = 0; attemp < MAX_ATTEMPS; attemp++) {
            try {
                T response = request.call();

                if (response != null)
                    return response;
            } catch (ConnectException e) {
                Log.e(LOG_TAG, "" + e.getMessage());
                Utils.tryNotThrow(() -> Thread.sleep(5000));
            } catch (Exception e) {
                Log.e(LOG_TAG, "Unable to complete the request: " + e.getMessage());
                Utils.tryNotThrow(() -> Thread.sleep(10000));
                maxAttemps = maxAttemps > MAX_ATTEMPS ? maxAttemps : 10;
            } finally {
                Thread.currentThread().setName("Bitcoin Provider");
            }
        }

        return null;
    }

    /**
     * Obtiene el historial de una dirección de forma asíncrona a través de un
     * {@link ListenableFutureTask}.
     *
     * @param address Dirección en bytes.
     * @return Una tarea encargada de gestionar la petición.
     */
    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    public List<BitcoinTransaction> getHistoryByAddress(byte[] address, int height)
            throws ExecutionException, InterruptedException {
        final String addressHex = Hex.toHexString(address);
        final List<BitcoinTransaction> history = new ArrayList<>();
        ListenableFutureTask<List<BitcoinTransaction>> task = ListenableFutureTask.create(() -> {
            Thread.currentThread().setName("Bitcoin Provider getHistoryByAddress");

            return tryDo(() -> {
                history.clear();
                Log.d(LOG_TAG, "Request history by address: " + addressHex);
                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<List<TxDataResponse>> response = mApi.getTxHistory(networkName, addressHex, height)
                        .execute();

                if (!response.isSuccessful() || response.body() == null)
                    return history;

                List<TxDataResponse> historyData = response.body();

                for (TxDataResponse data : historyData)
                    history.add(BitcoinTransaction.fromTxData(data, mWallet));

                return history;
            });
        });

        mExecutor.execute(task);

        return task.get();
    }

    /**
     * Obtiene la transacción de forma asíncrona especificando su identificador único.
     *
     * @param txid Identificador de la transacción en bytes.
     * @return Una tarea encargada de gestionar la petición.
     */
    public BitcoinTransaction getTransactionByTxID(byte[] txid) throws ExecutionException, InterruptedException {
        String txidHex = Hex.toHexString(txid);

        ListenableFutureTask<BitcoinTransaction> task = ListenableFutureTask.create(() -> {
            Thread.currentThread().setName("Bitcoin Provider getTransactionByTxID");
            return tryDo(() -> {
                Log.d(LOG_TAG, "Request transaction: " + txidHex);

                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<TxDataResponse> response = mApi.getTx(networkName, txidHex).execute();

                if (!response.isSuccessful() || response.body() == null)
                    return null;

                return BitcoinTransaction.fromTxData(response.body(), mWallet);
            });
        });

        mExecutor.execute(task);

        return task.get();
    }

    /**
     * Obtiene la información de la punta de la blockchain.
     *
     * @return Una tarea encargada de gestionar la petición.
     */
    public ChainTipInfo getChainTipInfo() throws ExecutionException, InterruptedException {
        ListenableFutureTask<ChainTipInfo> task = ListenableFutureTask.create(() -> {
            Thread.currentThread().setName("Bitcoin Provider getChainTipInfo");
            return tryDo(() -> {
                Log.d(LOG_TAG, "Request chaininfo");

                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<ChainInfoResponse> response = mApi.getChainInfo(networkName).execute();

                if (!response.isSuccessful() || response.body() == null)
                    return null;

                ChainInfoResponse info = response.body();

                return new ChainTipInfo.Builder()
                        .setHash(info.getHash())
                        .setHeight(info.getHeight())
                        .setTxn(info.getTxn())
                        .setTime(info.getTime())
                        .setNetwork(info.getNetwork())
                        .setStatus(info.getStatus())
                        .build();
            });
        });

        mExecutor.execute(task);

        return task.get();
    }

    /**
     * Propaga una nueva transacción por la red del cripto-activo.
     *
     * @param transaction Transacción a propagar.
     * @return Una tarea encargada de gestionar la petición.
     */
    public Boolean broadcastTx(BitcoinTransaction transaction) {
        if (transaction == null)
            throw new NullPointerException("Transaction is null");

        String hexTx = Hex.toHexString(transaction.serialize());

        ListenableFutureTask<Boolean> task = ListenableFutureTask.create(() -> {
            Thread.currentThread().setName("Bitcoin Provider broadcastTx");
            return tryDo(() -> {
                Log.d(LOG_TAG, "Request broadcast: " + hexTx);

                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<SuccessfulResponse> response = mApi.broadcastTx(networkName, hexTx)
                        .execute();

                if (!response.isSuccessful() || response.body() == null)
                    return false;

                return response.body().isSuccessful();
            });
        });

        mExecutor.execute(task);

        return Utils.tryReturnBoolean(task::get, false);
    }

    /**
     * Obtiene las transacciones dependencia de la indicada por el identificador.
     *
     * @param txid Identificador de la transacción.
     * @return Una tarea encargada de gestionar la petición.
     */
    public Map<String, BitcoinTransaction> getDependencies(byte[] txid)
            throws ExecutionException, InterruptedException {
        String txidHex = Hex.toHexString(txid);

        ListenableFutureTask<Map<String, BitcoinTransaction>> task = ListenableFutureTask.create(() -> {
            Thread.currentThread().setName("Bitcoin Provider getDependencies");
            final Map<String, BitcoinTransaction> deps = new HashMap<>();
            return tryDo(() -> {
                deps.clear();
                Log.d(LOG_TAG, "Request dependencies: " + txidHex);

                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<List<TxDataResponse>> response = mApi.getTxDeps(networkName, txidHex)
                        .execute();

                if (!response.isSuccessful() || response.body() == null)
                    return deps;

                List<TxDataResponse> depsData = response.body();

                for (TxDataResponse data : depsData) {
                    BitcoinTransaction transaction = BitcoinTransaction.fromTxData(data, mWallet);
                    deps.put(transaction.getID(), transaction);
                }

                return deps;
            });
        });

        mExecutor.execute(task);

        return task.get();
    }

    /**
     * Obtiene el historial de transacciones de multiples direcciones.
     *
     * @param addresses Direcciones a consultar.
     * @return Un tarea encargada de gestionar la petición.
     */
    public List<BitcoinTransaction> getHistory(byte[] addresses, int height)
            throws ExecutionException, InterruptedException {
        final String addressesHex = Hex.toHexString(addresses);

        ListenableFutureTask<List<BitcoinTransaction>> task = ListenableFutureTask.create(() -> {
            final List<BitcoinTransaction> transactions = new ArrayList<>();
            Thread.currentThread().setName("Bitcoin Provider getHistory");
            return tryDo(() -> {
                Log.d(LOG_TAG, "Request history: " + addressesHex);

                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<List<TxDataResponse>> response = mApi
                        .getHistory(networkName, addressesHex, height).execute();

                if (!response.isSuccessful() || response.body() == null)
                    return transactions;

                List<TxDataResponse> txData = response.body();

                for (TxDataResponse data : txData)
                    transactions.add(BitcoinTransaction.fromTxData(data, mWallet));

                return transactions;
            });
        });

        mExecutor.execute(task);

        return task.get();
    }

    /**
     * Registra el token en el servidor para poder recibir notificaciones.
     *
     * @param token     Token de notificaciones push (FCM)
     * @param walletId  Identificador de la billetera.
     * @param addresses Direcciones a registrar.
     * @return Un true si la subscripción finalizó correctamente.
     */
    public boolean subscribe(String token, String walletId, byte[] addresses) {
        if (walletId == null)
            throw new NullPointerException("WalletId is null");

        if (token == null)
            throw new NullPointerException("Token is null");

        if (addresses == null || addresses.length < 210)
            throw new IllegalArgumentException("Requires at least 100 address");

        ListenableFutureTask<Boolean> task = ListenableFutureTask.create(() -> {
            Thread.currentThread().setName("Bitcoin Provider subscribe");
            return tryDo(() -> {
                Log.d(LOG_TAG, "Request subscribe: " + walletId);
                String networkName = mWallet.getNetwork().getPaymentProtocolId() + "net";
                Response<SuccessfulResponse> response
                        = mApi.subscribe(networkName, token, walletId, Hex.toHexString(addresses))
                        .execute();

                if (!response.isSuccessful() || response.body() == null)
                    return false;

                return response.body().isSuccessful();
            });
        });

        mExecutor.execute(task);

        return Utils.tryReturnBoolean(task::get, false);
    }
}
