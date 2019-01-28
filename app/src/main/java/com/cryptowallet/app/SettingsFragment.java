package com.cryptowallet.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.SwitchPreferenceCompat;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.security.Security;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.utils.WifiManager;
import com.cryptowallet.wallet.SupportedAssets;
import com.squareup.okhttp.internal.NamedRunnable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static android.content.Context.FINGERPRINT_SERVICE;
import static android.content.Context.KEYGUARD_SERVICE;
import static com.cryptowallet.app.AppPreference.BLUE_THEME;
import static com.cryptowallet.app.AppPreference.DARK_THEME;
import static com.cryptowallet.app.AppPreference.LIGHT_THEME;

/**
 *
 */
public class SettingsFragment extends PreferenceFragment {

    /**
     * Etiqueta del fragmento.
     */
    private static String TAG = "Settings";

    /**
     * Evita que el escucha se cicle.
     */
    private boolean mDisableListener;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.preference, s);
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        super.setPreferenceScreen(preferenceScreen);

        Context context = getActivity().getApplicationContext();

        Preference version = findPreference("version_app");

        if (!Utils.isNull(version))
            version.setSummary(AppPreference.getVesion(context));

        ListPreference selectedCurrency
                = (ListPreference) findPreference("selectedCurrency");

        if (!Utils.isNull(selectedCurrency)) {
            String[] currencies = new String[]{
                    SupportedAssets.USD.name(),
                    SupportedAssets.MXN.name()
            };
            selectedCurrency.setEntries(currencies);
            selectedCurrency.setEntryValues(currencies);
            selectedCurrency.setValue(AppPreference.getSelectedCurrency(context));

            selectedCurrency.setOnPreferenceChangeListener(this::onSelectedCurrency);
        }

        ListPreference selectedTheme = (ListPreference) findPreference("selectedTheme");

        if (!Utils.isNull(selectedTheme)) {
            String themeName = AppPreference.getThemeName();
            switch (themeName) {
                case LIGHT_THEME:
                    selectedTheme.setValue("0");
                    break;

                case DARK_THEME:
                    selectedTheme.setValue("1");
                    break;

                case BLUE_THEME:
                    selectedTheme.setValue("2");
                    break;
            }
            selectedTheme.setOnPreferenceChangeListener(this::onSelectedTheme);
        }

        Preference reconnect = findPreference("reconnect");

        reconnect.setOnPreferenceClickListener(this::onReconnectNode);

        SwitchPreferenceCompat onlyWifi
                = (SwitchPreferenceCompat) findPreference("onlyWifi");

        if (!Utils.isNull(onlyWifi)) {
            onlyWifi.setOnPreferenceChangeListener(this::enableOnlyWifi);
            boolean useOnlyWifi
                    = AppPreference.getUseOnlyWifi(getActivity().getApplicationContext());
            onlyWifi.setChecked(useOnlyWifi);
        }

        ListPreference lockTime = (ListPreference) findPreference("lockTime");

        if (!Utils.isNull(lockTime)) {
            lockTime.setValue(Integer.toString(AppPreference.getLockTime(context)));
            lockTime.setOnPreferenceChangeListener(this::onChangeLockTime);
            lockTime.setEntries(new String[]{
                    getString(R.string.immediately_text),
                    getString(R.string.second_caption, 15),
                    getString(R.string.second_caption, 30),
                    getString(R.string.second_caption, 60),
                    getString(R.string.minute_caption, 5),
                    getString(R.string.second_caption, 10),
                    getString(R.string.disable_caption)
            });
        }

        checkCanUseFingerprint();

    }

    /**
     * Este método es llamado cuando se cambia el tiempo de bloqueo de la billetera.
     */
    private boolean onChangeLockTime(Preference preference, Object o) {
        Context context = getActivity().getApplicationContext();
        AppPreference.setLockTime(context, Integer.parseInt(o.toString()));

        return true;
    }

    /**
     * Este método es llamado cuando se activa el uso del wifi para descargar la blockchain.
     */
    private boolean enableOnlyWifi(Preference preference, Object value) {
        Context context = getActivity().getApplicationContext();
        Boolean onlyWifi = Boolean.parseBoolean(value.toString());

        AppPreference.setUseOnlyWifi(context, onlyWifi);

        boolean wifiConnected = WifiManager.hasInternet(context);

        if (onlyWifi && !wifiConnected)
            BitcoinService.get().disconnectNetwork();
        else if (!onlyWifi)
            BitcoinService.get().connectNetwork();

        return true;
    }

    /**
     * Este método es llamado cuando se hace clic en el botón "Reconectar".
     */
    private boolean onReconnectNode(Preference preference) {
        BitcoinService.get().disconnectNetwork();
        BitcoinService.get().connectNetwork();
        return true;
    }

    /**
     * Este método es llamado cuando se hace clic en el botón  "Tema".
     */
    private boolean onSelectedTheme(Preference preference, Object o) {
        Context context = getActivity().getApplicationContext();

        switch (Integer.parseInt(o.toString())) {
            case 0:
                AppPreference.enableLightTheme(context);
                break;
            case 1:
                AppPreference.enableDarkTheme(context);
                break;
            case 2:
                AppPreference.enableBlueTheme(context);
                break;
        }

        getActivity().recreate();

        return true;
    }

    /**
     * Este método es llamado cuando se hace clic en el botón "Divisa".
     */
    private boolean onSelectedCurrency(Preference preference, Object o) {
        Context context = getActivity().getApplicationContext();

        AppPreference.setSelectedCurrency(
                context, o.toString());

        return true;
    }

    /**
     * Verifica si se puede utilizar el lector de huellas.
     */
    private void checkCanUseFingerprint() {
        Preference useFingerprint = findPreference("useFingerprint");
        Preference useOrChangePin = findPreference("useOrChangePin");

        if (BitcoinService.get().isRunning() && BitcoinService.get().isUnencrypted()) {
            useOrChangePin.setTitle(R.string.init_pin);
            useFingerprint.setEnabled(false);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !enableFingerprint())
            useFingerprint.setVisible(false);
        else
            useFingerprint.setVisible(true);

        useOrChangePin.setOnPreferenceClickListener(this::handleConfigurePin);
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

        Context context = getActivity().getApplicationContext();

        KeyguardManager keyguardManager
                = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
        FingerprintManager fingerprintManager
                = (FingerprintManager) context.getSystemService(FINGERPRINT_SERVICE);

        if (!fingerprintManager.isHardwareDetected())
            return false;

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED)
            return false;

        if (!fingerprintManager.hasEnrolledFingerprints())
            return false;

        if (!keyguardManager.isKeyguardSecure())
            return false;

        SwitchPreferenceCompat useFingerprint = (SwitchPreferenceCompat) findPreference("useFingerprint");

        if (Utils.isNull(useFingerprint))
            return false;

        useFingerprint.setChecked(AppPreference.getUseFingerprint(context));

        Preference useOrChangePin = findPreference("useOrChangePin");

        if (Utils.isNull(useOrChangePin))
            return false;

        if (AppPreference.getUseFingerprint(context))
            useOrChangePin.setVisible(false);

        useFingerprint.setOnPreferenceChangeListener(this::onEnableFingerprint);

        return true;
    }

    /**
     * Este método es llamado cuando se cambia el uso de la huella digital.
     */
    private boolean onEnableFingerprint(Preference preference, Object o) {

        Boolean enableFingerprint = AppPreference.getUseFingerprint(getActivity());
        Preference useOrChangePin = findPreference("useOrChangePin");
        SwitchPreferenceCompat useFingerprint
                = (SwitchPreferenceCompat) findPreference("useFingerprint");

        if (!enableFingerprint) {
            new AuthenticateDialog()
                    .dismissOnAuth()
                    .setWallet(BitcoinService.get())
                    .setMode(AuthenticateDialog.REG_FINGER)
                    .setOnDesmiss(() ->
                            useOrChangePin.setVisible(false))
                    .setOnCancel(() -> {
                        useFingerprint.setChecked(false);
                    })
                    .show(getActivity());
        } else
            new AuthenticateDialog()
                    .dismissOnAuth()
                    .setWallet(BitcoinService.get())
                    .setMode(AuthenticateDialog.AUTH)
                    .setOnDesmiss(() -> {
                        AppPreference.setUseFingerprint(
                                getActivity(), false);
                        AppPreference.removeData(getActivity());
                        Security.get().removeKeyFromStore();
                        useOrChangePin.setVisible(true);
                    })
                    .setOnCancel(() -> {
                        useFingerprint.setChecked(true);
                    })
                    .show(getActivity());

        return false;
    }

    /**
     * Este método es llamado cuando se hace clic en el botón "Use PIN".
     */
    public boolean handleConfigurePin(Preference preference) {
        Executor executor = Executors.newSingleThreadExecutor();
        Preference useOrChangePin = findPreference("useOrChangePin");

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

                BitcoinService.get().encryptWallet(() -> {
                    regDialog.show(getActivity());

                    if (authDialog.isShowing())
                        authDialog.dismiss();

                    try {
                        return regDialog.getAuthData();
                    } catch (InterruptedException e) {
                        return null;
                    } finally {
                        regDialog.showUIProgress(getString(R.string.encrypt_message));
                    }
                }, () -> {
                    authDialog.show(getActivity());
                    try {
                        return authDialog.getAuthData();
                    } catch (InterruptedException e) {
                        return null;
                    }
                });

                if (regDialog.isShowing())
                    regDialog.dismiss();

                getActivity().runOnUiThread(() -> {
                    useOrChangePin.setTitle(R.string.change_pin_setting);
                    checkCanUseFingerprint();
                });
            }
        });

        return true;
    }
}
