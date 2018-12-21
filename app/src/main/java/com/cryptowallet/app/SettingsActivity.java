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
import com.cryptowallet.utils.WifiManager;

/**
 * Esta actividad permite realizar configuraciones en la aplicación.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class SettingsActivity extends ActivityBase {

    /**
     * Indica si se intenta remover la huella.
     */
    private boolean mTryRemoveFingerprint = false;

    /**
     * Indica si se está configurando el PIN.
     */
    private boolean mConfigPin = false;

    /**
     * Indica si se está configurando la autenticación con huella.
     */
    private boolean mConfigFingerprint = false;

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

        if (!BitcoinService.get().isEncrypted()) {
            ((Button) findViewById(R.id.mChangePin)).setText(R.string.init_pin);
            findViewById(R.id.mUseFingerprint).setEnabled(false);
        }

        ((CheckBox) findViewById(R.id.mOnlyWifi))
                .setChecked(AppPreference.getUseOnlyWifi(this));

        ((CheckBox) findViewById(R.id.mOnlyWifi))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean onlyWifi) {
                        AppPreference.setUseOnlyWifi(buttonView.getContext(), onlyWifi);

                        boolean wifiConnected = WifiManager
                                .hasInternet(buttonView.getContext());

                        if (onlyWifi
                                && !wifiConnected)
                            BitcoinService.get().disconnectNetwork();
                        else if (!onlyWifi)
                            BitcoinService.get().connectNetwork();
                    }
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

        if (!BitcoinService.get().isEncrypted())
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

    /**
     * Este método es llamado cuando se hace clic en el botón "Use PIN".
     *
     * @param view Botón que fue presionado.
     */
    public void handleConfigurePin(View view) {
        mConfigPin = true;
        Intent intent = new Intent(this, LoginWalletActivity.class);
        intent.putExtra(ExtrasKey.REG_PIN, true);

        if (BitcoinService.get().isEncrypted())
            intent.putExtra(ExtrasKey.REQ_AUTH, true);

        startActivityForResult(intent, 0);
    }


    /**
     * Este método es llamado cuando la actividad llamada con anterioridad devuelve un resultado.
     *
     * @param requestCode Código de petición.
     * @param resultCode  Código de resultado.
     * @param data        Información devuelta.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            if (data != null && data.hasExtra(ExtrasKey.PIN_DATA)) {

                if (mTryRemoveFingerprint) {
                    AppPreference.setUseFingerprint(this, false);
                    AppPreference.removeData(this);
                    Security.get().removeKeyFromStore();
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
