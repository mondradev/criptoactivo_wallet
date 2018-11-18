package com.cryptowallet.utils;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Clase que permite la implementación del evento <code>afterTextChanged</code>, sin necesidad de
 * utilizar toda la interfaz de <code>TextWatcher</code>.
 */
public abstract class AfterTextWatcher implements TextWatcher {

    /**
     * Método no implementado.
     */
    @Override
    public final void onTextChanged(
            CharSequence s,
            int start,
            int before,
            int count
    ) {
    }

    /**
     * Método no implementado.
     */
    @Override
    public final void beforeTextChanged(
            CharSequence s,
            int start,
            int count,
            int after
    ) {
    }

    /**
     * Este método es llamado cuando se cambió el texto del EditText.
     * @param s Texto final.
     */
    @Override
    public void afterTextChanged(Editable s) {

    }
}
