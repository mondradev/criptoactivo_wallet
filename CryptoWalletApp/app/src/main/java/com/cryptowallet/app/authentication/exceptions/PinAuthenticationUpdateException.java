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

package com.cryptowallet.app.authentication.exceptions;

/**
 * Esta clase representa una excepción al momento de actualizar el PIN de autenticación.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class PinAuthenticationUpdateException extends RuntimeException {

    /**
     * Crea una nueva instancia especificando un mensaje de la excepción.
     *
     * @param message Mensaje de la excepción.
     */
    public PinAuthenticationUpdateException(String message) {
        super(message);
    }

    /**
     * Crea una nueva instancia especificando un mensaje y la causa de la excepción.
     *
     * @param message Mensaje de la excepción.
     * @param cause   Causa de la excepción.
     */
    public PinAuthenticationUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
