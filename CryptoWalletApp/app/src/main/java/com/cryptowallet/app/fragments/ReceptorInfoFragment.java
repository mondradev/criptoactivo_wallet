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

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import com.cryptowallet.BuildConfig;
import com.cryptowallet.Constants;
import com.cryptowallet.R;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.AbstractWallet;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.graphics.Bitmap.Config.ARGB_8888;

/**
 * Este fragmento provee de un cuadro de dialogo inferior que permite visualizar los datos para
 * aceptar pagos de otras billeteras.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class ReceptorInfoFragment extends BottomSheetDialogFragment {

    /**
     * Proveedor de archivos.
     */
    private static final String FILE_PROVIDER
            = String.format("%s.provider", BuildConfig.APPLICATION_ID);

    /**
     * Tamaño del código QR.
     */
    private static final int QR_CODE_SIZE = 250;

    /**
     * TAG del fragmento.
     */
    private static final String TAG_FRAGMENT = "ReceptorInfoFragment";


    /**
     * Nombre del archivo temporal del código QR.
     */
    private static final String FILENAME_QR_TEMP = "qrcode_";

    /**
     * Extensión del archivo temporal del código QR.
     */
    private static final String EXTENSION_QR_TEMP = ".jpeg";

    /**
     * Muestra un cuadro de diálogo inferior con los datos de recepción de la billetera.
     *
     * @param activity Actividad que invoca.
     * @param wallet   Instancia de la billetera.
     */
    static void show(@NonNull FragmentActivity activity, @NonNull AbstractWallet wallet) {
        final ReceptorInfoFragment fragment = new ReceptorInfoFragment();
        final Bundle parameters = new Bundle();

        parameters.putString(Constants.EXTRA_ADDRESS, wallet.getCurrentPublicAddress());
        parameters.putString(Constants.EXTRA_RECEIVER_URI, wallet.generateUri().toString());
        parameters.putInt(Constants.EXTRA_ICON_RES_ID, wallet.getIcon());
        fragment.setArguments(parameters);
        fragment.show(activity.getSupportFragmentManager(), TAG_FRAGMENT);
    }


    /**
     * Este método es llamado se crea una nueva instancia de la vista del fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.bsd_receptor_info, container, false);

        if (root == null)
            throw new UnsupportedOperationException();

        final Bundle arguments = requireArguments();

        root.findViewById(R.id.mReceInfoShareButton).setOnClickListener(this::onPressedButton);
        root.findViewById(R.id.mReceInfoCopyButton).setOnClickListener(this::onPressedButton);

        ((TextView) root.findViewById(R.id.mReceInfoAddress))
                .setText(arguments.getString(Constants.EXTRA_ADDRESS));
        ((ImageView) root.findViewById(R.id.mReceInfoQrCode)).setImageBitmap(
                Utils.getQrCode(arguments.getString(Constants.EXTRA_RECEIVER_URI), QR_CODE_SIZE));
        ((ImageView) root.findViewById(R.id.mSendIcon))
                .setImageResource(arguments.getInt(Constants.EXTRA_ICON_RES_ID));

        return root;
    }

    /**
     * Este método es llamado cuando un botón del fragmento es presionado.
     *
     * @param view Botón que es presionado.
     */
    private void onPressedButton(View view) {
        switch (view.getId()) {
            case R.id.mReceInfoCopyButton:
                ClipboardManager clipboard = (ClipboardManager) requireActivity()
                        .getSystemService(CLIPBOARD_SERVICE);

                Objects.requireNonNull(clipboard);

                ClipData data = ClipData.newPlainText(getString(R.string.address_title),
                        requireArguments().getString(Constants.EXTRA_ADDRESS));
                clipboard.setPrimaryClip(data);

                Snackbar.make(
                        requireView(),
                        R.string.address_copy_to_clipboard_text,
                        Snackbar.LENGTH_SHORT
                ).setAnchorView(requireView())
                        .show();

                break;

            case R.id.mReceInfoShareButton:
                Bitmap qrCode = createBitmap(R.id.mReceInfoQrCode);
                Bitmap assetIcon = createBitmap(R.id.mSendIcon);

                Bitmap bitmap = Bitmap.createBitmap(
                        qrCode.getWidth(),
                        qrCode.getHeight(),
                        qrCode.getConfig()
                );

                Canvas c = new Canvas(bitmap);

                c.drawBitmap(qrCode, 0, 0, null);
                c.drawBitmap(assetIcon,
                        (qrCode.getWidth() - assetIcon.getWidth()) / 2f,
                        (qrCode.getHeight() - assetIcon.getHeight()) / 2f,
                        null);

                shareQrImage(saveImage(bitmap));

                break;
        }
    }

    /**
     * Este método es llamado se crea una nueva instancia del diálogo.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final BottomSheetDialog sheetDialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        sheetDialog.setOnShowListener(dialog -> ((BottomSheetDialog) dialog).getBehavior()
                .setState(BottomSheetBehavior.STATE_EXPANDED));

        return sheetDialog;
    }

    /**
     * Crea un mapa de bits a partir de la vista especificada.
     *
     * @param id Identificador de la vista.
     * @return Mapa de bits de la vista.
     */
    private @NonNull
    Bitmap createBitmap(int id) {
        View view = requireView().findViewById(id);
        Bitmap bitmap = Bitmap.createBitmap(
                view.getMeasuredWidth(),
                view.getMeasuredHeight(),
                ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }

    /**
     * Guarda el código QR para ser compartido.
     *
     * @param qrImage Mapa de bits del código QR.
     */
    private File saveImage(Bitmap qrImage) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            qrImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            File qrFile = File.createTempFile(FILENAME_QR_TEMP, EXTENSION_QR_TEMP,
                    requireContext().getExternalCacheDir());

            FileOutputStream fileStream = new FileOutputStream(qrFile);
            fileStream.write(stream.toByteArray());

            return qrFile;
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Comparte el código QR.
     */
    private void shareQrImage(File qrFile) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                FILE_PROVIDER,
                qrFile);

        Intent sendIntent = new Intent()
                .setAction(Intent.ACTION_SEND)
                .setType("image/jpeg")
                .putExtra(Intent.EXTRA_TEXT, requireArguments().getString(Constants.EXTRA_ADDRESS))
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_title)));
    }


}
