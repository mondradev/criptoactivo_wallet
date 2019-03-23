package com.cryptowallet.wallet.coinmarket.coins;

import com.cryptowallet.wallet.SupportedAssets;


public final class Usd extends Fiat {

    public Usd(long value) {
        super(SupportedAssets.USD);
        setValue(value);
    }

    public Usd() {
        this(0);
    }
}
