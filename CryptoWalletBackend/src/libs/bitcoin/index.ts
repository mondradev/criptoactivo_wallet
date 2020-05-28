import BtcBlockchain from "./BtcBlockchain"
import BtcWallet from "./BtcWallet"
import BtcNetwork from "./BtcNetwork"
import * as Stores from './stores'

const Bitcoin = {
    Network: BtcNetwork,
    Blockchain: BtcBlockchain,
    Wallet: BtcWallet,
    Stores
}

export default Bitcoin