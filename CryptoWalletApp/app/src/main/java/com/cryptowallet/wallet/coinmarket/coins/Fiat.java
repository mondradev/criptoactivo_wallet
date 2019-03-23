package com.cryptowallet.wallet.coinmarket.coins;

import com.cryptowallet.wallet.SupportedAssets;

public class Fiat extends CoinBase {

    public Fiat(SupportedAssets asset) {
        super(asset, 2, 2);
    }

    @Override
    public String toStringFriendly() {
        return "$ " + super.toStringFriendly();
    }
}
