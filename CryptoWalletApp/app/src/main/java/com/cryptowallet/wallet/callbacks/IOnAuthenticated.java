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

package com.cryptowallet.wallet.callbacks;

/**
 * Representa una función que es invocada cuando se realiza el proceso de autenticación de una
 * billetera. Cuando la autenticación es completada satisfactoriamente, la función
 * {@link #successful()} es invocada. En algún caso de fallo, la función
 * {@link #fail(Exception)} si no ha sido sobre-escrita, una excepción será lanzada.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public interface IOnAuthenticated {

    /**
     * Este método es invocado cuando la billetera se ha autenticado de manera satisfactoria.
     */
    void successful();

    /**
     * Este método es invocado cuando ocurre un error en la autenticación de la billetera con
     * respecto al cifrado y descifrada así como alguna otra configuración interna del proceso de
     * autenticación de billetera. Esto es independiente del proceso de autenticación del usuario,
     * ya que este se realiza a través de {@link com.cryptowallet.app.authentication.Authenticator}.
     *
     * @param ex Excepción ocurrida cuando se estaba realizando la autenticación.
     */
    default void fail(Exception ex) {
        throw new RuntimeException(ex);
    }
}
