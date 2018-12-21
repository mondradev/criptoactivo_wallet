package com.cryptowallet.wallet;

import java.util.Date;

/**
 * Representa la información de la descarga de la blockchain de algún activo.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class BlockchainStatus {

    /**
     * Bloques restantes por descargar.
     */
    private final int mLeftBlocks;

    /**
     * Fecha del bloque descargado.
     */
    private final Date mTime;

    /**
     * Crea una nueva instancia.
     *
     * @param leftBlocks Cantidad de bloques restantes por descargar.
     * @param time       Fecha/hora del último bloque descargado.
     */
    private BlockchainStatus(int leftBlocks, Date time) {
        mLeftBlocks = leftBlocks;
        mTime = time;
    }

    /**
     * Crea una nueva instancia de {@link  BlockchainStatus}
     *
     * @param leftBlocks Cantidad de bloques restantes por descargar.
     * @param time       Fecha/hora del último bloque descargado.
     * @return El estado de la blockchain.
     */
    public static BlockchainStatus create(int leftBlocks, Date time) {
        return new BlockchainStatus(leftBlocks, time);
    }

    /**
     * Obtiene el número restante de bloques que por descargar.
     *
     * @return El número de bloques restantes.
     */
    public int getLeftBlocks() {
        return mLeftBlocks;
    }

    /**
     * Obtiene la fecha y hora del último bloque descargado.
     *
     * @return Fecha y hora del último bloque.
     */
    public Date getTime() {
        return mTime;
    }
}
