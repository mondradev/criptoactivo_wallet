/*
 * Copyright 2019 InnSy Tech
 * Copyright 2019 Ing. Javier de Jesús Flores Mondragón
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

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.widgets.GenericTransactionBase;
import com.cryptowallet.wallet.widgets.IAddressBalance;
import com.cryptowallet.wallet.widgets.ICoinFormatter;

import org.spongycastle.util.encoders.Hex;

import java.security.KeyException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;


/**
 * Provee una clase base para la implementación de una billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.2
 */
public abstract class WalletServiceBase extends IntentService {

    /**
     * Conjuto de soporte de activos.
     */
    private static Map<SupportedAssets, WalletServiceBase> mAssets
            = new HashMap<>();

    /**
     * Activo de la billetera.
     */
    private SupportedAssets mAsset;
    /**
     * Estado del servicio.
     */
    private WalletServiceState mState = WalletServiceState.STOPPED;

    /**
     * Crea una nueva instancia de billetera.
     *
     * @param asset El activo de la billetera.
     */
    protected WalletServiceBase(SupportedAssets asset) {
        super("Wallet[Asset=" + asset.name() + "]");
        this.mAsset = asset;
    }

    /**
     * Registra los soporte de activos de la aplicación.
     *
     * @param asset   Activo a registrar.
     * @param service Servicio que lo controla.
     */
    protected static void registerAsset(SupportedAssets asset, WalletServiceBase service) {
        if (mAssets.containsKey(asset))
            return;

        mAssets.put(asset, service);
    }

    /**
     * Obtiene el servicio del activo especificado.
     *
     * @param asset Activo a solicitar el servicio.
     * @return Servicio del activo.
     */
    public static WalletServiceBase get(SupportedAssets asset) {
        return mAssets.get(asset);
    }

    /**
     * Genera un token de pago para validar que la dirección está solicitando un pago desde esta
     * aplicación.
     *
     * @param asset   Activo que está invocando el pago.
     * @param address Dirección que recibe el pago.
     * @return Un token de pago.
     */
    public static String generatePaymentToken(SupportedAssets asset, String address) {
        String tokenSrc = "CryptoWallet[Asset=" + asset.name() + ", Address=" + address + "]";
        byte[] bTokenSrc = tokenSrc.getBytes();
        byte[] bToken = Utils.toSha256(bTokenSrc);

        bToken = Utils.toSha256(bToken);

        Objects.requireNonNull(bToken);

        return Hex.toHexString(bToken);
    }

    /**
     * Obtiene la comisión por hacer un envío a una billetera fuera de la aplicación.
     *
     * @param asset Activo del cual se hace el  envío.
     * @return La comisión por el envío.
     */
    public static long getFeeForSendOutApp(SupportedAssets asset) {
        return get(asset).getFeeForSend();
    }

    /**
     * Obtiene un valor que indica si el servicio del activo especificado está en ejecución.
     *
     * @param asset Activo a verificar el servicio.
     * @return Un valor true si el servicio está en ejecución.
     */
    public static boolean isRunning(SupportedAssets asset) {
        WalletServiceBase service = get(asset);

        if (Utils.isNull(service))
            return false;

        return service.isRunning();
    }

    /**
     * Obtiene la comisión por envío a direcciones que no utilizan la aplicación.
     *
     * @return Comisión por envío.
     */
    public abstract long getFeeForSend();

    /**
     * Provee de un enlace a este servicio para comunicar con los clientes que lo requieren.
     *
     * @param intent Actividad que requiere hacer el enlace.
     * @return Un enlace al servicio.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Crea un gasto especificando la dirección destino y la cantidad a enviar. Posteriormente se
     * propaga a través de la red para esperar ser confirmada.
     *
     * @param address     Dirección destino.
     * @param amount      Cantidad a enviar expresada en su más pequeña porción.
     * @param feePerKb    Comisión por kilobyte utilizado en la transacción.
     * @param outOfTheApp Indica que el pago es fuera de la aplicación.
     * @param requestKey  Solicita la llave de acceso a la billetera en caso de estár cifrada.
     */
    public abstract void sendPayment(String address, long amount, long feePerKb,
                                     boolean outOfTheApp, IRequestKey requestKey)
            throws InSufficientBalanceException, KeyException;

