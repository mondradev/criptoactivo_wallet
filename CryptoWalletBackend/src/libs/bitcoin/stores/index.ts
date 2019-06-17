import { BtcAddrIndexStore, BtcAddrIndexDb, BtcUTXOIndexDb } from "./BtcAddrIndexStore";
import { BtcTxIndexStore } from "./BtcTxIndexStore";
import { BtcBlockStore, BtcBlkIndexDb, BtcChainStateDb } from "./BtcBlockStore";

export const Indexers = {
    AddrIndex: BtcAddrIndexStore,
    TxIndex: BtcTxIndexStore,
    BlockIndex: BtcBlockStore
}

export const Storages = {
    AddrDb: BtcAddrIndexDb,
    BlkIdx: BtcBlkIndexDb,
    ChainDb: BtcChainStateDb,
    UTXOIdx: BtcUTXOIndexDb
}

export async function stopService() {
    for (const storage in Storages)
        Storages[storage].isOpen() && await Storages[storage].close()

    BtcAddrIndexStore.stopMonitorCache()
}

process.on('beforeExit', () => stopService())