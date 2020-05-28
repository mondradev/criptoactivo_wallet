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
 * Implementación de las funciones de respuesta del autenticador especificadas por
 * {@link IAuthenticationCallback}.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class AuthenticationCallback implements IAuthenticationCallback {

    /**
     * Este evento surge cuando la autenticación es satisfactoria.
     *
     * @param authenticationToken Token de autenticación.
     */
    @Override
    public void onAuthenticationSucceeded(byte[] authenticationToken) {

    }

    /**
     * Este evento surge cuando el intento de autenticación es fallida al ingresar el PIN o lectura
     * de biométricos.
     */
    @Override
    public void onAuthenticationFailed() {

    }

    /**
     * Este evento surge cuando ocurre un error y se completa la operación del autenticador.
     *
     * @param errorCode Un valor entero que identifica el error.
     * @param errString Un mensaje de error que puede ser mostrado en la IU.
     */
    @Override
    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {

    }

    /**
     * Este evento surge cuando se actualiza el PIN del autenticador.
     *
     * @param oldToken El token de autenticación previo (última vez que se utilizará).
     * @param newToken El token nuevo de la autenticación.
     */
    @Override
    public void onAuthenticationUpdated(byte[] oldToken, byte[] newToken) {

    }
}
