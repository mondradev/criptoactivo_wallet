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

package com.cryptowallet.wallet;

import android.util.Log;

import androidx.annotation.NonNull;

import com.cryptowallet.utils.Consumer;
import com.cryptowallet.utils.ExecutableConsumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

/**
 * Administrador de controladores de billeteras. Permite tener la funcionalidad de cada billetera
 * según sus funciones de la red del criptoactivo. Cada activo deberá tener un controlador de
 * billetera que implemente {@link IWallet} y registrarse en el administrador usando la función
 * {@link #registerWallet(IWallet)}.
 * <p></p>
 * Posteriormente se deberá invocar el controlador, especificando el criptoactivo que soporta de la
 * siguiente manera.
 * <pre>
 *
 *     WalletManager.get(SupportedAssets.BTC)
 *
 * </pre>
 * Y de esta forma acceder a las caracteristicas de la billetera.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.1
 * @see IWallet
 */
public abstract class WalletManager {

    /**
     * Etiqueta del log.
     */
    private static final String LOG_TAG = "WalletManager";

    /**
     * Colección de controladores de billetera.
     */
    private static Map<SupportedAssets, IWallet> mWalletServices;

    /***
     * Escuchas de cambio del saldo (fiat y cripto).
     */
    private static CopyOnWriteArraySet<ExecutableConsumer<Double>> mOnChangedBalanceListeners;

    /**
     * Escuchas de cambio del historial de transacciones.
     */
    private static CopyOnWriteArraySet<ExecutableConsumer<ITransaction>> mOnChangedTxHistoryListeners;

    // Constructor estático.
    static {
        mWalletServices = new HashMap<>();
        mOnChangedBalanceListeners = new CopyOnWriteArraySet<>();
        mOnChangedTxHistoryListeners = new CopyOnWriteArraySet<>();
    }

    /**
     * Remueve el escucha del cambio del saldo.
     *
     * @param listener Escucha a remover.
     */
    public static void removeChangedBalanceListener(@NonNull Consumer<Double> listener) {
        Objects.requireNonNull(listener);

        for (ExecutableConsumer<Double> executableConsumer : mOnChangedBalanceListeners)
            if (executableConsumer.getConsumer().equals(listener)) {
                mOnChangedBalanceListeners.remove(executableConsumer);
                break;
            }
    }


    /**
     * Remueve el escucha del cambio del historial de transacciones.
     *
     * @param listener Escucha a remover.
     */
    public static void removeChangedTxHistoryListener(@NonNull Consumer<ITransaction> listener) {
        Objects.requireNonNull(listener);

        for (ExecutableConsumer<ITransaction> executableConsumer : mOnChangedTxHistoryListeners)
            if (executableConsumer.getConsumer().equals(listener)) {
                mOnChangedTxHistoryListeners.remove(executableConsumer);
                break;
            }
    }

    /**
     * Añade un escucha del cambio del historial de transacciones de las billeteras.
     *
     * @param listener Escucha del historial.
     */
    public static void addChangedTxHistoryListener(@NonNull Executor executor,
                                                   @NonNull Consumer<ITransaction> listener) {
        Objects.requireNonNull(listener);

        for (ExecutableConsumer<ITransaction> executableConsumer : mOnChangedTxHistoryListeners)
            if (executableConsumer.getConsumer().equals(listener))
                return;

        mOnChangedTxHistoryListeners.add(new ExecutableConsumer<>(executor, listener));
    }

    /**
     * Añade un escucha del saldo de las billeteras.
     *
     * @param listener Escucha del saldo.
     */
    public static void addChangedBalanceListener(@NonNull Executor executor,
                                                 @NonNull Consumer<Double> listener) {
        Objects.requireNonNull(listener);

        for (ExecutableConsumer<Double> executableConsumer : mOnChangedBalanceListeners)
            if (executableConsumer.getConsumer().equals(listener))
                return;

        mOnChangedBalanceListeners.add(new ExecutableConsumer<>(executor, listener));
    }

    /**
     * @throws UnsupportedOperationException No es posible crear instancias.
     */
    private WalletManager() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Unable to create a new instance");
    }

    /**
     * Obtiene el controlador de billetera del activo especificado.
     *
     * @param asset Criptoactivo del controlador.
     * @return El controlador de billetera.
     */
    public static IWallet get(SupportedAssets asset) {
        if (asset.isFiat())
            throw new IllegalArgumentException();

        if (!mWalletServices.containsKey(asset))
            throw new IllegalArgumentException("The wallet doesn't exists");

        return mWalletServices.get(asset);
    }

    /**
     * Registra un nuevo controlador de billetera.
     *
     * @param wallet Instancia del controlador.
     */
    public static void registerWallet(IWallet wallet) {
        if (mWalletServices.containsKey(wallet.getAsset()))
            return;

        mWalletServices.put(wallet.getAsset(), wallet);

        Log.d(LOG_TAG, "Added wallet for " + wallet.getAsset().name());
    }

    /**
     * Borra el controlador de billetera del administrador.
     *
     * @param asset Activo que se desea eliminar el controlador.
     */
    public static void unregisterWallet(SupportedAssets asset) {
        mWalletServices.remove(asset);
    }

    /**
     * Indica si existe al menos una billetera almacenada en el dispositivo.
     *
     * @return Un true si existe.
     */
    public static boolean any() {
        for (IWallet wallet : mWalletServices.values())
            if (wallet.exists())
                return true;

        return false;
    }

    /**
     * Obtiene las transacciones de todas las billeteras.
     *
     * @return Lista de transacciones.
     */
    public static List<ITransaction> getTransactions() {
        Collection<IWallet> wallets = mWalletServices.values();
        List<ITransaction> txs = new ArrayList<>();

        for (IWallet wallet : wallets)
            txs.addAll(wallet.getTransactions());

        return txs;
    }

    /**
     * Ejecuta una función por el activo de cada billetera registrada.
     *
     * @param consumer Una función de consumo.
     */
    public static void forEachAsset(Consumer<SupportedAssets> consumer) {
        Collection<IWallet> wallets = mWalletServices.values();

        for (IWallet wallet : wallets)
            consumer.accept(wallet.getAsset());
    }

    /**
     * Obtiene el total en fiat de las billeteras registradas.
     *
     * @return Total en fiat.
     */
    public static double getBalance() {
        Collection<IWallet> wallets = mWalletServices.values();
        double amount = 0;

        for (IWallet wallet : wallets)
            amount += wallet.getFiatBalance();

        return amount;
    }

    /**
     * Notifica que el saldo de alguna de las billeteras ha cambiado su saldo.
     */
    public static void notifyChangedBalance() {
        final double balance = getBalance();
        for (ExecutableConsumer<Double> listener : mOnChangedBalanceListeners)
            listener.execute(balance);
    }

    /**
     * Nofica que el historial de transacciones de alguna billetera ha cambiado.
     *
     * @param newTx Transacción recibida.
     */
    public static void nofityChangedTxHistory(@NonNull final ITransaction newTx) {
        Objects.requireNonNull(newTx);

        for (ExecutableConsumer<ITransaction> listener : mOnChangedTxHistoryListeners)
            listener.execute(newTx);
    }

    /**
     * Obtiene la cantidad de billeteras registradas.
     * @return Cantidad de billeteras.
     */
    public static int getCount() {
        return mWalletServices.size();
    }
}
