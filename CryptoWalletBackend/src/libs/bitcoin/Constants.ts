/**
 * Constantes utilizadas en el módulo de Bitcoin.
 */
const CONSTANTS = {
    BITCOIN: "bitcoin",
    TIMEOUTS: {
        WAIT_FOR_HEADERS: 5000,
        WAIT_FOR_BLOCKS: 5000,
        WAIT_FOR_CONNECT_NET: 20000,
        WAIT_FOR_CONNECT_PEER: 5000
    },
    NULL_HASH: Array(65).join('0'),
    BIN_NULL_HASH: Buffer.alloc(64, 0)
}

export default CONSTANTS