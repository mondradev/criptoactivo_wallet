package com.cryptowallet.bitcoin;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cryptowallet.R;
import com.cryptowallet.app.AppPreference;
import com.cryptowallet.app.ExtrasKey;
import com.cryptowallet.app.TransactionActivity;
import com.cryptowallet.utils.ConnectionManager;
import com.cryptowallet.utils.Helper;
import com.cryptowallet.wallet.BroadcastListener;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletServiceBase;
import com.google.common.util.concurrent.ListenableFuture;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Ofrece un servicio de billetera de Bitcoin permitiendo recibir y enviar pagos, consultar saldo y
 * ver transacciones pasadas.
 */
public final class BitcoinService extends WalletServiceBase<Coin, Address, Transaction> {

    /**
     * Valor del BTC en satoshis.
     */
    public static final long BTC_IN_SATOSHIS = 100000000;

    /**
     * Lista de escucha de eventos de la billetera.
     */
    private final static CopyOnWriteArrayList<BitcoinListener> mListeners
            = new CopyOnWriteArrayList<>();

    private static Logger mLogger = LoggerFactory.getLogger(BitcoinService.class);

    /**
     * Semila de la billetera.
     */
    private static String mSeed = "";

    /**
     * Servicio de la billetera.
     */
    private static BitcoinService mService;
    /**
     * Parametros de la red utilizada en Bitcoin.
     */
    private static NetworkParameters mNetworkParameters = TestNet3Params.get();
    /**
     * Prefijo utilizado para los archivos de la billetera.
     */
    private final String PREFIX = "bitcoin.data";
    /**
     * Instancia de kit de billetera.
     */
    private WalletAppKit mKitApp;
    /**
     *
     */
    private boolean mSyncronizedBlockchain = false;
    /**
     * Número máximo de conexiones administradas por el grupo de P2P.
     */
    private int mMaxConnections = 10;
    /**
     *
     */
    private WalletCoinsReceivedEventListener mReceivedNotifier
            = new WalletCoinsReceivedEventListener() {
        @Override
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {

            if (tx.getValue(wallet).isNegative())
                return;

            String btcValue = Coin.valueOf(getValueFromTx(tx))
                    .toFriendlyString();

            String id = tx.getHashAsString();

            Intent intent = new Intent(BitcoinService.get(), TransactionActivity.class);
            intent.putExtra(ExtrasKey.TX_ID, id);
            intent.putExtra(ExtrasKey.SELECTED_COIN, SupportedAssets.BTC.name());

            Helper.sendReceiveMoneyNotification(
                    getApplicationContext(),
                    R.mipmap.img_bitcoin,
                    btcValue,
                    id,
                    intent
            );
        }
    };
    private WalletCoinsSentEventListener mSentNotifier
            = new WalletCoinsSentEventListener() {
        @Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {

            if (tx.getValue(wallet).isNegative())
                return;

            String btcValue = Coin.valueOf(getValueFromTx(tx))
                    .toFriendlyString();
            String messge = String.format(getString(R.string.notify_send), btcValue);
            String template = "%s\nTxID: %s";
            String id = tx.getHashAsString();

            Intent intent = new Intent(BitcoinService.get(), TransactionActivity.class);
            intent.putExtra(ExtrasKey.TX_ID, id);
            intent.putExtra(ExtrasKey.SELECTED_COIN, SupportedAssets.BTC.name());

            Helper.sendLargeTextNotificationOs(
                    getApplicationContext(),
                    R.mipmap.img_bitcoin,
                    getString(R.string.app_name),
                    messge,
                    String.format(template,
                            messge,
                            id
                    ),
                    intent
            );
        }
    };

    private Intent mIntent;

    private boolean mCanConnect = true;

    /**
     * Crea una instancia de WalletServiceBase.
     */
    public BitcoinService() {
        mService = this;

        final Handler handler = new Handler();
        Threading.USER_THREAD = new Executor() {
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };
    }

