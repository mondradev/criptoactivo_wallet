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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cryptowallet.BuildConfig;
import com.cryptowallet.services.coinmarket.PriceTracker;
import com.cryptowallet.utils.Consumer;
import com.cryptowallet.utils.ExecutableCommand;
import com.cryptowallet.utils.ExecutableConsumer;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.exceptions.InvalidAmountException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

/**
 * Define la estructura básica de una billetera de un cripto-activo. Cuando se define una billetera
 * que extienda de esta misma clase, se deberá generar un constructor que reciba únicamente el
 * argumento {@link Context}.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 3.0
 * @see WalletProvider
 * @see SupportedAssets
 */
public abstract class AbstractWallet {

    /**
     * Clave del identificador de la billetera.
     */
    private static final String WALLET_ID
            = String.format("%s.keys.WALLET_ID", BuildConfig.APPLICATION_ID);

    /**
     * Cripto-activo de la billetera.
     */
    private final SupportedAssets mCryptoAsset;

    /**
     * Archivo de la billetera.
     */
    private final File mWalletFile;

    /**
     * Preferencias de aplicación.
     */
    private final SharedPreferences mPreference;

    /**
     * Identificador de la billetera.
     */
    private byte[] mWalletId;

    /**
     * Indica si la billetera fue inicializada.
     */
    private boolean mInitialized;

    /**
     * Conjunto de escuchas para cuando el saldo ha cambiado.
     */
    private CopyOnWriteArraySet<ExecutableConsumer<AbstractWallet>> mBalanceChangedListeners;

    /**
     * Conjunto de escuchas para cuando se agrega una nueva transacción a la billetera.
     */
    private CopyOnWriteArraySet<ExecutableConsumer<ITransaction>> mNewTransactionListeners;

    /**
     * Conjunto de escuchas para cuando se finaliza la sincronización.
     */
    private CopyOnWriteArraySet<ExecutableCommand> mFullSyncListener;

    /**
     * Seguidores de precio.
     */
    private Map<SupportedAssets, PriceTracker> mPriceTrackers;

    /**
     * Crea una instancia de la billetera.
     *
     * @param cryptoAsset    Tipo de activo de la billetera.
     * @param context        Contexto de la aplicación android.
     * @param walletFilename Nombre del archivo de la billetera.
     */
    public AbstractWallet(SupportedAssets cryptoAsset, Context context, String walletFilename) {
        Context mContext = context.getApplicationContext();

        mCryptoAsset = cryptoAsset;
        mWalletFile = new File(mContext.getApplicationInfo().dataDir, walletFilename);
        mPriceTrackers = new HashMap<>();
        mFullSyncListener = new CopyOnWriteArraySet<>();
        mNewTransactionListeners = new CopyOnWriteArraySet<>();
        mBalanceChangedListeners = new CopyOnWriteArraySet<>();
        mPreference = mContext.getSharedPreferences(
                String.format("%s.PREFERENCE", this.getClass().getName()), Context.MODE_PRIVATE);
        mWalletId = mPreference.contains(WALLET_ID)
                ? Hex.decodeStrict(mPreference.getString(WALLET_ID, ""))
                : new byte[32];
    }

    /**
     * Obtiene la instancia del archivo de la billetera.
     *
     * @return Archivo de la billetera.
     */
    protected File getWalletFile() {
        return mWalletFile;
    }

    /**
     * Obtiene el identificador único de la billetera. Si la billetera no ha sido autenticada durante
     * el ciclo de vida del hilo de ejecución, es posible que no se haya generado el walletId.
     *
     * @return Hash identificador.
     */
    public byte[] getWalletId() {
        return mWalletId;
    }

    /**
     * Genera el hash de la billetera.
     *
     * @param data Información utilizada para generar el identificador de la billetera.
     */
    protected void generateWalletId(byte[] data) {
        this.mWalletId = Utils.sha256(data);
        mPreference.edit()
                .putString(WALLET_ID, Hex.toHexString(mWalletId))
                .apply();
    }

