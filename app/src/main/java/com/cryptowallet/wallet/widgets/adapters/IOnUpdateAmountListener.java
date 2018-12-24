/*
 * Copyright 2018 InnSy Tech
 * Copyright 2018 Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.wallet.widgets.adapters;

/**
 * Provee de un método que es llamado cuando el precio cambia de activo.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public interface IOnUpdateAmountListener {

    /**
     * Este método se desencadena cuando activo del precio es reemplazado.
     */
    void onUpdate();
}
