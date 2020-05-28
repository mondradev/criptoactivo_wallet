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
 * método {@link #execute(Object)} es invocado, se realiza a través del ejecutor especificado al
 * inicializar la instancia.
 *
 * @param <T> Tipo del parametro del consumidor.
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class ExecutableConsumer<T> {

    /**
     * Consumidor.
     */
    private final Consumer<T> mConsumer;

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
    public ExecutableConsumer(Executor executor, Consumer<T> command) {
        mExecutor = executor;
        mConsumer = command;
    }

    /**
     * Invoca el consumidor a través del ejecutor.
     *
     * @param value Valor que recibe el consumidor.
     */
    public void execute(T value) {
        mExecutor.execute(() -> mConsumer.accept(value));
    }

    /**
     * Obtiene el consumidor de la instancia.
     *
     * @return El consumidor.
     */
    public Consumer<T> getConsumer() {
        return mConsumer;
    }

}
