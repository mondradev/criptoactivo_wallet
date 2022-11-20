/*
 * Copyright © 2020. Criptoactivo
 * Copyright © 2020. InnSy Tech
 * Copyright © 2020. Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.app.fragments;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.cryptowallet.R;
import com.cryptowallet.app.BackupActitivy;
import com.cryptowallet.app.Configure2FaActivity;
import com.cryptowallet.app.Preferences;
import com.cryptowallet.app.ProgressDialog;
import com.cryptowallet.app.SplashActivity;
import com.cryptowallet.app.authentication.Authenticator;
import com.cryptowallet.app.authentication.IAuthenticationSucceededCallback;
import com.cryptowallet.app.authentication.IAuthenticationUpdatedCallback;
import com.cryptowallet.app.authentication.TwoFactorAuthentication;
import com.cryptowallet.services.WalletProvider;
import com.cryptowallet.wallet.SupportedAssets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * Este fragmento permite configurar los ajustes de la aplicación.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.1
 * @see Preferences
 */
public class SettingsFragment extends PreferenceFragmentCompat {


    /**
     * Identificador de la petición de configuración de 2FA.
     */
    private static final int CONFIGURE_2FA_REQUEST = 1;

    /**
     * Este método es llamado durante la creación del fragmento.
     *
     * @param bundle  Datos del estado del fragmento.
     * @param rootKey Clave de la preferencia de {@link PreferenceScreen}.
     */
    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_structure, rootKey);
    }

    /**
     * Establece las preferencias que se mostrarán en este fragmento.
     *
     * @param preferenceScreen La raíz de las preferencias.
     */
    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        super.setPreferenceScreen(preferenceScreen);

        Preferences preferences = Preferences.get();

        configureGeneralCategory();
        configureWalletCategory();
        configureSecurityCategory();

        Preference version = requirePreference("version");
        version.setSummary(preferences.getVesion());
    }

    /**
     * Configura la categoría de "Billetera".
     */
    private void configureWalletCategory() {
        requirePreference("backup").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(requireActivity(), BackupActitivy.class);
            startActivityForResult(intent, 0);

            return false;
        });
        requirePreference("delete")
                .setOnPreferenceClickListener(preference -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.deleting_title)
                            .setMessage(R.string.delete_warn)
                            .setPositiveButton(R.string.delete_text, (dialog, which)
                                    -> Preferences.get().authenticate(requireActivity(),
                                    new Handler()::post,
                                    (IAuthenticationSucceededCallback)
                                            (byte[] authenticationToken) -> deleteWallets()))
                            .create()
                            .show();

                    return false;
                });
    }

    /**
     * Elimina todas la billeteras de la aplicación y restaura las configuraciones de la misma.
     */
    private void deleteWallets() {
        WalletProvider.getInstance().forEachWallet((wallet) -> {
            if (!wallet.delete())
                throw new IllegalStateException("Unable to delete wallet");
        });

        Preferences.get().clear();
        Authenticator.reset(requireContext());

        requireActivity().finishAffinity();
        Intent intent = new Intent(requireContext(),
                SplashActivity.class);
        startActivity(intent);
    }

    /**
     * Este método es llamado cuando se cambia el uso de la autenticación de dos factores.
     *
     * @param preference Preferencia que llama al método.
     * @param isEnabled  Si está activando o no el uso de 2FA.
     * @return Un true si se cambió la preferencia.
     */
    private boolean onTwoFactorEnabledChange(Preference preference, Object isEnabled) {
        final boolean enabled = isEnabled.equals(true);

        if (enabled)
            Preferences.get().authenticate(requireActivity(),
                    new Handler(Looper.getMainLooper())::post,
                    (IAuthenticationSucceededCallback) authenticationToken -> {
                        Intent intent = new Intent(requireActivity(), Configure2FaActivity.class);
                        startActivityForResult(intent, CONFIGURE_2FA_REQUEST);
                    });
        else {
            TwoFactorAuthenticationFragment.show(requireActivity(),
                    () -> {
                        TwoFactorAuthentication.get(requireContext()).reset();
                        ((SwitchPreference) preference).setChecked(false);
                    });
        }

        return false;
    }

    /**
     * Recibe el resultado de las actividades llamadas a través de la función
     * {@link #startActivityForResult(Intent, int)}.
     *
     * @param requestCode Código de la petición,
     * @param resultCode  Código de resultado.
     * @param data        Datos de respuesta.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CONFIGURE_2FA_REQUEST && resultCode == RESULT_OK)
            ((SwitchPreference) requirePreference("2factor")).setChecked(true);
    }

    /**
     * Configura la categoría "Seguridad".
     */
    private void configureSecurityCategory() {
        checkBiometrics();

        Preferences preferences = Preferences.get();

        requirePreference("changepin").setEnabled(!preferences.isEnabledBiometric());
        ((SwitchPreference) requirePreference("2factor"))
                .setChecked(TwoFactorAuthentication.get(requireContext()).isEnabled());
        requirePreference("2factor")
                .setOnPreferenceChangeListener(this::onTwoFactorEnabledChange);

        ListPreference lockTime = requirePreference("locktime");
        lockTime.setValue(Integer.toString(preferences.getLockTimeout()));
        lockTime.setOnPreferenceChangeListener(this::onChangeLockTime);
        lockTime.setEntries(new String[]{
                getString(R.string.immediately_text),
                getString(R.string.time_seconds_pattern_text, 15),
                getString(R.string.time_seconds_pattern_text, 30),
                getString(R.string.time_seconds_pattern_text, 60),
                getString(R.string.time_minute_pattern_text, 5),
                getString(R.string.time_minute_pattern_text, 10),
                getString(R.string.disable_text)
        });
        lockTime.setEntryValues(new String[]{
                "0", "15", "30", "60", "300", "600", "-1"
        });
    }

    /**
     * Configura la categoría "General".
     */
    private void configureGeneralCategory() {
        Preferences preferences = Preferences.get();

        List<String> currenciesNames = new ArrayList<>();
        List<String> currenciesValues = new ArrayList<>();

        for (SupportedAssets asset : SupportedAssets.getSupportedFiatAssets()) {
            currenciesNames.add(requireContext().getString(asset.getName()));
            currenciesValues.add(asset.name());
        }

        ListPreference currenciesList = requirePreference("currency");
        currenciesList.setEntries(currenciesNames.toArray(new CharSequence[0]));
        currenciesList.setEntryValues(currenciesValues.toArray(new CharSequence[0]));
        currenciesList.setValue(preferences.getFiat().name());
        currenciesList.setOnPreferenceChangeListener(this::onSelectedCurrency);
        currenciesList.setSummary(preferences.getFiat().getName());

        List<String> themeNames = new ArrayList<>();
        List<String> themeCaptions = new ArrayList<>();

        for (Preferences.ThemeValue theme : Preferences.get().getThemes()) {
            themeNames.add(theme.getName());
            themeCaptions.add(requireContext().getString(theme.getCaption()));
        }

        ListPreference themeList = requirePreference("theme");
        themeList.setEntries(themeCaptions.toArray(new CharSequence[0]));
        themeList.setEntryValues(themeNames.toArray(new CharSequence[0]));
        themeList.setValue(preferences.getTheme().getName());
        themeList.setSummary(preferences.getTheme().getCaption());
        themeList.setOnPreferenceChangeListener(this::onSelectedTheme);

        List<String> languageNames = new ArrayList<>();
        List<String> languageTags = new ArrayList<>();

        for (Preferences.SupportedLanguage language : preferences.getSupportedLanguages()) {
            languageNames.add(requireContext().getString(language.getCaption()));
            languageTags.add(language.getTag());
        }

        ListPreference languageList = requirePreference("language");
        languageList.setEntries(languageNames.toArray(new CharSequence[0]));
        languageList.setEntryValues(languageTags.toArray(new CharSequence[0]));
        languageList.setSummary(preferences.getLanguage().getCaption());
        languageList.setValue(preferences.getLanguage().getTag());
        languageList.setOnPreferenceChangeListener(this::onSelectedLanguage);
    }

    /**
     * Obtiene la preferencia de forma obligatoria.
     *
     * @param key Identificador de la preferencia.
     * @param <T> Tipo de la preferencia.
     * @return La preferencia solicitada.
     */
    @SuppressWarnings("unchecked")
    private <T extends Preference> T requirePreference(String key) {
        Preference preference = findPreference(key);
        Objects.requireNonNull(preference);

        return (T) preference;
    }

    /**
     * Este método es llamado cuando se cambia el tiempo de bloqueo de la billetera.
     *
     * @param preference La preferencia que llama el método.
     * @param newTime    Valor seleccionado en la preferencia.
     *                   * @return Un true si la preferencia fue establecida.
     */
    private boolean onChangeLockTime(Preference preference, Object newTime) {
        Preferences.get().setLockTimeout(Integer.parseInt(newTime.toString()));
        return true;
    }

    /**
     * Este método es llamado cuando se hace clic en el botón  "Tema".
     *
     * @param preference    La preferencia que llama el método.
     * @param selectedTheme Valor seleccionado en la preferencia.
     * @return Un true si la preferencia fue establecida.
     */
    private boolean onSelectedTheme(Preference preference, Object selectedTheme) {
        Preferences.get().enableTheme(requireActivity(), selectedTheme.toString());
        requirePreference("theme")
                .setSummary(Preferences.get().getTheme().getCaption());
        return true;
    }

    /**
     * Este método es llamado cuando se hace clic en el botón "Divisa".
     *
     * @param preference       La preferencia que llama el método.
     * @param selectedCurrency Valor seleccionado en la preferencia.
     * @return Un true si la preferencia fue establecida.
     */
    private boolean onSelectedCurrency(Preference preference, Object selectedCurrency) {
        SupportedAssets fiatAsset = SupportedAssets.valueOf(selectedCurrency.toString());
        Preferences.get().setFiat(fiatAsset);
        requirePreference("currency").setSummary(Preferences.get().getFiat().getName());

        WalletProvider.getInstance().updateFiatCurrency(fiatAsset);

        return true;
    }

    /**
     * Verifica si se puede utilizar el lector de huellas.
     */
    private void checkBiometrics() {
        Preference biometric = requirePreference("biometric");
        Preference changePin = requirePreference("changepin");

        if (!canUseBiometric()) biometric.setVisible(false);
        else biometric.setVisible(true);

        changePin.setOnPreferenceClickListener(this::onUpdatedPin);
        biometric.setOnPreferenceChangeListener(this::onBiometricEnabledChange);
    }

    /**
     * Intenta activar el uso de biometricos para la autenticación.
     *
     * @return Un valor true si se activó el autenticador biometrico.
     */
    private boolean canUseBiometric() {
        return BiometricManager.from(requireContext()).canAuthenticate()
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /**
     * Este método es llamado cuando se cambia el uso de biometricos.
     *
     * @param preference La preferencia que llama el método.
     * @param isEnabled  Valor seleccionado en la preferencia.
     * @return Un true si la preferencia fue establecida.
     */
    private boolean onBiometricEnabledChange(final Preference preference, Object isEnabled) {
        final boolean enabled = isEnabled.equals(true);

        Preferences.get().authenticate(requireActivity(),
                new Handler()::post, (IAuthenticationSucceededCallback) ignoredToken -> {
                    if (!enabled) {
                        ((SwitchPreference) preference).setChecked(false);
                        Preferences.get().setEnabledBiometric(false);
                        requirePreference("changepin").setEnabled(true);
                    } else
                        Authenticator.authenticateWithBiometric(requireActivity(),
                                new Handler()::post,
                                (IAuthenticationSucceededCallback) ignoredToken2 -> {
                                    ((SwitchPreference) preference).setChecked(true);
                                    Preferences.get().setEnabledBiometric(true);
                                    requirePreference("changepin").setEnabled(false);
                                });
                });
        return false;
    }

    /**
     * Este método es llamado cuando se hace clic en el botón "Cambiar PIN".
     *
     * @param preference Preferencia que llama el método.
     * @return Un true si el Pin se puede cambiar.
     */
    private boolean onUpdatedPin(Preference preference) {
        Preferences preferences = Preferences.get();

        if (preferences.isEnabledBiometric())
            return false;

        Authenticator.updatePin(requireActivity(), Executors.newSingleThreadExecutor(),
                (IAuthenticationUpdatedCallback) (byte[] oldToken, byte[] newToken) -> {
                    ProgressDialog.show(requireActivity());
                    WalletProvider.getInstance()
                            .forEachWallet((wallet) -> {
                                wallet.updatePassword(oldToken, newToken);
                                ProgressDialog.hide();
                            });
                });

        return true;
    }

    /**
     * Este método es llamado cuando el lenguaje es cambiado.
     *
     * @param preference       Preferencia que llama el método.
     * @param selectedLanguage El lenguaje seleccionado.
     * @return Un valor true si es cambiado el lenguaje.
     */
    private boolean onSelectedLanguage(Preference preference, Object selectedLanguage) {
        if (selectedLanguage.toString().equals(Preferences.get().getLanguage().getTag()))
            return false;

        Preferences.get().setLanguage(requireActivity(), selectedLanguage.toString());
        requirePreference("language").setSummary(Preferences.get().getLanguage().getCaption());
        requireActivity().recreate();
        return true;
    }
}
