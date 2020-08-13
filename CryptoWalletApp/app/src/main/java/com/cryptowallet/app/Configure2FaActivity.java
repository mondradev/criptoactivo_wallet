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

package com.cryptowallet.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.cryptowallet.R;
import com.cryptowallet.app.authentication.TwoFactorAuthentication;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.utils.textwatchers.IAfterTextChangedListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Esta actividad permite configurar la autenticación de dos factores en la aplicación. La
 * autenticación de dos factores será solicitada para realizar envíos, respaldar o eliminar la
 * billetera.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 * @see TwoFactorAuthentication
 * @see Preferences
 */
public class Configure2FaActivity extends LockableActivity {

    /**
     * Tamaño del código QR.
     */
    private static final int QR_CODE_SIZE = 250;

    /**
     * Identificadores de los digitos del código de autenticación.
     */
    private final List<Integer> mDigitsAuthCode = Arrays.asList(
            R.id.m2FaCodeDigit1, R.id.m2FaCodeDigit2, R.id.m2FaCodeDigit3,
            R.id.m2FaCodeDigit4, R.id.m2FaCodeDigit5, R.id.m2FaCodeDigit6
    );

    /**
     * Frase secreta.
     */
    private String mSecretPhrase;

    /**
     * Observador del ciclo de vida de la aplicación.
     */
    private LifecycleObserver mLifeCycleObserver;

    /**
     * Indica que la aplicación está a la espera del código de autenticación.
     */
    private boolean mOnPause;

    /**
     * Este método es llamado cuando la actividad es creada.
     *
     * @param savedInstanceState Datos de estado de la actividad.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure_2fa);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.configure_2fa_title);

        mSecretPhrase = TwoFactorAuthentication.get(this.getApplicationContext())
                .generateSecret();

        findViewById(R.id.m2FaRegisterButton).setOnClickListener(this::onRegisterPhrase);
        findViewById(R.id.m2FaCopyButton).setOnClickListener(this::onCopyPhrase);

        ((TextView) findViewById(R.id.m2FaSecretPhrase)).setText(mSecretPhrase);
        ((ImageView) findViewById(R.id.m2FaQrCode)).setImageBitmap(Utils.getQrCode(
                String.format("otpauth://totp/CryptoWallet?secret=%s", mSecretPhrase),
                QR_CODE_SIZE
        ));

        // TODO Delete with keyboard the code

        for (int id : mDigitsAuthCode) {
            final EditText editor = requireEditTextById(id);

            editor.addTextChangedListener((IAfterTextChangedListener) s -> {
                for (InputFilter filter : editor.getFilters())
                    if (filter instanceof InputFilter.LengthFilter)
                        if (s.length() == ((InputFilter.LengthFilter) filter).getMax()) {
                            View view = editor.focusSearch(View.FOCUS_RIGHT);

                            if (view != null)
                                view.requestFocus();
                        }
            });

            editor.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DEL) {
                    CharSequence text = ((TextView) v).getText();
                    if (!Objects.toString(text).isEmpty()) return false;

                    final int currentIndex = mDigitsAuthCode.indexOf(v.getId());

                    if (currentIndex <= 0) return false;

                    final int idView = mDigitsAuthCode.get(currentIndex - 1);

                    final EditText editText = this.findViewById(idView);

                    editText.requestFocus();
                    editText.setSelection(editText.getText().length());
                }

                return false;
            });
        }

        mLifeCycleObserver = new LifecycleObserver() {

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onStop() {
                mOnPause = true;
                stopTimer();
                unlockApp();
            }
        };

        ProcessLifecycleOwner.get().getLifecycle().addObserver(mLifeCycleObserver);
    }

    /**
     * @param hasFocus
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!mOnPause) return;

        mOnPause = false;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) return;

        ClipData clipData = clipboard.getPrimaryClip();

        if (clipData == null) return;
        if (clipData.getItemCount() == 0) return;

        CharSequence text = clipData.getItemAt(0)
                .coerceToText(getApplicationContext());

        if (text.length() != 6)
            return;

        if (!Pattern.matches("[0-9]{6}", text)) return;

        for (int i = 0; i < mDigitsAuthCode.size(); i++)
            requireEditTextById(mDigitsAuthCode.get(i))
                    .setText(Character.toString(text.charAt(i)));

        requireEditTextById(R.id.m2FaCodeDigit6).setSelection(1);
    }

    /**
     * Este método es llamado cuando la actividad es destruída.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        ProcessLifecycleOwner.get().getLifecycle().removeObserver(mLifeCycleObserver);
    }

    /**
     * Se llama a este método cada vez que se selecciona un elemento en su menú de opciones.
     *
     * @param item El elemento del menú que fue seleccionado.
     * @return boolean Un valor true si fue procesado.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        setResult(RESULT_CANCELED);
        finish();

        return true;
    }

    /**
     * Este método es llamado cuando se presiona el botón "Copiar". Copia la frase secreta al
     * portapapeles.
     *
     * @param view Vista que llama al método.
     */
    private void onCopyPhrase(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        Objects.requireNonNull(clipboard);

        ClipData data = ClipData.newPlainText(
                getString(R.string.secret_phrase_text), mSecretPhrase);
        clipboard.setPrimaryClip(data);

        Snackbar.make(
                (View) findViewById(R.id.m2FaRegisterButton).getParent(),
                R.string.phrase_copy_to_clipboard_text,
                Snackbar.LENGTH_SHORT
        ).show();

    }

    /**
     * Este método es llamado cuando se presiona el botón "Registrar". Valida si el código ingresado
     * ha sido generado con la frase secreta.
     *
     * @param view Vista que llama al método
     */
    private void onRegisterPhrase(View view) {
        final StringBuilder builder = new StringBuilder();

        for (int id : mDigitsAuthCode)
            if (Strings.isNullOrEmpty(requireEditTextById(id).getText().toString()))
                return;
            else
                builder.append(requireEditTextById(id).getText());

        final int authCode = Integer.parseInt(builder.toString());

        if (TwoFactorAuthentication.get(this).trySave(mSecretPhrase, authCode)) {
            setResult(RESULT_OK);
            finish();
        } else {
            for (int id : mDigitsAuthCode)
                requireEditTextById(id).setText("");

            requireEditTextById(R.id.m2FaCodeDigit1).requestFocus();

            Snackbar.make((View) findViewById(R.id.m2FaRegisterButton).getParent(),
                    R.string.error_msg_auth_code,
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Obtiene el cuadro de texto especificado por identificador especificado.
     *
     * @param id Identificador de la vista.
     * @return Un cuadro de texto.
     */
    private EditText requireEditTextById(int id) {
        View view = findViewById(id);
        Objects.requireNonNull(view);

        if (view instanceof EditText)
            return (EditText) view;

        throw new IllegalArgumentException(String.format("EditText not found {id: %d}", id));
    }

}
