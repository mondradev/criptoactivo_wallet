package com.cryptowallet.wallet.coinmarket.coins;

import com.cryptowallet.wallet.SupportedAssets;

import java.text.DecimalFormat;

public class CoinBase implements Cloneable {

    private int mMinDecimals;
    private int mMaxDecimals;

    private long mValue;

    private SupportedAssets mAsset;

    public CoinBase(SupportedAssets asset, int minDecimals, int maxDecimals) {
        this.mMinDecimals = minDecimals;
        this.mMaxDecimals = maxDecimals;
        this.mAsset = asset;
        this.mValue = 0;
    }

    private static String getPattern(int minDecimals, int maxDecimals) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (; i < minDecimals; i++)
            builder.append("0");

        for (; i < maxDecimals; i++)
            builder.append("#");

        return builder.toString();
    }

    public CoinBase add(CoinBase right) {
        this.mValue += right.mValue;
        return this;
    }

    public CoinBase substract(CoinBase right) {
        this.mValue -= right.mValue;
        return this;
    }

    public CoinBase multiply(CoinBase right) {
        this.mValue *= right.mValue;
        return this;
    }

    public CoinBase divide(CoinBase right) {
        if (right.isZero())
            return this;

        this.mValue /= right.mValue;
        return this;
    }

    public int getMaxDecimals() {
        return mMaxDecimals;
    }

    public long getValue() {
        return mValue;
    }

    public void setValue(long value) {
        mValue = value;
    }

    public String toPlainString() {
        DecimalFormat format
                = new DecimalFormat("0." + getPattern(mMinDecimals, mMaxDecimals));
        return format.format(((double) (mValue)) / Math.pow(10, mMaxDecimals));
    }

    public String toStringFriendly() {
        DecimalFormat format = new DecimalFormat(
                "0." + getPattern(mMinDecimals, mMaxDecimals) + " " + mAsset.name());
        return format.format(((double) (mValue)) / Math.pow(10, mMaxDecimals));
    }

    public CoinBase getUnsigned() {
        try {
            CoinBase coin = (CoinBase) this.clone();
            if (coin.mValue < 0)
                coin.mValue = Math.abs(coin.mValue);

            return coin;
        } catch (CloneNotSupportedException ignored) {

        }
        return null;
    }

    public SupportedAssets getAsset() {
        return mAsset;
    }

    public boolean isNegative() {
        return mValue < 0;
    }

    public boolean isPositive() {
        return mValue > 0;
    }

    public boolean isZero() {
        return mValue == 0;
    }
}
