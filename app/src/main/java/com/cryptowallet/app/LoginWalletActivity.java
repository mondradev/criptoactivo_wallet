package com.cryptowallet.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.Helper;
import com.cryptowallet.utils.IdealPasswordParameter;

import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.utils.Threading;
import org.spongycastle.crypto.params.KeyParameter;

import java.util.Objects;

public class LoginWalletActivity extends ActivityBase {

    private static final int MIN_LENGTH = 4;
    private final int MAX_ATTEMP = 3;
    private boolean mAuthenticated = false;
    private boolean mRegMode;
    private boolean mToCommit;
    private int mCountDigit;
    private String[] mPinValues = new String[4];
    private String[] mPinToCommit;
    private Dialog mDialogOnLoad;
    private byte[] mKeyPrev;
    private ImageView[] mPinDigitViews = new ImageView[4];
    private boolean mRequirePin = false;
    private int mAttemp;
    private boolean mLocked = false;
    private boolean mHasError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_wallet);

        setTitle(R.string.authenticate_title);

        Intent intent = getIntent();

        if (intent.hasExtra(ExtrasKey.REG_PIN)) {
            mRegMode = true;
            mRequirePin = BitcoinService.get().requireDecrypted();
        }

        mPinDigitViews[0] = findViewById(R.id.mPin1);
        mPinDigitViews[1] = findViewById(R.id.mPin2);
        mPinDigitViews[2] = findViewById(R.id.mPin3);
        mPinDigitViews[3] = findViewById(R.id.mPin4);

    }

    public void handlerPad(View view) {
        if (mLocked)
            return;

        if (mHasError) {
            hideInfo();
            mHasError = true;
        }

        Button mPad = (Button) view;

        if (mCountDigit >= mPinDigitViews.length)
            return;

        mPinValues[mCountDigit] = mPad.getText().toString();

        fillPin(mCountDigit);

        mCountDigit++;

        if (hasMinLenght(mCountDigit)) {

            Thread task = new Thread(new Runnable() {
                @Override
                public void run() {
                    mLocked = true;

                    if (mRegMode && (!mRequirePin || mKeyPrev != null)) {

                        if (mToCommit) {

                            if (!pinEquals(mPinValues, mPinToCommit)) {
                                setInfo(R.string.pin_no_equal);
                                mPinValues = new String[4];
                                mToCommit = false;
                                mCountDigit = 0;

                                cleanPin();
                            } else
                                encryptWallet();

                        } else {
                            mCountDigit = 0;
                            mPinToCommit = mPinValues;
                            mToCommit = true;

                            cleanPin();

                            setInfo(R.string.commit_pin);
                        }

                    } else {

                        if (mAuthenticated) {
                            mLocked = false;
                            return;
                        }

                        byte[] dataPin = getBytes(mPinValues);
                        if (BitcoinService.get().validatePin(dataPin)) {

                            if (mRequirePin) {
                                setInfo(R.string.indications_pin_setup);
                                mKeyPrev = dataPin;
                                mCountDigit = 0;
                                cleanPin();
                            } else {
                                mAuthenticated = true;
                                Intent intent = new Intent();
                                intent.putExtra(ExtrasKey.PIN_DATA, dataPin);
                                setResult(Activity.RESULT_OK, intent);
                                finish();
                            }
                        } else {

                            setInfo(R.string.error_pin);
                            mHasError = true;
                            mCountDigit = 0;
                            mPinValues = new String[4];
                            mAttemp++;

                            if (mAttemp >= MAX_ATTEMP)
                                finish();
                            else
                                cleanPin();
                        }

                    }

                    mLocked = false;
                }
            });

            task.start();
        }

    }

    private void hideInfo() {
        Threading.USER_THREAD.execute(new Runnable() {
            @Override
            public void run() {
                TextView mInfoLabel = findViewById(R.id.pinInfo);
                mInfoLabel.setVisibility(View.GONE);
            }
        });
    }

    private void setInfo(@StringRes final int idRes) {
        Threading.USER_THREAD.execute(new Runnable() {
            @Override
            public void run() {
                TextView mInfoLabel = findViewById(R.id.pinInfo);
                mInfoLabel.setText(idRes);
                if (mInfoLabel.getVisibility() != View.VISIBLE)
                    mInfoLabel.setVisibility(View.VISIBLE);
            }
        });
    }

    private void cleanPin() {
        Threading.USER_THREAD.execute(new Runnable() {
            @Override
            public void run() {
                for (ImageView pin : mPinDigitViews)
                    pin.setImageDrawable(getDrawable(R.drawable.br_pin));
            }
        });
    }

    private void fillPin(int digit) {
        if (Helper.between(digit, 0, mPinDigitViews.length - 1))
            mPinDigitViews[digit].setImageDrawable(getDrawable(R.drawable.bg_pin));
    }

    private void cleanPin(int digit) {
        if (Helper.between(digit, 0, mPinDigitViews.length - 1))
            mPinDigitViews[digit].setImageDrawable(getDrawable(R.drawable.br_pin));
    }

    private void encryptWallet() {
        Threading.USER_THREAD.execute(new Runnable() {
            @Override
            public void run() {
                mDialogOnLoad = new AlertDialog.Builder(LoginWalletActivity.this)
                        .setTitle(R.string.encrypt_wallet)
                        .setMessage(R.string.encrypt_message)
                        .create();

                mDialogOnLoad.setCanceledOnTouchOutside(false);
                mDialogOnLoad.setCancelable(false);

                mDialogOnLoad.show();
            }
        });

        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {

                IdealPasswordParameter idealParameter
                        = new IdealPasswordParameter(concatAll(mPinValues));
                KeyCrypterScrypt scrypt = new KeyCrypterScrypt(idealParameter.realIterations);

                if (BitcoinService.get().requireDecrypted())
                    BitcoinService.get().getWallet().decrypt(new KeyParameter(mKeyPrev));

                BitcoinService.get().getWallet()
                        .encrypt(scrypt, scrypt.deriveKey(concatAll(mPinValues)));

                mDialogOnLoad.dismiss();
                LoginWalletActivity.this.finish();
            }
        });

        task.start();
    }

    private boolean pinEquals(String[] left, String[] right) {
        for (int i = 0; i < MIN_LENGTH; i++) {
            if (IsNullOrEmpty(left[i]) || IsNullOrEmpty(right[i]))
                return false;

            if (!left[i].contentEquals(right[i]))
                return false;
        }
        return true;
    }

    private boolean IsNullOrEmpty(String text) {
        if (text == null)
            return true;
        return text.isEmpty();
    }

    private byte[] getBytes(String[] pin) {
        String pinText = concatAll(pin);
        KeyCrypter scrypt = BitcoinService.get().getWallet().getKeyCrypter();
        KeyParameter key = Objects.requireNonNull(scrypt).deriveKey(pinText);

        return key.getKey();
    }

    private String concatAll(String[] pin) {
        StringBuilder builder = new StringBuilder();
        for (String aPin : pin) builder.append(aPin);

        return builder.toString();
    }

    private boolean hasMinLenght(int currentLength) {
        return currentLength == MIN_LENGTH;
    }

    public void handlerBackspace(View view) {
        if (mLocked)
            return;

        if (mCountDigit > 0) {
            mCountDigit--;
            cleanPin(mCountDigit);
        }
    }
}
