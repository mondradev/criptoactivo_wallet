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

package com.cryptowallet.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.BlockchainStatus;
import com.cryptowallet.wallet.IWalletListener;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletListenerBase;
import com.cryptowallet.wallet.WalletServiceBase;
import com.cryptowallet.wallet.coinmarket.ExchangeService;
import com.cryptowallet.wallet.coinmarket.coins.CoinBase;
import com.cryptowallet.wallet.coinmarket.coins.CoinFactory;
import com.cryptowallet.wallet.widgets.GenericTransactionBase;
import com.cryptowallet.wallet.widgets.adapters.RecentListAdapter;

import org.bitcoinj.core.Coin;

import java.util.Objects;
import java.util.concurrent.Executors;

import com.cryptowallet.utils.NamedRunnable;

/**
 * Actividad principal de la billetera. Desde esta actividad se establece el flujo de trabajo de
 * toda la aplicación.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class WalletAppActivity extends ActivityBase {

    /**
     * Etiqueta de la actividad.
     */
    private static final String TAG = "WalletApp";

    /**
     * Indica si se puede actualizar los precios.
     */
    private boolean mCanUpdate = false;

    /**
     * TextView que visualiza el saldo de la billetera.
     */
    private TextView mBalanceText;

    /**
     * Es un cuadro de diálogo que permite congelar la actividad hasta que la billetera se inicia.
     */
    private AuthenticateDialog mDialogOnLoad;

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
     * Escuchas del servicio de Exchange.
     */
    private ExchangeService.IListener mExchangeListener
            = new ExchangeService.IListener() {
        /**
         * Este método se ejecuta cuando el precio de un activo es actualizado.
         *
         * @param asset        Activo que fue actualizado.
         * @param price Precio del activo expresado en su unidad más pequeña.
         */
        @Override
        public void onUpdatePrice(SupportedAssets asset, CoinBase price) {

            Log.d(TAG, "Actualizando precio de " + asset.name() + ": "
                    + price.toStringFriendly());

            SupportedAssets currentAsset =
                    AppPreference.getSelectedCurrency(WalletAppActivity.this);

            if (asset != currentAsset || !WalletServiceBase.isRunning(SupportedAssets.BTC))
                return;

            if (mCanUpdate)
                BitcoinService.notifyOnBalanceChange();
        }

    };
    /**
     * La billetera fue autenticada.
     */
    private boolean mAuthenticated;

    /**
     * Activo seleccionado.
     */
    private SupportedAssets mSelectedAsset = SupportedAssets.BTC;
    /**
     * Escucha de los eventos de la billetera de bitcoin.
     */
    private IWalletListener mListener = new WalletListenerBase() {

        /**
         * India si la descarga ha iniciado.
         */
        private boolean mInitDownload = false;

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
                onBalanceChanged(service, CoinFactory.getZero(service.getAsset()));
        }

        /**
         * Este método se ejecuta cuando el saldo de la billetera ha cambiado.
         *
         * @param service Información de la billetera que desencadena el evento.
         * @param ignored Balance nuevo en la unidad más pequeña de la moneda o token.
         */
        @Override
        public void onBalanceChanged(final WalletServiceBase service, CoinBase ignored) {
            if (!mCanUpdate)
                return;

            final CoinBase value = service.getBalance();
            final TextView mUsdBalance = findViewById(R.id.mBalanceFiat);

            SupportedAssets assets =
                    AppPreference.getSelectedCurrency(WalletAppActivity.this);

            final String balanceFiat
                    = ExchangeService.get().getExchange(SupportedAssets.BTC)
                    .convertTo(assets, value).toStringFriendly();

            runOnUiThread(() -> mBalanceText.setText(value.toStringFriendly()));

            runOnUiThread(() -> mUsdBalance.setText(balanceFiat));

        }

        /**
         * Este método se ejecuta cuando la billetera está inicializada correctamente.
         *
         * @param service Información de la billetera que desencadena el evento.
         */
        @Override
        public void onReady(WalletServiceBase service) {

            ExchangeService.get().addEventListener(mExchangeListener);

            if (mDialogOnLoad != null)
                if (mAuthenticated) {
                    Log.v(TAG, "Ocultando cuadro de diálogo, ya está autenticada la billetera.");
                    mAuthenticated = false;
                    mDialogOnLoad.dismiss();
                    showData();
                } else {
                    Log.v(TAG, "Mostrando cuadro de diálogo para autenticar al iniciar el " +
                            "servicio.");

                    mDialogOnLoad
                            .setMode(AuthenticateDialog.AUTH)
                            .setWallet(service)
                            .dismissOnAuth().setOnFail(() -> {
                        WalletAppActivity.this.moveTaskToBack(true);
                        finishAffinity();
                        System.exit(0);
                    })
                            .setOnCancel(() -> {
                                lockApp();
                                WalletAppActivity.this.moveTaskToBack(true);
                            })
                            .setOnDismiss(WalletAppActivity.this::showData)
                            .showUIAuth();
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

            runOnUiThread(() -> {
                mStatus.setVisibility(View.GONE);
                mInitDownload = false;
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

            runOnUiThread(() -> {
                if (status.getLeftBlocks() == 0) {
                    mStatus.setVisibility(View.GONE);
                    return;
                }

                mBlockCountDown.cancel();

                if (mStatus.getVisibility() == View.GONE)
                    mStatus.setVisibility(View.VISIBLE);

                mLastBlock.setText(
                        getString(R.string.last_block_date_text, status.getLeftBlocks()));
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
            final TextView mLastBlock = findViewById(R.id.mLastBlock);

            runOnUiThread(() -> {
                mBlockCountDown.start();
                mInitDownload = true;

                mStatus.setVisibility(View.VISIBLE);

                mLastBlock.setText(getString(R.string.calculate_blocks));
            });
        }

        /**
         * Este método es llamado cuando se lanza una excepción dentro de la billetera.
         *
         * @param service   Información de la billetera que desencadena el evento.
         * @param exception Excepción que causa el evento.
         */
        @Override
        public void onException(WalletServiceBase service, Exception exception) {
            Utils.showSnackbar(findViewById(R.id.mSendFab), exception.getMessage());

            if (Utils.isNull(service))
                System.exit(0);
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
        mLeftDrawer.setNavigationItemSelectedListener(item -> {
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

        mSwipeRefreshData.setOnRefreshListener(() -> Executors.newSingleThreadExecutor()
                .execute(new NamedRunnable(
                        "WalletAppActivity.SwipeRefreshLayout.OnRefresh") {
                    @Override
                    public void execute() {
                        BitcoinService.notifyOnBalanceChange();
                        ExchangeService.get().reloadMarketPrice();

                        mRecentsAdapter.setSource(BitcoinService.get()
                                .getRecentTransactions(5));

                        runOnUiThread(new NamedRunnable(
                                "WalletAppActivity.SwipeRefreshLayout") {
                            @Override
                            protected void execute() {
                                mSwipeRefreshData.setRefreshing(false);
                            }
                        });
                    }
                }));

        BitcoinService.addEventListener(mListener);

        mCanUpdate = false;
    }

    /**
     * Crea el cuadro de diálogo de autenticación.
     *
     * @return El cuadro de diálogo.
     */
    private AuthenticateDialog createAuthDialog() {

        if (!Utils.isNull(mDialogOnLoad) && mDialogOnLoad.isShowing())
            return null;

        Log.v(TAG, "Configurando un nuevo cuadro de autenticación.");

        return (mDialogOnLoad = new AuthenticateDialog())
                .setOnFail(() -> {
                    WalletAppActivity.this.moveTaskToBack(true);
                    finishAffinity();
                    System.exit(0);
                })
                .setOnCancel(() -> {
                    lockApp();
                    WalletAppActivity.this.moveTaskToBack(true);
                })
                .setOnDismiss(this::showData);
    }

    /**
     * Muestra la información de la billetera.
     */
    private void showData() {

        runOnUiThread(() -> {
            unlockApp();

            Log.v(TAG, "Visualizando la información sensible.");

            mCanUpdate = true;
            mRecentsAdapter.addAll(BitcoinService.get().getRecentTransactions(5));

            BitcoinService.notifyOnBalanceChange();

            if (ExchangeService.isInitialized())
                ExchangeService.get().reloadMarketPrice();

            ImageView qrCode = findViewById(R.id.mReceiveQR);
            String address = WalletServiceBase.get(mSelectedAsset).getReceiveAddress();

            qrCode.setImageBitmap(Utils.generateQrCode(Uri.parse(address), 100));
        });
    }

    /**
     * Oculta la información de la billetera.
     */
    private void hideData() {
        Log.v(TAG, "Ocultando la información sensible.");
        mCanUpdate = false;
        mRecentsAdapter.clear();

        ((TextView) findViewById(R.id.mBalanceText)).setText(R.string.hide_data);
        ((TextView) findViewById(R.id.mBalanceFiat)).setText(R.string.hide_data);


        ImageView qrCode = findViewById(R.id.mReceiveQR);
        qrCode.setImageBitmap(null);
        qrCode.setImageDrawable(getDrawable(R.mipmap.ic_launcher_round));
    }

    /**
     * Este método es llamado cuando se recupera la aplicación al haber cambiado a otra.
     */
    @Override
    protected void onResume() {
        hideData();

        super.onResume();

        // Ajustar algoritmo de bloqueo.

        boolean reqAuth = getIntent().getBooleanExtra(ExtrasKey.REQ_AUTH, false);

        mAuthenticated
                = getIntent().getBooleanExtra(ExtrasKey.AUTHENTICATED, false);

        getIntent().removeExtra(ExtrasKey.REQ_AUTH);
        getIntent().removeExtra(ExtrasKey.AUTHENTICATED);

        Log.v(TAG, "Billetera autenticada: " + (mAuthenticated && !reqAuth));

        boolean requireLock = isLockApp();
        boolean unlockWallet = !reqAuth && !requireLock
                && WalletServiceBase.isRunning(SupportedAssets.BTC);

        Log.d(TAG, "Billetera bloqueada: " + !unlockWallet);

        if (unlockWallet) {
            showData();
            return;
        }

        if (createAuthDialog() == null) {
            Log.d(TAG, "El cuadro de diálogo a sido mostrado");
            return;
        }

        if (!WalletServiceBase.isRunning(SupportedAssets.BTC))
            mDialogOnLoad.showUIProgress(getString(R.string.loading_text), this);
        else
            mDialogOnLoad.setMode(AuthenticateDialog.AUTH)
                    .dismissOnAuth()
                    .setWallet(BitcoinService.get())
                    .show(this);


        Log.v(TAG, "Billetera iniciada.");

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
        unlockApp();

        ScrollView mScroll = findViewById(R.id.mMainScroll);
        mScroll.smoothScrollTo(0, 0);

        if (data != null && data.hasExtra(ExtrasKey.OP_ACTIVITY))
            if (ActivitiesOperation.valueOf(data.getStringExtra(ExtrasKey.OP_ACTIVITY))
                    == ActivitiesOperation.SEND_PAYMENT)
                Utils.showSnackbar(findViewById(R.id.mSendFab),
                        String.format(getString(R.string.sent_payment_text), coinToStringFriendly(
                                SupportedAssets.valueOf(data.getStringExtra(ExtrasKey.SELECTED_COIN)),
                                data.getLongExtra(ExtrasKey.SEND_AMOUNT, 0)
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


    /**
     * Este método es llamado cuando el usuario deja la aplicación, lo que provoca que el cuadro de
     * autenticación sea ocultado.
     */
    @Override
    protected void onUserLeaveHint() {
        if (mCallActivity) {
            mCallActivity = false;
            return;
        }
        Log.d(TAG, "Abandonado la vista principal");

        if (!Utils.isNull(mDialogOnLoad) && mDialogOnLoad.isShowing())
            mDialogOnLoad.cancel();
        else
            super.onUserLeaveHint();

    }

    /**
     * Indica que se puede salir de la aplicación.
     */
    private boolean mCanExit;

    /**
     * Este método es llamado cuando el usuario presiona el botón hacia atrás, lo que provoca el
     * bloquedo de la aplicación.
     */
    @Override
    public void onBackPressed() {
        if (mCanExit) {
            super.onBackPressed();
            mCanExit = false;
        } else {
            Utils.showSnackbar(findViewById(R.id.mSendFab), getString(R.string.exit_indications));
            mCanExit = true;
            new Handler().postDelayed(() ->
                    mCanExit = false, 3000);
        }

    }

    public void handlerCopyToClipboard(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        ClipData data = ClipData.newPlainText(getString(R.string.request_payment_text),
                WalletServiceBase.get(mSelectedAsset).getReceiveAddress());

        clipboard.setPrimaryClip(data);

        Utils.showSnackbar(findViewById(R.id.mSendFab), getString(R.string.copy_to_clipboard_text));
    }
}
