package com.cryptowallet.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class DeleteWalletActivity extends ActivityBase {

    private AlertDialog mAlertDialog;
    private FutureTask<Void> mDeleteWalletTask = new FutureTask<>(new DeleteCallable());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_wallet);

        setTitle(R.string.drop_wallet);

        mAlertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.deleting_title)
                .setMessage(R.string.deleting_text)
                .setCancelable(false)
                .create();

    }

    public void handlerDelete(View view) {
        mAlertDialog.show();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(mDeleteWalletTask);
    }

    private class DeleteCallable implements Callable<Void> {

        @Override
        public Void call() throws Exception {
            BitcoinService.get().shutdown();
            BitcoinService.get().deleteWallet();

            AppPreference.enableTheme(DeleteWalletActivity.this);

            Intent intent = new Intent(DeleteWalletActivity.this,
                    InitWalletActivity.class);

            mAlertDialog.dismiss();

            startActivity(intent);
            finish();

            return null;
        }
    }
}
