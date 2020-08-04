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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.cryptowallet.Constants;
import com.cryptowallet.R;
import com.cryptowallet.app.TxBottomSheetDialogActivity;
import com.cryptowallet.services.WalletSyncForegroundService;
import com.cryptowallet.services.WalletSyncService;
import com.cryptowallet.utils.Consumer;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.callbacks.IOnAuthenticated;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.bitcoinj.crypto.MnemonicCode;
import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Administrador de controladores de billeteras. Permite tener la funcionalidad de cada billetera
 * según sus funciones de la red del criptoactivo. Cada activo deberá tener un controlador de
 * billetera que implemente {@link AbstractWallet} y registrarse en el administrador usando el
 * recurso {@link R.array#supported_wallets}.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.1
 * @see AbstractWallet
 */
public final class WalletProvider {

    /**
     * Etiqueta del log.
     */
    private static final String LOG_TAG = WalletProvider.class.getSimpleName();

    /**
     * Instancia del singletón.
     */
    private static WalletProvider mInstance;

    /**
     * Contexto de la aplicación de android.
     */
    private final Context mContext;

    /**
     * Ejecutor para actividades en un subproceso.
     */
    private ExecutorService mExecutor;

    /**
     * Colección de controladores de billetera.
     */
    private Map<SupportedAssets, AbstractWallet> mWallets;

    /**
     * Comando para procesar las peticiones al servicio.
     */
    private Consumer<Intent> mProcessingRequest = intent -> {
        Thread.currentThread().setName("WalletService ProcessingRequest");

        String assetValue = intent.getStringExtra(Constants.EXTRA_ASSET);
        String networkValue = intent.getStringExtra(Constants.EXTRA_NETWORK);
        String action = intent.getAction();

        if (Strings.isNullOrEmpty(assetValue) || Strings.isNullOrEmpty(networkValue)
                || Strings.isNullOrEmpty(action))
            throw new IllegalArgumentException("Requires asset, network and type data.");

        SupportedAssets asset = SupportedAssets.valueOf(assetValue.toUpperCase());

        AbstractWallet wallet = get(asset);

        if (action.equals(Constants.NEW_TRANSACTION)) {
            String txid = intent.getStringExtra(Constants.EXTRA_TXID);

            if (Strings.isNullOrEmpty(txid))
                throw new IllegalArgumentException("Requires a TxID");

            wallet.requestNewTransaction(txid);
        } else if (action.equals(Constants.NEW_BLOCK)) {
            final int height = intent.getIntExtra(Constants.EXTRA_HEIGHT, -1);
            final int time = intent.getIntExtra(Constants.EXTRA_TIME, 0);
            final String hash = intent.getStringExtra(Constants.EXTRA_HASH);

            String[] txs = intent.getStringArrayExtra(Constants.EXTRA_TXS);

            if (txs == null)
                txs = new String[0];

            if (Strings.isNullOrEmpty(hash))
                throw new IllegalArgumentException("Hash can´t be null or empty");

            wallet.requestNewBlock(height, hash, time, txs);
        }
    };

    /**
     * Consumidor del evento saldo ha cambiado.
     */
    private Consumer<Long> mOnBalanceChangedConsumer;

    /**
     * Consumidor del evento nueva transacción.
     */
    private Consumer<ITransaction> mOnNewTransactionConsumer;

    /**
     * Crea una instancia nueva del proveedor.
     *
     * @param context Contexto de la aplicación.s
     */
    private WalletProvider(Context context) {
        this.mContext = context.getApplicationContext();
        this.mWallets = new HashMap<>();
        this.mExecutor = Executors.newCachedThreadPool();
        this.mOnBalanceChangedConsumer = (ignored) -> notifyChangedBalance();
        this.mOnNewTransactionConsumer = this::notifyNewTransaction;

        final String[] supportedWallets = mContext.getResources()
                .getStringArray(R.array.supported_wallets);

        Utils.tryNotThrow(() -> {
            for (String supportedWallet : supportedWallets) {
                AbstractWallet wallet = (AbstractWallet) Class.forName(supportedWallet)
                        .getConstructor(Context.class).newInstance(mContext);

                registerWallet(wallet);
            }
        });
    }

    /**
     * Obtiene la instancia del proveedor de billeteras.
     *
     * @return Instancia del singletón.
     */
    public static WalletProvider getInstance(Context context) {
        if (mInstance == null)
            mInstance = new WalletProvider(context);

        return mInstance;
    }

    /**
     * Registra un nuevo controlador de billetera.
     *
     * @param wallet Instancia del controlador.
     */
    private void registerWallet(AbstractWallet wallet) {
        if (mWallets.containsKey(wallet.getCryptoAsset()))
            return;

        mWallets.put(wallet.getCryptoAsset(), wallet);

        wallet.addBalanceChangedListener(mExecutor, mOnBalanceChangedConsumer);
        wallet.addNewTransactionListener(mExecutor, mOnNewTransactionConsumer);

        Log.d(LOG_TAG, "Added wallet for " + wallet.getCryptoAsset().name());
    }

    /**
     * Notifica que el saldo de alguna de las billeteras ha cambiado su saldo.
     */
    private synchronized void notifyChangedBalance() {
        final long balance = getFiatBalance();

        Intent intent = new Intent()
                .setAction(Constants.UPDATED_BALANCE)
                .putExtra(Constants.EXTRA_BALANCE, balance);

        mContext.sendBroadcast(intent);
    }

    /**
     * Nofica que el historial de transacciones de alguna billetera ha cambiado.
     *
     * @param newTx Transacción recibida.
     */
    private synchronized void notifyNewTransaction(@NonNull final ITransaction newTx) {
        Objects.requireNonNull(newTx);

        Intent intent = new Intent()
                .setAction(Constants.NEW_TRANSACTION)
                .putExtra(Constants.EXTRA_ASSET, newTx.getCryptoAsset().name())
                .putExtra(Constants.EXTRA_TXID, newTx.getID());

        mContext.sendBroadcast(intent);

        try {
            final String assetName = intent.getStringExtra(Constants.EXTRA_ASSET);
            final String txId = intent.getStringExtra(Constants.EXTRA_TXID);
            final SupportedAssets asset = SupportedAssets.valueOf(assetName);
            final AbstractWallet wallet = get(asset);
            final ITransaction tx = wallet.findTransaction(txId);

            if (tx == null) return;

            Log.d(LOG_TAG, "Received new transaction: " + tx.getID());

            if (tx.isPay()) return;

            if (newTx.getWallet().isInitialDownload())
                return;

            Intent txIntent = TxBottomSheetDialogActivity.createIntent(mContext, tx);
            sendNotification(tx, txIntent);
        } catch (Exception ex) {
            Log.w(LOG_TAG, "Fail to receive a new transaction " + ex.getMessage());
        }
    }

    /**
     * Envía una notificación de recepción de dinero al sistema operativo.
     */
    private synchronized void sendNotification(ITransaction tx, Intent onTap) {
        Log.d(LOG_TAG, "Send a notification to SO: " + tx.getID());

        Objects.requireNonNull(tx);

        final String txId = tx.getID();
        final SupportedAssets asset = tx.getCryptoAsset();
        final String amount = asset.toStringFriendly(tx.getAmount());
        final NumberFormat formatter = NumberFormat.getIntegerInstance();

        if (Strings.isNullOrEmpty(txId))
            throw new IllegalArgumentException("TxID can't be null or empty");

        if (Strings.isNullOrEmpty(amount))
            throw new IllegalArgumentException("Amount can't be null or empty");

        final String message = mContext.getString(R.string.notify_receive, amount);
        final String content = (tx.isConfirm() ? mContext.getString(R.string.notify_receive_completed,
                amount, formatter.format(tx.getBlockHeight()))
                : mContext.getString(R.string.notify_receive_pending, amount))
                + "\n\nID\n" + txId;

        Uri soundUri = Uri.parse(String.format(Locale.getDefault(), "%s://%s/%d",
                ContentResolver.SCHEME_ANDROID_RESOURCE,
                mContext.getPackageName(),
                R.raw.snd_receive_money));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                mContext.getString(R.string.default_notification_channel_id))
                .setSmallIcon(asset.getIcon())
                .setContentTitle(mContext.getString(asset.getName()))
                .setContentText(message)
                .setColor(Utils.resolveColor(mContext, R.attr.colorAccent))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content));

        if (onTap != null) {
            onTap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent
                    = PendingIntent.getActivity(mContext, 0, onTap,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(pendingIntent);
        }

        NotificationManager notificationCompat
                = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationCompat == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    mContext.getString(R.string.default_notification_channel_id),
                    mContext.getString(R.string.receive_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setSound(soundUri, new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build());

            notificationCompat.createNotificationChannel(channel);
        }

        int notifyID = ByteBuffer.wrap(Hex.decode(txId)).getInt();

        notificationCompat.notify(notifyID, builder.build());
    }

    /**
     * Procesa las peticiones del servicio enlazado como {@link Constants#NEW_TRANSACTION}
     * o {@link Constants#NEW_BLOCK}.
     *
     * @param intent Intención a procesar.
     */
    private void processingRequest(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            if (!Strings.isNullOrEmpty(action))
                mExecutor.submit(() -> mProcessingRequest.accept(intent));
        }
    }

    /**
     * Carga la información de cada una de las billeteras previamente inicializadas.
     */
    public void loadWallets() {
        for (AbstractWallet wallet : mWallets.values())
            wallet.loadWallet();
    }

    /**
     *
     */
    public void syncWallets() {
        if (!anyCreated()) return;

        Intent intent = new Intent(mContext, WalletSyncService.class);

        mContext.startService(intent);
    }

    public void syncWalletsForeground() {
        if (!anyCreated()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            mContext.startForegroundService(new Intent(mContext, WalletSyncForegroundService.class));
        else
            mContext.startService(new Intent(mContext, WalletSyncService.class));

    }

    /**
     * Obtiene el total en fiat de las billeteras registradas.
     *
     * @return Total en fiat.
     */
    public synchronized long getFiatBalance() {
        return 0;
    }

    /**
     * Obtiene el total en fiat de la billeteras especificada.
     *
     * @return Total del saldo expresado en fiat.
     */
    public synchronized long getFiatBalance(SupportedAssets cryptoAsset) {
        return 0;
    }

    /**
     * Obtiene el controlador de billetera del activo especificado.
     *
     * @param asset Criptoactivo del controlador.
     * @return El controlador de billetera.
     */
    public synchronized AbstractWallet get(SupportedAssets asset) {
        if (asset.isFiat())
            throw new IllegalArgumentException();

        if (!mWallets.containsKey(asset))
            throw new IllegalArgumentException("The wallet doesn't exists");

        return mWallets.get(asset);
    }

    /**
     * Obtiene la cantidad de billeteras registradas.
     *
     * @return Cantidad de billeteras.
     */
    public synchronized int getCount() {
        return mWallets.size();
    }

    /**
     * Ejecuta una función por cada billetera inicializada.
     *
     * @param consumer Una función de consumo.
     */
    public void forEachWallet(Consumer<AbstractWallet> consumer) {
        Collection<AbstractWallet> wallets = mWallets.values();

        for (AbstractWallet wallet : wallets)
            if (wallet.isInitialized()) consumer.accept(wallet);
    }

    /**
     * Indica si existe al menos una billetera almacenada en el dispositivo.
     *
     * @return Un true si existe.
     */
    public synchronized boolean anyCreated() {
        for (AbstractWallet wallet : mWallets.values())
            if (wallet.exists())
                return true;

        return false;
    }

    /**
     * Obtiene las transacciones de todas las billeteras.
     *
     * @return Lista de transacciones.
     */
    public synchronized List<ITransaction> getTransactions() {
        Collection<AbstractWallet> wallets = mWallets.values();
        List<ITransaction> txs = new ArrayList<>();

        for (AbstractWallet wallet : wallets)
            txs.addAll(wallet.getTransactions());

        return txs;
    }

    /**
     * Ejecuta una función por el activo de cada billetera registrada.
     *
     * @param consumer Una función de consumo.
     */
    public void forEachAsset(Consumer<SupportedAssets> consumer) {
        Collection<AbstractWallet> wallets = mWallets.values();

        for (AbstractWallet wallet : wallets)
            consumer.accept(wallet.getCryptoAsset());
    }

    /**
     * Autentica las billeteras ya inicializadas.
     *
     * @param token           Token de autenticación.
     * @param onAuthenticated Una función consumidora que es lanzada cuando se finaliza la
     *                        autenticación en la billetera. Esta función recibe un parametro
     *                        que indica si la autenticación finalizó correctamente.
     */
    public synchronized void authenticateWallet(byte[] token, IOnAuthenticated onAuthenticated) {
        mExecutor.submit(() -> {
            try {
                for (AbstractWallet wallet : mWallets.values())
                    wallet.authenticateWallet(token);

                onAuthenticated.successful();
            } catch (Exception ex) {
                onAuthenticated.fail(ex);
            }

            syncWallets();
        });
    }

    /**
     * Obtiene la lista de palabras utilizada para la restauración de las billeteras con soporte
     * BIT39.
     *
     * @return Lista de palabras.
     */
    public List<String> getWordsList() {
        String[] wordlist = mContext.getResources()
                .getStringArray(R.array.bip39_wordlist);

        return Lists.newArrayList(wordlist);
    }

    /**
     * Verifica si la combinación de palabras para restaurar la billetera, es válida.
     *
     * @param mnemonicCode Lista de palabras.
     * @return True indica que las palabras son correctas.
     */
    public boolean verifyMnemonicCode(List<String> mnemonicCode) {
        return Utils.tryNotThrow(() -> MnemonicCode.INSTANCE.check(mnemonicCode));
    }

    /**
     * Configura las billeteras para ser restauradas. Se deberá invocar la función
     * {@link #authenticateWallet(byte[], IOnAuthenticated)} para finalizar el proceso.
     *
     * @param assets Activos a restaurar.
     * @param seeds  Semilla usada para la restauración.
     */
    public void restore(List<SupportedAssets> assets, List<String> seeds) {
        for (SupportedAssets asset : assets) {
            AbstractWallet wallet = get(asset);
            wallet.restore(seeds);
        }
    }

    /**
     * Actualiza los escuchas de los precios obtenidos por los seguidores en los exchanges.
     *
     * @param fiatAsset Activo a usar para los precios de los activos.
     */
    public void updateFiatCurrency(SupportedAssets fiatAsset) {
        // TODO Actualizar los escuchas de precio.
    }

    /**
     * Obtiene el último precio del activo especificado.
     *
     * @param cryptoAsset Cripto-activo a obtener el precio.
     * @return Último precio del cripto-activo.
     */
    public long getLastPrice(SupportedAssets cryptoAsset) {
        return 0;
    }

    /**
     * Envía peticiones al servicio para realizar el procesamiento en su hilo. La intención debe
     * tener definida la acción a realizar.
     *
     * @param intent La intención con la información para procesar la petición.
     * @see Constants#NEW_BLOCK
     * @see Constants#NEW_TRANSACTION
     */
    public void sendRequest(@NonNull Intent intent) {
        if (Strings.isNullOrEmpty(intent.getAction()))
            return;

        processingRequest(intent);
    }
}