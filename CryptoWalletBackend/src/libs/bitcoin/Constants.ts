/**
 * Constantes utilizadas en el módulo de Bitcoin.
 */
const CONSTANTS = {
    Bitcoin: "Bitcoin",
    Timeouts: {
        WaitForHeaders: 5000,
        WaitForBlocks: 5000,
        WaitForConnectNet: 20000,
        WaitForConnectPeer: 5000
    },
    NullHash: Array(65).join('0'),
    BinNullHash: Buffer.alloc(64, 0)
}

export default CONSTANTS