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

package com.cryptowallet.assets.bitcoin.services;

import android.content.ContextWrapper;

import com.cryptowallet.assets.bitcoin.wallet.Transaction;
import com.cryptowallet.assets.bitcoin.wallet.Wallet;
import com.cryptowallet.wallet.ChainTipInfo;
import com.cryptowallet.wallet.ITransaction;
import com.google.common.util.concurrent.ListenableFutureTask;

import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.WalletTransaction;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Prueba las funciones del proveedor de datos para billeteras de Bitcoin.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class BitcoinProviderTest {

    /**
     * Proveedor de billetera.
     */
    private BitcoinProvider mProvider;

    /**
     * Ejecutor de resultados.
     */
    private Executor mExecutor;

    /**
     * Configuración inicial.
     */
    @Before
    public void setUp() {
        mProvider = BitcoinProvider.get(new Wallet(new ContextWrapper(null)));
        mExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Prueba de solicitud de historial de una dirección serializada.
     */
    @Test
    public void getHistorialByAddress() throws ExecutionException, InterruptedException {
        final byte[] address = Hex.decode("6ff022a844844d252781139cf40113760e6361688a");
        final Runnable onSuccess = mock(Runnable.class);
        final ListenableFutureTask<List<WalletTransaction>> task
                = mProvider.getHistoryByAddress(address);

        task.addListener(onSuccess, mExecutor);

        verify(onSuccess, timeout(5000)).run();

        assertNotNull(task.get());
        assertNotEquals(0, task.get().size());
    }

    /**
     * Prueba de solicitud de una transacción.
     */
    @Test
    public void getTx() throws ExecutionException, InterruptedException {
        final byte[] txid = Hex
                .decode("3d82b2b0e145a676887a19f29e02fc9cf238a578c690ab3cfd5b3844e7481db2");
        final Runnable onSuccess = mock(Runnable.class);
        final ListenableFutureTask<WalletTransaction> task = mProvider.getTransactionByTxID(txid);

        task.addListener(onSuccess, mExecutor);

        verify(onSuccess, timeout(5000)).run();

        assertNotNull(task.get());
        assertEquals("b21d48e744385bfd3cab90c678a538f29cfc029ef2197a8876a645e1b0b2823d",
                task.get().getTransaction().getTxId().toString());
    }

    /**
     * Prueba de solicitud de la información de la blockchain.
     */
    @Test
    public void getChaininfo() throws ExecutionException, InterruptedException {
        final Runnable onSuccess = mock(Runnable.class);
        final ListenableFutureTask<ChainTipInfo> task = mProvider.getChainTipInfo();

        task.addListener(onSuccess, mExecutor);

        verify(onSuccess, timeout(5000)).run();

        assertNotNull(task.get());
        assertEquals(TestNet3Params.get().getId(), task.get().getNetwork().getId());
    }

    /**
     * Prueba de solicitud de las transacciones de dependencias.
     */
    @Test
    public void getTxDependencies() throws ExecutionException, InterruptedException {
        final byte[] txid = Hex
                .decode("3d82b2b0e145a676887a19f29e02fc9cf238a578c690ab3cfd5b3844e7481db2");
        final Runnable onSuccess = mock(Runnable.class);
        final ListenableFutureTask<Map<String, ITransaction>> task = mProvider.getDependencies(txid);

        task.addListener(onSuccess, mExecutor);

        verify(onSuccess, timeout(5000)).run();

        assertNotNull(task.get());
        assertEquals(1, task.get().size());
    }

    /**
     * Prueba para obtener la comisión de una transacción con sus dependencias completas.
     */
    @Test
    public void getFeeAndFromAddress() throws ExecutionException, InterruptedException {
        final byte[] txid = Hex
                .decode("3d82b2b0e145a676887a19f29e02fc9cf238a578c690ab3cfd5b3844e7481db2");
        final Runnable onSuccess = mock(Runnable.class);
        final ListenableFutureTask<WalletTransaction> task = mProvider.getTransactionByTxID(txid);

        task.addListener(onSuccess, mExecutor);

        verify(onSuccess, timeout(5000)).run();
        assertNotNull(task.get());
        assertEquals("b21d48e744385bfd3cab90c678a538f29cfc029ef2197a8876a645e1b0b2823d",
                task.get().getTransaction().getTxId().toString());

        final ListenableFutureTask<Map<String, ITransaction>> task2 = mProvider.getDependencies(txid);

        task2.addListener(onSuccess, mExecutor);

        verify(onSuccess, timeout(5000)).run();
        assertNotNull(task2.get());
        assertEquals(1, task2.get().size());

        assertTrue(task.get().getTransaction() instanceof Transaction);

        final Transaction tx = (Transaction) task.get().getTransaction();
        tx.fillDependencies(task2.get());

        assertEquals(0, tx.getNetworkFee());
        assertArrayEquals(new String[]{"mj2tZKFhiLkxWz6eJpubj5Y5SiGrmBrgtm"},
                tx.getFromAddress().toArray());
    }
}
