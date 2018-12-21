package com.cryptowallet.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.BlockchainStatus;
import com.cryptowallet.wallet.IWalletListener;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletServiceBase;
import com.cryptowallet.wallet.coinmarket.ExchangeService;
import com.cryptowallet.wallet.widgets.GenericTransactionBase;
import com.cryptowallet.wallet.widgets.adapters.RecentListAdapter;

import org.bitcoinj.core.Coin;

import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * Actividad principal de la billetera. Desde esta actividad se establece el flujo de trabajo de
 * toda la aplicación.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class WalletAppActivity extends ActivityBase {

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
     * Botón hambuerguesa.
     */
    private ActionBarDrawerToggle mToggle;

    /**
     * Temporizador de la tarjeta de actualización de la blockchain.
     */
    private CountDownTimer mBlockCountDown
            = new CountDownTimer(5000, 1) {

        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            handlerHideCard();
        }

    };

    /**
     * Handler que ejecuta las funciones en el hilo principal
     */
    private Handler mHandler = new Handler();

    /**
     * Escuchas del servicio de Exchange.
     */
    private ExchangeService.IListener mExchangeListener
            = new ExchangeService.IListener() {
        /**
         * Este método se ejecuta cuando el precio de un activo es actualizado.
         *
         * @param asset        Activo que fue actualizado.
         * @param smallestUnit Precio del activo expresado en su unidad más pequeña.
         */
        @Override
        public void onChangePrice(SupportedAssets asset, long smallestUnit) {

            SupportedAssets currentAsset = SupportedAssets.valueOf(
                    AppPreference.getSelectedCurrency(WalletAppActivity.this));

            if (asset != currentAsset || !BitcoinService.isRunning())
                return;

            final String balanceFiat = ExchangeService.get()
                    .getBtcPrice(SupportedAssets.BTC, BitcoinService.get().getBalance());

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    TextView mUsdBalance = findViewById(R.id.mBalanceFiat);
                    mUsdBalance.setText(balanceFiat);
                }
            });
        }

    };

    /**
     * Escucha de los eventos de la billetera de bitcoin.
     */
    private IWalletListener mListener = new IWalletListener() {

        /**
         * India si la descarga ha iniciado.
         */
        private boolean mInitDownload = false;

        /**
         * Indica si el cifrado está en ejecución.
         */
        private boolean mEncrypting = false;

        /**
         * Este método se ejecuta cuando la billetera recibe una transacción.
         *
         * @param service Información de la billetera que desencadena el evento.
         * @param tx      Transacción recibida.
         */
        @Override
        public void onReceived(WalletServiceBase service, GenericTransactionBase tx) {
            mRecentsAdapter.add(tx);
        }

        /**
         * Este método se ejecuta cuando la billetera envía una transacción.
         *
         * @param service Información de la billetera que desencadena el evento.
         * @param tx      Transacción enviada.
         */
        @Override
        public void onSent(WalletServiceBase service, GenericTransactionBase tx) {
            mRecentsAdapter.add(tx);
        }

        /**
         * Este método se ejecuta cuando una transacción es confirmada.
         *
         * @param service Información de la billetera que desencadena el evento.
         * @param tx      Transacción que fue confirmada.
         */
        @Override
        public void onCommited(WalletServiceBase service, GenericTransactionBase tx) {
            if (tx.getDepth() == 1)
                onBalanceChanged(service, 0);
        }

        /**
         * Este método se ejecuta cuando la billetera sufre un cambio.
         *
         * @param service Información de la billetera que desencadena el evento.
         */
        @Override
        public void onWalletChanged(WalletServiceBase service) {

        }

        /**
         * Este método se ejecuta cuando el saldo de la billetera ha cambiado.
         *
         * @param service Información de la billetera que desencadena el evento.
         * @param ignored Balance nuevo en la unidad más pequeña de la moneda o token.
         */
        @Override
        public void onBalanceChanged(WalletServiceBase service, long ignored) {
            long value = service.getBalance();
            TextView mUsdBalance = findViewById(R.id.mBalanceFiat);

            SupportedAssets assets = SupportedAssets.valueOf(
                    AppPreference.getSelectedCurrency(WalletAppActivity.this));

            String balanceFiat
                    = ExchangeService.get().getExchange(assets)
                    .ToStringFriendly(SupportedAssets.BTC, value);

            mBalanceText.setText(service.getFormatter().format(value));
            mUsdBalance.setText(balanceFiat);
        }

        /**
         * Este método se ejecuta cuando la billetera está inicializada correctamente.
         *
         * @param service Información de la billetera que desencadena el evento.
         */
        @Override
        public void onReady(WalletServiceBase service) {
            onBalanceChanged(service, service.getBalance());
            ExchangeService.get().addEventListener(mExchangeListener);

            Intent intent = WalletAppActivity.this.getIntent();

            if (intent.hasExtra(ExtrasKey.PIN_DATA)) {
                final byte[] key = intent.getByteArrayExtra(ExtrasKey.PIN_DATA);

                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mEncrypting = true;

                        BitcoinService.get().encryptWallet(key, null);

                        if (mDialogOnLoad != null && mDialogOnLoad.isShowing() && mEncrypting)
                            mDialogOnLoad.dismiss();
                    }
                });
            }
        }

        /**
         * Este método se ejecuta cuando la blockchain de la billetera ha sido descargada
         * completamente.
         *
         * @param service Información de la billetera que desencadena el evento.
         */
        @Override
        public void onCompletedDownloaded(WalletServiceBase service) {
            if (!mInitDownload)
                return;

            Utils.sendNotificationOs(
                    WalletAppActivity.this,
                    getString(R.string.app_name),
                    getString(R.string.blockchain_downloaded),
                    null,
                    100001
            );

            final CardView mStatus = findViewById(R.id.mBlockchainDownloadCard);

            mStatus.post(new Runnable() {
                @Override
                public void run() {
                    mStatus.setVisibility(View.GONE);
                    mInitDownload = false;
                }
            });
        }

        /**
         * Este método se ejecuta cuando se descarga un bloque nuevo.
         *
         * @param service Información de la billetera que desencadena el evento.
         * @param status  Estado actual de la blockchain.
         */
        @Override
        public void onBlocksDownloaded(WalletServiceBase service, final BlockchainStatus status) {
            final TextView mLastBlock = findViewById(R.id.mLastBlock);
            final CardView mStatus = findViewById(R.id.mBlockchainDownloadCard);
            mBlockCountDown.cancel();

            mStatus.post(new Runnable() {
                @Override
                public void run() {
                    if (mStatus.getVisibility() == View.GONE)
                        mStatus.setVisibility(View.VISIBLE);

                    mLastBlock.setText(String.format(getString(R.string.last_block_date_text),
                            status.getLeftBlocks()));

                    if (status.getLeftBlocks() == 0)
                        mStatus.setVisibility(View.GONE);
                }
            });
        }

        /**
         * Este método se ejecuta al comienzo de la descarga de los bloques nuevos.
         *
         * @param service Información de la billetera que desencadena el evento.
         * @param status  Estado actual de la blockchain.
         */
        @Override
        public void onStartDownload(WalletServiceBase service, BlockchainStatus status) {
            final CardView mStatus = findViewById(R.id.mBlockchainDownloadCard);

            mStatus.post(new Runnable() {
                @Override
                public void run() {
                    mBlockCountDown.start();
                    mInitDownload = true;

                    mStatus.setVisibility(View.VISIBLE);

                    TextView mLastBlock = findViewById(R.id.mLastBlock);
                    mLastBlock.setText(getString(R.string.calculate_blocks));
                }
            });
        }

        /**
         * Este método se ejecuta cuando la propagación es completada.
         *
         * @param service Información de la billetera que desencadena el evento.
         * @param tx      Transacción que ha sido propagada.
         */
        @Override
        public void onPropagated(WalletServiceBase service, GenericTransactionBase tx) {

        }

        /**
         * Este método es llamado cuando se lanza una excepción dentro de la billetera.
         *
         * @param service   Información de la billetera que desencadena el evento.
         * @param exception Excepción que causa el evento.
         */
        @Override
        public void onException(WalletServiceBase service, Exception exception) {

        }

        /**
         * Este método es llamado cuando la billetera a iniciado.
         *
         * @param service Información de la billetera que desencadena el evento.
         */
        @Override
        public void onStarted(WalletServiceBase service) {
            if (mDialogOnLoad != null && mDialogOnLoad.isShowing() && !mEncrypting)
                mDialogOnLoad.dismiss();
        }
    };

    /**
     * Este método es llamado cuando el temporizador finaliza.
     */
    public void handlerHideCard() {
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

        Objects.requireNonNull(getSupportActionBar())
                .setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar())
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

                    case R.id.mAddressViewer:
                        handlerShowAddressesActivity();
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

        final SwipeRefreshLayout mSwipeRefreshData = findViewById(R.id.mSwipeRefreshData);

        mSwipeRefreshData.setColorSchemeColors(
                Utils.getColorFromTheme(this, R.attr.textIconsColor));

        mSwipeRefreshData.setProgressBackgroundColorSchemeColor(
                Utils.getColorFromTheme(this, R.attr.colorAccent));

        mSwipeRefreshData.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                BitcoinService.notifyOnBalanceChange();
                ExchangeService.get().reloadMarketPrice();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecentsAdapter.clear();
                        mRecentsAdapter.addAll(BitcoinService.get().getTransactionsByTime());
                        mSwipeRefreshData.setRefreshing(false);
                    }
                });
            }
        });

        BitcoinService.addEventListener(mListener);

        if (!BitcoinService.isRunning()) {

            mDialogOnLoad = new AlertDialog.Builder(this)
                    .setTitle(R.string.loading_title_text)
                    .setMessage(R.string.loading_text)
                    .setCancelable(false)
                    .create();

            mDialogOnLoad.show();
        }

    }

    /**
     * Muestra la actividad que muestra las direcciones de recepción de la billetera.
     */
    private void handlerShowAddressesActivity() {
        Intent intent = new Intent(this, AddressViewerActivity.class);
        startActivityForResult(intent, 0);
    }

    /**
     * Muestra la actividad de elimanar billetera.
     */
    private void handlerShowDropWalletActivity() {
        Intent intent = new Intent(this, DeleteWalletActivity.class);
        startActivity(intent);
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
        ExchangeService.get().removeEventListener(mExchangeListener);
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
                Utils.showSnackbar(findViewById(R.id.mSendFab),
                        String.format(getString(R.string.sent_payment_text), coinToStringFriendly(
                                SupportedAssets.valueOf(data.getStringExtra(ExtrasKey.SELECTED_COIN)),
                                data.getLongExtra(ExtrasKey.TX_ID, 0)
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
