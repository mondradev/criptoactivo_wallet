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

package com.cryptowallet.utils;

import java.util.concurrent.Executor;

/**
 * Esta clase provee una estructura que permite almacenar un consumidor y un ejecutor. Cuando el
 * método {@link #execute(Object, Object)} es invocado, se realiza a través del ejecutor especificado
 * al inicializar la instancia.
 *
 * @param <T1> Tipo del primer parametro del consumidor.
 * @param <T2> Tipo del segundo parametro del consumidor.
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class ExecutableBiConsumer<T1, T2> {

    /**
     * Consumidor.
     */
    private final BiConsumer<T1, T2> mConsumer;

    /**
     * Ejecutor del consumidor.
     */
    private final Executor mExecutor;

    /**
     * Crea una nueva instancia especificando el consumidor y su ejecutor.
     *
     * @param executor Instancia del ejecutor.
     * @param command  Función consumidora.
     */
    public ExecutableBiConsumer(Executor executor, BiConsumer<T1, T2> command) {
        mExecutor = executor;
        mConsumer = command;
    }

    /**
     * Invoca el consumidor a través del ejecutor.
     *
     * @param value  Valor del primer parametro que recibe el consumidor.
     * @param value2 Valor del segundo parametro que recibe el consumidor.
     */
    public void execute(T1 value, T2 value2) {
        mExecutor.execute(() -> mConsumer.accept(value, value2));
    }

    /**
     * Obtiene el consumidor de la instancia.
     *
     * @return El consumidor.
     */
    public BiConsumer<T1, T2> getConsumer() {
        return mConsumer;
    }

}
