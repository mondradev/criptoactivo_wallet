package com.cryptowallet.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.security.Security;
import com.cryptowallet.security.TimeBasedOneTimePassword;
import com.cryptowallet.utils.Utils;
import com.google.common.base.Strings;

import java.security.GeneralSecurityException;

/**
 *
 */
public class Configure2faActivity extends ActivityBase {

    private String mSecretPhrase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure2fa);
        setTitle(R.string.configure_2fa);

        mSecretPhrase = TimeBasedOneTimePassword.generateBase32Secret();

        TextView secretPhraseText = findViewById(R.id.mSecretPhrase);
        secretPhraseText.setText(mSecretPhrase);

        ImageView secretPhraseQr = findViewById(R.id.mTwofactorQr);
        secretPhraseQr.setImageBitmap(Utils.generateQrCode(
                Uri.parse("otpauth://totp/CryptoWallet?secret=" + mSecretPhrase), 250));

        findViewById(R.id.mRegister2fa)
                .setOnClickListener(this::validateCode);
    }


    private void validateCode(View view) {

        TextView code = findViewById(R.id.mCode2fa);

        String codeStr = code.getText().toString();

        boolean valid = false;

        if (!Strings.isNullOrEmpty(codeStr)) {
            try {
                valid = TimeBasedOneTimePassword.validateCurrentNumber(
                        mSecretPhrase,
                        Integer.parseInt(codeStr),
                        0
                );
            } catch (GeneralSecurityException ignored) {
            }
        }

        if (valid)
            register2Fa();
        else
            code.setError(getString(R.string.error_2fa_code));
    }

    private void register2Fa() {
        String code = Security.encryptAES(mSecretPhrase);
        AppPreference.setSecretPhrase(this, code);

        if (!Strings.isNullOrEmpty(code)) {
            Intent intent = new Intent();
            intent.putExtra(ExtrasKey.OP_ACTIVITY, ExtrasKey.ACTIVED_2FA);
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }
}
