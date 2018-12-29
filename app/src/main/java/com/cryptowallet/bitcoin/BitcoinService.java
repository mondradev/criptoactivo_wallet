/*
 * Copyright 2018 InnSy Tech
 * Copyright 2018 Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.bitcoin;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.cryptowallet.R;
import com.cryptowallet.app.AppPreference;
import com.cryptowallet.app.ExtrasKey;
import com.cryptowallet.app.TransactionActivity;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.utils.WifiManager;
import com.cryptowallet.wallet.BlockchainStatus;
import com.cryptowallet.wallet.IRequestKey;
import com.cryptowallet.wallet.IWalletListener;
import com.cryptowallet.wallet.InSufficientBalanceException;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletServiceBase;
import com.cryptowallet.wallet.coinmarket.ExchangeService;
import com.cryptowallet.wallet.widgets.GenericTransactionBase;
import com.cryptowallet.wallet.widgets.IAddressBalance;
import com.cryptowallet.wallet.widgets.ICoinFormatter;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

/**
 * Ofrece un servicio de billetera de Bitcoin permitiendo recibir y enviar pagos, consultar saldo y
 * ver transacciones pasadas.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public final class BitcoinService extends WalletServiceBase {

    /**
     * Valor del BTC en satoshis.
     */
    public static final long BTC_IN_SATOSHIS = 100000000;

    /**
     * Parametro de red.
     */
    public static final NetworkParameters NETWORK_PARAMS = TestNet3Params.get();

    /**
     * Direcciónes de comisión a las billetera. Los primeros 8 bytes corresponden al valor de la
     * comisión, mientras que lo restante corresponde a la dirección pública.
     */
    static final List<byte[]> FEE_DATA = new ArrayList<>();

    /**
     * Lista de escucha de eventos de la billetera.
     */
    private final static CopyOnWriteArrayList<IWalletListener> mListeners
            = new CopyOnWriteArrayList<>();

    /**
     * Extensión de los archivos utilizados en la billetera de Bitcoin.
     */
    private final static String FILE_EXT = ".btc";

    /**
     * Etiqueta de la clase
     */
    private static final String TAG = "Wallet[Asset=BTC]";


    /**
     * Indica si el servicio de la billetera se encuentra en ejecución.
     */
    private static boolean mRunning = false;

    static {
        FEE_DATA.add(Hex
                .decode("00000000000053fc6ff022a844844d252781139cf40113760e6361688a322a2999"));
        FEE_DATA.add(Hex
                .decode("00000000000053fc6fb59398057bba9163a297d18f001b3c0c9a35b680efd97238"));
    }

    /**
     * Billetera de bitcoin.
     */
    private Wallet mWallet;

    /**
     * Grupo de puntos remotos.
     */
    private PeerGroup mPeerGroup;

    /**
     * Directorio de la aplicación.
     */
    private String mDirectory;

    /**
     * Almacén de bloques.
     */
    private BlockStore mStore;

    /**
     * Cadena de bloques.
     */
    private BlockChain mChain;

    /**
     * Contexto de Bitcoin.
     */
    private Context mContext;

    /**
     * Crea una nueva instancia de billetera.
     */
    public BitcoinService() {
        super(SupportedAssets.BTC);

        registerAsset(getAsset(), this);
    }

    /**
     * Obtiene la instancia del servicio de la billetera de Bitcoin.
     *
     * @return Servicio de la billetera.
     */
    public static BitcoinService get() {
        BitcoinService instance
                = (BitcoinService) WalletServiceBase.get(SupportedAssets.BTC);

        Context.propagate(instance.getContext());

        return instance;
    }

    /**
     * Notifica a los escuchas que la transacción ha sido confirmada hasta 7.
     *
     * @param transaction Transacción confirmada.
     */
    static void notifyOnCommited(final BitcoinTransaction transaction) {
        for (final IWalletListener listener : mListeners)
            listener.onCommited(BitcoinService.get(), transaction);
    }

    /**
     * Obtiene un valor que indica si el servicio de la billetera está en ejecución.
     *
     * @return Un valor true cuando la billetera está en ejecución.
     */
    public static boolean isRunning() {
        return mRunning;
    }

    /**
     * Agrega un escucha de los eventos de la billetera.
     *
     * @param listener Escucha que se desea añadir.
     */
    public static void addEventListener(IWalletListener listener) {
        if (mListeners.contains(listener))
            return;

        mListeners.add(listener);

        notifyOnReady(listener);
    }

    /**
     * Remueve un escucha de los eventos de la billetera.
     *
     * @param listener Escucha que se desea remover.
     */
    public static void removeEventListener(IWalletListener listener) {
        if (!mListeners.contains(listener))
            return;

        mListeners.remove(listener);
    }

    /**
     * Notifica al escucha que la billetera fue inicializada.
     *
     * @param listener Escucha a notificar.
     */
    private static void notifyOnReady(IWalletListener listener) {
        if (mRunning)
            listener.onReady(BitcoinService.get());
    }

    /**
     * Notifica a los escuchas de eventos que ocurrió un error en la billetera.
     *
     * @param e Excepción producida que se notificará.
     */
    private static void notifyOnException(final Exception e) {
        for (IWalletListener listener : mListeners)
            listener.onException(BitcoinService.get(), e);
    }

    /**
     * Notifica a los escuchas de eventos que una transacción ha sido propagada en la red.
     *
     * @param transaction Transacción propagada.
     */
    private static void notifyOnPropagated(final Transaction transaction) {
        for (IWalletListener listener : mListeners)
            listener.onPropagated(BitcoinService.get(),
                    new BitcoinTransaction(transaction, BitcoinService.get().mWallet));
    }

    /**
     * Notifica a los escuchas que la billetera fue inicializada.
     */
    private static void notifyOnReady() {
        for (IWalletListener listener : mListeners)
            listener.onReady(BitcoinService.get());
    }

    /**
     * Notifica a los escuchas que la descarga ha finalizado.
     */
    private static void notifyOnCompletedDownload() {
        for (IWalletListener listener : mListeners)
            listener.onCompletedDownloaded(BitcoinService.get());
    }

    /**
     * Notifica a los escuchas que la descarga ha comenzado.
     *
     * @param blocks Total de bloques a descargar.
     */
    private static void notifyOnStartDownload(final int blocks) {
        for (IWalletListener listener : mListeners)
            listener.onStartDownload(BitcoinService.get(),
                    BlockchainStatus.build(blocks, null));
    }

    /**
     * Notifica a los escuchas el progreso de la descarga.
     *
     * @param blocksSoFar Bloques restantes a descargar.
     * @param date        Fecha del último bloque descargado.
     */
    private static void notifyOnDownloaded(final int blocksSoFar, final Date date) {
        for (IWalletListener listener : mListeners)
            listener.onBlocksDownloaded(BitcoinService.get(),
                    BlockchainStatus.build(blocksSoFar, date));
    }

    /**
     * Notifica a los escuchas que la billetera ha recibido pagos.
     *
     * @param transaction Transacción que envía pagos a esta billetera.
     */
    private static void notifyOnReceived(final BitcoinTransaction transaction) {
        for (IWalletListener listener : mListeners) {
            listener.onReceived(BitcoinService.get(), transaction);
            listener.onBalanceChanged(BitcoinService.get(),
                    BitcoinService.get().getBalance());
        }
    }

    /**
     * Notifica a los escuchas que la billetera ha enviado pagos.
     *
     * @param transaction Transacción que envía pagos fuera de esta billetera.
     */
    private static void notifyOnSent(final BitcoinTransaction transaction) {
        for (IWalletListener listener : mListeners) {
            listener.onSent(BitcoinService.get(), transaction);
            listener.onBalanceChanged(BitcoinService.get(),
                    BitcoinService.get().getBalance());
        }
    }

    /**
     * Notifica a los escucha que el servicio ha conectado a la red de Bitcoin.
     */
    private static void nofityOnConnected() {
        for (final IWalletListener listener : mListeners)
            listener.onConnected(BitcoinService.get());
    }

    /**
     * Notifica a los escucha que la billetera ha cambiado.
     */
    public static void notifyOnBalanceChange() {
        for (final IWalletListener listener : mListeners)
            listener.onBalanceChanged(BitcoinService.get(),
                    BitcoinService.get().getBalance());
    }

    /**
     * Obtiene el contexto de la librería BitcoinJ.
     *
     * @return Contexto de la aplicación.
     */
    private Context getContext() {
        if (mContext == null)
            mContext = new Context(NETWORK_PARAMS);

        return mContext;
    }

    /**
     * Este método es llamado cuando el sistema inicia el servicio por primera vez.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        final Handler handler = new Handler();

        Threading.USER_THREAD = new Executor() {
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };

        mDirectory = getApplicationContext().getApplicationInfo().dataDir;

        Objects.requireNonNull(mDirectory);

        WifiManager.init(this);
        WifiManager.addEventListener(new WifiManager.IListener() {
            @Override
            public void onConnect() {
                if (!BitcoinService.isRunning())
                    return;

                if (AppPreference.getUseOnlyWifi(BitcoinService.get()))
                    BitcoinService.get().connectNetwork();
            }

            @Override
            public void onDisconnect() {
                if (!BitcoinService.isRunning())
                    return;

                if (AppPreference.getUseOnlyWifi(BitcoinService.get()))
                    BitcoinService.get().disconnectNetwork();
            }
        });

        Log.d(TAG, "Creación del servicio correctamente.");
    }

    /**
     * Configura los escuchas de eventos.
     *
     * @param wallet Billetera a configurar.
     */
    private void configureListeners(final Wallet wallet) {
        Log.d(TAG, "Configurando escuchas de la billetera de Bitcoin.");

        wallet.addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {

            /**
             * Este método es llamado cuando se recibe una transacción que envía un pago a esta
             * billetera.
             *
             * @param wallet      Billetera que recibe la transacción.
             * @param tx          Transacción que envía un pago a esta billetera.
             * @param prevBalance Saldo previo a la recepción.
             * @param newBalance  Saldo estimado después de recibir la transacción.
             */
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance,
                                        Coin newBalance) {
                saveWallet();
                notifyReceived(tx);
                notifyOnReceived(new BitcoinTransaction(tx, wallet));
            }
        });

        wallet.addCoinsSentEventListener(new WalletCoinsSentEventListener() {

            /**
             * Este método es llamado cuando se recibe una transacción que envía un pago fuera de
             * esta billetera.
             * @param wallet Billetera que recibe la transacción.
             * @param tx Transacción que envía un pago fuera de esta billetera.
             * @param prevBalance Saldo previo a la recepción.
             * @param newBalance Saldo estimado después de recibir la transacción.
             */
            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance,
                                    Coin newBalance) {
                saveWallet();
                notifyOnSent(new BitcoinTransaction(tx, wallet));
            }
        });
    }

    /**
     * Notifica al sistema operativo que ocurrió una recepción.
     *
     * @param tx Transacción recibida.
     */
    private void notifyReceived(Transaction tx) {

        if (new BitcoinTransaction(tx, mWallet).getAmount() < 0) return;

        Intent intent = new Intent(this, TransactionActivity.class);
        intent.putExtra(ExtrasKey.TX_ID, tx.getHashAsString());
        intent.putExtra(ExtrasKey.SELECTED_COIN, SupportedAssets.BTC.name());

        Utils.sendReceiveMoneyNotification(getApplicationContext(), R.mipmap.img_bitcoin,
                tx.getValueSentToMe(mWallet).toFriendlyString(), tx.getHashAsString(),
                intent);
    }

    /**
     * El sistema llama a este método cuando el servicio ya no se utiliza y se lo está destruyendo.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        mListeners.clear();

        mPeerGroup.removeWallet(mWallet);
        mPeerGroup.stop();

        mRunning = false;
        mStore = null;
        mWallet = null;
        mPeerGroup = null;
        mDirectory = null;

        Log.d(TAG, "Servicio fue destruido correctamente.");
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call {@link #stopSelf}.
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(Intent)}.
     *               This may be null if the service is being restarted after
     *               its process has gone away; see
     *               {@link Service#onStartCommand}
     *               for details.
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if (!ExchangeService.isInitialized())
            ExchangeService.init(this);

        DeterministicSeed seed = null;

        if (intent != null && intent.hasExtra(ExtrasKey.SEED)) {
            List<String> words = intent.getStringArrayListExtra(ExtrasKey.SEED);
            seed = new DeterministicSeed(words, null, "", 0);
        }

        byte[] key = null;

        if (intent != null && intent.hasExtra(ExtrasKey.PIN_DATA))
            key = intent.getByteArrayExtra(ExtrasKey.PIN_DATA);

        final byte[] finalKey = key;
        final DeterministicSeed finalSeed = seed;

        init(finalSeed, finalKey);
    }

    /**
     * Inicia la billetera y comienza la sincronización.
     */
    private synchronized void init(final DeterministicSeed seed, final byte[] key) {
        if (mRunning) {
            if (!mPeerGroup.isRunning())
                mPeerGroup.startAsync();

            Log.v(TAG, "El servicio ha sido iniciado con anterioridad.");
            return;
        }

        Context.propagate(getContext());

        try {
            Log.d(TAG, "Inicializando la billetera de Bitcoin.");

            String walletFilename = String.format("wallet%s", FILE_EXT);
            File walletFile = new File(mDirectory, walletFilename);

            if (walletFile.exists())
                mWallet = Wallet.loadFromFile(walletFile);
            else if (seed != null) {
                Log.i(TAG, "Restaurando la billetera.");

                mWallet = Wallet.fromSeed(NETWORK_PARAMS, seed);
            } else {
                mWallet = new Wallet(NETWORK_PARAMS);
                mWallet.freshReceiveKey();
                saveWallet();

                mWallet = Wallet.loadFromFile(walletFile);
            }

            configureListeners(mWallet);

            if (key != null) {
                Log.d(TAG, "Cifrando la billetera de Bitcoin.");

                int iterations =
                        Utils.calculateIterations(Hex.toHexString(key));
                KeyCrypterScrypt scrypt = new KeyCrypterScrypt(iterations);
                mWallet.encrypt(scrypt, scrypt.deriveKey(Hex.toHexString(key)));
                saveWallet();
            }

            Log.d(TAG, "Inicializando la bockchain de Bitcoin.");

            String blockStoreFilename = String.format("blockchain%s", FILE_EXT);
            File blockStoreFile = new File(mDirectory, blockStoreFilename);

            mStore = new SPVBlockStore(NETWORK_PARAMS, blockStoreFile);

            mChain = new BlockChain(NETWORK_PARAMS, mStore);

            Log.d(TAG, "Inicializando el grupo de puntos de Bitcoin.");

            mPeerGroup = new PeerGroup(NETWORK_PARAMS, mChain);

            mChain.addWallet(mWallet);
            mPeerGroup.addWallet(mWallet);

            mRunning = true;

            notifyOnReady();

            if (AppPreference.getUseOnlyWifi(this))
                if (!WifiManager.hasInternet(this)) {
                    mPeerGroup.removeWallet(mWallet);
                    mPeerGroup = null;
                } else
                    preparePeerGroup();
            else
                preparePeerGroup();

            Log.d(TAG, "Servicio iniciado correctamente.");

            wait();

        } catch (InterruptedException | UnreadableWalletException | BlockStoreException ex) {
            Log.d(TAG, "Servicio finalizado forzadamente.");

            notifyOnException(ex);
            stopSelf();
        }
    }

    /**
     * Prepara todas las configuraciones del grupo de puntos remotos.
     */
    private void preparePeerGroup() {
        final int MAX_PEERS = 10;

        Log.d(TAG, "Configurando el grupo de puntos remotos de Bitcoin.");

        mPeerGroup.addPeerDiscovery(new DnsDiscovery(NETWORK_PARAMS));
        mPeerGroup.setUserAgent("CryptoWallet.BTC",
                AppPreference.getVesion(getApplicationContext()).toString());
        mPeerGroup.setStallThreshold(10, 20 * 1024);
        mPeerGroup.setMaxConnections(MAX_PEERS);
        mPeerGroup.startBlockChainDownload(new DownloadProgressTracker() {

            /**
             * Este método es llamado cuando el progreso avanza.
             *
             * @param peer El punto que está descargando la cadena.
             * @param block El bloque descargado.
             * @param filteredBlock Filtro de bloques.
             * @param blocksLeft Cantidad de bloques restantes.
             */
            @Override
            public void onBlocksDownloaded(Peer peer, Block block,
                                           @Nullable FilteredBlock filteredBlock, int blocksLeft) {
                super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft);
                notifyOnDownloaded(blocksLeft, block.getTime());
            }

            /**
             * Este método es llamado cuando inicia la descarga.
             *
             * @param peer       El punto que está descargando la cadena.
             * @param blocksLeft El número estimado a descargar.
             */
            @Override
            public void onChainDownloadStarted(Peer peer, int blocksLeft) {
                super.onChainDownloadStarted(peer, blocksLeft);
                notifyOnStartDownload(blocksLeft);
            }

            /**
             * Este método es llamado cuando la descarga avanza 1%.
             *
             * @param pct         El porcentaje estimado de la descarga.
             * @param blocksSoFar Cantidad de bloques restantes.
             * @param date        Fecha del último bloque descargado.
             */
            @Override
            protected void progress(double pct, int blocksSoFar, Date date) {
                super.progress(pct, blocksSoFar, date);
                saveWallet();
            }

            /**
             * Este método es llamado cuando la descarga finaliza.
             */
            @Override
            protected void doneDownload() {
                super.doneDownload();
                saveWallet();
                notifyOnCompletedDownload();
            }
        });

        Futures.addCallback(mPeerGroup.startAsync(), new FutureCallback() {
            @Override
            public void onSuccess(@Nullable Object result) {
                Log.d(TAG, "El grupo de puntos remotos se ha inicilizado.");
                nofityOnConnected();
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                notifyOnException(new IllegalStateException(t));
                stopSelf();
            }
        });
    }

    /**
     * Obtiene la comisión por envío a direcciones que no utilizan la aplicación.
     *
     * @return Comisión por envío.
     */
    @Override
    public long getFeeForSend() {
        long fees = 0;

        for (byte[] target : BitcoinService.FEE_DATA) {
            Coin amount = Coin.valueOf(ByteBuffer.wrap(target, 0, 8).getLong());
            fees += amount.value;
        }

        return fees;
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
    @Override
    public void sendPayment(String address, long amount, long feePerKb, boolean outOfTheApp,
                            IRequestKey requestKey)
            throws InSufficientBalanceException, KeyException {

        Utils.throwIfNullOrEmpty(address,
                "La dirección no puede ser una cadena nula o vacía.");
        Preconditions.checkArgument(amount > 0,
                "La cantidad debe ser mayor a 0");

        try {
            Transaction txSend = new Transaction(NETWORK_PARAMS);
            txSend.addOutput(Coin.valueOf(amount), Address.fromBase58(NETWORK_PARAMS, address));

            if (outOfTheApp) {
                for (byte[] feeTarget : FEE_DATA) {
                    Coin fee = Coin.valueOf(ByteBuffer.wrap(feeTarget, 0, 8).getLong());
                    Address feeAdress = Address.fromBase58(NETWORK_PARAMS, Base58
                            .encode(Arrays.copyOfRange(feeTarget, 8, feeTarget.length)));

                    txSend.addOutput(fee, feeAdress);
                }

                Log.d(TAG, "Enviando pago fuera de la aplicación.");
            }

            SendRequest request = SendRequest.forTx(txSend);
            request.feePerKb = Coin.valueOf(feePerKb);

            if (mWallet.isEncrypted() && mWallet.getKeyCrypter() != null && requestKey != null) {
                request.aesKey = mWallet.getKeyCrypter()
                        .deriveKey(Hex.toHexString(requestKey.onRequest()));
                if (request.aesKey == null)
                    throw new KeyException("No se proporcionó la llave");
            }

            mWallet.completeTx(request);
            mWallet.commitTx(request.tx);


            final ListenableFuture<Transaction> future = mPeerGroup
                    .broadcastTransaction(txSend).future();

            Futures.addCallback(future, new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(@Nullable Transaction result) {
                    BitcoinService.notifyOnPropagated(result);
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    BitcoinService.notifyOnException(new ExecutionException(t));
                }
            });

        } catch (InsufficientMoneyException ex) {
            throw new InSufficientBalanceException(
                    mWallet.getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE).getValue(),
                    SupportedAssets.BTC, Objects.requireNonNull(ex.missing).getValue(), ex);
        }
    }

    /**
     * Obtiene el saldo actual expresado en la porción más pequeña del activo.
     *
     * @return El saldo actual.
     */
    @Override
    public long getBalance() {
        return mWallet.getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE).getValue();
    }

    /**
     * Obtiene todas las transacciones ordenadas por fecha y hora de manera descendente.
     *
     * @return La lista de transacciones.
     */
    @Override
    public List<GenericTransactionBase> getTransactionsByTime() {
        List<Transaction> transactions = mWallet.getTransactionsByTime();
        List<GenericTransactionBase> btcTransaction = new ArrayList<>();

        for (Transaction tx : transactions)
            btcTransaction.add(new BitcoinTransaction(tx, mWallet));

        return btcTransaction;
    }

    /**
     * Obtiene la transacciones más recientes.
     *
     * @param count Número de transacciones.
     * @return Lista de transacciones.
     */
    @Override
    public List<GenericTransactionBase> getRecentTransactions(int count) {
        List<Transaction> transactions = mWallet.getRecentTransactions(count, false);
        List<GenericTransactionBase> btcTransaction = new ArrayList<>();

        for (Transaction tx : transactions)
            btcTransaction.add(new BitcoinTransaction(tx, mWallet));

        return btcTransaction;
    }

    /**
     * Obtiene el listado de direcciones que pueden recibir pagos.
     *
     * @return Lista de direcciones.
     */
    @Override
    public List<IAddressBalance> getAddresses() {
        List<BitcoinAddress> addresses = BitcoinAddress
                .getAll(mWallet.getTransactionsByTime(), mWallet);

        return new ArrayList<IAddressBalance>(addresses);
    }

    /**
     * Determina si la dirección es válida en la red del activo.
     *
     * @param address Dirección a validar.
     * @return Un valor true en caso que la dirección sea válida.
     */
    @Override
    public boolean isValidAddress(String address) {
        try {
            Address.fromBase58(NETWORK_PARAMS, address);
        } catch (AddressFormatException ignored) {
            return false;
        }

        return true;
    }

    /**
     * Detiene los servicios de la billetera actual y elimina la información almacenada localmente.
     */
    @Override
    public void deleteWallet() {
        stopSelf();

        String walletFileName = String.format("wallet%s", FILE_EXT);
        String blockStoreFileName = String.format("blockchain%s", FILE_EXT);

        File walletFile = new File(mDirectory, walletFileName);
        File blockStoreFile = new File(mDirectory, blockStoreFileName);

        if (walletFile.exists() && walletFile.delete())
            Log.v(TAG, "Billetera eliminada.");

        if (blockStoreFile.exists() && blockStoreFile.delete())
            Log.v(TAG, "Blockchain eliminada.");
    }

    /**
     * Obtiene las palabras semilla de la billetera, las cuales permiten restaurarla en caso de
     * perdida.
     *
     * @param requestKey Método que permite obtener las información de autenticación.
     * @return Lista de las palabras de recuperación.
     */
    @Override
    public List<String> getSeedWords(IRequestKey requestKey) {
        if (mWallet.getKeyChainSeed().isEncrypted()) {

            byte[] authData = requestKey.onRequest();

            if (Utils.isNull(authData))
                return null;

            KeyCrypter keyCrypter = mWallet.getKeyCrypter();

            Objects.requireNonNull(keyCrypter);

            DeterministicSeed deterministicSeed = mWallet.getKeyChainSeed().decrypt(
                    keyCrypter, "", keyCrypter.deriveKey(Hex.toHexString(authData)));

            return deterministicSeed.getMnemonicCode();
        }

        return mWallet.getKeyChainSeed().getMnemonicCode();
    }

    /**
     * Encripta la billetera de forma segura especificando la llave utilizada para ello.
     *
     * @param newPinRequest Método que permite obtener el nuevo pin
     * @param pinRequest    Método que permite obtener el pin si la billetera ya está cifrada.
     */
    @Override
    public void encryptWallet(IRequestKey newPinRequest, IRequestKey pinRequest) {
        Objects.requireNonNull(newPinRequest);

        boolean connected = mPeerGroup != null && mPeerGroup.isRunning();

        String hash;

        if (mWallet.isEncrypted()) {
            Objects.requireNonNull(pinRequest);

            byte[] data = pinRequest.onRequest();

            if (Utils.isNull(data))
                return;

            String prevHash = Hex.toHexString(data);


            byte[] newData = newPinRequest.onRequest();

            if (Utils.isNull(newData))
                return;

            hash = Hex.toHexString(newData);

            if (connected)
                disconnectNetwork();

            mWallet.decrypt(prevHash);
        } else {
            byte[] newData = newPinRequest.onRequest();

            if (Utils.isNull(newData))
                return;

            hash = Hex.toHexString(newData);

            if (connected)
                disconnectNetwork();
        }

        int iterations = Utils.calculateIterations(hash);

        try {
            KeyCrypter keyCrypter = new KeyCrypterScrypt(iterations);
            mWallet.encrypt(keyCrypter, keyCrypter.deriveKey(hash));
            saveWallet();

            if (connected)
                connectNetwork();

        } catch (KeyCrypterException ignored) {
        }
    }

    /**
     * Valida el acceso a la billetera.
     *
     * @param key Llave a validar.
     * @return Un valor true si la llave es válida.
     */
    @Override
    public boolean validateAccess(byte[] key) {
        if (!mWallet.isEncrypted())
            throw new IllegalStateException("La billetera no está encriptada.");

        Objects.requireNonNull(key);
        Objects.requireNonNull(mWallet.getKeyCrypter());

        return mWallet.checkAESKey(mWallet.getKeyCrypter().deriveKey(Hex.toHexString(key)));
    }

    /**
     * Obtiene la dirección de recepción de la billetera.
     *
     * @return Una dirección para recibir pagos.
     */
    @Override
    public String getReceiveAddress() {
        return mWallet.currentReceiveAddress().toBase58();
    }

    /**
     * Obtiene el formateador utilizado para visualizar los montos de la transacción.
     *
     * @return Instancia del formateador.
     */
    @Override
    public ICoinFormatter getFormatter() {
        return new ICoinFormatter() {
            @Override
            public String format(long value) {
                return Coin.valueOf(value).toFriendlyString();
            }
        };
    }

    /**
     * Busca en la billetera la transacción especificada por el ID.
     *
     * @param id Identificador único de la transacción.
     * @return La transacción que coincide con el ID.
     */
    @Override
    public GenericTransactionBase findTransaction(String id) {
        Transaction transaction = mWallet.getTransaction(Sha256Hash.wrap(id));

        if (transaction == null)
            return null;

        return new BitcoinTransaction(transaction, mWallet);
    }

    /**
     * Indica si la billetera está encriptada.
     *
     * @return Un valor true que indica que la billetera está cifrada.
     */
    @Override
    public boolean isUnencrypted() {
        return !mWallet.isEncrypted();
    }

    /**
     * Desconecta la billetera de la red.
     */
    @Override
    public void disconnectNetwork() {
        if (mPeerGroup == null)
            return;

        Log.d(TAG, "Desconectando el servicio de la red.");

        mPeerGroup.removeWallet(mWallet);
        mPeerGroup.setMaxConnections(0);
        mPeerGroup.stop();

        mPeerGroup = null;
    }

    /**
     * Guarda la información de la billetera en un archivo cifrado.
     */
    private void saveWallet() {
        synchronized (this) {
            try {
                String walletFileName = String.format("wallet%s", FILE_EXT);
                File walletFile = new File(mDirectory, walletFileName);
                mWallet.saveToFile(walletFile);

                Log.v(TAG, "La billetera se guardó correctamente.");
            } catch (IOException ex) {
                notifyOnException(ex);
            }
        }
    }

    /**
     * Conecta la billetera a la red.
     */
    @Override
    public void connectNetwork() {
        if (mPeerGroup != null)
            return;

        Log.v(TAG, "Conectando el servicio a la red.");

        mPeerGroup = new PeerGroup(NETWORK_PARAMS, mChain);
        mPeerGroup.addWallet(mWallet);

        preparePeerGroup();
    }
}
