/*
 * Copyright 2019 InnSy Tech
 * Copyright 2019 Ing. Javier de Jesús Flores Mondragón
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cryptowallet.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.DialogInterface;
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
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.security.Security;
import com.cryptowallet.utils.WifiManager;
import com.squareup.okhttp.internal.NamedRunnable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Esta actividad permite realizar configuraciones en la aplicación.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class SettingsActivity extends ActivityBase {

    /**
     * Evita que el escucha se cicle.
     */
    private boolean mDisableListener;

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        setTitle(R.string.settings);


        final Button mDarkMode = findViewById(R.id.mSelectMode);

        mDarkMode.setText(AppPreference.isDarkTheme()
                ? R.string.darkMode : AppPreference.isBlueTheme()
                ? R.string.blueMode : R.string.lightMode);

        ((Button) findViewById(R.id.mSelectFiat))
                .setText(AppPreference.getSelectedCurrency(this));

        if (BitcoinService.get().isRunning() && BitcoinService.get().isUnencrypted()) {
            ((Button) findViewById(R.id.mChangePin)).setText(R.string.init_pin);
            findViewById(R.id.mUseFingerprint).setEnabled(false);
        }

        ((CheckBox) findViewById(R.id.mOnlyWifi))
                .setChecked(AppPreference.getUseOnlyWifi(this));

        ((CheckBox) findViewById(R.id.mOnlyWifi))
                .setOnCheckedChangeListener((buttonView, onlyWifi) -> {
                    AppPreference.setUseOnlyWifi(buttonView.getContext(), onlyWifi);

                    boolean wifiConnected = WifiManager
                            .hasInternet(buttonView.getContext());

                    if (onlyWifi
                            && !wifiConnected)
                        BitcoinService.get().disconnectNetwork();
                    else if (!onlyWifi)
                        BitcoinService.get().connectNetwork();
                });


        checkCanUseFingerprint();


        ((TextView) findViewById(R.id.mVersion)).setText(
                AppPreference.getVesion(this.getApplicationContext()));

    }

    /**
     * Verifica si se puede utilizar el lector de huellas.
     */
    private void checkCanUseFingerprint() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !enableFingerprint())
            findViewById(R.id.mUseFingerprintLayout).setVisibility(View.GONE);
        else
            findViewById(R.id.mUseFingerprintLayout).setVisibility(View.VISIBLE);
    }

    /**
     * Activa el uso del lector de huellas para autenticar la billetera.
     *
     * @return Un valor true si se activó el lector de huellas.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private boolean enableFingerprint() {

        if (BitcoinService.get().isUnencrypted())
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
                .setChecked(AppPreference.getUseFingerprint(this));

        if (AppPreference.getUseFingerprint(this))
            findViewById(R.id.mChangePin).setVisibility(View.GONE);

        ((CheckBox) findViewById(R.id.mUseFingerprint))
                .setOnCheckedChangeListener((buttonView, isChecked) -> {

                    if (mDisableListener) {
                        mDisableListener = false;
                        return;
                    }

                    if (isChecked) {
                        new AuthenticateDialog()
                                .dismissOnAuth()
                                .setWallet(BitcoinService.get())
                                .setMode(AuthenticateDialog.REG_FINGER)
                                .setOnDesmiss(() ->
                                        findViewById(R.id.mChangePin).setVisibility(View.GONE))
                                .setOnCancel(() -> {
                                    mDisableListener = true;
                                    ((CheckBox) findViewById(R.id.mUseFingerprint))
                                            .setChecked(false);
                                })
                                .show(SettingsActivity.this);
                    } else
                        new AuthenticateDialog()
                                .dismissOnAuth()
                                .setWallet(BitcoinService.get())
                                .setMode(AuthenticateDialog.AUTH)
                                .setOnDesmiss(() -> {
                                    AppPreference.setUseFingerprint(
                                            SettingsActivity.this, false);
                                    AppPreference.removeData(SettingsActivity.this);
                                    Security.get().removeKeyFromStore();
                                    findViewById(R.id.mChangePin).setVisibility(View.VISIBLE);
                                })
                                .setOnCancel(() -> {
                                    mDisableListener = true;
                                    ((CheckBox) findViewById(R.id.mUseFingerprint))
                                            .setChecked(true);
                                })
                                .show(SettingsActivity.this);

                });

        return true;
    }

    /**
     * Este método es llamado cuando se hace clic en el botón "Use PIN".
     *
     * @param view Botón que fue presionado.
     */
    public void handleConfigurePin(View view) {
        Executor executor = Executors.newSingleThreadExecutor();

        final AuthenticateDialog authDialog = new AuthenticateDialog()
                .setWallet(BitcoinService.get())
                .setMode(AuthenticateDialog.AUTH);

        final AuthenticateDialog regDialog = new AuthenticateDialog()
                .setWallet(BitcoinService.get())
                .setMode(AuthenticateDialog.REG_PIN);


        executor.execute(new NamedRunnable("AuthenticateDialog") {
            @Override
            protected void execute() {
                if (!BitcoinService.get().isRunning())
                    return;

                setCanLock(false);

                BitcoinService.get().encryptWallet(() -> {
                    regDialog.show(SettingsActivity.this);

                    if (authDialog.isShowing())
                        authDialog.dismiss();

                    try {
                        return regDialog.getAuthData();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return null;
                    } finally {
                        regDialog.showUIProgress(getString(R.string.encrypt_message));
                    }
                }, () -> {
                    authDialog.show(SettingsActivity.this);
                    try {
                        return authDialog.getAuthData();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return null;
                    }
                });

                regDialog.dismiss();

                runOnUiThread(() -> {
                    ((Button) findViewById(R.id.mChangePin))
                            .setText(R.string.change_pin_setting);
                    checkCanUseFingerprint();
                });

                setCanLock(true);
            }
        });

    }

    /**
     * Este método es llamado cuando se hace clic en el botón  "Tema".
     *
     * @param view Botón que fue presionado.
     */
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
                                AppPreference.enableLightTheme(SettingsActivity.this);
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

    /**
     * Este método es llamado cuando se hace clic en el botón "Divisa".
     *
     * @param view Botón que fue presionado.
     */
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

    /**
     * Esté método es llamado cuando se hace clic en el botón "Reconectar".
     *
     * @param view Botón que fue presionado.
     */
    public void handlerReconnect(View view) {
        BitcoinService.get().disconnectNetwork();
        BitcoinService.get().connectNetwork();
    }
}
