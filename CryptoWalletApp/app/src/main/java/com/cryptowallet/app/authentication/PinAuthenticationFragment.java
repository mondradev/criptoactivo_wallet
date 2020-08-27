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

package com.cryptowallet.app.authentication;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.cryptowallet.R;
import com.cryptowallet.app.authentication.exceptions.AuthenticationException;
import com.cryptowallet.app.authentication.exceptions.PinAuthenticationRegisterException;
import com.cryptowallet.app.authentication.exceptions.PinAuthenticationUpdateException;
import com.cryptowallet.utils.Utils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Autenticador por número PIN. Fragmento que muestra un teclado numérico que permite la inserción
 * de los valores del PIN requerido para la autenticación del usuario.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.1
 * @see PinAuthenticationMode
 */
public class PinAuthenticationFragment extends BottomSheetDialogFragment {

    /**
     * TAG del fragmento.
     */
    private static final String TAG_FRAGMENT = "PinAuthenticationFragment";

    /**
     * Etiqueta del log.
     */
    private static final String LOG_TAG = "PinAuthentication";

    /**
     * Intentos para validar el PIN.
     */
    private static final int ATTEMPS = 3;

    /**
     * Número de digitos del PIN.
     */
    private static final int DIGITS = 6;

    /**
     * Ejecutor utilizado para llamar las funciones del callback.
     */
    private final Executor mExecutor;

    /**
     * Indica el modo en el que funcionará el autenticador.
     */
    private final PinAuthenticationMode mMode;

    /**
     * Función de vuelta para validar el PIN ingrensado.
     */
    private final IAuthenticationCallback mAuthPinCallback;

    /**
     * Handler para ejecutar funciones con un retraso.
     */
    private final Handler mHandler;

    /**
     * Listado de indicadores de digito.
     */
    private List<ImageView> mPinDigits;

    /**
     * Digito a capturar.
     */
    private int mPosition;

    /**
     * Valores del pin ingresado.
     */
    private byte[] mPin;

    /**
     * Valor del PIN que requiere confirmación.
     */
    private byte[] mPinConfirmation;

    /**
     * Ejecutor de la validación del PIN.
     */
    private Executor mAuthenticatorExecutor;

    /**
     * Vista del fragmento.
     */
    private View mRoot;

    /**
     * Intento actual de validación.
     */
    private int mAttemp;

    /**
     * Token de autenticación requerido para actualizar el PIN.
     */
    private byte[] mAuthenticationToken;

    /**
     * Indica si hay un fallo.
     */
    private boolean mFailed;

    /**
     * Crea un nuevo autenticador por PIN.
     *
     * @param executor Ejecutor que llama las funciones del callback.
     * @param mode     Indica el modo en el cual funcionará el autenticador.
     * @param callback Funciones de llamada de vuelta para recibir las respuestas o validar el PIN
     *                 sin bloquear el fragmento.
     */
    private PinAuthenticationFragment(
            Executor executor,
            PinAuthenticationMode mode,
            IAuthenticationCallback callback) {
        this.mExecutor = executor;
        this.mAuthPinCallback = callback;
        this.mMode = mode;
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mPinDigits = new ArrayList<>();
        this.mAuthenticatorExecutor = Executors.newSingleThreadExecutor();
        this.mAuthenticatorExecutor.execute(() -> Thread.currentThread().setName("Authenticator"));
        this.mAuthenticatorExecutor.execute(Looper::prepare);
    }

    /**
     * Muestra el autenticador por PIN.
     *
     * @param executor Ejecutor que llama las funciones del callback.
     * @param mode     Indica el modo en el cual funcionará el autenticador.
     * @param callback Funciones de llamada de vuelta para recibir las respuestas o validar el PIN
     *                 sin bloquear el fragmento.
     */
    static void show(
            @NonNull FragmentActivity activity,
            @NonNull Executor executor,
            @NonNull PinAuthenticationMode mode,
            @NonNull IAuthenticationCallback callback) {

        PinAuthenticationFragment fragment =
                new PinAuthenticationFragment(executor, mode, callback);

        fragment.show(activity.getSupportFragmentManager(), TAG_FRAGMENT);
    }

