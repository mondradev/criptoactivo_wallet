/*
 * Copyright 2018 InnSy Tech
 * Copyright 2018 Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * Permite agregar escuchas de eventos y determinar si la interface de WiFi tiene conexión a
 * internet.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public final class WifiManager {

    /**
     * Escuchas de las conexión y desconexión de la interface Wifi.
     */
    private static CopyOnWriteArrayList<IListener> mListeners
            = new CopyOnWriteArrayList<>();

    /**
     * Obtiene un valor que indica si la conexión WiFi tiene conexión a internet.
     *
     * @param context Contexto de la aplicación de Android.
     * @return Un valor true si hay internet.
     */
    public static boolean hasInternet(final Context context) {
        Objects.requireNonNull(context);

        final ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        for (Network network : cm.getAllNetworks()) {
            NetworkCapabilities nc = cm.getNetworkCapabilities(network);

            if (nc == null) return false;

            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                return true;
        }

        return false;
    }

    /**
     * Inicializa el administrador de WiFi.
     *
     * @param context Contexto de la aplicación Andrdoid.
     */
    public static void init(final Context context) {
        Objects.requireNonNull(context);

        final ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        cm.registerNetworkCallback(
                new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build(),
                new ConnectivityManager.NetworkCallback() {

                    @Override
                    public void onAvailable(Network network) {
                        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);

                        Boolean isWifi = capabilities
                                .hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                        Boolean hasInternet = capabilities
                                .hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

                        if (isWifi && hasInternet)
                            notifyConnect();
                    }

                    @Override
                    public void onLost(Network network) {
                        notifyDisconnect();
                    }
                }
        );

    }

    /**
     * Notifica a los escuchas que ha surgido el evento {@link IListener#onConnect()}.
     */
    private static void notifyConnect() {
        for (final IListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onConnect();
                }
            });
    }

    /**
     * Notifica a los escuchas que ha surgido el evento {@link IListener#onDisconnect()}.
     */
    private static void notifyDisconnect() {
        for (final IListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onDisconnect();
                }
            });
    }

    /**
     * Agrega un escucha para los eventos desencadenados por la interface WiFi.
     *
     * @param listener Escucha de los eventos.
     * @see IListener
     */
    public static void addEventListener(IListener listener) {
        if (mListeners.contains(listener))
            return;

        mListeners.add(listener);
    }

    /**
     * Remueve el escucha de eventos especificado.
     *
     * @param listener Escucha de los eventos.
     */
    public static void removeEventListener(IListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Provee una estructura para la escucha de los eventos del administrador de conexiones.
     *
     * @author Ing. Javier Flores
     * @version 1.1
     */
    public interface IListener {

        /**
         * Este método se desencadena cuando la interface es conectada.
         */
        void onConnect();

        /**
         * Este método se desencadena cuando la interface es desconectada.
         */
        void onDisconnect();

    }

}
