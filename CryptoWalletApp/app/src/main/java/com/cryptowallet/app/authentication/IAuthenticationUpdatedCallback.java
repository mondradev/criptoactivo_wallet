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

import androidx.annotation.NonNull;

/**
 * Define la función de llamada cuando la autenticación fue actualizada. Esta interfaz implementa
 * la función {@link #onAuthenticationUpdated(byte[], byte[])} evitando tener que escribir el resto de
 * las funciones de {@link IAuthenticationCallback}.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public interface IAuthenticationUpdatedCallback extends IAuthenticationCallback {

    /**
     * Este evento surge cuando el intento de autenticación es fallida al ingresar el PIN o lectura
     * de biométricos.
     */
    @Override
    default void onAuthenticationFailed() {
    }

    /**
     * Este evento surge cuando ocurre un error y se completa la operación del autenticador.
     *
     * @param errorCode Un valor entero que identifica el error.
     * @param errString Un mensaje de error que puede ser mostrado en la IU.
     */
    @Override
    default void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
    }

    /**
     * Este evento surge cuando la autenticación es satisfactoria.
     *
     * @param authenticationToken Token de autenticación.
     */
    @Override
    default void onAuthenticationSucceeded(byte[] authenticationToken) {
    }
}
