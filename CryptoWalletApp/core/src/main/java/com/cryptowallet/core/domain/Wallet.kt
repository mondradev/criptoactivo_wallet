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

import kotlinx.coroutines.flow.Flow

typealias TransactionList = List<Transaction>
typealias AuthenticationToken = ByteArray

interface Wallet {

    val balance: Flow<Coin>

    val recentTransactions: Flow<Transaction>

    val asset: Currency

    suspend fun delete()

    suspend fun exists(): Boolean

    suspend fun authenticate(token: AuthenticationToken)

    suspend fun initialize()

    suspend fun restore(method: RestoreMethod)

    suspend fun sync()

    suspend fun send(address: Address, amount: Coin)

    suspend fun generateAddress(): Address

    suspend fun getTransactions(): TransactionList
}