    /**
     * Notifica a los escuchas que el saldo de la billetera ha cambiado.
     */
    protected void notifyBalanceChanged() {
        for (ExecutableConsumer<AbstractWallet> executable : mBalanceChangedListeners)
            executable.execute(this);
    }

    /**
     * Notifica a los escuchas que una nueva transacción fue agregada a la billetera.
     *
     * @param tx Transacción agregada.
     */
    protected void notifyNewTransaction(ITransaction tx) {
        for (ExecutableConsumer<ITransaction> executable : mNewTransactionListeners)
            executable.execute(tx);
    }

    /**
     * Notifica a los escuchas que la billetera a finalizado la descarga de los datos desde el
     * servidor.
     */
    protected void notifyFullSync() {
        for (ExecutableCommand executable : mFullSyncListener)
            executable.execute();
    }

    /**
     * Elimina la billetera existente.
     *
     * @return Un true si la billetera fue borrada.
     */
    public boolean delete() {
        boolean deleted = true;

        if (exists())
            deleted = mWalletFile.delete();

        mPreference.edit().remove(WALLET_ID).apply();
        mWalletId = new byte[32];

        return deleted;
    }

    /**
     * Determina si ya existe una billetera de criptoactivo almacenada en el dispositivo.
     *
     * @return Un true si existe.
     */
    public boolean exists() {
        return mWalletFile.exists();
    }

    /**
     * Autentica la identidad del propietario de la billetera, y se carga los datos sencibles.
     *
     * @param authenticationToken Token de autenticación.
     * @throws Exception Si no se lográ iniciar la billetera, deberá lanzarse una excepción para
     *                   notificar que esta función falló.
     */
    public abstract void authenticateWallet(byte[] authenticationToken) throws Exception;

    /**
     * Carga la información de la billetera si ya fue creada.
     */
    public abstract void loadWallet();

    /**
     * Restaura la billetera a partir del listado de palabras utilizadas para generar la semilla.
     * Es necesario que en esta función se configure la instancia para poder generar la billetera
     * con el cifrado en base al token de autenticación que será proveido por a través de la función
     * {@link #authenticateWallet(byte[])}.
     *
     * @param seed Palabras usadas como semilla de la billetera.
     */
    public abstract void restore(List<String> seed);

    /**
     * Obtiene el criptoactivo soportado.
     *
     * @return Criptoactivo de la billetera.
     */
    public final SupportedAssets getCryptoAsset() {
        return mCryptoAsset;
    }

    /**
     * Agrega un escucha del evento de sincronización completa. Este evento es lanzado cuando la
     * billetera a descargado la información relevante para si misma desde el servidor remoto.
     *
     * @param executor Ejecutor del escucha del evento.
     * @param listener Función a llamar cuando el evento sea generado.
     */
    public synchronized void addFullSyncListener(Executor executor, Runnable listener) {
        for (ExecutableCommand executable : mFullSyncListener)
            if (executable.getRunnable().equals(listener))
                return;

        mFullSyncListener.add(new ExecutableCommand(executor, listener));
    }

    /**
     * Remueve un escucha del evento de sincronización completa.
     *
     * @param listener Función a llamar cuando el evento sea generado.
     */
    public void removeFullSyncListener(Runnable listener) {
        for (ExecutableCommand executable : mFullSyncListener)
            if (executable.getRunnable().equals(listener))
                mFullSyncListener.remove(executable);
    }

    /**
     * Agrega un escucha del evento de saldo cambiado. Este evento es lanzado cuando el saldo es
     * cambiado por agregar o restar alguna cantidad al mismo.
     *
     * @param executor Ejecutor del escucha del evento.
     * @param listener Función a llamar cuando el evento sea generado.
     */
    public void addBalanceChangedListener(Executor executor, Consumer<AbstractWallet> listener) {
        for (ExecutableConsumer<AbstractWallet> executable : mBalanceChangedListeners)
            if (executable.getConsumer().equals(listener))
                return;

        mBalanceChangedListeners.add(new ExecutableConsumer<>(executor, listener));
    }

