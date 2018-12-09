package com.cryptowallet.wallet;


import android.content.Context;

import com.cryptowallet.app.AppPreference;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HttpsURLConnection;

public final class ExchangeService {

    private static final String USD_URL = "https://api.bitfinex.com/v1/pubticker/btcusd";
    private static final String MXN_URL = "https://api.bitso.com/v3/ticker/?book=btc_mxn";

    private static CopyOnWriteArrayList<ExchangeServiceListener> mListeners
            = new CopyOnWriteArrayList<>();

    private static Exchangeable BitcoinCurrency = new Exchangeable() {
        @Override
        public String ToStringFriendly(SupportedAssets currencyBase, long value) {
            Coin btcValue = Coin.valueOf(value);

            switch (currencyBase) {
                case BTC:
                    return btcValue.toFriendlyString();
                default:
                    return "";
            }
        }
    };

    private static FutureTask<Long> mGetUsdPriceTask = new FutureTask<>(new UsdCallable());
    private static FutureTask<Long> mGetMxnPriceTask = new FutureTask<>(new MxnCallable());

    private static Map<SupportedAssets, Exchangeable> mCurrencies
            = new HashMap<>();

    private static Logger mLogger = LoggerFactory.getLogger(ExchangeService.class);

    private static Exchangeable UsdCurrency = new Exchangeable() {
        @Override
        public String ToStringFriendly(SupportedAssets currencyBase, long value) {

            switch (currencyBase) {
                case BTC:
                    return Fiat.valueOf(SupportedAssets.USD.name(), btcToUsd(value)).toFriendlyString();
                default:
                    return "";
            }
        }
    };
    private static Exchangeable MexCurrency = new Exchangeable() {
        @Override
        public String ToStringFriendly(SupportedAssets currencyBase, long value) {
            switch (currencyBase) {
                case BTC:
                    return Fiat.valueOf(SupportedAssets.MXN.name(), btcToMxn(value)).toFriendlyString();
                default:
                    return "";
            }
        }
    };

    static {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(mGetUsdPriceTask);
        executor.execute(mGetMxnPriceTask);

        mCurrencies.put(SupportedAssets.BTC, ExchangeService.BitcoinCurrency);
        mCurrencies.put(SupportedAssets.USD, ExchangeService.UsdCurrency);
        mCurrencies.put(SupportedAssets.MXN, ExchangeService.MexCurrency);
    }

    public static void reloadMarketPrice() {
        mGetUsdPriceTask = new FutureTask<>(new UsdCallable());
        mGetMxnPriceTask = new FutureTask<>(new MxnCallable());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(mGetUsdPriceTask);
        executor.execute(mGetMxnPriceTask);
    }

    private static String readStream(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }

        return sb.toString();
    }

    public static long btcToUsd(long smallestUnit) {
        try {
            return new ExchangeRate(
                    Coin.COIN, Fiat.valueOf(SupportedAssets.USD.name(),
                    mGetUsdPriceTask.get(600, TimeUnit.MILLISECONDS)))
                    .coinToFiat(Coin.valueOf(smallestUnit)).getValue();
        } catch (TimeoutException | ExecutionException | InterruptedException info) {
            mLogger.info("Falla al realizar la conversión: {}, excepción: {}",
                    SupportedAssets.USD, info.getMessage());
        }
        return 0L;
    }

    private static long btcToMxn(long smallestUnit) {
        try {
            return new ExchangeRate(
                    Coin.COIN, Fiat.valueOf(SupportedAssets.MXN.name(),
                    mGetMxnPriceTask.get(600, TimeUnit.MILLISECONDS)))
                    .coinToFiat(Coin.valueOf(smallestUnit)).getValue();
        } catch (TimeoutException | ExecutionException | InterruptedException info) {
            mLogger.info("Falla al realizar la conversión: {}, excepción: {}",
                    SupportedAssets.MXN, info.getMessage());
        }
        return 0L;
    }

    private static JSONObject request(String url) {
        try {
            HttpsURLConnection urlConnection
                    = (HttpsURLConnection) new URL(url).openConnection();

            int responseCode = urlConnection.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String response = readStream(urlConnection.getInputStream());
                return new JSONObject(response);
            }

        } catch (JSONException | IOException info) {
            mLogger.info("Falla al consultar: {}, excepción: {}", url, info.getMessage());
        }
        return null;
    }

    public static Exchangeable getExchange(SupportedAssets symbol) {
        return mCurrencies.get(symbol);
    }

    public static String btcToSelectedFiat(Context context, long value) {
        return getExchange(SupportedAssets.valueOf(AppPreference.getSelectedCurrency(context)))
                .ToStringFriendly(SupportedAssets.BTC, value);
    }

    private static void notifyListeners(final SupportedAssets assets, final long smallestUnit) {
        for (final ExchangeServiceListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onResponse(assets, smallestUnit);
                }
            });
    }

    public static void addEventListener(ExchangeServiceListener listener) {
        if (!mListeners.contains(listener))
            mListeners.add(listener);

        completedTasks(listener);
    }

    private static void completedTasks(ExchangeServiceListener listener) {
        try {
            if (mGetMxnPriceTask.isDone())
                listener.onResponse(SupportedAssets.MXN, mGetMxnPriceTask.get());
            if (mGetUsdPriceTask.isDone())
                listener.onResponse(SupportedAssets.USD, mGetUsdPriceTask.get());
        } catch (ExecutionException | InterruptedException ignored) {

        }
    }

    public static void removeEventListener(ExchangeServiceListener listener) {
        mListeners.remove(listener);
    }

    public interface Exchangeable {
        String ToStringFriendly(SupportedAssets currencyBase, long value);
    }

    public interface ExchangeServiceListener {
        void onResponse(SupportedAssets asset, long smallestUnit);
    }

    private static class UsdCallable implements Callable<Long> {

        @Override
        public Long call() throws Exception {
            JSONObject response = request(USD_URL);
            long value = 1L;

            if (response == null) return value;

            value = (long) (response.getDouble("last_price") * 10000);

            notifyListeners(SupportedAssets.USD, value);

            return value;
        }
    }

    private static class MxnCallable implements Callable<Long> {

        @Override
        public Long call() throws Exception {
            JSONObject response = request(MXN_URL);
            long value = 1L;

            if (response == null) return value;

            value = (long) (response.getJSONObject("payload").getDouble("last") * 10000);

            notifyListeners(SupportedAssets.MXN, value);

            return value;
        }
    }

}
