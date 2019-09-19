/**
 * Estados de la conexión a la red de Bitcoin.
 */
enum NetworkStatus {
    /**
     * Red desconectada, no acepta conexiones entrandes ni salientes.
     */
    Disconnected,
    /**
     * Red intentando conectar, buscando nodos que enlazar.
     */
    Connecting,
    /**
     * Red conectada, nodos enlazados listo para aceptar peticiones.
     */
    Connected,
    /**
     * Red en sincronización, descarga iniciar.
     */
    Sync,
    /**
     * Red sincronizada. Se aceptan conexiones entrantes.
     */
    Synchronized
}

export default NetworkStatus