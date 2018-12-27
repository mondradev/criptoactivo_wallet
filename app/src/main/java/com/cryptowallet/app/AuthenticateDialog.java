/*
 * Copyright 2018 InnSy Tech
 * Copyright 2018 Ing. Javier de Jesús Flores Mondragón
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
import android.app.Activity;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.security.FingerprintHandler;
import com.cryptowallet.security.Security;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.WalletServiceBase;
import com.google.common.base.Strings;

import java.util.Objects;

import javax.crypto.Cipher;

import static android.content.Context.FINGERPRINT_SERVICE;
import static android.content.Context.KEYGUARD_SERVICE;

/**
 * Cuadro de diálogo que permite la autenticación en la billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class AuthenticateDialog extends DialogFragment {

    /**
     *
     */
    public static final int AUTH = 0;

    /**
     *
     */
    public static final int REG_PIN = 1;

    /**
     *
     */
    public static final int REG_FINGER = 2;

    /**
     * Longitud del pin.
     */
    private static final int PIN_LENGHT = 4;

    /**
     * Etiqueta del cuadro de diálogo.
     */
    private static final String TAG = "AuthenticateDialog";

    /**
     * Modo del cuadro de diálogo de autenticación.
     */
    private int mMode;

    /**
     * Imágenes del PIN.
     */
    private ImageView[] mPinDigitViews = new ImageView[4];

    /**
     * Información de autenticación.
     */
    private byte[] mAuthData;

    /**
     * Indica si el cuadro de diálogo finalizó.
     */
    private boolean mDone;

    /**
     * Indica si existe un error.
     */
    private boolean mHasError;

    /**
     * Digito actual a presionar.
     */
    private int mPinIndex;

    /**
     *
     */
    private View.OnClickListener mHandlerBackSpace = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mPinIndex > 0) {
                mPinIndex--;
                cleanPin(mPinIndex);
            }
        }
    };

    /**
     * Valores del pin.
     */
    private String[] mPinValues;
    /**
     *
     */
    private boolean mToCommit;
    /**
     *
     */
    private String[] mPinToCommit;
    /**
     *
     */
    private boolean mShowing;
    /**
     *
     */
    private WalletServiceBase mWallet;
    /**
     *
     */
    private int mAttemp;
    /**
     *
     */
    private byte[] mAuthKey;
    /**
     *
     */
    private View.OnClickListener mHandlerPad = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mHasError) {
                hideInfo();
                mHasError = true;
            }

            Button mPad = (Button) view;

            if (isCompleted(mPinIndex))
                return;

            mPinValues[mPinIndex] = mPad.getText().toString();

            fillPin(mPinIndex);

            mPinIndex++;

            processAuth();
        }
    };

    /**
     *
     */
    public AuthenticateDialog() {
        mPinValues = new String[PIN_LENGHT];
    }

    private void processAuth() {
        if (isCompleted(mPinIndex)) {

            if (mMode == REG_PIN && mAuthKey != null) {

                if (mToCommit) {

                    if (!pinEquals(mPinValues, mPinToCommit)) {
                        setInfo(R.string.pin_no_equal);
                        mPinValues = new String[4];
                        mToCommit = false;
                        mPinIndex = 0;
                        mHasError = true;

                        cleanPin();
                    } else {
                        byte[] dataPin = Base64.decode(Utils.concatAll(mPinValues),
                                Base64.DEFAULT);

                        Security.get().createKey(dataPin);

                        mAuthData = Security.get().getKey();
                        done();
                    }

                } else {
                    mPinIndex = 0;
                    mPinToCommit = mPinValues;
                    mPinValues = new String[4];
                    mToCommit = true;

                    cleanPin();

                    setInfo(R.string.commit_pin);
                }

            } else {
                new ValidatePinThread().execute(this);
            }


        }
    }

    /**
     * @param mode
     */
    public AuthenticateDialog setMode(@Mode int mode) {
        if (mShowing)
            throw new IllegalStateException("Solo se puede establecer el modo antes de mostrar " +
                    "el cuadro de diálogo");
        mMode = mode;

        return this;
    }

    /**
     * Indica si el PIN alcanzó su longitud.
     *
     * @param currentLength Longitud actual del PIN.
     * @return Un valor true si alcanzó la longitud.
     */
    private boolean isCompleted(int currentLength) {
        return currentLength == PIN_LENGHT;
    }

    /**
     * Este métedo es llamado cuando se crea el cuadro de diálogo por primera vez.
     *
     * @param savedInstanceState Estado de la aplicación.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);
    }

    /**
     * Este método es llamado cuando se crea la vista.
     *
     * @param inflater           XML de los objetos de las vistas.
     * @param container          Contenedor de las vistas.
     * @param savedInstanceState Estado de la aplicación.
     * @return La vista que representa al cuadro de diálogo.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final Context context = new ContextWrapper(getActivity());
        AppPreference.loadTheme(context);

        LayoutInflater localInflater = inflater.cloneInContext(context);

        View view = localInflater.inflate(R.layout.dialog_loginwallet_fullscreen, container,
                false);

        view.findViewById(R.id.mNum0).setOnClickListener(mHandlerPad);
        view.findViewById(R.id.mNum1).setOnClickListener(mHandlerPad);
        view.findViewById(R.id.mNum2).setOnClickListener(mHandlerPad);
        view.findViewById(R.id.mNum3).setOnClickListener(mHandlerPad);
        view.findViewById(R.id.mNum4).setOnClickListener(mHandlerPad);
        view.findViewById(R.id.mNum5).setOnClickListener(mHandlerPad);
        view.findViewById(R.id.mNum6).setOnClickListener(mHandlerPad);
        view.findViewById(R.id.mNum7).setOnClickListener(mHandlerPad);
        view.findViewById(R.id.mNum8).setOnClickListener(mHandlerPad);
        view.findViewById(R.id.mNum9).setOnClickListener(mHandlerPad);

        view.findViewById(R.id.mBackSpace).setOnClickListener(mHandlerBackSpace);

        mPinDigitViews[0] = view.findViewById(R.id.mPin1);
        mPinDigitViews[1] = view.findViewById(R.id.mPin2);
        mPinDigitViews[2] = view.findViewById(R.id.mPin3);
        mPinDigitViews[3] = view.findViewById(R.id.mPin4);

        return view;
    }

    /**
     * Este método es llamado cuando se inicia el cuadro de diálogo.
     */
    @Override
    public void onStart() {
        super.onStart();

        Dialog self = getDialog();

        if (self == null)
            return;

        Objects.requireNonNull(self.getWindow()).setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    /**
     * Muestra el cuadro de diálogo.
     */
    public void show(Activity caller) {
        Objects.requireNonNull(caller);
        FragmentManager manager = ((FragmentActivity) caller).getSupportFragmentManager();
        Objects.requireNonNull(manager);
        FragmentTransaction transaction = manager.beginTransaction();
        show(transaction, TAG);
        mShowing = true;
    }

    /**
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (AppPreference.getUseFingerprint(getActivity()))
            initFingerprint();
        else if (mMode == REG_PIN)
            setInfo(R.string.indications_pin_setup);
        else
            setInfo(R.string.enter_pin);
    }


    /**
     * Inicializa el lector de huellas. Solo para versiones Marshmallow o superior.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void initFingerprint() {

        View view = getView();

        if (view == null)
            return;

        view.findViewById(R.id.mUsePin).setVisibility(View.GONE);
        view.findViewById(R.id.mFingerprintLayout).setVisibility(View.VISIBLE);

        KeyguardManager keyguardManager = (KeyguardManager) Objects.requireNonNull(getActivity())
                .getSystemService(KEYGUARD_SERVICE);
        FingerprintManager fingerprintManager
                = (FingerprintManager) getActivity().getSystemService(FINGERPRINT_SERVICE);

        if (fingerprintManager.isHardwareDetected()) {
            if (ActivityCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                Utils.showSnackbar(view.findViewById(R.id.mLoginContainer),
                        getString(R.string.require_finger_permission));
            } else {

                if (!fingerprintManager.hasEnrolledFingerprints()) {
                    Utils.showSnackbar(view.findViewById(R.id.mLoginContainer),
                            getString(R.string.require_finger_register));
                } else {

                    if (!keyguardManager.isKeyguardSecure()) {
                        Utils.showSnackbar(view.findViewById(R.id.mLoginContainer),
                                getString(R.string.no_lock_screen));
                    } else {
                        Security.get().createAndroidKeyIfRequire();

                        FingerprintManager.CryptoObject cryptoObject
                                = new FingerprintManager.CryptoObject(
                                Security.get().getCipher(getActivity(),
                                        mMode != REG_FINGER));

                        new FingerprintHandler(getActivity()) {

                            @Override
                            public void onAuthenticationSucceeded(
                                    FingerprintManager.AuthenticationResult result) {
                                Cipher cipher = result.getCryptoObject().getCipher();

                                if (mMode == REG_FINGER) {
                                    byte[] dataPin = Base64.decode(Utils.concatAll(mPinValues),
                                            Base64.DEFAULT);
                                    Security.get().encrypteKeyData(getActivity(), cipher, dataPin);
                                    AppPreference.setUseFingerprint(getActivity(), true);
                                } else
                                    Security.get().decryptKey(getActivity(), cipher);

                                mAuthData = Security.get().getKey();
                                done();
                            }

                            @Override
                            public void onAuthenticationFailed() {
                                done();
                            }

                        }.startAuth(fingerprintManager, cryptoObject);
                    }
                }
            }
        }
    }

    public AuthenticateDialog setWallet(WalletServiceBase wallet) {
        if (mShowing)
            throw new IllegalStateException("Solo se puede establecer la billetera antes de " +
                    "mostrar el cuadro de diálogo");

        mWallet = wallet;

        return this;
    }

    /**
     *
     */
    private synchronized void done() {
        mDone = true;
        notify();
        dismiss();
    }

    /**
     * Obtiene la información de autenticación.
     *
     * @return Información de autenticación.
     */
    public synchronized byte[] getAuthData() throws InterruptedException {
        if (!mDone)
            wait();

        return mAuthData;
    }

    /**
     * Obtiene la información de autenticación.
     *
     * @return Información de autenticación.
     */
    public synchronized byte[] getAuthKey() throws InterruptedException {
        if (!mDone)
            wait();

        return mAuthKey;
    }

    /**
     * Oculta el elemento que visualiza el texto de información.
     */
    private void hideInfo() {
        final TextView mInfoLabel = Objects.requireNonNull(getView()).findViewById(R.id.mInfo);
        mInfoLabel.post(new Runnable() {
            @Override
            public void run() {
                mInfoLabel.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Establece y muestra la información sobre el estado de la autenticación.
     *
     * @param idRes Identificador del texto a visualizar.
     */
    private void setInfo(@StringRes final int idRes) {
        final TextView mInfoLabel = Objects.requireNonNull(getView()).findViewById(R.id.mInfo);
        mInfoLabel.post(new Runnable() {
            @Override
            public void run() {
                mInfoLabel.setText(idRes);
                if (mInfoLabel.getVisibility() != View.VISIBLE)
                    mInfoLabel.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Limpia los digitos seleccionados del PIN.
     */
    private void cleanPin() {
        for (final ImageView pin : mPinDigitViews)
            pin.post(new Runnable() {
                @Override
                public void run() {
                    pin.setImageDrawable(Objects.requireNonNull(getView()).getContext()
                            .getDrawable(R.drawable.br_pin));
                }
            });
    }

    /**
     * Rellena el PIN para visualizarlo como seleccionado.
     *
     * @param digit Digito del pin a rellenar.
     */
    private void fillPin(int digit) {
        if (Utils.between(digit, 0, mPinDigitViews.length - 1))
            mPinDigitViews[digit].setImageDrawable(Objects.requireNonNull(getView()).getContext()
                    .getDrawable(R.drawable.bg_pin));
    }

    /**
     * Limpia el digito especificado del PIN.
     *
     * @param digit Digito a limpiar.
     */
    private void cleanPin(int digit) {
        if (Utils.between(digit, 0, mPinDigitViews.length - 1))
            mPinDigitViews[digit].setImageDrawable(Objects.requireNonNull(getView()).getContext()
                    .getDrawable(R.drawable.br_pin));
    }

    /**
     * Compara si el PIN elegido y su confirmación son correctas.
     *
     * @param left  PIN A.
     * @param right PIN B.
     * @return Si ambos PINs son iguales.
     */
    private boolean pinEquals(String[] left, String[] right) {
        for (int i = 0; i < PIN_LENGHT; i++) {
            if (Strings.isNullOrEmpty(left[i]) || Strings.isNullOrEmpty(right[i]))
                return false;

            if (!left[i].contentEquals(right[i]))
                return false;
        }
        return true;
    }

    /**
     *
     */
    @IntDef(value = {AUTH, REG_PIN, REG_FINGER})
    public @interface Mode {
    }

    /**
     * Subproceso que valida si el PIN es válido y notifica a la interfaz que oculte el cuadro de
     * diálogo.
     *
     * @author Ing. Javier Flores
     * @version 1.1
     */
    private static class ValidatePinThread
            extends AsyncTask<AuthenticateDialog, Void, AuthenticateDialog> {

        /**
         * Indica si se requiere finalizar la actividad.
         */
        boolean isFinalized = false;

        /**
         * Realiza en un subproceso la validación del PIN de la billetera.
         *
         * @param activities La actividad que invocan al subproceso.
         * @return Se retorna la actividad que invocó al subproceso.
         */
        @Override
        protected AuthenticateDialog doInBackground(AuthenticateDialog... activities) {
            final AuthenticateDialog self = activities[0];

            byte[] dataPin;

            if (self.mAuthData == null) {
                dataPin = Base64.decode(Utils.concatAll(self.mPinValues), Base64.DEFAULT);
                Security.get().createKey(dataPin);
                dataPin = Security.get().getKey();
            } else
                dataPin = self.mAuthData;


            if (self.mWallet.validateAccess(dataPin)) {

                if (self.mMode > AUTH) {

                    if (self.mMode == REG_FINGER) {
                        Objects.requireNonNull(self.getView()).post(new Runnable() {
                            @Override
                            public void run() {
                                self.initFingerprint();
                            }
                        });
                    } else {
                        self.mAuthKey = dataPin;
                        self.setInfo(R.string.indications_pin_setup);
                        self.mPinIndex = 0;
                        self.cleanPin();
                    }
                } else {
                    self.mAuthData = dataPin;
                    isFinalized = true;
                }
            } else {
                self.setInfo(R.string.error_pin);
                self.mHasError = true;
                self.mPinIndex = 0;
                self.mPinValues = new String[4];
                self.mAttemp++;

                int MAX_ATTEMP = 3;
                if (self.mAttemp >= MAX_ATTEMP)
                    isFinalized = true;
                else
                    self.cleanPin();
            }

            return self;
        }

        /**
         * Este método es llamado al finalizar el proceso, y notifica a la interfaz que debe cerrarse
         * la actividad si se requiere, así como ocultar el cuadro diálogo.
         *
         * @param activity Actividad que invocó a la tarea.
         */
        @Override
        protected void onPostExecute(AuthenticateDialog activity) {
            if (isFinalized)
                activity.done();
        }
    }
}
