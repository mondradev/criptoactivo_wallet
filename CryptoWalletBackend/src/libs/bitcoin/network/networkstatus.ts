/**
 * Estados de la conexión a la red de Bitcoin.
 */
enum NetworkStatus {
    /**
     * Red desconectada, no acepta conexiones entrandes ni salientes.
     */
    DISCONNECTED,
    /**
     * En proceso de desconexión de los nodos.
     */
    DISCONNECTING,
    /**
     * Red intentando conectar, buscando nodos que enlazar.
     */
    CONNECTING,
    /**
     * Red conectada, nodos enlazados listo para aceptar peticiones.
     */
    CONNECTED,
    /**
     * Red en sincronización, descarga iniciar.
     */
    SYNC,
    /**
     * Red sincronizada. Se aceptan conexiones entrantes.
     */
    SYNCHRONIZED
}

export default NetworkStatus