    /**
     * Remueve un escucha del evento de saldo cambiado.
     *
     * @param listener Función a llamar cuando el evento sea generado.
     */
    public void removeBalanceChangedListener(Consumer<AbstractWallet> listener) {
        for (ExecutableConsumer<AbstractWallet> executable : mBalanceChangedListeners)
            if (executable.getConsumer().equals(listener))
                mBalanceChangedListeners.remove(executable);
    }

    /**
     * Agrega un escucha del evento de nueva transacción. Este evento es lanzado cuando la billetera
     * recibe una nueva transacción sin importar si envia, recibe o transafiere saldo.
     *
     * @param executor Ejecutor del escucha del evento.
     * @param listener Función a llmaar cuando el evento sea generado.
     */
    public void addNewTransactionListener(Executor executor, Consumer<ITransaction> listener) {
        for (ExecutableConsumer<ITransaction> executable : mNewTransactionListeners)
            if (executable.getConsumer().equals(listener))
                return;

        mNewTransactionListeners.add(new ExecutableConsumer<>(executor, listener));
    }

    /**
     * Remueve el escucha del evento de nueva transacción.
     *
     * @param listener Función a llamar cuando el evento sea generado.
     */
    public void removeNewTransactionListener(Consumer<ITransaction> listener) {
        for (ExecutableConsumer<ITransaction> executable : mNewTransactionListeners)
            if (executable.getConsumer().equals(listener))
                mNewTransactionListeners.remove(executable);
    }


    /**
     * Obtiene el seguidor del precio según el activo utilizado para visualizarlo.
     *
     * @param fiat Activo fiduciario.
     * @return Seguidor de precio.
     * @throws IllegalArgumentException En caso de no existir un seguidor de precio.
     */
    public PriceTracker getPriceTracker(SupportedAssets fiat) {
        if (!mPriceTrackers.containsKey(fiat))
            throw new IllegalArgumentException("Fiat asset unsupported: " + fiat);

        return mPriceTrackers.get(fiat);
    }

    /**
     * Registra un seguidor de precio.
     *
     * @param fiat         Activo fiat en el cual se expresa el precio.
     * @param priceTracker Seguidor de precio.
     */
    protected void registerPriceTracker(SupportedAssets fiat, @NonNull PriceTracker priceTracker) {
        if (mPriceTrackers.containsKey(fiat)) return;

        mPriceTrackers.put(fiat, priceTracker);
    }

    /**
     * Obtiene el total del saldo de la billetera. El saldo es expresado en su unidad mínima, lo cual
     * es determinado por {@link SupportedAssets}.
     *
     * @return Saldo de la billetera.
     */
    public abstract long getBalance();

    /**
     * Genera la uri utilizada para solicitar pagos a esta billetera.
     *
     * @return Uri de pagos.
     */
    public abstract Uri generateUri();

    /**
     * Descarga las transacciones relevantes para esta billetera.
     */
    public abstract void syncWallet();

    /**
     * Obtiene la dirección pública que actualmente es utilizada para recibir pagos.
     *
     * @return Dirección pública de recepción de pagos.
     */
    public abstract String getCurrentPublicAddress();

    /**
     * Actualiza la clave de seguridad la billetera. Esta función es invocada en un hilo diferente al
     * principal.
     *
     * @param currentToken Clave actual de la billetera.
     * @param newToken     Nueva clave de la billetera.
     */
    public abstract void updatePassword(byte[] currentToken, byte[] newToken);

    /**
     * Obtiene la lista de las palabras semilla de la billetera. Utiliza esta función para generar
     * un respaldo de la semilla de la billetera.
     *
     * @param authenticationToken Token de autenticación de la billetera.
     * @return Una lista de palabras.
     */
    public abstract List<String> getCurrentSeed(byte[] authenticationToken);

    /**
     * Determina si la dirección especificada es válida.
     *
     * @param address Una dirección de envío.
     * @return Un true si la dirección es correcta.
     */
    public abstract boolean isValidAddress(String address);

