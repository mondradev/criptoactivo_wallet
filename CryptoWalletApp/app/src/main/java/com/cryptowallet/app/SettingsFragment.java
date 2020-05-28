package com.cryptowallet.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.ActivityCompat;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.security.Security;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.utils.WifiManager;
import com.cryptowallet.wallet.BlockchainStatus;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletListenerBase;
import com.cryptowallet.wallet.WalletServiceBase;
import com.google.common.base.Strings;

import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.cryptowallet.utils.NamedRunnable;

import static android.content.Context.FINGERPRINT_SERVICE;
import static android.content.Context.KEYGUARD_SERVICE;
import static com.cryptowallet.app.AppPreference.BLUE_THEME;
import static com.cryptowallet.app.AppPreference.DARK_THEME;
import static com.cryptowallet.app.AppPreference.LIGHT_THEME;

/**
 *
 */
public class SettingsFragment extends PreferenceFragmentCompat {

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

        BitcoinService.addEventListener(new WalletListenerBase() {
            /**
             * Este método se ejecuta cuando la blockchain de la billetera ha sido descargada
             * completamente.
             *
             * @param service Información de la billetera que desencadena el evento.
             */
            @Override
            public void onCompletedDownloaded(WalletServiceBase service) {
                if (Utils.isNull(getActivity()))
                    return;

                getActivity().runOnUiThread(() ->
                        findPreference("blockchain_status")
                                .setSummary(getString(
                                        R.string.progress_blockchain,
                                        "100.00%",
                                        Utils.getDateTime(
                                                BitcoinService.get().getBlockchainDate(),
                                                getString(R.string.today_text),
                                                getString(R.string.yesterday_text)
                                        ))));
            }

            /**
             * Este método se ejecuta cuando se descarga un bloque nuevo.
             *
             * @param service Información de la billetera que desencadena el evento.
             * @param status  Estado actual de la blockchain.
             */
            @Override
            public void onBlocksDownloaded(WalletServiceBase service, BlockchainStatus status) {
                Date time = status.getTime();
                double blocks = status.getLeftBlocks();

                if (Utils.isNull(getActivity()))
                    return;

                getActivity().runOnUiThread(() -> {
                    String timeStr = Utils.getDateTime(
                            time,
                            getString(R.string.today_text),
                            getString(R.string.yesterday_text)
                    );

                    findPreference("blockchain_status")
                            .setSummary(getString(
                                    R.string.progress_blockchain,
                                    String.format(
                                            Locale.getDefault(),
                                            "%.2f%%",
                                            (status.getTotalBlocks() - blocks)
                                                    / status.getTotalBlocks() * 100.0
                                    ),
                                    timeStr
                            ));
                });
            }
        });
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        super.setPreferenceScreen(preferenceScreen);

        Context context = Objects.requireNonNull(getActivity()).getApplicationContext();

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
            selectedCurrency.setValue(AppPreference.getSelectedCurrency(context).name());

            selectedCurrency.setOnPreferenceChangeListener(this::onSelectedCurrency);

