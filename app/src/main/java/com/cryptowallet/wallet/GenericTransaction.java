package com.cryptowallet.wallet;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.DrawableRes;

import com.cryptowallet.utils.Helper;

import java.util.Date;

/**
 * Provee de una clase que permite representar una transacción de manera generica.
 */
public final class GenericTransaction {

    /**
     * Tipo de operación (Envío|Recepción).
     */
    private TxKind mOperationKind;

    /**
     * Fecha/hora de la transacción.
     */
    private Date mTime;

    /**
     * Comisión de la transacción.
     */
    private String mFee;

    /**
     * Monto de la transacción.
     */
    private String mAmount;

    /**
     * Icono de la moneda o token.
     */
    private Drawable mImage;

    /**
     * Acción a realizar cuando se confirma la transacción.
     */
    private Runnable mOnCommited;

    /**
     * Un handler que permite enviar las tareas al hilo principal.
     */
    private Handler mHandler = new Handler();

    /**
     * Identificador de la transacción.
     */
    private String mTxID;

    /**
     * Direcciones de la transacción.
     */
    private String mAddress = "";

    /**
     * Contexto de la aplicación.
     */
    private Context mContext;

    /**
     * Número de confirmaciónes.
     */
    private int mCommits = 0;

    /**
     * Crea una nueva instancia de <code>GenericTransaction</code>.
     *
     * @param context Contexto de la aplicación.
     * @param icon    Icono de la moneda o token.
     */
    private GenericTransaction(Context context, Drawable icon) {
        mContext = context;
        mImage = icon;
    }

    /**
     * Establece una acción cuando la transacción es confirmada.
     *
     * @param onCommited Acción a realizar.
     */
    public void setOnCommited(Runnable onCommited) {
        mOnCommited = onCommited;
    }

    /**
     * Establece la transacción como confirmada.
     */
    public void commite() {
        this.mCommits = 1;

        mHandler.post(mOnCommited);
    }

    /**
     * Obtiene el tipo de operación de la transacción.
     *
     * @return Tipo de operación.
     */
    public TxKind getOperationKind() {
        return mOperationKind;
    }

    /**
     * Obtiene el tiempo de la transacción.
     *
     * @return Fecha/hora de la transacción.
     */
    public String getTimeToStringFriendly() {
        return Helper.getDateTime(mContext, mTime);
    }

    /**
     * Obtiene la comisión de la transacción.
     *
     * @return Comisión de la transacción.
     */
    public String getFee() {
        return mFee;
    }

    /**
     * Obtiene el monto de la transacción.
     *
     * @return El monto de la transacción.
     */
    public String getAmount() {
        return mAmount;
    }

    /**
     * Obtiene el icono de la moneda o token.
     *
     * @return Icono de la moneda o token.
     */
    public Drawable getImage() {
        return mImage;
    }

    /**
     * Obtiene un valor que indica si la transacción está confirmada.
     *
     * @return Un valor true si está confirmada de la transacción.
     */
    public boolean isCommited() {
        return mCommits > 0;
    }

    /**
     * Obtiene la fecha/hora de la transacción.
     *
     * @return Fecha/hora de la transacción.
     */
    public Date getTime() {
        return mTime;
    }

    /**
     * Obtiene el número de confirmaciones.
     *
     * @return Número de confirmaciones.
     */
    public int getCommits() {
        return mCommits;
    }

    /**
     * Establece el número de confirmaciones de la transacción.
     *
     * @param mCommits Número de confirmaciones.
     */
    public void setCommits(int mCommits) {
        this.mCommits = mCommits;

        if (mOnCommited != null)
            mHandler.post(mOnCommited);
    }

    /**
     * Obtiene el identificador de la transacción.
     *
     * @return TxID de la transacción.
     */
    public String getTxID() {
        return mTxID;
    }

    /**
     * Obtiene las direcciones de la transacción.
     *
     * @return Direcciones base58.
     */
    public String getAddress() {
        return mAddress;
    }

    /**
     * Tipo de transacción disponible.
     */
    public enum TxKind {
        RECEIVE,
        SEND
    }

    /**
     * Constructor de una transacción genérica.
     */
    public static class GenericTransactionBuilder {

        /**
         * Instancia a crear.
         */
        private GenericTransaction mTransaction;


        /**
         * Crea una nueva instancia del constructo de <code>GenericTransaction</code>.
         *
         * @param context    Contexto de la aplicación.
         * @param assetImage Logo de la moneda.
         */
        public GenericTransactionBuilder(Context context, @DrawableRes int assetImage) {
            mTransaction = new GenericTransaction(context, context.getDrawable(assetImage));
        }

        /**
         * Establece la fecha y hora de la transacción.
         *
         * @param time Fecha/hora de la transacción.
         * @return El constructor.
         */
        public GenericTransactionBuilder setTime(Date time) {
            mTransaction.mTime = time;

            return this;
        }

        /**
         * Establece las direcciones de la transacción, si es un envío se agregan las direcciones
         * de las salidas o de ser recepción son las direcciones de entrada.
         *
         * @param address Direcciones de la billetera.
         * @return El constructor.
         */
        public GenericTransactionBuilder setAddress(String address) {
            mTransaction.mAddress = address;

            return this;
        }

        /**
         * Añade la dirección a la transacción.
         *
         * @param address Direción a agregar.
         * @return El constructor.
         */
        public GenericTransactionBuilder appendAddress(String address) {
            if (!mTransaction.mAddress.isEmpty())
                mTransaction.mAddress += "\n";

            if (!mTransaction.mAddress.contains(address))
                mTransaction.mAddress += address;

            return this;
        }

        /**
         * Establece el tipo de la operación (Envío|Recibido)
         *
         * @param kind Tipo de operación.
         * @return El constructor.
         */
        public GenericTransactionBuilder setKind(TxKind kind) {
            mTransaction.mOperationKind = kind;

            return this;
        }

        /**
         * Establece el identificador de la transacción.
         *
         * @param id Identificador.
         * @return El constructor.
         */
        public GenericTransactionBuilder setTxID(String id) {
            mTransaction.mTxID = id;

            return this;
        }

        /**
         * Establece el número de confirmaciones de la transacción.
         *
         * @param commits Número de confirmaciones.
         * @return El constructor.
         */
        public GenericTransactionBuilder setCommits(int commits) {
            mTransaction.mCommits = commits;

            return this;
        }

        /**
         * Establece el monto de la transacción.
         *
         * @param amount Monto total.
         * @return El constructor.
         */
        public GenericTransactionBuilder setAmount(String amount) {
            mTransaction.mAmount = amount;

            return this;
        }

        /**
         * Establece la comisión de la transacción.
         *
         * @param fee Comisión.
         * @return El constructor.
         */
        public GenericTransactionBuilder setFee(String fee) {
            mTransaction.mFee = fee;

            return this;
        }

        /**
         * Establece una acción a ejecutar cuando se confirma la transacción.
         *
         * @param onCommited Acción a ejecutar cuando se confirma.
         * @return El constructor.
         */
        public GenericTransactionBuilder setOnCommited(Runnable onCommited) {
            mTransaction.mOnCommited = onCommited;

            return this;
        }

        /**
         * Construye la transacción y la devuelve.
         *
         * @return Transacción resultado.
         */
        public GenericTransaction build() {
            return mTransaction;
        }

    }
}
