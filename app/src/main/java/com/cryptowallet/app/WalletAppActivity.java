package com.cryptowallet.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinListener;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.Helper;
import com.cryptowallet.wallet.ExchangeService;
import com.cryptowallet.wallet.GenericTransaction;
import com.cryptowallet.wallet.RecentListAdapter;
import com.cryptowallet.wallet.SupportedAssets;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Actividad principal de la billetera. Desde esta actividad se establece el flujo de trabajo de
 * toda la aplicación.
 */
public class WalletAppActivity extends ActivityBase {

    /**
     * Gestiona todos los logs de la clase.
     */
    private Logger mLogger = LoggerFactory.getLogger(WalletAppActivity.class);

    /**
     * TextView que visualiza el saldo de la billetera.
     */
    private TextView mBalanceText;
    /**
     * Es un cuadro de diálogo que permite congelar la actividad hasta que la billetera se inicia.
     */
    private AlertDialog mDialogOnLoad;
    /**
     * Lista de las transacciones.
     */
    private RecentListAdapter mRecentsAdapter;
    /**
     *
     */
    private ActionBarDrawerToggle mToggle;
    private CountDownTimer mUpdateQrCountDown
            = new CountDownTimer(5000, 1) {

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            handlerRefreshCode(null);
        }

    };
    /**
     * Escucha de los eventos de la billetera de bitcoin.
     */
    private BitcoinListener mListener = new BitcoinListener() {

        private boolean mInitDownload = false;

        /**
         * Este método se ejecuta cuando la billetera recibe una transacción.
         *
         * @param service Servicio de la billetera.
         * @param tx      Transacción recibida.
         */
        @Override
        public void onReceived(BitcoinService service, Transaction tx) {
            addToRecents(tx, service);
        }

        /**
         * Este método se ejecuta cuando el saldo de la billetera ha cambiado.
         *
         * @param service Servicio de la billetera.
         * @param balance Balance nuevo en la unidad más pequeña de la moneda o token.
         */
        @Override
        public void onBalanceChanged(BitcoinService service, final Coin balance) {
            Threading.USER_THREAD.execute(new Runnable() {
                @Override
                public void run() {
                    final Fiat balanceUsd = Fiat.valueOf("USD",
                            ExchangeService.btcToUsd(balance.getValue()));

                    mBalanceText.setText(balance.toFriendlyString());
                    TextView mUsdBalance = findViewById(R.id.mBalanceFiat);
                    mUsdBalance.setText(balanceUsd.toFriendlyString());
                }
            });
        }

        /**
         * Este método se ejecuta cuando la billetera está inicializada correctamente.
         *
         * @param service Servicio de la billetera.
         */
        @Override
        public void onReady(final BitcoinService service) {
            final Coin balance = service.getBalance();
            final Fiat balanceUsd
                    = Fiat.valueOf("MXN", ExchangeService.btcToMxn(balance.getValue()));

            mBalanceText.setText(balance.toFriendlyString());
            List<Transaction> transactions = service.getTransactionsByTime();

            TextView mUsdBalance = findViewById(R.id.mBalanceFiat);
            mUsdBalance.setText(balanceUsd.toFriendlyString());

            for (final Transaction tx : transactions)
                addToRecents(tx, service);

            if (mDialogOnLoad != null)
                mDialogOnLoad.dismiss();
        }

        /**
         * Añade las transacciones de Bitcoin a la lista de recientes.
         *
         * @param tx Transacción de bitcoin.
         * @param service Servicio de la billetera.
         */
        private void addToRecents(final Transaction tx, final BitcoinService service) {
            boolean isPay = tx.getValue(service.getWallet()).isNegative();

            final GenericTransaction item = new GenericTransaction.Builder(
                    WalletAppActivity.this, R.mipmap.img_bitcoin,
                    ExchangeService.Currencies.BTC)
                    .setKind(Helper.getTxKind(isPay))
                    .setTime(tx.getUpdateTime())
                    .setFee(isPay ? tx.getFee().toFriendlyString() : "")
                    .setAmount(service.getValueFromTx(tx))
                    .setTxID(tx.getHashAsString())
                    .setCommits(tx.getConfidence().getAppearedAtChainHeight())
                    .appendFromAddress(BitcoinService.getFromAddresses(tx,
                            getString(R.string.coinbase_address),
                            getString(R.string.unknown_address)
                    ))
                    .appendToAddress(BitcoinService.getToAddresses(tx))
                    .create();

            if (tx.getConfidence().getConfidenceType()
                    != TransactionConfidence.ConfidenceType.BUILDING) {
                tx.getConfidence().addEventListener(new TransactionConfidence.Listener() {
                    @Override
                    public void onConfidenceChanged(TransactionConfidence confidence,
                                                    ChangeReason reason) {
                        if (confidence.getConfidenceType()
                                == TransactionConfidence.ConfidenceType.BUILDING) {
                            tx.getConfidence().removeEventListener(this);
                            item.commite();
                        }

                    }
                });
            } else
                item.commite();

            mRecentsAdapter.addItem(item);
        }

        /**
         * Este método se ejecuta cuando la blockchain de la billetera ha sido descargada
         * completamente.
         *
         * @param service Servicio de la billetera.
         */
        @Override
        public void onCompletedDownloaded(BitcoinService service) {
            if (!mInitDownload)
                return;

            Helper.sendNotificationOs(
                    WalletAppActivity.this,
                    getString(R.string.app_name),
                    getString(R.string.blockchain_downloaded)
            );

            Threading.USER_THREAD.execute(new Runnable() {
                @Override
                public void run() {
                    CardView mStatus = findViewById(R.id.mBlockchainDownloadCard);
                    mStatus.setVisibility(View.GONE);

                    mInitDownload = false;
                }
            });
        }

        /**
         * @param service
         * @param leftBlocks
         * @param totalBlocksToDownload
         * @param blockTime
         */
        @Override
        public void onBlocksDownloaded(BitcoinService service, final int leftBlocks,
                                       final int totalBlocksToDownload, final Date blockTime) {

            Threading.USER_THREAD.execute(new Runnable() {
                @Override
                public void run() {

                    mUpdateQrCountDown.cancel();

                    CardView mStatus = findViewById(R.id.mBlockchainDownloadCard);
                    if (mStatus.getVisibility() == View.GONE)
                        mStatus.setVisibility(View.VISIBLE);

                    TextView mLastBlock = findViewById(R.id.mLastBlock);
                    mLastBlock.setText(String.format(getString(R.string.last_block_date_text), leftBlocks));

                    if (leftBlocks == 0)
                        mStatus.setVisibility(View.GONE);
                }
            });
        }

        @Override
        public void onStartDownload(BitcoinService bitcoinService, int blocksTodownload) {
            mInitDownload = true;

            Threading.USER_THREAD.execute(new Runnable() {
                @Override
                public void run() {
                    mUpdateQrCountDown.start();

                    CardView mStatus = findViewById(R.id.mBlockchainDownloadCard);
                    mStatus.setVisibility(View.VISIBLE);

                    TextView mLastBlock = findViewById(R.id.mLastBlock);
                    mLastBlock.setText(getString(R.string.calculate_blocks));
                }
            });
        }
    };

    public void handlerRefreshCode(View view) {
        CardView mStatus = findViewById(R.id.mBlockchainDownloadCard);
        mStatus.setVisibility(View.GONE);
    }

    /**
     * Este método se ejecuta cuando la actividad es creada.
     *
     * @param savedInstanceState Estado actual de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_app);

        String actionBarError = "No se logró obtener el ActionBar";

        Objects.requireNonNull(getSupportActionBar(), actionBarError)
                .setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar(), actionBarError)
                .setIcon(R.drawable.ic_bitcoin);

        ScrollView mScroll = findViewById(R.id.mMainScroll);
        mScroll.smoothScrollTo(0, 0);

        mRecentsAdapter = new RecentListAdapter();
        mBalanceText = findViewById(R.id.mBalanceText);

        DrawerLayout mDrawerLayout = findViewById(R.id.mDrawerLayout);
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close);

        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();

        NavigationView mLeftDrawer = findViewById(R.id.mLeftDrawer);
        mLeftDrawer.setNavigationItemSelectedListener(new NavigationView
                .OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch (id) {
                    case R.id.mSettings:
                        handlerShowSettingsActivity();
                        break;

                    case R.id.mBackup:
                        handlerShowBackupFundsActivity();
                        break;

                    case R.id.mDrop:
                        handlerShowDropWalletActivity();
                        break;

                    case R.id.mSend:
                        handlerShowSendPaymentsActivity(null);
                        break;

                    case R.id.mReceive:
                        handlerShowRequestPaymentsActivity();
                        break;

                    case R.id.mHistory:
                        handlerShowHistory(null);
                        break;
                }

                return true;
            }
        });

        RecyclerView mRecentsRecycler = findViewById(R.id.mRecentsRecycler);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);

        mRecentsRecycler.setAdapter(mRecentsAdapter);
        mRecentsRecycler.setHasFixedSize(true);
        mRecentsRecycler.setLayoutManager(mLayoutManager);

        mRecentsAdapter.setEmptyView(findViewById(R.id.mEmptyRecents));

        BitcoinService.addEventListener(mListener);

        mLogger.info("Actividad principal creada");

        if (!BitcoinService.get().isInitialized()) {

            mDialogOnLoad = new AlertDialog.Builder(this)
                    .setTitle(R.string.loading_title_text)
                    .setMessage(R.string.loading_text)
                    .create();

            mDialogOnLoad.setCanceledOnTouchOutside(false);
            mDialogOnLoad.setCancelable(false);
            mDialogOnLoad.show();
        }
    }

    /**
     * Muestra la actividad de elimanar billetera.
     */
    private void handlerShowDropWalletActivity() {

    }

    /**
     * Muestra la actividad de configuración.
     */
    private void handlerShowSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Muestra la actividad de respaldo de fondos.
     */
    private void handlerShowBackupFundsActivity() {
        Intent intent = new Intent(this, BackupFundsActitivy.class);
        startActivityForResult(intent, 0);
    }


    /**
     * Este método es llamado cuando la actividad es destruída.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        BitcoinService.removeEventListener(mListener);

        mLogger.info("Actividad principal destruida");
    }

    /**
     * Este método se ejecuta cuando se crea el menú de opciones de la actividad.
     *
     * @param menu Interfaz que administra los menús.
     * @return Un valor true al completar la creación del menú.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mn_request_payment, menu);
        return true;
    }

    /**
     * Este método se ejecuta cuando se hace clic en un elemento del menú de la actividad.
     *
     * @param item Elemento con el cual el usuario interactuó.
     * @return Un valor true que indica que la acción fue completada.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mToggle.onOptionsItemSelected(item))
            return true;

        switch (item.getItemId()) {
            case R.id.request_payment_qr:
                handlerShowRequestPaymentsActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Acción que muestra la actividad de <code>SendPaymentsActivity</code>.
     *
     * @param view Elemento visual que hace la invocación.
     */
    public void handlerShowSendPaymentsActivity(View view) {
        Intent intent = new Intent(this, SendPaymentsActivity.class);
        intent.putExtra(ExtrasKey.SELECTED_COIN, SupportedAssets.BTC.name());
        startActivityForResult(intent, 0);
    }

    /**
     * Acción que muestra la actividad de <code>ReceiveCoinsActivity</code>.
     */
    public void handlerShowRequestPaymentsActivity() {
        Intent intent = new Intent(this, ReceiveCoinsActivity.class);
        intent.putExtra(ExtrasKey.SELECTED_COIN, SupportedAssets.BTC.name());
        startActivityForResult(intent, 0);
    }

    /**
     * Este método es llamado cuando se invoca una actividad y está devuelve un valor.
     *
     * @param requestCode Código de petición.
     * @param resultCode  Código de resultado.
     * @param data        Información devuelta por la actividad invocada.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        ScrollView mScroll = findViewById(R.id.mMainScroll);
        mScroll.smoothScrollTo(0, 0);

        if (data != null && data.hasExtra(ExtrasKey.OP_ACTIVITY))
            if (ActivitiesOperation.valueOf(data.getStringExtra(ExtrasKey.OP_ACTIVITY))
                    == ActivitiesOperation.SEND_PAYMENT)
                Helper.showSnackbar(findViewById(R.id.mSendFab),
                        String.format(getString(R.string.sent_payment_text), coinToStringFriendly(
                                SupportedAssets.valueOf(data.getStringExtra(ExtrasKey.SELECTED_COIN)),
                                data.getLongExtra(ExtrasKey.TX_VALUE, 0)
                        )));
            else super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Obtiene una cadena que representa la cantidad especificada en la fracción más pequeña de la
     * moneda.
     *
     * @param assets Activo seleccionado.
     * @param amount Cantidad a representar.
     * @return Una cadena que muestra la cantidad a representar.
     */
    private String coinToStringFriendly(SupportedAssets assets, long amount) {
        switch (assets) {
            case BTC:
                return Coin.valueOf(amount).toFriendlyString();
        }

        return String.format("(Moneda no disponible: %s)", amount);
    }

    /**
     * Acción que muestra la actividad del historial de transacciones.
     *
     * @param view Componente que desencadena el evento Click.
     */
    public void handlerShowHistory(View view) {
        Intent intent = new Intent(this, TransactionHistoryActivity.class);
        startActivityForResult(intent, 0);
    }
}
