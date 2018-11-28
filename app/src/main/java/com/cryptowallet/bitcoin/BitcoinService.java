package com.cryptowallet.bitcoin;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cryptowallet.R;
import com.cryptowallet.utils.Helper;
import com.cryptowallet.wallet.BroadcastListener;
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
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

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

    /**
     * Semila de la billetera.
     */
    private static String mSeed = "";

    /**
     * Servicio de la billetera.
     */
    private static BitcoinService mService;
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
     * Parametros de la red utilizada en Bitcoin.
     */
    private NetworkParameters mNetworkParameters = TestNet3Params.get();

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
            String messge = String.format(getString(R.string.notify_receive), btcValue);
            String template = "%s\nTxID: %s";

            Helper.sendLargeTextNotificationOs(
                    getApplicationContext(),
                    R.mipmap.img_bitcoin,
                    getString(R.string.app_name),
                    messge,
                    String.format(template,
                            messge,
                            tx.getHash()
                    )
            );
        }
    };

    /**
     * Crea una instancia de WalletServiceBase.
     */
    public BitcoinService() {
        super("bitcoin-service");
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

        BitcoinService service = get();
        NetworkParameters params = service.getNetwork();

        List<TransactionInput> inputs = tx.getInputs();

        for (TransactionInput input : inputs) {
            if (builder.length() > 0)
                builder.append("\n");

            try {

                if (input.isCoinBase()) {
                    builder.append(coinbaseLabel);
                    continue;
                }

                Script script = input.getScriptSig();
                byte[] key = script.getPubKey();

                builder.append(Address
                        .fromP2SHHash(params, Utils.sha256hash160(key)).toBase58());


            } catch (ScriptException ex) {
                get().mLogger.warn("Dirección no decodificada");
                builder.append(unknownAdress);
            }
        }

        return builder.toString();
    }

    public static String getToAddresses(Transaction tx) {
        StringBuilder builder = new StringBuilder();
        BitcoinService service = get();
        NetworkParameters params = service.getNetwork();

        Wallet wallet = service.getWallet();
        List<TransactionOutput> outputs = tx.getOutputs();

        for (TransactionOutput output : outputs) {
            if (builder.length() > 0)
                builder.append("\n");

            Address address = output.getAddressFromP2SH(params);

            if (address == null)
                address = output.getAddressFromP2PKHScript(params);

            if (address != null)
                builder.append(address.toBase58());
        }

        return builder.toString();
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

        if (mService.isInitialized())
            return;

        String dataDir = getApplicationInfo().dataDir;

        mKitApp = new WalletAppKit(mNetworkParameters, new File(dataDir), PREFIX) {

            /**
             * Este método es ejecutado cuando la billetera termina de configurarse.
             * Esto ocurre cuando se lee el archivo de la billetera, o es restaurada.
             */
            @Override
            protected void onSetupCompleted() {
                peerGroup().setStallThreshold(10, 1024 * 50);
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
                        get().mSyncronizedBlockchain = true;
                        for (BitcoinListener listener : mListeners)
                            listener.onCompletedDownloaded(BitcoinService.this);
                    }
                }).setUserAgent("CryptoWallet", "0.8.2311_beta")
                .startAsync();
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
        return mKitApp.wallet().getBalance(Wallet.BalanceType.ESTIMATED_SPENDABLE);
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

    /**
     * Valida si la dirección especificada es correcta.
     *
     * @param address Dirección a validar.
     * @return Un valor true si es válida.
     */
    @Override
    public boolean validateAddress(String address) {
        try {
            Address.fromBase58(mNetworkParameters, address);
            return true;
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

    /**
     * Este método se llama cuando el servicio es destruido.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mKitApp.stopAsync();
    }

    /**
     * Obtiene los parametros de la red conectada.
     *
     * @return Los parametros de la red.
     */
    public NetworkParameters getNetwork() {
        return mNetworkParameters;
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
