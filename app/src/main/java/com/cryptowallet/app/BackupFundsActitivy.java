package com.cryptowallet.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Esta actividad permite obtener las 12 palabras de la billetera.
 */
public class BackupFundsActitivy extends AppCompatActivity {

    /**
     * Palabra que está actualmente mostrandose.
     */
    private int mCurrentWord = -1;

    /**
     * Palabra a probar.
     */
    private int mWord;

    /**
     * Error de ingreso de palabras.
     */
    private int mError;
    /**
     * Palabras testeadas.
     */
    private List<Integer> mWordsTested = new ArrayList<>();

    /**
     * Este método es llamado cuando se crea la actividad.
     *
     * @param savedInstanceState Estado de la instancia.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_funds_actitivy);

        setTitle(R.string.backup_funds_title);
    }

    /**
     * Muestra la primera o la siguiente palabra. Cuando finaliza las palabras este las prueba de
     * manera aleatoria y finaliza al actividad.
     *
     * @param view Componente que desencadena que el evento Click.
     */
    public void handlerStartOrNextWord(View view) {
        TextView mSeed = findViewById(R.id.mSeed);
        Button mNextWord = findViewById(R.id.mShowNextWord);
        EditText mTestWord = findViewById(R.id.mTestWord);

        if (mCurrentWord == -1) {
            mNextWord.setText(R.string.next_word_text);
            mSeed.setVisibility(View.VISIBLE);
        }

        if (mCurrentWord > 13)
            finish();
        else if (mCurrentWord == 11) {
            mNextWord.setText(R.string.test_words);
            mSeed.setVisibility(View.GONE);
            mTestWord.setVisibility(View.VISIBLE);

            mWord = getNextWords();

            mTestWord.setHint(String.format(Locale.getDefault(),
                    getString(R.string.testing_word_text), mWord + 1));

            mCurrentWord++;

        } else if (mCurrentWord > 11) {

            String word = mTestWord.getText().toString();

            if (word.contentEquals(BitcoinService.get().getSeedCode().get(mWord))) {
                mWord = getNextWords();

                mTestWord.setHint(String.format(Locale.getDefault(),
                        getString(R.string.testing_word_text), mWord + 1));
                mTestWord.setText(null);
                mTestWord.setError(null);

                mCurrentWord++;

            } else {
                mError++;

                mTestWord.setError(getString(R.string.error_word));

                if (mError > 3)
                    finish();
            }
        } else {

            mCurrentWord++;

            mSeed.setText(BitcoinService.get().getSeedCode().get(mCurrentWord));
        }
    }

    /**
     * Obtiene la nueva palabra.
     *
     * @return Índice de la palabra a probar.
     */
    private int getNextWords() {
        int index;

        while (true) {
            if (!mWordsTested.contains((
                    index = ThreadLocalRandom.current().nextInt(0, 11))))
                break;
        }

        mWordsTested.add(index);

        return index;
    }
}
