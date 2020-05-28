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

package com.cryptowallet.utils.textwatchers;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Es una implementación de {@link TextWatcher} para unicamente utilizar la función
 * {@link #afterTextChanged(Editable)}.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public interface IAfterTextChangedListener extends TextWatcher {

    /**
     * Este método es llamado para notificar que dentro de la secuencia <code>s</code>, el texto fue
     * cambiado.
     * <p></p>
     * Se debe ser cuidadoso al intentar cambiar <code>s</code> ya que podría generar un ciclo
     * infinito de llamadas a esta función.
     *
     * @param s Texto que sufrió el cambio.
     */
    @Override
    void afterTextChanged(Editable s);

    /**
     * Este método es llamado para notificar que en la secuencia <code>s</code>, la cantidad
     * <code>count</code> de caracteres comenzando en <code>start</code> será reemplazado por el
     * nuevo texto con la longitud <code>after</code>. Es un error intentar cambiar la secuencia
     * desde esta función.
     *
     * @param s     Secuencia de caracteres.
     * @param start Posición donde inicia el cambio de caracteres.
     * @param count Cantidad de caracteres.
     * @param after Longitud del nuevo texto.
     */
    @Override
    default void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    /**
     * Este método es llamado para notificar que en la secuencia <code>s</code>, la cantidad
     * <code>count</code> de caracteres comenzando en <code>start</code> reemplazó al texto con
     * una longitud de <code>before</code>. Es un error intentar cambiar la secuencia
     * desde esta función.
     *
     * @param s      Secuencia de caracteres.
     * @param start  Posición donde inicia el cambio de caracteres.
     * @param before Cantidad de caracteres del texto viejo.
     * @param count  Cantidad de caracteres.
     */
    @Override
    default void onTextChanged(CharSequence s, int start, int before, int count) {
    }
}
