package com.cryptowallet.wallet;


import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.net.ssl.HttpsURLConnection;

public final class ExchangeService {

    private static final String USD_URL = "https://api.bitfinex.com/v1/pubticker/btcusd";
    private static final String MXN_URL = "https://api.bitso.com/v3/ticker/?book=btc_mxn";
    public static Exchangeable BitcoinCurrency = new Exchangeable() {
        @Override
        public String ToStringFriendly(Currencies currencyBase, long value) {
            Coin btcValue = Coin.valueOf(value);

            switch (currencyBase) {
                case BTC:
                    return btcValue.toFriendlyString();
                default:
                    return "";
            }
        }
    };
    private static Map<Currencies, Exchangeable> mCurrencies
            = new HashMap<>();
    private static FutureTask<Long> mGetUsdPriceTask = new FutureTask<>(new UsdCallable());

    public static Exchangeable UsdCurrency = new Exchangeable() {
        @Override
        public String ToStringFriendly(Currencies currencyBase, long value) {

            switch (currencyBase) {
                case BTC:
                    return Fiat.valueOf(Currencies.USD.name(), btcToUsd(value)).toFriendlyString();
                default:
                    return "";
            }
        }
    };
    private static FutureTask<Long> mGetMxnPriceTask = new FutureTask<>(new MxnCallable());
    public static Exchangeable MexCurrency = new Exchangeable() {
        @Override
        public String ToStringFriendly(Currencies currencyBase, long value) {
            switch (currencyBase) {
                case BTC:
                    return Fiat.valueOf(Currencies.MXN.name(), btcToMxn(value)).toFriendlyString();
                default:
                    return "";
            }
        }
    };

    static {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(mGetUsdPriceTask);
        executor.execute(mGetMxnPriceTask);

        mCurrencies.put(ExchangeService.Currencies.BTC, ExchangeService.BitcoinCurrency);
        mCurrencies.put(ExchangeService.Currencies.USD, ExchangeService.UsdCurrency);
        mCurrencies.put(ExchangeService.Currencies.MXN, ExchangeService.MexCurrency);
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
                    Coin.COIN, Fiat.valueOf(Currencies.USD.name(), mGetUsdPriceTask.get()))
                    .coinToFiat(Coin.valueOf(smallestUnit)).getValue();
        } catch (ExecutionException | InterruptedException ignored) {

        }
        return 0L;
    }

    public static long btcToMxn(long smallestUnit) {
        try {
            return new ExchangeRate(
                    Coin.COIN, Fiat.valueOf(Currencies.MXN.name(), mGetMxnPriceTask.get()))
                    .coinToFiat(Coin.valueOf(smallestUnit)).getValue();
        } catch (ExecutionException | InterruptedException ignored) {

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

        } catch (JSONException | IOException ignored) {
        }
        return null;
    }

    public static Exchangeable getExchange(Currencies symbol) {
        return mCurrencies.get(symbol);
    }


    public enum Currencies {
        BTC,
        USD,
        MXN
    }

    public interface Exchangeable {
        String ToStringFriendly(ExchangeService.Currencies currencyBase, long value);
    }

    private static class UsdCallable implements Callable<Long> {

        @Override
        public Long call() throws Exception {
            JSONObject response = request(USD_URL);

            if (response == null) return 0L;

            return (long) (response.getDouble("last_price") * 10000);
        }
    }

    private static class MxnCallable implements Callable<Long> {

        @Override
        public Long call() throws Exception {
            JSONObject response = request(MXN_URL);

            if (response == null) return 0L;

            return (long) (response.getJSONObject("payload").getDouble("last") * 10000);
        }
    }

}
