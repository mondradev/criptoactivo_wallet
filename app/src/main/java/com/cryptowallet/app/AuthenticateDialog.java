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
import android.content.DialogInterface;
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
import android.util.Log;
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
import com.squareup.okhttp.internal.NamedRunnable;

import java.util.Objects;
import java.util.logging.Logger;

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
     *
     */
    private static final Logger mLogger = Logger.getLogger("AuthenticateDialog");

    /**
     * Modo del cuadro de diálogo de autenticación.
     */
    private int mMode;

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
    private DialogInterface mListener;
    /**
     *
     */
    private boolean mDismissOnAuth;
    /**
     *
     */
    private boolean mShowUIProgress;
    /**
     *
     */
    private String mMessage;
    /**
     *
     */
    private View.OnClickListener mHandlerPad = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mHasError) {
                if (mMode == REG_PIN)
                    setInfo(R.string.indications_pin_setup);
                else
                    setInfo(R.string.enter_pin);
                mHasError = false;
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

    /**
     * @param dialog
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mShowing = false;

        if (!Utils.isNull(mListener))
            mListener.dismiss();
    }

    /**
     * @param message
     */
    public void showUIProgress(String message) {
        showUIProgress(message, getActivity());
    }

    /**
     *
     */
    public void showUIProgress(final String message, Activity caller) {
        if (!isShowing() && !mShowUIProgress) {
            show(caller);
            mShowUIProgress = true;
            mMessage = message;
        } else {
            mShowUIProgress = false;
            mMessage = null;

            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setCancelable(false);

                    View view = Objects.requireNonNull(getView());

                    view.findViewById(R.id.mAuthMain).setVisibility(View.GONE);
                    view.findViewById(R.id.mProgress).setVisibility(View.VISIBLE);

                    ((TextView) view.findViewById(R.id.mCaptionText)).setText(message);
                }
            });
        }
    }

    public void showUIAuth() {
        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setCancelable(true);

                View view = Objects.requireNonNull(getView());

                view.findViewById(R.id.mProgress).setVisibility(View.GONE);
                view.findViewById(R.id.mAuthMain).setVisibility(View.VISIBLE);
            }
        });
    }

    private void processAuth() {
        if (isCompleted(mPinIndex)) {

            if (mMode == REG_PIN) {

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
                showUIProgress(getString(R.string.validate_pin_text));
                new ValidatePinThread().execute(this);
            }


        }
    }

    /**
     * @param mode
     */
    public AuthenticateDialog setMode(@Mode int mode) {
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

        return view;
    }

    /**
     * @param index
     * @return
     */
    public ImageView getPinView(int index) {
        switch (index) {
            case 0:
                return Objects.requireNonNull(getView()).findViewById(R.id.mPin1);
            case 1:
                return Objects.requireNonNull(getView()).findViewById(R.id.mPin2);
            case 2:
                return Objects.requireNonNull(getView()).findViewById(R.id.mPin3);
            case 3:
                return Objects.requireNonNull(getView()).findViewById(R.id.mPin4);
        }

        return null;
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

        if (mShowUIProgress)
            showUIProgress(mMessage, getActivity());
    }

    /**
     * Muestra el cuadro de diálogo.
     */
    public void show(final Activity caller) {
        Objects.requireNonNull(caller);

        caller.runOnUiThread(new NamedRunnable("Run-Dialog") {
            @Override
            protected void execute() {
                FragmentManager manager = ((FragmentActivity) caller).getSupportFragmentManager();
                Objects.requireNonNull(manager);
                FragmentTransaction transaction = manager.beginTransaction();
                show(transaction, TAG);
                mShowing = true;
            }
        });

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

        Log.v(TAG, "Inicializando lector de huellas.");

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
                Utils.showSnackbar(view.findViewById(R.id.mAuthMain),
                        getString(R.string.require_finger_permission));
            } else {

                if (!fingerprintManager.hasEnrolledFingerprints()) {
                    Utils.showSnackbar(view.findViewById(R.id.mAuthMain),
                            getString(R.string.require_finger_register));
                } else {

                    if (!keyguardManager.isKeyguardSecure()) {
                        Utils.showSnackbar(view.findViewById(R.id.mAuthMain),
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

    /**
     * @param wallet
     * @return
     */
    public AuthenticateDialog setWallet(WalletServiceBase wallet) {
        mWallet = wallet;

        return this;
    }

    /**
     *
     */
    public void cancel() {
        if (isShowing() && isCancelable())
            getDialog().cancel();
    }

    /**
     *
     */
    private synchronized void done() {
        mDone = true;
        notify();

        if (mDismissOnAuth)
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

        mLogger.info("Returning data");
        return mAuthData;
    }

    /**
     * @param dialog
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        mDone = true;

        if (!Utils.isNull(mListener))
            mListener.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Establece y muestra la información sobre el estado de la autenticación.
     *
     * @param idRes Identificador del texto a visualizar.
     */
    private void setInfo(@StringRes final int idRes) {
        final TextView mInfoLabel = Objects.requireNonNull(getView()).findViewById(R.id.mInfo);
        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
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
        for (int i = 0; i < PIN_LENGHT; i++) {
            final int index = i;
            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Objects.requireNonNull(getPinView(index))
                            .setImageDrawable(Objects.requireNonNull(getView()).getContext()
                                    .getDrawable(R.drawable.br_pin));
                }
            });
        }
    }

    /**
     * Rellena el PIN para visualizarlo como seleccionado.
     *
     * @param digit Digito del pin a rellenar.
     */
    private void fillPin(int digit) {
        if (Utils.between(digit, 0, PIN_LENGHT - 1))
            Objects.requireNonNull(getPinView(digit))
                    .setImageDrawable(Objects.requireNonNull(getView()).getContext()
                            .getDrawable(R.drawable.bg_pin));
    }

    /**
     * Limpia el digito especificado del PIN.
     *
     * @param digit Digito a limpiar.
     */
    private void cleanPin(int digit) {
        if (Utils.between(digit, 0, PIN_LENGHT - 1))
            Objects.requireNonNull(getPinView(digit))
                    .setImageDrawable(Objects.requireNonNull(getView()).getContext()
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
     * @return
     */
    public boolean isShowing() {
        return mShowing;
    }

    /**
     * @param listener
     */
    public void setListener(DialogInterface listener) {
        mListener = listener;
    }

    public AuthenticateDialog dismissOnAuth() {
        mDismissOnAuth = true;

        return this;
    }

    public AuthenticateDialog setOnDesmiss(final Runnable command) {
        if (mListener != null)
            mListener = new DialogInterface() {
                @Override
                public void cancel() {
                    mListener.cancel();
                }

                @Override
                public void dismiss() {
                    mListener.dismiss();
                    command.run();
                }
            };
        else
            mListener = new DialogInterface() {
                @Override
                public void cancel() {

                }

                @Override
                public void dismiss() {
                    command.run();
                }
            };
        return this;
    }


    public AuthenticateDialog setOnCancel(final Runnable command) {
        if (mListener != null)
            mListener = new DialogInterface() {
                @Override
                public void cancel() {
                    mListener.cancel();
                    command.run();
                }

                @Override
                public void dismiss() {
                    mListener.dismiss();
                }
            };
        else
            mListener = new DialogInterface() {
                @Override
                public void cancel() {
                    command.run();
                }

                @Override
                public void dismiss() {

                }
            };
        return this;
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
         * Indica si se deberá mostrar la UI de autenticación.
         */
        boolean isRetryAuth = false;

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


            if (self.mWallet != null && self.mWallet.validateAccess(dataPin)) {

                if (self.mMode == REG_FINGER) {
                    Objects.requireNonNull(self.getView()).post(new Runnable() {
                        @Override
                        public void run() {
                            self.initFingerprint();
                            isRetryAuth = true;
                        }
                    });
                } else {
                    self.mAuthData = dataPin;
                    isFinalized = true;
                }
            } else {
                isRetryAuth = true;
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
         * @param dialog Cuadro de diálogo que invocó a la tarea.
         */
        @Override
        protected void onPostExecute(AuthenticateDialog dialog) {
            if (isFinalized)
                if (dialog.mHasError)
                    dialog.getDialog().cancel();
                else
                    dialog.done();

            if (isRetryAuth)
                dialog.showUIAuth();
        }
    }
}
