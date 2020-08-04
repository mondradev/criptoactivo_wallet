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

package com.cryptowallet.app.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import com.cryptowallet.BuildConfig;
import com.cryptowallet.Constants;
import com.cryptowallet.R;
import com.cryptowallet.app.Preferences;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.SupportedAssets;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.common.base.Joiner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.graphics.Bitmap.Config.ARGB_8888;

/**
 * Este fragmento provee de un cuadro de dialogo inferior que permite visualizar los datos de la
 * transacción realizada.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class SuccessfulPaymentFragment extends BottomSheetDialogFragment {

    /**
     * Proveedor de archivos.
     */
    private static final String FILE_PROVIDER
            = String.format("%s.provider", BuildConfig.APPLICATION_ID);

    /**
     * TAG del fragmento.
     */
    private static final String TAG_FRAGMENT = "SuccessfulPaymentFragment";

    /**
     * Nombre del archivo temporal de la prueba de pago.
     */
    private static final String FILENAME_PROOF_TEMP = "proof_";

    /**
     * Extensión del archivo temporal de la prueba de pago.
     */
    private static final String EXTENSION_PROOF_TEMP = ".jpeg";

    /**
     * Muestra un cuadro de diálogo inferior con los datos de la transacción enviada.
     *
     * @param activity Actividad que invoca.
     * @param tx       Transacción completada.
     */
    public static void show(@NonNull FragmentActivity activity,
                            @NonNull ITransaction tx, long lastPrice) {
        final Bundle parameters = new Bundle();
        final SupportedAssets fiat = Preferences.get().getFiat();
        final long fiatAmount
                = Utils.cryptoToFiat(tx.getAmount(), tx.getCryptoAsset(), lastPrice, fiat);

        parameters.putString(Constants.EXTRA_ASSET, tx.getCryptoAsset().name());
        parameters.putString(Constants.EXTRA_TXID, tx.getID());
        parameters.putLong(Constants.EXTRA_AMOUNT, tx.getAmount());
        parameters.putLong(Constants.EXTRA_FIAT_AMOUNT, fiatAmount);
        parameters.putLong(Constants.EXTRA_FEE, tx.getFee());
        parameters.putStringArray(Constants.EXTRA_TO_ADDRESSES,
                tx.getToAddress().toArray(new String[0]));

        SuccessfulPaymentFragment fragment = new SuccessfulPaymentFragment();
        fragment.setArguments(parameters);
        fragment.show(activity.getSupportFragmentManager(), TAG_FRAGMENT);
    }

    /**
     * Este método es llamado se crea una nueva instancia de la vista del fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.bsd_successful_payment, container, false);

        if (root == null)
            throw new UnsupportedOperationException();

        final Bundle arguments = requireArguments();
        final SupportedAssets asset = Enum.valueOf(SupportedAssets.class,
                arguments.getString(Constants.EXTRA_ASSET, SupportedAssets.BTC.name()));

        String[] toAddresses = arguments.getStringArray(Constants.EXTRA_TO_ADDRESSES);
        toAddresses = toAddresses == null ? new String[]{} : toAddresses;

        root.<TextView>findViewById(R.id.mSuPayId).setText(arguments.getString(Constants.EXTRA_TXID));
        root.<TextView>findViewById(R.id.mSuPayFee)
                .setText(asset.toStringFriendly(arguments.getLong(Constants.EXTRA_FEE)));
        root.<TextView>findViewById(R.id.mSuPayTo).setText(Joiner.on("\n").join(toAddresses));

        root.<TextView>findViewById(R.id.mSuPayMessage)
                .setText(getString(R.string.successful_payment_pattern, toFriendlyString(
                        arguments.getLong(Constants.EXTRA_AMOUNT),
                        arguments.getLong(Constants.EXTRA_FIAT_AMOUNT),
                        asset
                )));

        root.findViewById(R.id.mSuPayShareButton).setOnClickListener(this::onPressedButton);

        return root;
    }

    /**
     * Este método es llamado cuando el cuadro de diálogo es finalizado.
     *
     * @param dialog Funciones de eventos del cuadro de diálogo.
     */
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        requireActivity().finish();
    }

    /**
     * Obtiene una cadena del monto enviado en la transacción que puede ser leído de una forma más sencilla.
     *
     * @param amount     Monto en cripto.
     * @param fiatAmount Monto en fiat.
     * @param asset      Activo de la transacción.
     * @return Cadena que representa el monto enviado.
     */
    private String toFriendlyString(long amount, long fiatAmount, SupportedAssets asset) {
        SupportedAssets fiat = Preferences.get().getFiat();

        return String.format("%s (%s)",
                asset.toStringFriendly(amount),
                fiat.toStringFriendly(fiatAmount));
    }

    /**
     * Este método es llamado cuando un botón del fragmento es presionado.
     *
     * @param view Botón que es presionado.
     */
    private void onPressedButton(View view) {
        Bitmap screenshot = createBitmap();
        shareImage(saveImage(screenshot));
    }

    /**
     * Crea un mapa de bits a partir de la vista raiz del fragmento.
     *
     * @return Mapa de bits de la vista raiz.
     */
    private @NonNull
    Bitmap createBitmap() {
        View view = requireView();
        Bitmap bitmap = Bitmap.createBitmap(
                view.getMeasuredWidth(),
                view.getMeasuredHeight(),
                ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Utils.resolveColor(requireContext(), R.attr.colorSurface));
        view.draw(canvas);

        return bitmap;
    }

    /**
     * Guarda la imagen especificada en una archivo temporal.
     *
     * @param image Mapa de bits de la imagen a guardar.
     */
    private File saveImage(Bitmap image) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            File tempFile = File.createTempFile(FILENAME_PROOF_TEMP, EXTENSION_PROOF_TEMP,
                    requireContext().getExternalCacheDir());

            FileOutputStream fileStream = new FileOutputStream(tempFile);
            fileStream.write(stream.toByteArray());

            return tempFile;
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Comparte la imagen especificada.
     */
    private void shareImage(File imageFile) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                FILE_PROVIDER,
                imageFile);

        Intent sendIntent = new Intent()
                .setAction(Intent.ACTION_SEND)
                .setType("image/jpeg")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_successful_payment_title)));
    }
}
