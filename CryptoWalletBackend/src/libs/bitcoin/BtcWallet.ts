import IWalletProvider from "./IWalletProvider";

class Wallet implements IWalletProvider {
    getRawTransactionsByAddress(address: string): Promise<string[]> {
        throw new Error("Method not implemented.");
    }
    getRawTransaction(txid: string): Promise<string> {
        throw new Error("Method not implemented.");
    }
    broadcastTrx(transaction: string): Promise<boolean> {
        throw new Error("Method not implemented.");
    }


}

const BtcWallet = new Wallet()
export default BtcWallet