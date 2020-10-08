import BufferHelper from "../../utils/bufferhelper"
import Config, { AssetConfig } from "../../../config"
import UInt256 from "./primitives/uint256"
import Block from "./primitives/blocks/block"
import Script, { OpCode, ScriptNum } from "./primitives/script"
import { MutableTransaction } from "./primitives/tx/transaction"
import TxIn from "./primitives/tx/input"
import TxOut from "./primitives/tx/output"
import Outpoint from "./primitives/tx/outpoint"
import { Stream } from './primitives/serializable'

/**
 * Valor de la unidad.
 */
const COIN = 100000000

/**
 * Define la configuración de la red de Bitcoin.
 * 
 * @author Ing. Javier Flores <jjflores@innsytech.com>
 * @version 1.0
 */
export class ChainParams {

    /**
     * Configuración de red de nodos.
     */
    protected _config: AssetConfig

    /**
     * Listado de servidores DNS.
     */
    protected _seeds: Array<string>

    /**
     * Bloque genesis.
     */
    protected _genesis: Block

    /**
     * Hash del bloque genesis.
     */
    protected _hashGenesis: UInt256

    /**
     * Lista de checkpoints.
     */
    protected _checkpoint: Array<{ height: number, hash: string }>

    /**
     * Puerto determinado de la red.
     */
    protected _defaultPort: number

    /**
     * Nombre de la red.
     */
    protected _name: 'mainnet' | 'testnet'

    /**
     * Crea una nueva instancia.
     */
    constructor() {
        this._checkpoint = new Array()
        this._seeds = new Array()
    }

    /**
     * Obtiene el nombre de la red.
     */
    public get name(): string {
        return this._name
    }

    /**
     * Obtiene el puuerto predeterminado que utilizan los nodos para 
     * interconectarse.
     */
    public get defaultPort(): number {
        return this._defaultPort
    }

    /**
     * Obtiene los puntos de control de la cadena de bloques.
     */
    public get checkpoints(): Array<{ height: number, hash: string }> {
        return this._checkpoint
    }

    /**
     * Obtiene el bloque genesis de la red.
     */
    public get genesis(): Block {
        return this._genesis
    }

    /**
     * Obtiene el hash del bloque genesis.
     */
    public get hashGenesis(): UInt256 {
        return this._hashGenesis
    }

    /**
     * Obtiene la lista de los servidores de nombres.
     */
    public get seeds(): Array<string> {
        return this._seeds
    }

    /**
     * Obtiene la configuración de la red de nodos.
     */
    public get config(): AssetConfig {
        return this._config
    }

    /**
     * Crea el bloque genesis de la cadena.
     * 
     * @param time Fecha/Hora en la cual se generó el bloque, expresado en segundo a partir de enero 1, 1970.
     * @param nonce Nonce del bloque.
     * @param bits Dificultad del bloque.
     * @param version Versión del bloque.
     * @param genesisReward Recompensa del bloque.
     */
    protected static createGenesisBlock(time: number, nonce: number, bits: number, version: number,
        genesisReward: number): Block {
        const timestamp = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
        const genesisOutputScript = Script.empty()
            .append(BufferHelper.fromHex("04678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5f"))
            .append(OpCode.OP_CHECKSIG);

        const newTx = new MutableTransaction()
        const output = TxOut.create(genesisReward, genesisOutputScript)
        const input = TxIn.create(UInt256.null(), Outpoint.NULL_INDEX, Script.empty()
            .append(486604799).append(new ScriptNum(4)).append(Buffer.from(timestamp)), TxIn.SEQUENCE_FINAL)

        newTx.version = 1
        newTx.inputs.push(input)
        newTx.outputs.push(output)

        const block = new Block()

        block.version = version
        block.hashPrevBlock = UInt256.null()
        block.hashMerkleRoot = newTx.hash
        block.time = time
        block.bits = bits
        block.nonce = nonce
        block.txs.push(newTx.toTransaction())

        const stream = Stream.empty()
        block.serialize(stream)

        return block
    }
}

/**
 * Configuración de la red principal de Bitcoin.
 */
class Mainnet extends ChainParams {

