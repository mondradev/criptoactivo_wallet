package com.cryptowallet.wallet;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.cryptowallet.R;
import com.cryptowallet.utils.Helper;

import java.util.Date;
import java.util.Objects;

/**
 *
 */
public abstract class GenericTransactionBase implements Comparable<GenericTransactionBase> {

    private OnUpdateDepthListener onUpdateDepthListener;

    private Kind mKind;

    /**
     *
     */
    private Context mContext;
    /**
     * Icono de la moneda o token.
     */
    private Drawable mImage;

    /**
     * @param context
     * @param coinIcon
     */
    public GenericTransactionBase(Context context, Kind kind, Drawable coinIcon) {
        this.mContext = context;
        this.mImage = coinIcon;
        this.mKind = kind;
    }

    /**
     * @return
     */
    public final Context getContext() {
        return mContext;
    }

    /**
     * @return
     */
    public final Drawable getImage() {
        return mImage;
    }

    public abstract String getFeeToStringFriendly();

    public abstract String getAmountToStringFriendly(SupportedAssets currency);

    public abstract String getInputsAddress();

    public abstract String getOutputAddress();

    public abstract Date getTime();

    public abstract int getDepth();

    public String getTimeToStringFriendly() {
        Objects.requireNonNull(mContext, "No se especificó el contexto de la aplicación a " +
                "inicializar la instancia actual.");
        Objects.requireNonNull(getTime(), "La función implementada " +
                "'GenericTransactionBase#getTime()' devuelve un valor null.");
        return Helper.getDateTime(mContext, getTime());
    }

    public OnUpdateDepthListener getOnUpdateDepthListener() {
        return onUpdateDepthListener;
    }

    public void setOnUpdateDepthListener(OnUpdateDepthListener listener) {
        onUpdateDepthListener = listener;
    }

    public String getKindToStringFriendly() {
        Objects.requireNonNull(mContext);

        switch (getKind()) {
            case SEND:
                return mContext.getString(R.string.send_text);
            case RECEIVE:
                return mContext.getString(R.string.receive_text);
        }

        return "(?)";
    }

    public abstract String getID();

    @Override
    public int compareTo(GenericTransactionBase o) {
        return this.getTime().compareTo(o.getTime());
    }

    public Kind getKind() {
        return mKind;
    }

    /**
     *
     */
    public enum Kind {
        SEND,
        RECEIVE
    }

    public interface OnUpdateDepthListener {
        void onUpdate(GenericTransactionBase tx);
    }
}
