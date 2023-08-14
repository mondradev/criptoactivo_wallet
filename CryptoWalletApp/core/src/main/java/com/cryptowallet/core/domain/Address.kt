/*
 * Copyright &copy; 2023. Criptoactivo
 * Copyright &copy; 2023. InnSy Tech
 * Copyright &copy; 2023. Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.core.domain


typealias AddressFormatter = (ByteArray) -> String

data class Address(val data: ByteArray) {
    override fun equals(other: Any?) = when {
        this === other -> true

        other !is Address -> false

        else -> data.contentEquals(other.data)
    }

    override fun hashCode() = data.contentHashCode()

    fun toString(formatter: AddressFormatter) = formatter(data)

}