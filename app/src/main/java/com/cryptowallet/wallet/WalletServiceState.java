/*
 * Copyright 2019 InnSy Tech
 * Copyright 2019 Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.wallet;

/**
 * Representa los estados de un servicio de la billetera.
 */
public enum WalletServiceState {

    /**
     * Indica que el servicio está iniciando.
     */
    STARTING,
    /**
     * Indica que el servicio está en ejecución.
     */
    RUNNING,
    /**
     * Indica que el servicio está conectado.
     */
    CONNECTED,

    /**
     * Indica que el servicio está detenido.
     */
    STOPPED
}
