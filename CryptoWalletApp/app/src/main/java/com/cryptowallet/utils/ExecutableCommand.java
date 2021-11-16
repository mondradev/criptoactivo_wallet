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
 * Esta clase provee una estructura que permite almacenar un comando y un ejecutor. Cuando el
 * método {@link #execute()} es invocado, se realiza a través del ejecutor especificado al
 * inicializar la instancia.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class ExecutableCommand {

    /**
     * Ejecutable.
     */
    private final Runnable mCommand;

    /**
     * Ejecutor del comando.
     */
    private final Executor mExecutor;

    /**
     * Crea una nueva instancia especificando el comando y su ejecutor.
     *
     * @param executor Instancia del ejecutor.
     * @param command  Función a ejecutar.
     */
    public ExecutableCommand(Executor executor, Runnable command) {
        mExecutor = executor;
        mCommand = command;
    }

    /**
     * Invoca el comando a través del ejecutor.
     */
    public void execute() {
        mExecutor.execute(mCommand);
    }

    /**
     * Obtiene el comando de la instancia.
     *
     * @return El comando.
     */
    public Runnable getRunnable() {
        return mCommand;
    }

}
