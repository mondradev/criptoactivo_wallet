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

/**
 * Define un {@link Runnable} que puede ser nombrado durante su ejecución.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public abstract class NamedRunnable implements Runnable {

    /**
     * Nombre del ejecutable.
     */
    private final String mName;

    /**
     * Crea una nueva instancia especificando el nombre formateable y sus parametros.
     *
     * @param format Formato del nombre.
     * @param args   Parametros para componer el nombre.
     */
    public NamedRunnable(String format, Object... args) {
        this.mName = String.format(format, args);
    }

    /**
     * Crea una nueva instancia.
     *
     * @param name Nombre del ejecutable.
     */
    public NamedRunnable(String name) {
        this.mName = name;
    }

    /**
     * Establece el nombre del hilo y comienza la ejecución de esto. Finalmente devuelve el nombre
     * que tenía el hilo antes de comenzar la ejecución.
     */
    @Override
    public final void run() {
        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(mName);
        try {
            execute();
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }

    /**
     * Función a ejecutar.
     */
    protected abstract void execute();
}
