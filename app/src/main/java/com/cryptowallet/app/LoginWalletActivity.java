package com.cryptowallet.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.security.FingerprintHandler;
import com.cryptowallet.security.Security;
import com.cryptowallet.utils.Helper;
import com.cryptowallet.utils.IdealPasswordParameter;

import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.utils.Threading;
import org.spongycastle.crypto.params.KeyParameter;

import java.util.Objects;

import javax.crypto.Cipher;

public class LoginWalletActivity extends ActivityBase {


    private static final int MIN_LENGTH = 4;
    private final int MAX_ATTEMP = 3;

    private org.bitcoinj.core.Context mContext
            = org.bitcoinj.core.Context.getOrCreate(BitcoinService.get().getNetwork());

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
    private String mHashKey = "";
    private boolean mRegFingerprint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_wallet);

        setTitle(R.string.authenticate_title);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();

        mPinDigitViews[0] = findViewById(R.id.mPin1);
        mPinDigitViews[1] = findViewById(R.id.mPin2);
        mPinDigitViews[2] = findViewById(R.id.mPin3);
        mPinDigitViews[3] = findViewById(R.id.mPin4);

        mAlertDialog = new AlertDialog.Builder(LoginWalletActivity.this)
                .setCancelable(false)
                .create();

        // Registrando PIN
        mRegMode = intent.getBooleanExtra(ExtrasKey.REG_PIN, false);
        mRegFingerprint = intent.getBooleanExtra(ExtrasKey.REG_FINGER, false);
        mRequirePin = BitcoinService.get().requireDecrypted();

        if (mRequirePin && AppPreference.useFingerprint(this))
            loadFingerprint();
        else if (!mRegMode || mRequirePin || mRegFingerprint)
            setInfo(R.string.enter_pin);
        else
            setInfo(R.string.indications_pin_setup);

    }

    @TargetApi(Build.VERSION_CODES.M)
    private void loadFingerprint() {

        findViewById(R.id.mUsePin).setVisibility(View.GONE);
        findViewById(R.id.mFingerprintLayout).setVisibility(View.VISIBLE);

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        FingerprintManager fingerprintManager
                = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

        if (fingerprintManager.isHardwareDetected()) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                Helper.showSnackbar(findViewById(R.id.mLoginContainer),
                        getString(R.string.require_finger_permission));
            } else {

                if (!fingerprintManager.hasEnrolledFingerprints()) {
                    Helper.showSnackbar(findViewById(R.id.mLoginContainer),
                            getString(R.string.require_finger_register));
                } else {

                    if (!keyguardManager.isKeyguardSecure()) {
                        Helper.showSnackbar(findViewById(R.id.mLoginContainer),
                                getString(R.string.no_lock_screen));
                    } else {
                        Security.get().createKeyIfRequire();

                        FingerprintManager.CryptoObject cryptoObject
                                = new FingerprintManager.CryptoObject(
                                Security.get().requestCipher(LoginWalletActivity.this,
                                        !mRegFingerprint));

                        new FingerprintHandler(this) {

                            @Override
                            public void onAuthenticationSucceeded(
                                    FingerprintManager.AuthenticationResult result) {
                                Cipher cipher = result.getCryptoObject().getCipher();

                                Intent response = new Intent();
                                byte[] dataPin;
                                if (mRegFingerprint) {
                                    dataPin = Base64.decode(Helper.concatAll(mPinValues), Base64.DEFAULT);
                                    Security.get().encryptePin(
                                            LoginWalletActivity.this, cipher, dataPin);
                                    mHashKey = Security.get().getKeyAsString();
                                    dataPin = Security.get().getKey();
                                    AppPreference.useFingerprint(
                                            LoginWalletActivity.this, true);
                                } else {
                                    Security.get().decryptKey(
                                            LoginWalletActivity.this, cipher);

                                    dataPin = Objects
                                            .requireNonNull(BitcoinService.get().getWallet()
                                                    .getKeyCrypter())
                                            .deriveKey(Security.get().getKeyAsString()).getKey();

                                }

                                response.putExtra(ExtrasKey.PIN_DATA, dataPin);
                                LoginWalletActivity.this.setResult(Activity.RESULT_OK, response);
                                LoginWalletActivity.this.finish();

                            }

                            @Override
                            public void onAuthenticationFailed() {
                                LoginWalletActivity.this.setResult(Activity.RESULT_FIRST_USER);
                                LoginWalletActivity.this.finish();
                            }

                        }.startAuth(fingerprintManager, cryptoObject);
                    }
                }
            }
        }
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
                    } else {
                        byte[] dataPin = Base64.decode(Helper.concatAll(mPinValues),
                                Base64.DEFAULT);

                        Security.get().setKey(dataPin);

                        mHashKey = Security.get().getKeyAsString();

                        encryptWallet();
                    }

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

        if (!BitcoinService.get().requireDecrypted())
            return;

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 16908332) {
            finish();
        }

        return super.onOptionsItemSelected(item);
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

            org.bitcoinj.core.Context.propagate(mLoginWallet.mContext);

            byte[] dataPin;
            String hash;

            if (mLoginWallet.mHashKey.isEmpty()) {
                dataPin = Base64.decode(Helper.concatAll(mLoginWallet.mPinValues), Base64.DEFAULT);
                Security.get().setKey(dataPin);
                hash = Security.get().getKeyAsString();
            } else
                hash = mLoginWallet.mHashKey;

            dataPin = Objects.requireNonNull(BitcoinService.get().getWallet().getKeyCrypter())
                    .deriveKey(hash).getKey();

            if (BitcoinService.get().validatePin(dataPin)) {

                if (mLoginWallet.mRequirePin
                        && (mLoginWallet.mRegFingerprint || mLoginWallet.mRegMode)) {

                    if (mLoginWallet.mRegFingerprint) {
                        Threading.USER_THREAD.execute(new Runnable() {
                            @Override
                            public void run() {
                                mLoginWallet.loadFingerprint();
                            }
                        });
                    } else {
                        mLoginWallet.mKeyPrev = dataPin;
                        mLoginWallet.setInfo(R.string.indications_pin_setup);
                        mLoginWallet.mCountDigit = 0;
                        mLoginWallet.cleanPin();
                    }
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
            org.bitcoinj.core.Context.propagate(mLoginWallet.mContext);

            IdealPasswordParameter idealParameter
                    = new IdealPasswordParameter(mLoginWallet.mHashKey);
            KeyCrypterScrypt scrypt = new KeyCrypterScrypt(idealParameter.realIterations);

            if (BitcoinService.get().requireDecrypted())
                BitcoinService.get().getWallet().decrypt(new KeyParameter(mLoginWallet.mKeyPrev));

            BitcoinService.get().getWallet()
                    .encrypt(scrypt, scrypt.deriveKey(mLoginWallet.mHashKey));

            mLoginWallet.mAlertDialog.dismiss();

            mLoginWallet.setResult(Activity.RESULT_OK);
            mLoginWallet.finish();
        }
    }

}
