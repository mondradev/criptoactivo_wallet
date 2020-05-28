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
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.cryptowallet.R;
import com.cryptowallet.app.authentication.exceptions.AuthenticationException;
import com.cryptowallet.app.authentication.exceptions.PinAuthenticationRegisterException;
import com.cryptowallet.app.authentication.exceptions.PinAuthenticationUpdateException;
import com.cryptowallet.utils.Utils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import org.bouncycastle.util.encoders.Hex;

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
 * @version 2.0
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
     * Monitorea el ciclo de vida de la aplicación.
     */
    private LifecycleObserver mLifecycleObserver;

    /**
     * Token de autenticación requerido para actualizar el PIN.
     */
    private byte[] mAuthenticationToken;

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

        this.mLifecycleObserver = new LifecycleObserver() {

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            void onPause() {
                requireDialog().cancel();
            }

        };
    }

    /**
     * Muestra el autenticador por PIN.
     *
     * @param executor Ejecutor que llama las funciones del callback.
     * @param mode     Indica el modo en el cual funcionará el autenticador.
     * @param callback Funciones de llamada de vuelta para recibir las respuestas o validar el PIN
     *                 sin bloquear el fragmento.
     */
    public static void show(
            @NonNull FragmentActivity activity,
            @NonNull Executor executor,
            @NonNull PinAuthenticationMode mode,
            @NonNull IAuthenticationCallback callback) {

        PinAuthenticationFragment fragment =
                new PinAuthenticationFragment(executor, mode, callback);

        fragment.show(activity.getSupportFragmentManager(), TAG_FRAGMENT);
    }

    /**
     * Remueve el diálogo.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity()
                .getLifecycle().removeObserver(mLifecycleObserver);
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

        requireActivity()
                .getLifecycle().addObserver(mLifecycleObserver);

        createNumericalKeyboard();

        return view;
    }


    /**
     * Este método es llamado se crea una nueva instancia del diálogo.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();

        return new BottomSheetDialog(context,
                Utils.resolveStyle(context, R.attr.pinKeyboardTheme));
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


        clearPinDigits();
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

        TextView caption = mRoot.findViewById(R.id.mAuthDescription);

        if (caption == null)
            throw new Resources.NotFoundException();

        if (caption.getText() == mRoot.getContext().getString(R.string.not_match_pin_error))
            if (mMode == PinAuthenticationMode.REGISTER
                    || (mMode == PinAuthenticationMode.UPDATE
                    && mAuthenticationToken != null && mPinConfirmation == null))
                caption.setText(R.string.new_pin);
            else if (mPinConfirmation != null)
                caption.setText(R.string.commit_pin);
            else
                caption.setText(R.string.enter_pin);

        fillPin(mPinDigits.get(mPosition));

        mPin[mPosition] = Byte.parseByte(keyButton.getText().toString());

        mPosition++;

        if (mPosition == DIGITS)
            mHandler.postDelayed(() -> {
                if (mPinConfirmation == null)
                    handleCompletedPin();
                else {
                    if (Arrays.equals(mPin, mPinConfirmation))
                        handleCompletedPin();
                    else
                        handleFail();
                }
            }, 150);
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
            else {
                processing(() -> {
                    try {
                        byte[] token;
                        if (mMode == PinAuthenticationMode.UPDATE) {
                            token = AuthenticationHelper.getInstance(this.requireContext())
                                    .update(mAuthenticationToken, mPin);
                            handleUpdate(token);
                        } else
                            token = AuthenticationHelper.getInstance(this.requireContext())
                                    .register(mPin);

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
            }
        else processing(() -> validatePin(mPin));
    }

    /**
     * @param newToken
     */
    private void handleUpdate(byte[] newToken) {
        if (mAuthenticationToken == null)
            return;

        mHandler.post(() -> {
            mExecutor.execute(() -> mAuthPinCallback
                    .onAuthenticationUpdated(mAuthenticationToken, newToken));
        });
    }

    /**
     * Muestra la animación de carga mientras se ejecuta las operaciones en otro hilo.
     *
     * @param command Comando a ejecutar.
     */
    private void processing(Runnable command) {
        // TODO: Create animation for loadder

        final Dialog dialog = Objects.requireNonNull(this.getDialog());
        dialog.setCanceledOnTouchOutside(false);
        mRoot.findViewById(R.id.mAuthKeyboardLayout).setVisibility(View.GONE);

        mAuthenticatorExecutor.execute(() -> {
            command.run();
            mHandler.post(() -> {
                dialog.setCanceledOnTouchOutside(true);
                mRoot.findViewById(R.id.mAuthKeyboardLayout).setVisibility(View.VISIBLE);
            });
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
        mHandler.post(() -> {
            mAttemp++;

            mExecutor.execute(mAuthPinCallback::onAuthenticationFailed);

            // TODO: Implement time wait for next authentication.
            if (mAttemp >= ATTEMPS && mMode != PinAuthenticationMode.REGISTER
                    && mAuthenticationToken == null) {
                handleError(3, "Limit of failed attempts reached, try later");

                return;
            }

            TextView caption = mRoot.findViewById(R.id.mAuthDescription);

            if (caption == null)
                throw new Resources.NotFoundException();

            caption.setText(R.string.not_match_pin_error);

            clearPinDigits();
        });
    }

    /**
     * Esta función es llamada cuando ocurre un problema inrecuperable en el autenticador.
     */
    private void handleError(int code, String message) {
        mHandler.post(() -> {
            mExecutor.execute(() -> mAuthPinCallback.onAuthenticationError(code, message));
            dismiss();
        });
    }

    /**
     * Esta función es llamada cuando el validador del PIN indica que es correcto.
     *
     * @param authenticationToken Token de autenticación.
     */
    private void handleSuccess(byte[] authenticationToken) {
        mHandler.post(() -> {
            if (mMode == PinAuthenticationMode.UPDATE && mAuthenticationToken == null) {
                mAuthenticationToken = authenticationToken;
                clearPinDigits();
            } else {
                mExecutor.execute(() -> mAuthPinCallback
                        .onAuthenticationSucceeded(authenticationToken));
                Log.d(LOG_TAG, "Authentication: " + Hex.toHexString(authenticationToken));
                dismiss();
            }
        });
    }

    /**
     * Establece el indicador de digito del pin en su color activo.
     *
     * @param pinDigit Indicador de digito.
     */
    private void fillPin(@NonNull ImageView pinDigit) {
        pinDigit.setColorFilter(
                Utils.resolveColor(mRoot.getContext(), R.attr.colorAccent));
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
        pinDigit.setColorFilter(
                Utils.resolveColor(mRoot.getContext(), R.attr.colorOnSurface));
    }

    /**
     * Establece los inidicadores del pin en su color base.
     */
    private void clearPinDigits() {
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
            caption.setText(R.string.commit_pin);
        else if (mMode == PinAuthenticationMode.REGISTER
                || (mMode == PinAuthenticationMode.UPDATE && mAuthenticationToken != null))
            caption.setText(R.string.new_pin);
        else
            caption.setText(R.string.enter_pin);
    }

}