    /**
     * Agrega un escucha de eventos para la clase.
     *
     * @param listener Escucha de eventos.
     */
    public static void addEventListener(BitcoinListener listener) {
        if (listener == null)
            return;

        if (mListeners.contains(listener))
            return;

        mListeners.add(listener);

        if (get() != null) {
            if (get().isInitialized()) {
                listener.onReady(get());
                get().getWallet().addCoinsSentEventListener(listener);
                get().getWallet().addChangeEventListener(listener);
                get().getWallet().addReorganizeEventListener(listener);
                get().getWallet().addCoinsReceivedEventListener(listener);
            }
            if (get().isSyncronizedBlockchain())
                listener.onCompletedDownloaded(get());
        }


    }

    /**
     * Obtiene la instancia del servicio en ejecución.
     *
     * @return Instancia del servicio.
     */
    public static BitcoinService get() {
        return mService;
    }

    /**
     * Remueve el escucha de eventos del servicio.
     *
     * @param listener Escucha de eventos.
     */
    public static void removeEventListener(BitcoinListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Establece la semila de la billetera.
     *
     * @param seed Semilla.
     */
    public static void setSeed(String seed) {
        mSeed = seed;
    }

    public static String getFromAddresses(Transaction tx, String coinbaseLabel,
                                          String unknownAdress) {
        StringBuilder builder = new StringBuilder();

        NetworkParameters params = getNetwork();

        List<TransactionInput> inputs = tx.getInputs();

        for (TransactionInput input : inputs) {

            try {

                if (input.isCoinBase()) {
                    builder.append(coinbaseLabel);
                    continue;
                }

                Address address = null;

                if (input.getConnectedOutput() != null) {
                    address = input.getConnectedOutput().getAddressFromP2PKHScript(params);

                    if (address == null)
                        address = input.getConnectedOutput().getAddressFromP2SH(params);
                }

                if (address != null) {
                    if (!builder.toString().contains(address.toBase58())) {
                        if (builder.length() > 0 && !builder.toString().endsWith("\n"))
                            builder.append("\n");

                        builder.append(address.toBase58());
                    }
                } else
                    throw new ScriptException("Can´t get from address");

            } catch (ScriptException ex) {
                mLogger.warn("Dirección no decodificada");
                builder.append(unknownAdress);

                break;
            }
        }

        return builder.toString();
    }

    public static String getToAddresses(Transaction tx) {
        StringBuilder builder = new StringBuilder();
        Wallet wallet = get().getWallet();
        NetworkParameters params = getNetwork();
        Boolean isPay = isPay(tx);

        List<TransactionOutput> outputs = tx.getOutputs();

        for (TransactionOutput output : outputs) {

            if (isPay && output.isMine(wallet))
                continue;
            else if (!isPay && !output.isMine(wallet))
                continue;

            Address address = output.getAddressFromP2SH(params);

            if (address == null)
                address = output.getAddressFromP2PKHScript(params);

            if (address != null) {
                if (builder.length() > 0)
                    builder.append("\n");

                builder.append(address.toBase58());
            }
        }

        return builder.toString();
    }

    public static boolean isPay(Transaction tx) {
        return tx.getValue(get().getWallet()).isNegative();
    }

    public static boolean isRunning() {
        if (mService == null)
            return false;

        return mService.isInitialized();
    }

    /**
     * Obtiene los parametros de la red conectada.
     *
     * @return Los parametros de la red.
     */
    public static NetworkParameters getNetwork() {
        return mNetworkParameters;
    }

    public Transaction getTransaction(String txID) {
        mLogger.info("Buscando transacción: {}", txID);
        return getWallet().getTransaction(Sha256Hash.wrap(txID));
    }

    public PeerGroup getPeerGroup() {
        return mKitApp.peerGroup();
    }

    /**
     * Obtiene las 12 palabras de la billetera.
     */
    public List<String> getSeedCode(@Nullable byte[] key) {
        DeterministicSeed seed = getWallet().getKeyChainSeed();

        if (requireDecrypted())
            return seed.decrypt(Objects.requireNonNull(getWallet().getKeyCrypter()),
                    "", new KeyParameter(Objects.requireNonNull(key))).getMnemonicCode();

        return seed.getMnemonicCode();
    }

    /**
     * Obtiene la instancia que controla la billetera.
     *
     * @return Una billetera.
     */
    public Wallet getWallet() {
        return mKitApp.wallet();
    }

    /**
     * Este método es llamado cuando se crea el servicio.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        if (isRunning())
            return;

        String dataDir = getApplicationInfo().dataDir;

        mKitApp = new WalletAppKit(mNetworkParameters, new File(dataDir), PREFIX) {

            /**
             * Este método es ejecutado cuando la billetera termina de configurarse.
             * Esto ocurre cuando se lee el archivo de la billetera, o es restaurada.
             */
            @Override
            protected void onSetupCompleted() {
                peerGroup().setStallThreshold(10, 1024 * 20);
                peerGroup().setMaxConnections(mMaxConnections);
                setInitialized();

                for (BitcoinListener listener : mListeners) {
                    wallet().addCoinsReceivedEventListener(listener);
                    wallet().addReorganizeEventListener(listener);
                    wallet().addChangeEventListener(listener);
                    wallet().addCoinsSentEventListener(listener);

                    final BitcoinListener l = listener;

                    if (listener != null)
                        Threading.USER_THREAD.execute(new Runnable() {
                            @Override
                            public void run() {
                                l.onReady(BitcoinService.this);
                            }
                        });
                }

                wallet().addCoinsReceivedEventListener(mReceivedNotifier);
                wallet().addCoinsSentEventListener(mSentNotifier);

                peerGroup().addConnectedEventListener(new PeerConnectedEventListener() {
                    @Override
                    public void onPeerConnected(Peer peer, int peerCount) {
                        if (!mCanConnect) {
                            mLogger.info("Cerrando punto: {}, Estado de la conexión: {}", peer, mCanConnect);
                            peer.close();
                            peer.connectionClosed();
                        }
                    }
                });

                Context context = BitcoinService.get().getApplicationContext();

                if (AppPreference.useOnlyWifi(context)
                        && !ConnectionManager.isWifiConnected(context))
                    disconnect();

                ConnectionManager.registerHandlerConnection(BitcoinService.this,
                        new ConnectionManager.OnChangeConnectionState() {
                            @Override
                            public void onConnect(ConnectionManager.NetworkInterface network) {
                                if (AppPreference.useOnlyWifi(BitcoinService.this.getApplicationContext()))
                                    if (network == ConnectionManager.NetworkInterface.WIFI) {
                                        mLogger.info("Solo wifi: Conectando");
                                        connect();
                                    } else
                                        disconnect();
                            }

                            @Override
                            public void onDisconnect(ConnectionManager.NetworkInterface network) {
                                if (AppPreference.useOnlyWifi(BitcoinService.this.getApplicationContext()))
                                    if (network == ConnectionManager.NetworkInterface.WIFI) {
                                        mLogger.info("Solo wifi: Desconectando");
                                        disconnect();
                                    } else
                                        connect();
                            }
                        });
            }
        };