    /**
     * Este método es llamado se crea una nueva instancia de la vista del fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_pin_authenticator, container,
                false);

        if (view == null)
            throw new UnsupportedOperationException();

        mRoot = view;

        createNumericalKeyboard();
        clearPinDigits();

        return view;
    }

    /**
     * Este método es llamado se crea una nueva instancia del diálogo.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();

        BottomSheetDialog sheetDialog = new BottomSheetDialog(context,
                Utils.resolveStyle(context, R.attr.pinKeyboardTheme));
        sheetDialog.setOnShowListener(dialog -> ((BottomSheetDialog) dialog).getBehavior()
                .setState(BottomSheetBehavior.STATE_EXPANDED));

        return sheetDialog;
    }

    /**
     * Inicializa los eventos del teclado numérico y establece algunas referencias para cambiar el
     * estado de cada indicador.
     */
    private void createNumericalKeyboard() {
        LinearLayout pinLayout = mRoot.findViewById(R.id.mAuthPinDigitsLayout);

        if (pinLayout == null)
            throw new Resources.NotFoundException();

        if (mPinDigits.size() > 0)
            mPinDigits.clear();

        for (int i = 0; i < pinLayout.getChildCount(); i++)
            if (pinLayout.getChildAt(i) instanceof ImageView)
                mPinDigits.add((ImageView) pinLayout.getChildAt(i));

        LinearLayout keyboardLayout = mRoot.findViewById(R.id.mAuthKeyboardLayout);

        if (keyboardLayout == null)
            throw new Resources.NotFoundException();

        for (int i = 0; i < keyboardLayout.getChildCount(); i++)
            if (keyboardLayout.getChildAt(i) instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) keyboardLayout.getChildAt(i);
                for (int j = 0; j < row.getChildCount(); j++)
                    if (row.getChildAt(j) instanceof Button)
                        row.getChildAt(j).setOnClickListener(this::onPressedNumberButton);
            }
    }

    /**
     * Registra un digito del PIN, esta función es llamada cuando un botón numérico es presionado.
     *
     * @param sender Vista que desencadena el evento.
     */
    private void onPressedNumberButton(View sender) {
        MaterialButton keyButton = (MaterialButton) sender;

        if (keyButton.getIcon() != null) {
            onPressedBackspaceButton();
            return;
        }

        if (mPosition == DIGITS)
            return;

        setEnabledKeyboard(false);
        TextView caption = mRoot.findViewById(R.id.mAuthDescription);

        if (caption == null)
            throw new Resources.NotFoundException();

        if (mFailed && (mMode == PinAuthenticationMode.REGISTER
                || (mMode == PinAuthenticationMode.UPDATE
                && mAuthenticationToken != null && mPinConfirmation == null)))
            caption.setText(R.string.new_pin);

        fillPin(mPinDigits.get(mPosition));

        mPin[mPosition] = Byte.parseByte(keyButton.getText().toString());

        mPosition++;

        if (mPosition == DIGITS)
            if (mPinConfirmation == null)
                handleCompletedPin();
            else {
                if (Arrays.equals(mPin, mPinConfirmation))
                    handleCompletedPin();
                else
                    mAuthenticatorExecutor.execute(this::handleFail);
            }
        else
            mHandler.postDelayed(() -> setEnabledKeyboard(true), 100);
    }

    /**
     * Establece si el teclado está activo o no.
     *
     * @param enabled True para indicar que está activo.
     */
    private void setEnabledKeyboard(boolean enabled) {
        final LinearLayout keyboardLayout = mRoot.findViewById(R.id.mAuthKeyboardLayout);

        for (int i = 0; i < keyboardLayout.getChildCount(); i++)
            if (keyboardLayout.getChildAt(i) instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) keyboardLayout.getChildAt(i);

                for (int j = 0; j < row.getChildCount(); j++)
                    if (row.getChildAt(j) instanceof Button)
                        row.getChildAt(j).setEnabled(enabled);
            }
    }


    /**
     * Remueve el último digito del PIN registrado, esta función es llamada cuando el botón de
     * borrar es presionado.
     */
    private void onPressedBackspaceButton() {
        if (mPosition == 0)
            return;

        clearPin(mPinDigits.get(mPosition - 1));

        mPin[mPosition - 1] = 0;

        mPosition--;
    }

    /**
     * Esta función es llamada cuando se han ingresado todos los digitos del PIN.
     */
    private void handleCompletedPin() {
        if (mMode != PinAuthenticationMode.AUTHENTICATE)
            if (mAuthenticationToken == null && mMode == PinAuthenticationMode.UPDATE)
                processing(() -> validatePin(mPin));
            else if (mPinConfirmation == null)
                confirm();
            else
                processing(() -> {
                    try {
                        byte[] token;

                        AuthenticationHelper instance = AuthenticationHelper
                                .getInstance(this.requireContext());

                        if (mMode == PinAuthenticationMode.UPDATE) {
                            token = instance.update(mAuthenticationToken, mPin);
                            handleUpdate(token);
                        } else
                            token = instance.register(mPin);

                        handleSuccess(token);
                    } catch (PinAuthenticationUpdateException ex) {
                        Log.w(LOG_TAG, Objects.requireNonNull(ex.getMessage()));
                        handleError(1,
                                "Unable to update PIN due to internal application error");
                    } catch (PinAuthenticationRegisterException ex) {
                        Log.w(LOG_TAG, Objects.requireNonNull(ex.getMessage()));
                        handleError(2,
                                "Unable to register PIN due to internal application error");
                    }
                });
        else processing(() -> validatePin(mPin));
    }

    /**
     * Esta función es llamada cuando se finaliza la actualización del PIN de autenticación. Esto
     * invoca el evento "AuthenticationUpdated".
     *
     * @param newToken Nuevo token de autenticación.
     */
    private void handleUpdate(byte[] newToken) {
        if (mAuthenticationToken == null)
            return;

        mHandler.post(() -> mExecutor.execute(() -> mAuthPinCallback
                .onAuthenticationUpdated(mAuthenticationToken, newToken)));
    }

    /**
     * Muestra la animación de carga mientras se ejecuta las operaciones en otro hilo.
     *
     * @param command Comando a ejecutar.
     */
    private void processing(Runnable command) {
        final Dialog dialog = requireDialog();
        dialog.setCanceledOnTouchOutside(false);

        mAuthenticatorExecutor.execute(() -> {
            command.run();
            mHandler.post(() -> dialog.setCanceledOnTouchOutside(true));
        });
    }

    /**
     * Valida si el PIN ingresado es correcto.
     *
     * @param pinValue Valor del PIN a validar.
     */
    private void validatePin(byte[] pinValue) {
        try {
            final byte[] token = AuthenticationHelper
                    .getInstance(this.requireContext())
                    .authenticate(pinValue);

            if (token == null)
                handleFail();
            else
                handleSuccess(token);
        } catch (AuthenticationException ex) {
            Log.w(LOG_TAG, Objects.requireNonNull(ex.getMessage()));
            handleError(0,
                    "Unable to authenticate due to internal application error");
        }
    }

    /**
     * Este método es llamado cuando se cancela el autenticador.
     */
    @Override
    public void onCancel(@NonNull DialogInterface ignored) {
        handleError(4, "User canceled the operation");
    }

    /**
     * Esta función es llamada cuando el validador del PIN indica que no es correcto.
     */
    private void handleFail() {
        if (mPinConfirmation != null) mPinConfirmation = null;

        mFailed = true;
        mAttemp++;

        startErrorAnimation();

        mExecutor.execute(mAuthPinCallback::onAuthenticationFailed);

        mHandler.post(() -> {
            // TODO: Implement time wait for next authentication.
            if (mAttemp >= ATTEMPS && mMode != PinAuthenticationMode.REGISTER
                    && mAuthenticationToken == null) {
                handleError(3, "Limit of failed attempts reached, try later");

                return;
            }

            TextView caption = mRoot.findViewById(R.id.mAuthDescription);

            if (caption == null)
                throw new Resources.NotFoundException();

            clearPinDigits();
        });
    }

    /**
     * Esta función es llamada cuando ocurre un problema inrecuperable en el autenticador.
     */
    private void handleError(int code, String message) {
        startErrorAnimation();

        mHandler.post(() -> {
            mExecutor.execute(() -> mAuthPinCallback.onAuthenticationError(code, message));
            dismissAllowingStateLoss();
        });
    }

    /**
     * Inicia la animación de error en la entrada.
     */
    private void startErrorAnimation() {
        final int[] stateSet = {android.R.attr.state_checked, android.R.attr.state_selected};

        for (ImageView view : mPinDigits)
            if (view == null)
                throw new Resources.NotFoundException();
            else
                view.setImageState(stateSet, true);

        Utils.tryNotThrow(() -> Thread.sleep(550));
    }

    /**
     * Esta función es llamada cuando el validador del PIN indica que es correcto.
     *
     * @param authenticationToken Token de autenticación.
     */
    private void handleSuccess(byte[] authenticationToken) {
        startSuccessAnimation();

        mHandler.post(() -> {
            if (mMode == PinAuthenticationMode.UPDATE && mAuthenticationToken == null) {
                mAuthenticationToken = authenticationToken;
                clearPinDigits();
            } else {
                mExecutor.execute(() -> mAuthPinCallback
                        .onAuthenticationSucceeded(authenticationToken));
                dismiss();
            }
        });
    }

    /**
     * Inicia la animación de la entrada correcta.
     */
    private void startSuccessAnimation() {
        final int[] stateSet = {android.R.attr.state_checked, android.R.attr.state_pressed};

        for (ImageView view : mPinDigits) {
            if (view == null)
                throw new Resources.NotFoundException();
            else
                view.setImageState(stateSet, true);

            Utils.tryNotThrow(() -> Thread.sleep(10));
        }

        Utils.tryNotThrow(() -> Thread.sleep(600));
    }

    /**
     * Establece el indicador de digito del pin en su color activo.
     *
     * @param pinDigit Indicador de digito.
     */
    private void fillPin(@NonNull ImageView pinDigit) {
        final int[] stateSet = {android.R.attr.state_checked};
        pinDigit.setImageState(stateSet, true);
    }


    /**
     * Establece el autenticador en confirmación de PIN. Esto permite que se gestione la entrada
     * correcta del PIN.
     */
    private void confirm() {
        if (mPosition != DIGITS)
            return;

        mPinConfirmation = mPin;

        clearPinDigits();
    }

    /**
     * Establece el indicador de digito del pin en su color base.
     *
     * @param pinDigit Indicador de digito.
     */
    private void clearPin(@NonNull ImageView pinDigit) {
        final int[] stateSet = {-android.R.attr.state_checked, -android.R.attr.state_selected};
        pinDigit.setImageState(stateSet, true);
    }

    /**
     * Establece los inidicadores del pin en su color base.
     */
    private void clearPinDigits() {
        mHandler.post(() -> {
            for (ImageView view : mPinDigits)
                if (view == null)
                    throw new Resources.NotFoundException();
                else
                    clearPin(view);

            mPin = new byte[DIGITS];
            mPosition = 0;

            TextView caption = mRoot.findViewById(R.id.mAuthDescription);

            if (caption == null)
                throw new Resources.NotFoundException();

            if (mPinConfirmation != null)
                caption.setText(R.string.confirm_pin_text);
            else if (mMode == PinAuthenticationMode.REGISTER
                    || (mMode == PinAuthenticationMode.UPDATE && mAuthenticationToken != null))
                caption.setText(R.string.new_pin);
            else
                caption.setText(R.string.enter_pin);

            setEnabledKeyboard(true);
        });
    }

}
