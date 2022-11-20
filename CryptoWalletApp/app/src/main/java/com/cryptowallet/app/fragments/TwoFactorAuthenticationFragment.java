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

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.cryptowallet.R;
import com.cryptowallet.app.LockableActivity;
import com.cryptowallet.app.authentication.TwoFactorAuthentication;
import com.cryptowallet.utils.textwatchers.IAfterTextChangedListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Este fragmento provee de un cuadro de dialogo inferior que permite la captura del codigo de
 * autenticación de dos factores.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class TwoFactorAuthenticationFragment extends BottomSheetDialogFragment {

    /**
     * TAG del fragmento.
     */
    private static final String TAG_FRAGMENT = "TwoFactorAuthenticationFragment";

    /**
     *
     */
    private static final int MAX_LENGTH = 1;

    /**
     * Identificadores de los digitos del código de autenticación.
     */
    private final List<Integer> mDigitsAuthCode = Lists.newArrayList(
            R.id.m2FaCodeDigit1, R.id.m2FaCodeDigit2, R.id.m2FaCodeDigit3,
            R.id.m2FaCodeDigit4, R.id.m2FaCodeDigit5, R.id.m2FaCodeDigit6
    );

    /**
     * Observador del ciclo de vida de la aplicación.
     */
    private LifecycleObserver mLifeCycleObserver;

    /**
     * Indica que la aplicación está a la espera del código de autenticación.
     */
    private boolean mOnPause;

    /**
     * Función para indicar que se autenticó.
     */
    private IAuthenticationSucceeded mSuccededCallback;

    /**
     *
     */
    private ViewTreeObserver.OnWindowFocusChangeListener mWindowFocusListener;

    /**
     *
     */
    private View mRoot;

    /**
     * Crea una instancia nueva del autenticador.
     *
     * @param onSucceeded Función que permite indicar si fue completada la autenticación.
     */
    private TwoFactorAuthenticationFragment(IAuthenticationSucceeded onSucceeded) {
        mSuccededCallback = onSucceeded;
    }

    /**
     * Muestra un cuadro de diálogo inferior con los datos de recepción de la billetera.
     *
     * @param activity Actividad que invoca.
     */
    public static void show(@NonNull FragmentActivity activity, IAuthenticationSucceeded onSucceeded) {
        new TwoFactorAuthenticationFragment(onSucceeded)
                .show(activity.getSupportFragmentManager(), TAG_FRAGMENT);
    }

    /**
     * Este método es llamado se crea una nueva instancia de la vista del fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.bsd_authenticator_2fa, container, false);

        if (mRoot == null)
            throw new UnsupportedOperationException();

        mRoot.findViewById(R.id.m2FaSubmitButton).setOnClickListener(this::onSubmit);

        configureEditText();

        mWindowFocusListener = focused -> {
            if (!focused || !mOnPause) return;

            mOnPause = false;

            ClipboardManager clipboard = (ClipboardManager) requireActivity()
                    .getSystemService(CLIPBOARD_SERVICE);

            if (clipboard == null) return;

            ClipData clipData = clipboard.getPrimaryClip();

            if (clipData == null) return;
            if (clipData.getItemCount() == 0) return;

            CharSequence text = clipData.getItemAt(0)
                    .coerceToText(TwoFactorAuthenticationFragment.this.requireContext());

            if (text.length() != 6)
                return;

            if (!Pattern.matches("[0-9]{6}", text)) return;

            for (int i = 0; i < mDigitsAuthCode.size(); i++)
                requireEditTextById(mRoot, mDigitsAuthCode.get(i))
                        .setText(Character.toString(text.charAt(i)));
        };

        mLifeCycleObserver = new LifecycleObserver() {

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onStop() {
                mOnPause = true;

                FragmentActivity activity = requireActivity();

                if (activity instanceof LockableActivity) {
                    LockableActivity.stopTimer();
                    ((LockableActivity) activity).unlockApp();
                }
            }

        };

        ProcessLifecycleOwner.get().getLifecycle().addObserver(mLifeCycleObserver);
        mRoot.getViewTreeObserver().addOnWindowFocusChangeListener(mWindowFocusListener);

        return mRoot;
    }

    private void configureEditText() {
        for (int id : mDigitsAuthCode) {
            final EditText editor = requireEditTextById(mRoot, id);

            editor.addTextChangedListener((IAfterTextChangedListener) s -> {
                final int lenght = s.length();

                if (lenght > MAX_LENGTH) {
                    editor.setText(s.subSequence(0, MAX_LENGTH));

                    EditText view = (EditText) editor.focusSearch(View.FOCUS_RIGHT);

                    if (view == null) return;

                    view.setText(s.subSequence(MAX_LENGTH, s.length()));
                } else if (lenght == MAX_LENGTH && editor.hasFocus()) {
                    editor.setSelection(editor.getText().length());

                    EditText view = (EditText) editor.focusSearch(View.FOCUS_RIGHT);

                    if (view != null) {
                        view.requestFocus();
                        view.setSelection(view.getText().length());
                    }
                }
            });

            editor.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DEL) {
                    CharSequence text = ((TextView) v).getText();
                    if (!Objects.toString(text).isEmpty()) return false;

                    final int currentIndex = mDigitsAuthCode.indexOf(v.getId());

                    if (currentIndex <= 0) return false;

                    final int idView = mDigitsAuthCode.get(currentIndex - 1);

                    final EditText editText = mRoot.findViewById(idView);

                    editText.requestFocus();
                    editText.setSelection(editText.getText().length());
                }

                return false;
            });
        }
    }


    /**
     * Este método es llamado cuando la actividad es destruída.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        ProcessLifecycleOwner.get().getLifecycle().removeObserver(mLifeCycleObserver);
        mRoot.getViewTreeObserver().removeOnWindowFocusChangeListener(mWindowFocusListener);
    }

    /**
     * Este método es llamado cuando se presiona el botón "Registrar". Valida si el código ingresado
     * ha sido generado con la frase secreta.
     *
     * @param view Vista que llama al método
     */
    private void onSubmit(View view) {
        final StringBuilder builder = new StringBuilder();

        for (int id : mDigitsAuthCode)
            if (Strings.isNullOrEmpty(requireEditTextById(requireView(), id).getText().toString()))
                return;
            else
                builder.append(requireEditTextById(requireView(), id).getText());

        final int authCode = Integer.parseInt(builder.toString());

        if (TwoFactorAuthentication.get(requireContext()).validateAuthCode(authCode)) {
            dismiss();

            if (mSuccededCallback != null)
                mSuccededCallback.onSucceeded();
        } else {
            for (int id : mDigitsAuthCode)
                requireEditTextById(requireView(), id).setText("");

            requireEditTextById(requireView(), R.id.m2FaCodeDigit1).requestFocus();

            Snackbar.make(requireView(), R.string.error_msg_auth_code, Snackbar.LENGTH_SHORT)
                    .setAnchorView(requireView())
                    .show();
        }
    }


    /**
     * Obtiene el cuadro de texto especificado por identificador especificado.
     *
     * @param root Vista raiz del fragmento.
     * @param id   Identificador de la vista.
     * @return Un cuadro de texto.
     */
    private EditText requireEditTextById(View root, int id) {
        View view = root.findViewById(id);
        Objects.requireNonNull(view);

        if (view instanceof EditText)
            return (EditText) view;

        throw new IllegalArgumentException(String.format("EditText not found {id: %d}", id));
    }

    /**
     * Provee de una función para responder si el código de autenticación fue válido.
     */
    public interface IAuthenticationSucceeded {

        /**
         * Este método es llamado cuando la autenticación fue satisfactoria.
         */
        void onSucceeded();
    }

}
