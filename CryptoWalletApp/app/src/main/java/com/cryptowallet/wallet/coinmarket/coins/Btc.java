package com.cryptowallet.wallet.coinmarket.coins;

import com.cryptowallet.wallet.SupportedAssets;

public final class Btc extends CoinBase {
    public Btc(long value) {
        super(SupportedAssets.BTC, 2, 8);
        setValue(value);
    }

    public Btc() {
        this(0);
    }
}