    /**
     * Obtiene el saldo actual expresado en la porción más pequeña del activo.
     *
     * @return El saldo actual.
     */
    public abstract long getBalance();

    /**
     * Obtiene todas las transacciones ordenadas por fecha y hora de manera descendente.
     *
     * @return La lista de transacciones.
     */
    public abstract List<GenericTransactionBase> getTransactionsByTime();

    /**
     * Obtiene la transacciones más recientes.
     *
     * @param count Número de transacciones.
     * @return Lista de transacciones.
     */
    public abstract List<GenericTransactionBase> getRecentTransactions(int count);

    /**
     * Obtiene el listado de direcciones que pueden recibir pagos.
     *
     * @return Lista de direcciones.
     */
    public abstract List<IAddressBalance> getAddresses();

    /**
     * Determina si la dirección es válida en la red del activo.
     *
     * @param address Dirección a validar.
     * @return Un valor true en caso que la dirección sea válida.
     */
    public abstract boolean isValidAddress(String address);

    /**
     * Detiene los servicios de la billetera actual y elimina la información almacenada localmente.¿
     */
    public abstract void deleteWallet();

    /**
     * Obtiene las palabras semilla de la billetera, las cuales permiten restaurarla en caso de
     * perdida.
     *
     * @param requestKey Método que permite obtener las información de autenticación.
     * @return Lista de las palabras de recuperación.
     */
    public abstract List<String> getSeedWords(IRequestKey requestKey);

    /**
     * Encripta la billetera de forma segura especificando la llave utilizada para ello.
     *
     * @param newPinRequest Método que permite obtener el nuevo pin
     * @param pinRequest    Método que permite obtener el pin si la billetera ya está cifrada.
     */
    public abstract void encryptWallet(IRequestKey newPinRequest, IRequestKey pinRequest);

    /**
     * Valida el acceso a la billetera.
     *
     * @param key Llave a validar.
     * @return Un valor true si la llave es válida.
     */
    public abstract boolean validateAccess(byte[] key);

    /**
     * Obtiene la dirección de recepción de la billetera.
     *
     * @return Una dirección para recibir pagos.
     */
    public abstract String getReceiveAddress();

    /**
     * Obtiene el formateador utilizado para visualizar los montos de la transacción.
     *
     * @return Instancia del formateador.
     */
    public abstract ICoinFormatter getFormatter();

    /**
     * Obtiene el activo que maneja la billetera.
     *
     * @return El activo de la billetera.
     */
    public SupportedAssets getAsset() {
        return mAsset;
    }

    /**
     * Busca en la billetera la transacción especificada por el ID.
     *
     * @param id Identificador único de la transacción.
     * @return La transacción que coincide con el ID.
     */
    public abstract GenericTransactionBase findTransaction(String id);

    /**
     * Indica si la billetera no está encriptada.
     *
     * @return Un valor true que indica que la billetera no está cifrada.
     */
    public abstract boolean isUnencrypted();

    /**
     * Desconecta la billetera de la red.
     */
    public abstract void disconnectNetwork();

    /**
     * Conecta la billetera a la red.
     */
    public abstract void connectNetwork();

    /**
     * Obtiene el estado actual del servicio.
     *
     * @return El estado actual.
     */
    public final WalletServiceState getState() {
        return mState;
    }

    /**
     * Establece el estado del servicio.
     *
     * @param state Estado del servicio.
     */
    protected final void setState(WalletServiceState state) {
        mState = state;
    }

    /**
     * Obtiene un valor que indica que el servicio está en ejecución. Esto es un alias a
     * {@code getState() ==  WalletServiceState#RUNNING }
     *
     * @return Un valor true si el estado está en ejecución.
     */
    public final boolean isRunning() {
        return getState() == WalletServiceState.RUNNING
                || getState() == WalletServiceState.CONNECTED;
    }

    /**
     * You should not override this method for your IntentService. Instead,
     * override {@link #onHandleIntent}, which the system calls when the IntentService
     * receives a start request.
     *
     * @see Service#onStartCommand
     */
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }
}