    /**
     * Crea una nueva instancia.
     */
    public constructor() {
        super()

        this._defaultPort = 8333
        this._name = "mainnet"
        this._genesis = ChainParams.createGenesisBlock(1231006505, 2083236893, 0x1d00ffff, 1, 50 * COIN)
        this._hashGenesis = this._genesis.hash

        this._checkpoint.push({ height: 11111, hash: "0000000069e244f73d78e8fd29ba2fd2ed618bd6fa2ee92559f542fdb26e7c1d" })
        this._checkpoint.push({ height: 33333, hash: "000000002dd5588a74784eaa7ab0507a18ad16a236e7b1ce69f00d7ddfb5d0a6" })
        this._checkpoint.push({ height: 74000, hash: "0000000000573993a3c9e41ce34471c079dcf5f52a0e824a81e7f953b8661a20" })
        this._checkpoint.push({ height: 105000, hash: "00000000000291ce28027faea320c8d2b054b2e0fe44a773f3eefb151d6bdc97" })
        this._checkpoint.push({ height: 134444, hash: "00000000000005b12ffd4cd315cd34ffd4a594f430ac814c91184a0d42d2b0fe" })
        this._checkpoint.push({ height: 168000, hash: "000000000000099e61ea72015e79632f216fe6cb33d7899acb35b75c8303b763" })
        this._checkpoint.push({ height: 193000, hash: "000000000000059f452a5f7340de6682a977387c17010ff6e6c3bd83ca8b1317" })
        this._checkpoint.push({ height: 210000, hash: "000000000000048b95347e83192f69cf0366076336c639f9b7228e9ba171342e" })
        this._checkpoint.push({ height: 216116, hash: "00000000000001b4f4b433e81ee46494af945cf96014816a4e2370f11b23df4e" })
        this._checkpoint.push({ height: 225430, hash: "00000000000001c108384350f74090433e7fcf79a606b8e797f065b130575932" })
        this._checkpoint.push({ height: 250000, hash: "000000000000003887df1f29024b06fc2200b55f8af8f35453d7be294df2d214" })
        this._checkpoint.push({ height: 279000, hash: "0000000000000001ae8c72a0b0c301f67e3afca10e819efa9041e458e9bd7e40" })
        this._checkpoint.push({ height: 295000, hash: "00000000000000004d9b4ef50f0f9d686fd69db2e03af35a100370c64632a983" })

        this._seeds.push("seed.bitcoin.sipa.be")
        this._seeds.push("dnsseed.bluematt.me")
        this._seeds.push("dnsseed.bitcoin.dashjr.org")
        this._seeds.push("seed.bitcoinstats.com")
        this._seeds.push("seed.bitcoin.jonasschnelli.ch")
        this._seeds.push("seed.btc.petertodd.org")
        this._seeds.push("seed.bitcoin.sprovoost.nl")
        this._seeds.push("dnsseed.emzy.de")

        this._config = Config.getAsset('bitcoin')
    }
}

/**
 * Configuración de la red de pruebas de Bitcoin.
 */
class Testnet extends ChainParams {

    /**
     * Crea una nueva instancia.
     */
    public constructor() {
        super()

        this._defaultPort = 18333
        this._name = "testnet"
        this._genesis = ChainParams.createGenesisBlock(1296688602, 414098458, 0x1d00ffff, 1, 50 * COIN)
        this._hashGenesis = this._genesis.hash

        this._checkpoint.push({ height: 546, hash: "000000002a936ca763904c3c35fce2f3556c559c0214345d31b1bcebf76acb70" })

        this._seeds.push("testnet-seed.bitcoin.jonasschnelli.ch")
        this._seeds.push("testnet-seed.bitcoin.petertodd.org")
        this._seeds.push("testnet-seed.bitcoin.schildbach.de")
        this._seeds.push("seed.testnet.bitcoin.sprovoost.nl")
        this._seeds.push("testnet-seed.bluematt.me")
        this._seeds.push("testnet-seed.alexykot.me")
        this._seeds.push("seed.tbtc.petertodd.org")

        this._config = Config.getAsset('bitcoin-test')
    }
}

/**
 * Configuración para la cadena principal de Bitcoin.
 */
export const MainParams = new Mainnet()

/**
 * Configuración para la cadena de pruebas de Bitcoin.
 */
export const Test3Params = new Testnet()