    /**
     * Crea una transacción nueva para realizar un pago.
     *
     * @param address Dirección del pago.
     * @param amount  Cantidad a enviar.
     * @param feeByKB Comisión por KB.
     * @return Una transacción nueva.
     */
    public abstract ITransaction createTx(String address, long amount, long feeByKB);

    /**
     * Obtiene las comisiones de la red para realizar los envío de transacciones. Este método realiza
     * la petición al servidor y calcula las comisiones actuales de la red, para mejor rendimiento
     * la petición deberá ser cacheable.
     *
     * @return Comisiones de la red para hacer envíos a velocidad común o rápida.
     */
    public abstract IFees getCurrentFees();

    /**
     * Determina si la cantidad especificada cumple con las caracteristicas de mínimo y máximo
     * permito por la reglas de concenso de la red.
     *
     * @param amount         Cantidad a validar.
     * @param throwIfInvalid Indica si el método deberá lanzar la excepción si la cantidad no es
     *                       válida.
     * @return True si es válida la cantidad especificada.
     * @throws InvalidAmountException Si throwIfInvalid es true, la causa de la validación
     *                                fallida es lanzada como excepción.
     */
    @CanIgnoreReturnValue
    public abstract boolean isValidAmount(long amount, boolean throwIfInvalid)
            throws InvalidAmountException;

    /**
     * Obtiene las transacciones de la billetera.
     *
     * @return Lista de transacciones.
     */
    public abstract List<ITransaction> getTransactions();

    /**
     * Obtiene la dirección que será utilizada para enviar un pago a partir de una uri especificada.
     *
     * @param data Uri que contiene los datos.
     * @return Una dirección válida para realizar envíos de pagos.
     */
    public abstract String getAddressFromUri(Uri data);

    /**
     * Busca la transacción especificada por el hash.
     *
     * @param hash Identificador único de la transacción.
     * @return Una transacción o null en caso de no encontrarla.
     */
    @Nullable
    public abstract ITransaction findTransaction(String hash);

    /**
     * Firma una transacción utilizando las llaves privadas y la propaga por la red a través de la
     * conexión al servidor.
     *
     * @param tx                  Transacción a envíar.
     * @param authenticationToken Token de autenticación para firmar la transacción.
     * @return True si se logró enviar la transacción.
     */
    public abstract boolean sendTx(ITransaction tx, byte[] authenticationToken);

    /**
     * Indica si la billetera ha sido inicializada.
     *
     * @return True si la billetera fue inicializada.
     */
    public boolean isInitialized() {
        return mInitialized;
    }

    /**
     * Establece si la billetera ha sido inicializada.
     *
     * @param initialized Un valor true indica que fue inicializada.
     */
    protected void setInitialized(boolean initialized) {
        mInitialized = initialized;
    }

    /**
     * Este método es invocado cuando el token de notificaciones push (FCM) es actualizado. En este
     * método se deberá registrar el token en el servidor.
     *
     * @param token Token nuevo.
     */
    public abstract void onUpdatePushToken(String token);

    /**
     * Solicita la transacción especificada, si esta transacción no es relavante para la billetera,
     * será descartada.
     *
     * @param txid Identificador de la transacción.
     */
    public abstract void requestNewTransaction(String txid);

    /**
     * Solicita las transacciones relevantes que fueron incluidas en el bloque. Si se logran agregar
     * correctamente las transacciones, se establecerá la billetera que el bloque especificado fue
     * el último observado.
     *
     * @param height        Altura de la cadena.
     * @param hash          Hash del bloque en la punta de la cadena.
     * @param timeInSeconds Tiempo en segundo del bloque en la punta de la cadena.
     * @param txs           Identificadores de las transacciones.
     */
    public abstract void requestNewBlock(int height, String hash, long timeInSeconds, String[] txs);

    /**
     * Obtiene el identificador del recurso utilizado para mostrar el logo del cripto-activo.
     *
     * @return Identificador de recurso android.
     */
    @DrawableRes
    public abstract int getIcon();

    /**
     * Indica si es la descarga inicial de transacciones.
     *
     * @return True si es la primera descarga de la blockchain.
     */
    public abstract boolean isInitialDownload();
}
