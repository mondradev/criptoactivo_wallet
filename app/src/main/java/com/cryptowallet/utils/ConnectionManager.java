package com.cryptowallet.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import java.util.Objects;

public final class ConnectionManager {


    public static boolean isWifiConnected(final Context context) {
        return hasInternetConnection(context, NetworkCapabilities.TRANSPORT_WIFI);
    }

    public static boolean isCellularConnected(final Context context) {
        return hasInternetConnection(context, NetworkCapabilities.TRANSPORT_CELLULAR);
    }

    private static boolean hasInternetConnection(final Context context, int networkTransport) {

        Objects.requireNonNull(context);

        final ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        for (Network network : cm.getAllNetworks()) {
            NetworkCapabilities nc = cm.getNetworkCapabilities(network);

            if (nc == null) return false;

            if (nc.hasTransport(networkTransport)
                    && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                return true;
        }

        return false;
    }

    public static void registerHandlerConnection(final Context context,
                                                 final OnChangeConnectionState listener) {

        Objects.requireNonNull(listener);
        Objects.requireNonNull(context);

        final ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        cm.registerNetworkCallback(
                new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build(),
                new ConnectivityManager.NetworkCallback() {
                    private boolean mWifiConnected = false;
                    private boolean mCellularConnected = false;

                    @Override
                    public void onAvailable(Network network) {
                        NetworkInterface ni = getInterface(cm.getNetworkCapabilities(network));
                        mCellularConnected = ni == NetworkInterface.CELLULAR;
                        mWifiConnected = ni == NetworkInterface.WIFI;
                        listener.onConnect(ni);
                    }

                    private NetworkInterface getInterface(NetworkCapabilities capabilities) {
                        if (capabilities == null)
                            return null;

                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                            return NetworkInterface.CELLULAR;
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                            return NetworkInterface.WIFI;
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH))
                            return NetworkInterface.BLUETOOTH;
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                            return NetworkInterface.ETHERNET;
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
                            return NetworkInterface.VPN;
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE))
                            return NetworkInterface.WIFI_AWARE;
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN))
                            return NetworkInterface.LOWPAN;

                        return null;
                    }

                    @Override
                    public void onLost(Network network) {
                        NetworkInterface ni = getInterface(cm.getNetworkCapabilities(network));

                        if ((ni == NetworkInterface.WIFI && mWifiConnected)
                                || (ni == NetworkInterface.CELLULAR && mCellularConnected))
                            listener.onDisconnect(ni);
                    }

                    @Override
                    public void onLosing(Network network, int maxMsToLive) {
                        NetworkInterface ni = getInterface(cm.getNetworkCapabilities(network));

                        mCellularConnected = !(ni == NetworkInterface.CELLULAR && mCellularConnected);
                        mWifiConnected = !(ni == NetworkInterface.WIFI && mWifiConnected);

                        listener.onDisconnect(ni);
                    }
                }
        );

    }

    public enum NetworkInterface {
        CELLULAR,
        WIFI,
        BLUETOOTH,
        ETHERNET,
        VPN,
        WIFI_AWARE,
        LOWPAN
    }

    public interface OnChangeConnectionState {

        void onConnect(NetworkInterface network);

        void onDisconnect(NetworkInterface network);

    }

}
