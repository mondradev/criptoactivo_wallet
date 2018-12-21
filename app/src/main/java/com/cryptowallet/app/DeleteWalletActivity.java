package com.cryptowallet.app;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;

/**
 * Esta actividad permite el borrado de la billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class DeleteWalletActivity extends ActivityBase {

    /**
     * Cuadro de diálogo.
     */
    private AlertDialog mAlertDialog;

    /**
     * Tarea de borrado de la billetera.
     */
    private AsyncTask<DeleteWalletActivity, Void, DeleteWalletActivity> mDeleteWalletTask;

    /**
     * Este método es llamado cuando se crea la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
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

        mDeleteWalletTask = new DeleteTask();

    }

    /**
     * Este método es llamado cuando se hace clic en el botón de "Borrar".
     *
     * @param view Vista que llama a este método.
     */
    public void handlerDelete(View view) {
        mAlertDialog.show();
        mDeleteWalletTask.execute(this);
    }

    /**
     * Tarea que se encarga de borrar la billetera en segundo plano y nofica a la interfaz al
     * finalizar.
     *
     * @author Ing. Javier Flores
     * @version 1.1
     */
    private static class DeleteTask
            extends AsyncTask<DeleteWalletActivity, Void, DeleteWalletActivity> {

        /**
         * Este método es llamado para procesar la tarea en segundo plano.
         * <p/>
         * Se comienza la detención de los servicios de los activos y finaliza devolviendo la
         * instancia de la actividad.
         *
         * @param activities La instancia de la actividad.
         * @return La instancia de la actividad, que será utilizado para notificar la finalización
         * de la tarea.
         */
        @Override
        protected DeleteWalletActivity doInBackground(DeleteWalletActivity... activities) {
            DeleteWalletActivity self = activities[0];

            BitcoinService.get().deleteWallet();

            AppPreference.clear(self);

            return self;
        }

        /**
         * Este método es llamado cuando finaliza la tarea notificando al cuadro de diálogo que la
         * actividad que ya debe cerrarse.
         *
         * @param activity La actividad que contiene el cuadro de diálogo.
         */
        @Override
        protected void onPostExecute(DeleteWalletActivity activity) {
            if (activity.mAlertDialog.isShowing())
                activity.mAlertDialog.dismiss();

            Intent intent = new Intent(activity, InitWalletActivity.class);

            activity.startActivity(intent);
            activity.finish();
        }
    }
}
