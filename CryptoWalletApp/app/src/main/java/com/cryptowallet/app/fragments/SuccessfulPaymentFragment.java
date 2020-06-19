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
     * Clave del parametro activo.
     */
    private static final String ASSET_EXTRA
            = String.format("%s.AssetKey", SuccessfulPaymentFragment.class.getName());

    /**
     * Clave del parametro identificador de transacción.
     */
    private static final String TXID_EXTRA
            = String.format("%s.TxIDKey", SuccessfulPaymentFragment.class.getName());

    /**
     * Clave del parametro monto de la transacción.
     */
    private static final String AMOUNT_EXTRA
            = String.format("%s.AmountKey", SuccessfulPaymentFragment.class.getName());

    /**
     * Clave del parametro monto en fiat de la transacción.
     */
    private static final String FIAT_AMOUNT_EXTRA
            = String.format("%s.FiatAmountKey", SuccessfulPaymentFragment.class.getName());

    /**
     * Clave del parametro comisión de la transacción.
     */
    private static final String FEE_EXTRA
            = String.format("%s.FeeKey", SuccessfulPaymentFragment.class.getName());

    /**
     * Clave del parametro destino de la transacción.
     */
    private static final String TO_ADDRESSES_EXTRA
            = String.format("%s.ToAddressKey", SuccessfulPaymentFragment.class.getName());

    /**
     * Clave del parametro tamaño de la transacción.
     */
    private static final String SIZE_EXTRA
            = String.format("%s.SizeKey", SuccessfulPaymentFragment.class.getName());

    /**
     * Muestra un cuadro de diálogo inferior con los datos de la transacción enviada.
     *
     * @param activity    Actividad que invoca.
     * @param transaction Transacción completada.
     */
    public static void show(@NonNull FragmentActivity activity,
                            @NonNull ITransaction transaction) {
        Bundle parameters = new Bundle();

        parameters.putString(ASSET_EXTRA, transaction.getCriptoAsset().name());
        parameters.putString(TXID_EXTRA, transaction.getID());
        parameters.putDouble(AMOUNT_EXTRA, transaction.getAmount());
        parameters.putDouble(FIAT_AMOUNT_EXTRA, transaction.getFiatAmount());
        parameters.putDouble(FEE_EXTRA, transaction.getFee());
        parameters.putLong(SIZE_EXTRA, transaction.getSize());
        parameters.putStringArray(TO_ADDRESSES_EXTRA,
                transaction.getToAddress().toArray(new String[0]));

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
                arguments.getString(ASSET_EXTRA, SupportedAssets.BTC.name()));

        String[] toAddresses = arguments.getStringArray(TO_ADDRESSES_EXTRA);
        toAddresses = toAddresses == null ? new String[]{} : toAddresses;

        root.<TextView>findViewById(R.id.mSuPayId).setText(arguments.getString(TXID_EXTRA));
        root.<TextView>findViewById(R.id.mSuPayFee)
                .setText(asset.toStringFriendly(arguments.getDouble(FEE_EXTRA)));
        root.<TextView>findViewById(R.id.mSuPayTo).setText(Joiner.on("\n").join(toAddresses));
        root.<TextView>findViewById(R.id.mSuPaySize)
                .setText(Utils.toSizeFriendlyString(arguments.getLong(SIZE_EXTRA)));

        root.<TextView>findViewById(R.id.mSuPayMessage)
                .setText(getString(R.string.successful_payment_pattern, toFriendlyString(
                        arguments.getDouble(AMOUNT_EXTRA),
                        arguments.getDouble(FIAT_AMOUNT_EXTRA),
                        asset
                )));

        root.findViewById(R.id.mSuPayShareButton).setOnClickListener(this::onPressedButton);

        return root;
    }

    /**
     * Obtiene una cadena del monto enviado en la transacción que puede ser leído de una forma más sencilla.
     *
     * @param amount     Monto en cripto.
     * @param fiatAmount Monto en fiat.
     * @param asset      Activo de la transacción.
     * @return Cadena que representa el monto enviado.
     */
    private String toFriendlyString(double amount, double fiatAmount, SupportedAssets asset) {
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
