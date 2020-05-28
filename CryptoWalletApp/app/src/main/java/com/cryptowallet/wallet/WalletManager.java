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

import com.cryptowallet.utils.Consumer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
 * @version 1.0
 * @see IWallet
 * @see com.cryptowallet.services.IWalletProvider
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

    static {
        mWalletServices = new HashMap<>();
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
    public static Float getBalance() {
        Collection<IWallet> wallets = mWalletServices.values();
        Float amount = 0.0f;

        for (IWallet wallet : wallets)
            amount += wallet.getFiatBalance();

        return amount;
    }
}
