package com.cryptowallet.wallet;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;

import com.cryptowallet.utils.Helper;

import java.util.Date;

/**
 * Provee de una clase que permite representar una transacción genérica. Esta clase soporta
 * 6 atributos para poder visualizar los datos.
 * <pre>
 *     * Operation: Operación de la transacción (Envío|Recepción)
 *     * Amount: Monto de la transacción
 *     * Time: Fecha/hora de la transacción
 *     * Fee: Comisión por realizar la operación
 *     * Commited: Si la transacción está confirmada
 *     * Image: Icono de la moneda o token
 * </pre>
 */
public final class RecentItem {

    /**
     * Tipo de operación (Envío|Recepción).
     */
    private final TxKind mOperationKind;
    /**
     * Fecha/hora de la transacción.
     */
    private final Date mTime;
    /**
     * Comisión de la transacción.
     */
    private final String mFee;
    /**
     * Monto de la transacción.
     */
    private final String mAmount;
    /**
     * Icono de la moneda o token.
     */
    private final Drawable mImage;
    /**
     * Indica si la transacción está confirmada.
     */
    private boolean mCommited;
    /**
     * Acción a realizar cuando se confirma la transacción.
     */
    private Runnable mOnCommited;
    /**
     * Un handler que permite enviar las tareas al hilo principal.
     */
    private Handler mHandler = new Handler();

    /**
     * Contexto de la aplicación.
     */
    private Context mContext;

    /**
     * Crea una nueva instancia de <code>RecentItem</code>.
     *
     * @param operation Operación de la transacción.
     * @param time      Fecha/hora de la transacción.
     * @param fee       Comisión de la transacción.
     * @param amount    Monto de la transacción.
     * @param icon      Icono de la moneda o token.
     */
    public RecentItem(Context context, TxKind operation, Date time, String fee,
                      String amount, Drawable icon) {
        mContext = context;
        mOperationKind = operation;
        mTime = time;
        mFee = fee;
        mAmount = amount;
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
        this.mCommited = true;

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
        return mCommited;
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
     * Tipo de transacción disponible.
     */
    public enum TxKind {
        RECEIVE,
        SEND
    }
}
