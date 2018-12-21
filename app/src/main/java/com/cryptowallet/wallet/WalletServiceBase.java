package com.cryptowallet.wallet;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.cryptowallet.wallet.widgets.GenericTransactionBase;
import com.cryptowallet.wallet.widgets.IAddressBalance;
import com.cryptowallet.wallet.widgets.ICoinFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;


/**
 * Provee una clase base para la implementación de una billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public abstract class WalletServiceBase extends Service {

    /**
     * Conjuto de soporte de activos.
     */
    private static Map<SupportedAssets, WalletServiceBase> mAssets
            = new HashMap<>();
    /**
     * Indica si la billeta fue inicializada.
     */
    private boolean mInitialized;
    /**
     * Activo de la billetera.
     */
    private SupportedAssets mAsset;

    /**
     * Crea una nueva instancia de billetera.
     *
     * @param asset El activo de la billetera.
     */
    protected WalletServiceBase(SupportedAssets asset) {
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
     * @param address    Dirección destino.
     * @param amount     Cantidad a enviar expresada en su más pequeña porción.
     * @param feePerKb   Comisión por kilobyte utilizado en la transacción.
     * @param requestKey Solicita la llave de acceso a la billetera en caso de estár cifrada.
     */
    public abstract void sendPayment(String address, long amount, long feePerKb,
                                     IRequestKey requestKey) throws InSufficientBalanceException;

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
     * @param requestCallback Un método que solicita la clave de validación.
     * @return Lista de las palabras de recuperación.
     */
    public abstract List<String> getSeedWords(IRequestKey requestCallback);

    /**
     * Encripta la billetera de forma segura especificando la llave utilizada para ello.
     *
     * @param key     Llave de encriptación.
     * @param prevKey Llave anterior en caso de estár encriptada.
     * @return Un valor true en caso de que se encripte correctamente.
     */
    public abstract boolean encryptWallet(byte[] key, byte[] prevKey);

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
     * Obtiene un valor que indica si la billetera fue inicializada.
     *
     * @return Si la billetera se inicializó.
     */
    public boolean isInitialized() {
        return mInitialized;
    }

    /**
     * Establece un valor que indica si la billetera fue inicializada.
     *
     * @param initialized Un valor true para indicar que la billetera fue inicializada.
     */
    protected void setInitialized(boolean initialized) {
        this.mInitialized = initialized;
    }

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
     * Indica si la billetera está encriptada.
     *
     * @return Un valor true que indica que la billetera está cifrada.
     */
    public abstract boolean isEncrypted();


    /**
     * Desconecta la billetera de la red.
     */
    public abstract void disconnectNetwork();

    /**
     * Conecta la billetera a la red.
     */
    public abstract void connectNetwork();
}
