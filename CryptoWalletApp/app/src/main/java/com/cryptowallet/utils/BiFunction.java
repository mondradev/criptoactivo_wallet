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
 * Una estructura que representa una función que acepta dos parametros y devuelve un resultado.
 *
 * @param <T1> Tipo del primer parametro que acepta el consumidor.
 * @param <T2> Tipo del segundo parametro que acepta el consumidor.
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public interface BiFunction<T1, T2, R> {

    /**
     * Invoca la función.
     *
     * @param first  Valor del primer parametro.
     * @param second Valor del segundo parametro.
     */
    R accept(T1 first, T2 second);
}
