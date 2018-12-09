package com.cryptowallet.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.security.Security;
import com.cryptowallet.utils.ConnectionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsActivity extends ActivityBase {

    private static Logger mLogger = LoggerFactory.getLogger(SettingsActivity.class);
    private boolean mTryRemoveFingerprint = false;
    private boolean mConfigPin = false;
    private boolean mConfigFingerprint = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_settings);
        setTitle(R.string.settings);


        final Button mDarkMode = findViewById(R.id.mSelectMode);

        mDarkMode.setText(AppPreference.isDarkTheme()
                ? R.string.darkMode : AppPreference.isBlueTheme()
                ? R.string.blueMode : R.string.lightMode);

        ((Button) findViewById(R.id.mSelectFiat))
                .setText(AppPreference.getSelectedCurrency(this));

        if (!BitcoinService.get().requireDecrypted()) {
            ((Button) findViewById(R.id.mChangePin)).setText(R.string.init_pin);
            findViewById(R.id.mUseFingerprint).setEnabled(false);
        }

        ((CheckBox) findViewById(R.id.mOnlyWifi))
                .setChecked(AppPreference.useOnlyWifi(this));

        ((CheckBox) findViewById(R.id.mOnlyWifi))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean onlyWifi) {
                        AppPreference.useOnlyWifi(buttonView.getContext(), onlyWifi);

                        boolean wifiConnected = ConnectionManager
                                .isWifiConnected(buttonView.getContext());

                        mLogger.info("Usar solo conexión WiFi: {}, WiFi Estado: {}",
                                onlyWifi, wifiConnected);

                        if (onlyWifi
                                && !wifiConnected)
                            BitcoinService.get().disconnect();
                        else if (!onlyWifi)
                            BitcoinService.get().connect();
                    }
                });


        checkCanUseFingerprint();


        ((TextView) findViewById(R.id.mVersion)).setText(
                AppPreference.getVesion(this.getApplicationContext()));

    }

    private void checkCanUseFingerprint() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !enableFingerprint())
            findViewById(R.id.mUseFingerprintLayout).setVisibility(View.GONE);
        else
            findViewById(R.id.mUseFingerprintLayout).setVisibility(View.VISIBLE);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean enableFingerprint() {

        if (!BitcoinService.get().requireDecrypted())
            return false;

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        FingerprintManager fingerprintManager
                = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

        if (!fingerprintManager.isHardwareDetected())
            return false;

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED)
            return false;

        if (!fingerprintManager.hasEnrolledFingerprints())
            return false;

        if (!keyguardManager.isKeyguardSecure())
            return false;

        ((CheckBox) findViewById(R.id.mUseFingerprint))
                .setChecked(AppPreference.useFingerprint(this));

        if (AppPreference.useFingerprint(this))
            findViewById(R.id.mChangePin).setEnabled(false);

        ((CheckBox) findViewById(R.id.mUseFingerprint))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Intent intent = new Intent(SettingsActivity.this,
                                LoginWalletActivity.class);

                        mConfigFingerprint = isChecked;

                        if (isChecked)
                            intent.putExtra(ExtrasKey.REG_FINGER, true);
                        else
                            mTryRemoveFingerprint = true;

                        startActivityForResult(intent, 0);
                    }
                });

        return true;
    }

    public void handleConfigurePin(View view) {
        mConfigPin = true;
        Intent intent = new Intent(this, LoginWalletActivity.class);
        intent.putExtra(ExtrasKey.REG_PIN, true);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            if (data != null && data.hasExtra(ExtrasKey.PIN_DATA)) {

                if (mTryRemoveFingerprint) {
                    AppPreference.useFingerprint(this, false);
                    AppPreference.removeData(this);
                    Security.get().removeKey();
                    findViewById(R.id.mChangePin).setEnabled(true);
                } else if (mConfigFingerprint)
                    findViewById(R.id.mChangePin).setEnabled(false);

            } else if (mConfigPin) {
                ((Button) findViewById(R.id.mChangePin)).setText(R.string.change_pin_setting);
                checkCanUseFingerprint();
            }
        } else {
            if (mTryRemoveFingerprint)
                ((CheckBox) findViewById(R.id.mUseFingerprint)).setChecked(true);
            else
                ((CheckBox) findViewById(R.id.mUseFingerprint)).setChecked(false);

            super.onActivityResult(requestCode, resultCode, data);
        }

        mConfigPin = false;
        mConfigFingerprint = false;
        mTryRemoveFingerprint = false;
    }

    public void handleSelectTheme(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.select_theme)
                .setItems(new String[]{
                        getString(R.string.lightMode),
                        getString(R.string.darkMode),
                        getString(R.string.blueMode)
                }, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                AppPreference.enableTheme(SettingsActivity.this);
                                break;
                            case 1:
                                AppPreference.enableDarkTheme(SettingsActivity.this);
                                break;
                            case 2:
                                AppPreference.enableBlueTheme(SettingsActivity.this);
                                break;
                        }

                        recreate();

                        dialog.dismiss();
                    }
                }).create();

        dialog.show();
    }

    public void handleSelectFiat(View view) {
        final String[] currencies = new String[]{
                getString(R.string.usd_fiat),
                getString(R.string.mxn_fiat)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.select_currency)
                .setItems(currencies, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        AppPreference.setSelectedCurrency(
                                SettingsActivity.this, currencies[which]);

                        ((Button) findViewById(R.id.mSelectFiat)).setText(currencies[which]);

                        dialog.dismiss();
                    }
                }).create();

        dialog.show();
    }

    public void handlerReconnect(View view) {
        BitcoinService.get().disconnectPeers();
    }
}