            findPreference("selectedCurrency")
                    .setSummary(AppPreference.getSelectedCurrency(context).name());
        }

        ListPreference selectedTheme = (ListPreference) findPreference("selectedTheme");

        if (!Utils.isNull(selectedTheme)) {
            String themeName = AppPreference.getThemeName();
            switch (themeName) {
                case LIGHT_THEME:
                    selectedTheme.setValue("0");
                    findPreference("selectedTheme").setSummary(R.string.lightMode);
                    break;

                case DARK_THEME:
                    selectedTheme.setValue("1");
                    findPreference("selectedTheme").setSummary(R.string.darkMode);
                    break;

                case BLUE_THEME:
                    selectedTheme.setValue("2");
                    findPreference("selectedTheme").setSummary(R.string.blueMode);
                    break;
            }

            selectedTheme.setOnPreferenceChangeListener(this::onSelectedTheme);
        }

        Preference reconnect = findPreference("reconnect");

        reconnect.setOnPreferenceClickListener(this::onReconnectNode);

        SwitchPreference onlyWifi
                = (SwitchPreference) findPreference("onlyWifi");

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
                    getString(R.string.minute_caption, 10),
                    getString(R.string.disable_caption)
            });
        }

        long time = new Date().getTime() - BitcoinService.get().getBlockchainDate().getTime();

        ListPreference languageList = (ListPreference) findPreference("selectedLanguage");

        languageList.setSummary(getLanguageName(AppPreference.getLanguage(getActivity())));
        languageList.setOnPreferenceChangeListener(this::handlerLanguageChange);
        languageList.setValue(AppPreference.getLanguage(getActivity()));

        findPreference("blockchain_status")
                .setSummary(getString(
                        R.string.progress_blockchain,
                        time > 600000
                                ? getString(R.string.calculate_progress) : "100.00%",
                        Utils.getDateTime(
                                BitcoinService.get().getBlockchainDate(),
                                getString(R.string.today_text),
                                getString(R.string.yesterday_text)
                        )
                ));

        findPreference("use2factor")
                .setOnPreferenceChangeListener(this::handlerTwoFactorAuthentication);
        ((SwitchPreference) findPreference("use2factor"))
                .setChecked(!Strings.isNullOrEmpty(AppPreference.getSecretPhrase(getActivity())));

        checkCanUseFingerprint();

    }

    private boolean handlerTwoFactorAuthentication(Preference preference, Object value) {
        if (Utils.isNull(getActivity()))
            return false;

        boolean enabled = Boolean.parseBoolean(value.toString());

        Executors.newSingleThreadExecutor().execute(() -> {
            new AuthenticateDialog()
                    .dismissOnAuth()
                    .setWallet(BitcoinService.get())
                    .setMode(AuthenticateDialog.AUTH)
                    .setOnDismiss(() -> {
                        getActivity().runOnUiThread(() -> {
                            if (!enabled) {
                                disable2fa();
                            } else {
                                Intent intent = new Intent(this.getContext(),
                                        Configure2faActivity.class);
                                getActivity().startActivityFromFragment(
                                        this, intent, 0);
                            }
                        });
                    })
                    .show(getActivity());
        });

        return false;
    }

    private int getLanguageName(String language) {
        switch (language) {
            case "en_US":
                return R.string.english;
            case "es_MX":
                return R.string.spanish;
        }

        return R.string.no_language;
    }

    /**
     * Este método es llamado cuando se cambia el tiempo de bloqueo de la billetera.
     */
    private boolean onChangeLockTime(Preference preference, Object o) {
        Context context = Objects.requireNonNull(getActivity()).getApplicationContext();
        AppPreference.setLockTime(context, Integer.parseInt(o.toString()));

        return true;
    }

    /**
     * Este método es llamado cuando se activa el uso del wifi para descargar la blockchain.
     */
    private boolean enableOnlyWifi(Preference preference, Object value) {
        Context context = Objects.requireNonNull(getActivity()).getApplicationContext();
        boolean onlyWifi = Boolean.parseBoolean(value.toString());

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
        BitcoinService.get().reconnectNetwork();
        Preference reconnectPref = findPreference("reconnect");
        reconnectPref.setEnabled(false);
        reconnectPref.setTitle(R.string.reconnect_setting_disable);

        new Handler().postDelayed(() -> {
            reconnectPref.setEnabled(true);
            reconnectPref.setTitle(R.string.reconnect_title);
        }, 10000);

        return true;
    }

    /**
     * Este método es llamado cuando se hace clic en el botón  "Tema".
     */
    private boolean onSelectedTheme(Preference preference, Object o) {
        Context context = Objects.requireNonNull(getActivity()).getApplicationContext();

        switch (Integer.parseInt(o.toString())) {
            case 0:
                AppPreference.enableLightTheme(context);
                findPreference("selectedTheme").setSummary(R.string.lightMode);
                break;
            case 1:
                AppPreference.enableDarkTheme(context);
                findPreference("selectedTheme").setSummary(R.string.darkMode);
                break;
            case 2:
                AppPreference.enableBlueTheme(context);
                findPreference("selectedTheme").setSummary(R.string.blueMode);
                break;
        }

        getActivity().recreate();

        return true;
    }

    /**
     * Este método es llamado cuando se hace clic en el botón "Divisa".
     */
    private boolean onSelectedCurrency(Preference preference, Object o) {
        Context context = Objects.requireNonNull(getActivity())
                .getApplicationContext();

        AppPreference.setSelectedCurrency(
                context, o.toString());

        findPreference("selectedCurrency")
                .setSummary(AppPreference.getSelectedCurrency(context).name());

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

        Context context = Objects.requireNonNull(getActivity());

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

        SwitchPreference useFingerprint = (SwitchPreference) findPreference("useFingerprint");

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

        boolean enableFingerprint = AppPreference.getUseFingerprint(getActivity());
        Preference useOrChangePin = findPreference("useOrChangePin");
        SwitchPreference useFingerprint
                = (SwitchPreference) findPreference("useFingerprint");

        if (!enableFingerprint) {
            new AuthenticateDialog()
                    .dismissOnAuth()
                    .setWallet(BitcoinService.get())
                    .setMode(AuthenticateDialog.REG_FINGER)
                    .setOnDismiss(() ->
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
                    .setOnDismiss(() -> {
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

                Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                    useOrChangePin.setTitle(R.string.change_pin_setting);
                    checkCanUseFingerprint();
                });
            }
        });

        return true;
    }

    private boolean handlerLanguageChange(Preference preference, Object o) {
        AppPreference.loadLanguage(Objects.requireNonNull(getActivity()), o.toString());
        AppPreference.setLanguage(this.getActivity(), o.toString());

        findPreference("selectedLanguage")
                .setSummary(getLanguageName(AppPreference.getLanguage(getActivity())));

        getActivity().recreate();

        return true;
    }

    public void enable2fa() {
        ((SwitchPreference) findPreference("use2factor"))
                .setChecked(true);
    }

    public void disable2fa() {
        ((SwitchPreference) findPreference("use2factor"))
                .setChecked(false);
        AppPreference.setSecretPhrase(getContext(), "");
    }
}