        try {
            if (!mSeed.isEmpty()) {
                mKitApp.restoreWalletFromSeed(new DeterministicSeed(
                        mSeed, null, "", 0));

                mSeed = "";
            }
        } catch (Exception ignored) {
        }

        mKitApp
                .setBlockingStartup(false)
                .setDownloadListener(new DownloadProgressTracker() {

                    /**
                     *
                     */
                    private int mBlocksLeft;

                    /**
                     * @param peer
                     * @param blocksLeft
                     */
                    @Override
                    public void onChainDownloadStarted(Peer peer, int blocksLeft) {
                        if (!mCanConnect) return;

                        super.onChainDownloadStarted(peer, blocksLeft);
                        this.mBlocksLeft = blocksLeft;
                        get().mSyncronizedBlockchain = false;

                        for (BitcoinListener listener : mListeners)
                            listener.onStartDownload(BitcoinService.this, blocksLeft);
                    }

                    /**
                     * @param peer
                     * @param block
                     * @param filteredBlock
                     * @param blocksLeft
                     */
                    @Override
                    public void onBlocksDownloaded(Peer peer, Block block,
                                                   @Nullable FilteredBlock filteredBlock,
                                                   int blocksLeft) {
                        if (!mCanConnect) return;
                        super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft);
                        for (BitcoinListener listener : mListeners)
                            listener.onBlocksDownloaded(BitcoinService.this, blocksLeft,
                                    mBlocksLeft, block.getTime());
                    }

                    /**
                     * Este método es llamado cuando la blockchain es descargada.
                     */
                    @Override
                    protected void doneDownload() {
                        if (!mCanConnect) return;
                        get().mSyncronizedBlockchain = true;
                        for (BitcoinListener listener : mListeners)
                            listener.onCompletedDownloaded(BitcoinService.this);
                    }
                })
                .setUserAgent("CryptoWallet",
                        AppPreference.getVesion(getApplicationContext()).toString());

        mKitApp.startAsync();
    }

    public void disconnect() {
        mLogger.info("Desconectando puntos remotos");

        mCanConnect = false;

        for (Peer peer : getPeerGroup().getConnectedPeers()) {
            peer.close();
            peer.connectionClosed();
        }
    }

    public void connect() {
        mLogger.info("Conectando puntos remotos");
        mCanConnect = true;
    }

    /**
     * @return
     */
    public boolean isSyncronizedBlockchain() {
        return mSyncronizedBlockchain;
    }

    /**
     * Permite realizar un envío a una dirección especificada.
     *
     * @param value    Cantidad de monedas a enviar.
     * @param to       Dirección del receptor.
     * @param feePerKb Comisiones por Kilobyte.
     * @return Una transacción del pago.
     */
    @Override
    public Transaction sendPay(@NonNull Coin value, @NonNull Address to, @NonNull Coin feePerKb,
                               @Nullable byte[] password) throws InsufficientMoneyException,
            KeyCrypterException {
        Wallet wallet = mKitApp.wallet();

        Transaction txSend = new Transaction(mNetworkParameters);
        txSend.addOutput(value, to);

        SendRequest request = SendRequest.forTx(txSend);
        request.feePerKb = feePerKb;

        if (password != null)
            request.aesKey = new KeyParameter(password);

        wallet.completeTx(request);
        wallet.commitTx(request.tx);

        mLogger.info("Transacción lista para ser propagada por la red de Bitcoin, hash: "
                + request.tx.getHash().toString());

        return txSend;
    }

    /**
     * Propaga una transacción a través de la red, una vez finalizada esta operación se ejecuta
     * la función implementada por la interfaz <code>BroadcastListener</code>.
     *
     * @param tx       Transacción a propagar.
     * @param listener Instancia de la función a ejecutar al finalizar.
     */
    @Override
    public void broadCastTx(@NonNull final Transaction tx,
                            @Nullable final BroadcastListener<Transaction> listener) {

        final ListenableFuture<Transaction> future = mKitApp.peerGroup()
                .broadcastTransaction(tx).future();

        mLogger.info("Propagando transacción por la red de Bitcoin, hash: "
                + tx.getHash().toString());

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    final Transaction tx = future.get();
                    final TransactionConfidence txConfidence = tx.getConfidence();

                    if (listener != null)
                        listener.onCompleted(tx);

                    txConfidence.addEventListener(new TransactionConfidence.Listener() {
                        @Override
                        public void onConfidenceChanged(TransactionConfidence confidence,
                                                        ChangeReason reason) {
                            TransactionConfidence.ConfidenceType type
                                    = confidence.getConfidenceType();
                            if (type == TransactionConfidence.ConfidenceType.BUILDING) {
                                for (BitcoinListener bitcoinListener : mListeners)
                                    bitcoinListener.onCommited(get(), tx);
                                confidence.removeEventListener(this);
                            }
                        }
                    });

                } catch (ExecutionException e) {
                    mLogger.error("No se logró propagar la transacción a través de la red.");
                } catch (InterruptedException e) {
                    mLogger.error("Se interrumpió la propagación de la transacción.");
                } catch (NullPointerException ignored) {
                    mLogger.error("Error al obtener la confidencia de la transacción durante " +
                            "la transmisión a la red.");
                }
            }
        }, Threading.USER_THREAD);
    }

    /**
     * Obtiene la cantidad actual del saldo de la billetera.
     *
     * @return El saldo actual.
     */
    @Override
    public Coin getBalance() {
        Coin balance = mKitApp.wallet().getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE);
        mLogger.info("Saldo actual: {}", balance.toFriendlyString());
        return balance;
    }

    /**
     * Obtiene la lista completa del historial de las transacciones de la billetera.
     *
     * @return Una lista de transacciones ordenadas por su fecha de creación.
     */
    @Override
    public List<Transaction> getTransactionsByTime() {
        return mKitApp.wallet().getTransactionsByTime();
    }

    public void handlerWalletChange() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                org.bitcoinj.core.Context.propagate(Helper.BITCOIN_CONTEXT);
                for (BitcoinListener listener : mListeners)
                    listener.onBalanceChanged(BitcoinService.this, getBalance());
            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int state = super.onStartCommand(intent, flags, startId);
        mIntent = intent;

        return state;
    }

    /**
     * Valida si la dirección especificada es correcta.
     *
     * @param address Dirección a validar.
     * @return Un valor true si es válida.
     */
    @Override
    public boolean validateAddress(String address) {
        try {
            Address a = Address.fromBase58(mNetworkParameters, address);
            return !getWallet().getIssuedReceiveAddresses().contains(a);
        } catch (AddressFormatException ex) {
            return false;
        }
    }

    /**
     * Obtiene la dirección para recibir pagos.
     *
     * @return Dirección de recepción.
     */
    @Override
    public String getAddressRecipient() {
        return getWallet().freshReceiveAddress().toBase58();
    }

    /**
     * @return
     */
    @Override
    public boolean requireDecrypted() {
        return getWallet().isEncrypted();
    }

    @Override
    public boolean validatePin(byte[] pin) {
        return getWallet().checkAESKey(new KeyParameter(pin));
    }

    @Override
    public void shutdown() {
        mKitApp.stopAsync();
        mKitApp.awaitTerminated();

        getApplicationContext().stopService(mIntent);
    }

    @Override
    public void deleteWallet() throws IOException {
        try {
            mLogger.info("Desactivando los servicios de Bitcoin");

            if (mKitApp.isRunning())
                shutdown();

            String dataDir = getApplicationInfo().dataDir;
            String prefix = PREFIX;
            String walletExt = ".wallet";
            String blockExt = ".spvchain";

            File walletFile = new File(dataDir, prefix.concat(walletExt));
            File blockFile = new File(dataDir, prefix.concat(blockExt));

            Thread.sleep(5000);

            mLogger.info("Billetera a eliminar: " + walletFile.getAbsolutePath());
            mLogger.info("Blockchain a eliminar: " + blockFile.getAbsolutePath());

            if (walletFile.exists())
                if (!walletFile.delete())
                    throw new IOException("No se puede eliminar el archivo de la billetera.");

            if (blockFile.exists())
                if (!blockFile.delete())
                    throw new IOException("No se puede eliminar el archivo de la blockchain.");

            mService = null;

        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void disconnectPeers() {
        for (Peer peer : getPeerGroup().getConnectedPeers()) {
            peer.close();
            peer.connectionClosed();
        }
    }

    /**
     * Este método se llama cuando el servicio es destruido.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        shutdown();
    }

    /**
     * Obtiene el valor absoluto de la transacción.
     *
     * @param tx Transacción a obtener su valor.
     * @return Valor en la fracción máxima del activo.
     */
    public long getValueFromTx(@NonNull Transaction tx) {
        return getAbsolute(tx.getValue(getWallet()).isNegative()
                ? tx.getValue(getWallet()).add(tx.getFee())
                : tx.getValueSentToMe(getWallet())).getValue();
    }

    /**
     * Obtiene el valor absoluto de una cantidad en BTC.
     *
     * @param value Valor a manipular.
     * @return El valor absoluto.
     */
    private Coin getAbsolute(@NonNull Coin value) {
        return value.isNegative() ? value.multiply(-1) : value;
    }
}
