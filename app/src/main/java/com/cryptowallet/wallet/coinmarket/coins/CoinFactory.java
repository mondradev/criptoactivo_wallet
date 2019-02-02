package com.cryptowallet.wallet.coinmarket.coins;

import com.cryptowallet.wallet.SupportedAssets;

public final class CoinFactory {

    private static CoinBase generate(SupportedAssets asset) {
        switch (asset) {
            case USD:
                return new Usd();
            case MXN:
                return new Mxn();
            case BTC:
            default:
                return new Btc();
        }
    }

    public static CoinBase valueOf(SupportedAssets asset, long value) {
        CoinBase coin = generate(asset);

        coin.setValue(value);
        return coin;
    }

    public static CoinBase parse(SupportedAssets asset, String src) {
        CoinBase coin = generate(asset);
        double value;

        try {
            if (src.endsWith("."))
                value = Double.parseDouble(src.substring(0, src.length() - 1));
            else
                value = Double.parseDouble(src);
        } catch (NumberFormatException ignored) {
            value = 0;
        }

        coin.setValue((long) (value * Math.pow(10, coin.getMaxDecimals())));
        return coin;
    }

    public static CoinBase getZero(SupportedAssets asset) {
        return valueOf(asset, 0);
    }

    public static CoinBase getOne(SupportedAssets asset) {
        CoinBase coin = getZero(asset);
        return valueOf(asset, (long) Math.pow(10, coin.getMaxDecimals()));
    }

}
