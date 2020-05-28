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

import android.os.CountDownTimer;


/**
 * Ejecuta una actividad después de un tiempo en el futuro, el cual puedes detener antes o reiniciar
 * en caso de requerirlo.
 * <p>
 * Ejemplo para mostrar un mensaje después de 30 segundos.
 *
 * <pre class="prettyprint">
 * new Timeout(30000) {
 *
 *     public void onFinish() {
 *         mTextField.setText("done!");
 *     }
 *  }.start();
 * </pre>
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public abstract class Timeout extends CountDownTimer {

    /**
     * Crea un nuevo temporizador.
     *
     * @param millisInFuture El número de milisegundos a esperar para realizar la ejecución
     *                       de {@link #onFinish()}.
     */
    public Timeout(long millisInFuture) {
        super(millisInFuture, millisInFuture);
    }

    /**
     * Callback fired on regular interval.
     *
     * @param millisUntilFinished The amount of time until finished.
     */
    @Override
    public void onTick(long millisUntilFinished) {
        // Nothing do
    }

    /**
     * Llamada cuando el tiempo es agotado.
     */
    @Override
    public abstract void onFinish();

    /**
     * Reinicia el temporizador.
     */
    public void restart() {
        this.cancel();
        this.start();
    }
}
