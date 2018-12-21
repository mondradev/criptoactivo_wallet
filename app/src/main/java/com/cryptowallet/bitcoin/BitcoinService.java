package com.cryptowallet.bitcoin;

import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.BlockchainStatus;
import com.cryptowallet.wallet.IRequestKey;
import com.cryptowallet.wallet.IWalletListener;
import com.cryptowallet.wallet.InSufficientBalanceException;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletServiceBase;
import com.cryptowallet.wallet.widgets.GenericTransactionBase;
import com.cryptowallet.wallet.widgets.IAddressBalance;
import com.cryptowallet.wallet.widgets.ICoinFormatter;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
    public static final NetworkParameters NETWORK_PARAM = TestNet3Params.get();

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
     * Indica si el servicio de la billetera se encuentra en ejecución.
     */
    private static boolean mRunning = false;

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
     * Crea una nueva instancia de billetera.
     *
     * @param asset El activo de la billetera.
     */
    protected BitcoinService(SupportedAssets asset) {
        super(asset);

        registerAsset(getAsset(), this);
    }

    /**
     * Obtiene la instancia del servicio de la billetera de Bitcoin.
     *
     * @return Servicio de la billetera.
     */
    public static BitcoinService get() {
        return (BitcoinService) WalletServiceBase.get(SupportedAssets.BTC);
    }

    /**
     * Notifica a los escuchas que la transacción ha sido confirmada hasta 7.
     *
     * @param transaction Transacción confirmada.
     */
    public static void notifyOnCommited(final BitcoinTransaction transaction) {
        for (final IWalletListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onCommited(BitcoinService.get(), transaction);
                }
            });
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
        for (final IWalletListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onException(BitcoinService.get(), e);
                }
            });
    }

    /**
     * Notifica a los escuchas de eventos que una transacción ha sido propagada en la red.
     *
     * @param transaction Transacción propagada.
     */
    private static void notifyOnPropagated(final Transaction transaction) {
        for (final IWalletListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onPropagated(BitcoinService.get(),
                            new BitcoinTransaction(transaction, BitcoinService.get().mWallet));
                }
            });
    }

    /**
     * Notifica a los escuchas que la billetera fue inicializada.
     */
    private static void notifyOnReady() {
        for (final IWalletListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onReady(BitcoinService.get());
                }
            });
    }

    /**
     * Notifica a los escuchas que la descarga ha finalizado.
     */
    private static void notifyOnCompletedDownload() {
        for (final IWalletListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onCompletedDownloaded(BitcoinService.get());
                }
            });
    }

    /**
     * Notifica a los escuchas que la descarga ha comenzado.
     *
     * @param blocks Total de bloques a descargar.
     */
    private static void notifyOnStartDownload(final int blocks) {
        for (final IWalletListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onStartDownload(BitcoinService.get(),
                            BlockchainStatus.create(blocks, null));
                }
            });
    }

    /**
     * Notifica a los escuchas el progreso de la descarga.
     *
     * @param blocksSoFar Bloques restantes a descargar.
     * @param date        Fecha del último bloque descargado.
     */
    private static void notifyOnDownloaded(final int blocksSoFar, final Date date) {
        for (final IWalletListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onBlocksDownloaded(BitcoinService.get(),
                            BlockchainStatus.create(blocksSoFar, date));
                }
            });
    }

    /**
     * Notifica a los escuchas que la billetera ha recibido pagos.
     *
     * @param transaction Transacción que envía pagos a esta billetera.
     */
    private static void notifyOnReceived(final BitcoinTransaction transaction) {
        for (final IWalletListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onReceived(BitcoinService.get(), transaction);
                    listener.onBalanceChanged(BitcoinService.get(),
                            BitcoinService.get().getBalance());
                }
            });
    }

    /**
     * Notifica a los escuchas que la billetera ha enviado pagos.
     *
     * @param transaction Transacción que envía pagos fuera de esta billetera.
     */
    private static void notifyOnSent(final BitcoinTransaction transaction) {
        for (final IWalletListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onSent(BitcoinService.get(), transaction);
                    listener.onBalanceChanged(BitcoinService.get(),
                            BitcoinService.get().getBalance());
                }
            });
    }

    /**
     * Notifica a los escucha que el servicio ha inicializado.
     */
    private static void nofityOnStarted() {
        for (final IWalletListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onStarted(BitcoinService.get());
                }
            });
    }

    /**
     * Notifica a los escucha que la billetera ha cambiado.
     */
    public static void notifyOnBalanceChange() {
        for (final IWalletListener listener : mListeners)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    listener.onBalanceChanged(BitcoinService.get(),
                            BitcoinService.get().getBalance());
                }
            });
    }

    /**
     * Este método es llamado cuando el sistema inicia el servicio por primera vez.
     */
    @Override
    public void onCreate() {
        final Handler handler = new Handler();

        Threading.USER_THREAD = new Executor() {
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };

        mDirectory = getApplicationContext().getApplicationInfo().dataDir;

        Objects.requireNonNull(mDirectory);
    }

    /**
     * Configura los escuchas de eventos.
     *
     * @param wallet Billetera a configurar.
     */
    private void configureListeners(Wallet wallet) {
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
                notifyOnSent(new BitcoinTransaction(tx, wallet));
            }
        });

        wallet.addChangeEventListener(new WalletChangeEventListener() {

            /**
             * Este método es llamado cuando la billetera cambia.
             *
             * @param wallet Billetera que sufre el cambio.
             */
            @Override
            public void onWalletChanged(Wallet wallet) {
                try {
                    String walletFileName = String.format("wallet%s", FILE_EXT);
                    File walletFile = new File(mDirectory, walletFileName);
                    wallet.saveToFile(walletFile);
                } catch (IOException ex) {
                    notifyOnException(ex);
                }
            }
        });
    }

    /**
     * El sistema llama a este método cuando el servicio ya no se utiliza y se lo está destruyendo.
     */
    @Override
    public void onDestroy() {
        mListeners.clear();

        mPeerGroup.removeWallet(mWallet);
        mPeerGroup.stop();

        mRunning = false;
        mStore = null;
        mWallet = null;
        mPeerGroup = null;
        mDirectory = null;
    }

    /**
     * El sistema llama a este método cuando otro componente, como una actividad, solicita que se
     * inicie el servicio, llamando a {@link android.app.Activity#startService(Intent)}.
     *
     * @param intent  Intención que fue suministrada al servicio.
     * @param flags   Información adicional de la petición de inicio.
     * @param startId Identificador único que representa la petición de inicio.
     * @return Indica qué semántica debe usar el sistema para el estado de inicio actual del
     * servicio.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int res = super.onStartCommand(intent, flags, startId);

        DeterministicSeed seed = null;

        if (intent.hasExtra("WordList")) {
            List<String> words = intent.getStringArrayListExtra("WordList");
            seed = new DeterministicSeed(words, null, "", 0);
        }

        byte[] key = null;

        if (intent.hasExtra("Key"))
            key = intent.getByteArrayExtra("Key");

        init(seed, key);

        return res;
    }

    /**
     * Inicia la billetera y comienza la sincronización.
     */
    private void init(DeterministicSeed seed, byte[] key) {
        if (mRunning) {
            if (!mPeerGroup.isRunning())
                mPeerGroup.startAsync();
            return;
        }

        try {
            String walletFilename = String.format("wallet%s", FILE_EXT);
            File walletFile = new File(mDirectory, walletFilename);

            if (walletFile.exists())
                mWallet = Wallet.loadFromFile(walletFile);
            else if (seed != null)
                mWallet = Wallet.fromSeed(NETWORK_PARAM, seed);
            else
                mWallet = new Wallet(NETWORK_PARAM);

            configureListeners(mWallet);

            if (key != null) {
                int iterations = Utils.calculateIterations(Hex.toHexString(key));
                KeyCrypterScrypt scrypt = new KeyCrypterScrypt(iterations);
                mWallet.encrypt(scrypt, scrypt.deriveKey(Hex.toHexString(key)));
            }

            String blockStoreFilename = String.format("blockchain%s", FILE_EXT);
            File blockStoreFile = new File(mDirectory, blockStoreFilename);

            mStore = new SPVBlockStore(NETWORK_PARAM, blockStoreFile);

            mChain = new BlockChain(NETWORK_PARAM, mWallet, mStore);

            mPeerGroup = new PeerGroup(NETWORK_PARAM, mChain);
            mPeerGroup.addWallet(mWallet);

            notifyOnReady();

            preparePeerGroup();
        } catch (UnreadableWalletException | BlockStoreException ex) {
            notifyOnException(ex);
            stopSelf();
        }

    }

    /**
     * Prepara todas las configuraciones del grupo de puntos remotos.
     */
    private void preparePeerGroup() {
        final int MAX_PEERS = 10;

        mPeerGroup.setMaxConnections(MAX_PEERS);
        mPeerGroup.startBlockChainDownload(new DownloadProgressTracker() {

            /**
             * Este método es llamado cuando el progreso avanza.
             *
             * @param pct         Porcentaje estimado de la descarga.
             * @param blocksSoFar Cantidad de bloques restantes.
             * @param date        Fecha del último bloque.
             */
            @Override
            protected void progress(double pct, int blocksSoFar, Date date) {
                super.progress(pct, blocksSoFar, date);
                notifyOnDownloaded(blocksSoFar, date);
            }

            /**
             * Este método es llamado cuando inicia la descarga.
             *
             * @param blocks El número estimado a descargar.
             */
            @Override
            protected void startDownload(int blocks) {
                super.startDownload(blocks);
                notifyOnStartDownload(blocks);
            }

            /**
             * Este método es llamado cuando la descarga finaliza.
             */
            @Override
            protected void doneDownload() {
                super.doneDownload();
                notifyOnCompletedDownload();
            }
        });

        Futures.addCallback(mPeerGroup.startAsync(), new FutureCallback() {
            @Override
            public void onSuccess(@Nullable Object result) {
                mRunning = true;
                nofityOnStarted();
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                notifyOnException(new IllegalStateException(t));
                stopSelf();
            }
        });
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
    @Override
    public void sendPayment(String address, long amount, long feePerKb, IRequestKey requestKey)
            throws InSufficientBalanceException {
        Utils.throwIfNullOrEmpty(address,
                "La dirección no puede ser una cadena nula o vacía.");
        Preconditions.checkArgument(amount > 0,
                "La cantidad debe ser mayor a 0");

        try {
            Transaction txSend = new Transaction(NETWORK_PARAM);
            txSend.addOutput(Coin.valueOf(amount), Address.fromBase58(NETWORK_PARAM, address));

            SendRequest request = SendRequest.forTx(txSend);
            request.feePerKb = Coin.valueOf(feePerKb);

            if (mWallet.isEncrypted() && mWallet.getKeyCrypter() != null && requestKey != null)
                request.aesKey = mWallet.getKeyCrypter()
                        .deriveKey(Hex.toHexString(requestKey.onRequest()));

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
                    SupportedAssets.BTC, ex);
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
            Address.fromBase58(NETWORK_PARAM, address);
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

        if (walletFile.exists())
            walletFile.deleteOnExit();

        if (blockStoreFile.exists())
            blockStoreFile.deleteOnExit();
    }

    /**
     * Obtiene las palabras semilla de la billetera, las cuales permiten restaurarla en caso de
     * perdida.
     *
     * @return Lista de las palabras de recuperación.
     */
    @Override
    public List<String> getSeedWords(IRequestKey requestCallback) {
        if (mWallet.getKeyChainSeed().isEncrypted()) {

            byte[] key = requestCallback.onRequest();

            KeyCrypter keyCrypter = mWallet.getKeyCrypter();

            Objects.requireNonNull(keyCrypter);

            DeterministicSeed deterministicSeed = mWallet.getKeyChainSeed()
                    .decrypt(keyCrypter, "", new KeyParameter(key));

            return deterministicSeed.getMnemonicCode();
        }

        return mWallet.getKeyChainSeed().getMnemonicCode();
    }

    /**
     * Encripta la billetera de forma segura especificando la llave utilizada para ello.
     *
     * @param key     Llave de encriptación.
     * @param prevKey Llave anterior en caso de estár encriptada.
     * @return Un valor true en caso de que se encripte correctamente.
     */
    @Override
    public boolean encryptWallet(byte[] key, byte[] prevKey) {
        Objects.requireNonNull(key);

        if (mWallet.isEncrypted())
            mWallet.decrypt(Hex.toHexString(prevKey));

        String hash = Hex.toHexString(key);
        int iterations = Utils.calculateIterations(hash);

        String walletFilename = "wallet.btc";

        try {
            mWallet.encrypt(new KeyCrypterScrypt(iterations), new KeyParameter(key));
            mWallet.saveToFile(File.createTempFile(walletFilename, null),
                    new File(mDirectory, walletFilename));
            return true;
        } catch (IOException | KeyCrypterException ignored) {
            return false;
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
    public boolean isEncrypted() {
        return mWallet.isEncrypted();
    }

    /**
     * Desconecta la billetera de la red.
     */
    @Override
    public void disconnectNetwork() {
        if (mPeerGroup == null)
            return;

        mPeerGroup.removeWallet(mWallet);
        mPeerGroup.setMaxConnections(0);
        mPeerGroup.stop();

        mPeerGroup = null;
    }

    /**
     * Conecta la billetera a la red.
     */
    @Override
    public void connectNetwork() {
        if (mPeerGroup != null)
            return;

        mPeerGroup = new PeerGroup(NETWORK_PARAM, mChain);
        mPeerGroup.addWallet(mWallet);

        preparePeerGroup();
    }
}
