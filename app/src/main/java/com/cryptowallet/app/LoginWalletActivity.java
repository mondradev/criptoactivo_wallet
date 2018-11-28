package com.cryptowallet.app;

import android.app.Activity;
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

import org.bitcoinj.core.Context;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.utils.Threading;
import org.spongycastle.crypto.params.KeyParameter;

import java.util.Objects;

public class LoginWalletActivity extends ActivityBase {

    private static final int MIN_LENGTH = 4;
    private final int MAX_ATTEMP = 3;
    private boolean mRegMode;
    private boolean mToCommit;
    private int mCountDigit;
    private String[] mPinValues = new String[4];
    private String[] mPinToCommit;
    private AlertDialog mAlertDialog;
    private byte[] mKeyPrev;
    private ImageView[] mPinDigitViews = new ImageView[4];
    private boolean mRequirePin = false;
    private int mAttemp;
    private boolean mHasError = false;
    private Context mContext = Context.getOrCreate(BitcoinService.get().getNetwork());

    private static byte[] getBytes(String[] pin) {
        String pinText = Helper.concatAll(pin);
        KeyCrypter scrypt = BitcoinService.get().getWallet().getKeyCrypter();
        KeyParameter key = Objects.requireNonNull(scrypt).deriveKey(pinText);

        return key.getKey();
    }

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

        mAlertDialog = new AlertDialog.Builder(LoginWalletActivity.this)
                .setCancelable(false)
                .create();

        setInfo(R.string.enter_pin);

    }

    public void handlerPad(View view) {
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

            if (mRegMode && (!mRequirePin || mKeyPrev != null)) {

                if (mToCommit) {

                    if (!pinEquals(mPinValues, mPinToCommit)) {
                        setInfo(R.string.pin_no_equal);
                        mPinValues = new String[4];
                        mToCommit = false;
                        mCountDigit = 0;
                        mHasError = true;

                        cleanPin();
                    } else
                        encryptWallet();

                } else {
                    mCountDigit = 0;
                    mPinToCommit = mPinValues;
                    mPinValues = new String[4];
                    mToCommit = true;

                    cleanPin();

                    setInfo(R.string.commit_pin);
                }

            } else
                validatePin();


        }

    }

    private void validatePin() {

        mAlertDialog.setTitle(R.string.validate_pin);
        mAlertDialog.setMessage(getString(R.string.validate_pin_text));
        mAlertDialog.show();

        new ValidatePinThread()
                .setLoginWallet(this)
                .start();

    }

    private void hideInfo() {
        Threading.USER_THREAD.execute(new Runnable() {
            @Override
            public void run() {
                TextView mInfoLabel = findViewById(R.id.mInfo);
                mInfoLabel.setVisibility(View.GONE);
            }
        });
    }

    private void setInfo(@StringRes final int idRes) {
        Threading.USER_THREAD.execute(new Runnable() {
            @Override
            public void run() {
                TextView mInfoLabel = findViewById(R.id.mInfo);
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
        mAlertDialog.setTitle(R.string.encrypt_wallet);
        mAlertDialog.setMessage(getString(R.string.encrypt_message));
        mAlertDialog.show();

        new EncryptWalletThread()
                .setLoginWallet(this)
                .start();
    }

    private boolean pinEquals(String[] left, String[] right) {
        for (int i = 0; i < MIN_LENGTH; i++) {
            if (Helper.isNullOrEmpty(left[i]) || Helper.isNullOrEmpty(right[i]))
                return false;

            if (!left[i].contentEquals(right[i]))
                return false;
        }
        return true;
    }


    private boolean hasMinLenght(int currentLength) {
        return currentLength == MIN_LENGTH;
    }

    public void handlerBackspace(View view) {

        if (mCountDigit > 0) {
            mCountDigit--;
            cleanPin(mCountDigit);
        }
    }

    private static class ValidatePinThread extends Thread {

        private LoginWalletActivity mLoginWallet;

        ValidatePinThread setLoginWallet(LoginWalletActivity mLoginWallet) {
            this.mLoginWallet = mLoginWallet;
            return this;
        }

        @Override
        public void run() {

            Context.propagate(mLoginWallet.mContext);

            byte[] dataPin = getBytes(mLoginWallet.mPinValues);

            if (BitcoinService.get().validatePin(dataPin)) {

                if (mLoginWallet.mRequirePin) {
                    mLoginWallet.setInfo(R.string.indications_pin_setup);
                    mLoginWallet.mKeyPrev = dataPin;
                    mLoginWallet.mCountDigit = 0;
                    mLoginWallet.cleanPin();
                } else {
                    Intent intent = new Intent();
                    intent.putExtra(ExtrasKey.PIN_DATA, dataPin);
                    mLoginWallet.setResult(Activity.RESULT_OK, intent);

                    mLoginWallet.finish();
                }
            } else {

                mLoginWallet.setInfo(R.string.error_pin);
                mLoginWallet.mHasError = true;
                mLoginWallet.mCountDigit = 0;
                mLoginWallet.mPinValues = new String[4];
                mLoginWallet.mAttemp++;

                if (mLoginWallet.mAttemp >= mLoginWallet.MAX_ATTEMP)
                    mLoginWallet.finish();
                else
                    mLoginWallet.cleanPin();
            }

            mLoginWallet.mAlertDialog.dismiss();
        }
    }

    private static class EncryptWalletThread extends Thread {

        private LoginWalletActivity mLoginWallet;

        EncryptWalletThread setLoginWallet(LoginWalletActivity mLoginWallet) {
            this.mLoginWallet = mLoginWallet;
            return this;
        }

        @Override
        public void run() {
            Context.propagate(mLoginWallet.mContext);

            IdealPasswordParameter idealParameter
                    = new IdealPasswordParameter(Helper.concatAll(mLoginWallet.mPinValues));
            KeyCrypterScrypt scrypt = new KeyCrypterScrypt(idealParameter.realIterations);

            if (BitcoinService.get().requireDecrypted())
                BitcoinService.get().getWallet().decrypt(new KeyParameter(mLoginWallet.mKeyPrev));

            BitcoinService.get().getWallet()
                    .encrypt(scrypt, scrypt.deriveKey(Helper.concatAll(mLoginWallet.mPinValues)));

            mLoginWallet.mAlertDialog.dismiss();

            mLoginWallet.finish();
        }
    }

}
