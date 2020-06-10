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

package com.cryptowallet.wallet;

import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.cryptowallet.services.coinmarket.PriceTracker;
import com.cryptowallet.utils.Consumer;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Define la estructura básica de una billetera de criptoactivos.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 * @see WalletManager
 * @see SupportedAssets
 */
public interface IWallet {

    /**
     * Elimina la billetera existente.
     *
     * @return Un true si la billetera fue borrada.
     */
    boolean delete();

    /**
     * Determina si ya existe una billetera de criptoactivo almacenada en el dispositivo.
     *
     * @return Un true si existe.
     */
    boolean exists();

    /**
     * Verifica si las palabras ingresadas como semilla para la creación de una billetera son
     * validas.
     *
     * @param seed Palabras semillas.
     * @return Un true en caso de ser validas.
     */
    boolean verifySeed(String seed);

    /**
     * Inicializa la instancia de la billetera con el token de autenticación de la misma.
     *
     * @param authenticationToken Token de autenticación.
     * @param onInitialized       Una función de vuelta donde el valor boolean indica si hay error.
     */
    void initialize(byte[] authenticationToken, Consumer<Boolean> onInitialized);

    /**
     * Restaura la billetera a partir de las palabras semilla.
     *
     * @param seed Palabras semilla.
     */
    void restore(String seed);

    /**
     * Obtiene la lista de las palabras válidas para usarse como semilla.
     *
     * @return Lista de las palabras.
     */
    List<String> getWordsList();

    /**
     * Obtiene el criptoactivo soportado.
     *
     * @return Criptoactivo de la billetera.
     */
    SupportedAssets getAsset();

    /**
     * Agrega un escucha de cambio de precio del activo.
     *
     * @param listener Escucha de precio.
     */
    void addPriceChangeListener(Executor executor, Consumer<Double> listener);

    /**
     * Agrega un escucha de cambio de saldo.
     *
     * @param listener Escucha de saldo.
     */
    void addBalanceChangeListener(Executor executor, Consumer<Double> listener);

    /**
     * Remueve el escucha de cambio de saldo.
     *
     * @param listener Escucha a remover.
     */
    void removeBalanceChangeListener(Consumer<Double> listener);

    /**
     * Remueve el escucha de cambio de precio.
     *
     * @param listener Escucha a remover.
     */
    void removePriceChangeListener(Consumer<Double> listener);

    /**
     * Obtiene el total del saldo de la billetera.
     *
     * @return Saldo de la billetera.
     */
    double getBalance();

    /**
     * Obtiene el total del saldo en su precio fiat.
     *
     * @return Saldo de la billetera.
     */
    double getFiatBalance();

    /**
     * Registra un nuevo seguidor de precio.
     *
     * @param tracker Seguidor de precio.
     * @param asset   Activo en el que se representa el precio.
     */
    void registerPriceTracker(PriceTracker tracker, SupportedAssets asset);


    /**
     * Remueve el registro de un seguidor de precio.
     *
     * @param asset Activo en el que se representa el precio.
     */
    void unregisterPriceTracker(SupportedAssets asset);

    /**
     * Actualiza los escuchas del precio.
     */
    void updatePriceListeners();

    /**
     * Obtiene el identificador del dibujable utilizado como icono.
     *
     * @return Recurso del dibujable del icono.
     */
    @DrawableRes
    int getIcon();

    /**
     * Obtiene la información de recepción.
     *
     * @return Información de recepción.
     */
    Uri getReceiverUri();

    /**
     * Dirección de recepción de la billetera.
     *
     * @return Dirección de recepción.
     */
    String getReceiverAddress();

    /**
     * Actualiza la clave de seguridad la billetera.
     *
     * @param currentToken Clave actual de la billetera.
     * @param newToken     Nueva clave de la billetera.
     */
    void updatePassword(byte[] currentToken, byte[] newToken);

    /**
     * Obtiene las palabras semilla de la billetera.
     *
     * @param authenticationToken Token de autenticación de la billetera.
     * @return Una lista de palabras.
     */
    List<String> getSeeds(byte[] authenticationToken);

    /**
     * Determina si la dirección especificada es válida.
     *
     * @param address Una dirección de envío.
     * @return Un true si la dirección es correcta.
     */
    boolean validateAddress(String address);

    /**
     * Obtiene el último precio del criptoactivo.
     *
     * @return Último precio.
     */
    double getLastPrice();

    /**
     * Crea una transacción nueva para realizar un pago.
     *
     * @param address Dirección del pago.
     * @param amount  Cantidad a enviar.
     * @param feeByKB Comisión por KB.
     * @return Una transacción nueva.
     */
    ITransaction createTx(String address, double amount, double feeByKB);

    /**
     * Obtiene las comisiones de la red para realizar los envío de transacciones.
     *
     * @return Comisión de la red.
     */
    ITransactionFee getFees();

    /**
     * Determina si la cantidad especificada es considerada polvo. El polvo es una cantidad pequeña
     * utilizada para realizar la trazabilidad de una transacción.
     *
     * @param amount Cantidad a evaluar.
     * @return True si la cantidad es considerada polvo.
     */
    boolean isDust(double amount);

    /**
     * Obtiene la cantidad máxima del activo.
     *
     * @return Cantidad máxima.
     */
    double getMaxValue();

    /**
     * Obtiene las transacciones de la billetera.
     *
     * @return Lista de transacciones.
     */
    List<ITransaction> getTransactions();

    /**
     * Obtiene la dirección desde la uri especificada.
     *
     * @param data Uri que contiene los datos.
     * @return Una dirección válida para realizar envíos.
     */
    String getAddressFromUri(Uri data);

    /**
     * Busca la transacción especificada por el hash.
     *
     * @param hash Identificador único de la transacción.
     * @return Una transacción o null en caso de no encontrarla.
     */
    @Nullable
    ITransaction findTransaction(String hash);
}
