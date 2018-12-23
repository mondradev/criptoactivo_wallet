/*
 *    Copyright 2018 InnSy Tech
 *    Copyright 2018 Ing. Javier de Jesús Flores Mondragón
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.cryptowallet.app;

/**
 * Contiene constantes que representan las llaves de los campos pasados a través de las actividades.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class ExtrasKey {

    /**
     * Moneda seleccionada.
     */
    public static final String SELECTED_COIN = "selected-coin";

    /**
     * Valor de la transacción.
     */
    public static final String TX_ID = "tx-id";

    /**
     * Semilla de la billetera.
     */
    public static final String SEED = "seed";

    /**
     * Especifica que se requiere autenticar.
     */
    static final String REQ_AUTH = "req-auth";

    /**
     * Registro de PIN.
     */
    static final String REG_PIN = "reg-pin";

    /**
     * Información del PIN.
     */
    public static final String PIN_DATA = "pin-data";

    /**
     * Restauración de la billetera.
     */
    static final String RESTORED_WALLET = "restored-wallet";

    /**
     * Operación que realiza la actividad.
     */
    static final String OP_ACTIVITY = "op-activity";

    /**
     * Registro de huella digital.
     */
    static final String REG_FINGER = "reg-finger";
}
