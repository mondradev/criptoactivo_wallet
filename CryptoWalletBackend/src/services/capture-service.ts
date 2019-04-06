import { BtcWallet } from "../basedbitcoin-assets/bitcoin/btc-wallet-service";
import { BasedBtcBlockStore } from "../../src/basedbitcoin-assets/block-store";
import { BasedBtcTxStore } from "../../src/basedbitcoin-assets/tx-store";
import { BasedBtcCoinStore } from "../../src/basedbitcoin-assets/coin-store";

(async () => {
    await BasedBtcBlockStore.connect();
    await BasedBtcTxStore.connect();
    await BasedBtcCoinStore.connect();

    BtcWallet.sync();
})();
