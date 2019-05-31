export class Signature {

}

export class Network {

}

export class Input {
    public prevTxId: Buffer;
    public outputIndex: number;
    public sequenceNumber: number;
    public script: Script;
    public output: Output;

    /**
     * @returns true if this is a coinbase input (represents no input)
     */
    public isNull(): boolean;
}

export class Output {
    public script: Script;
    public satoshis: number;
}

export class Networks {

    /**
     * @function
     * @member Networks#get
     * Retrieves the network associated with a magic number or string.
     * @param {string|number|Network} arg
     * @param {string|Array} keys - if set, only check if the magic number associated with this name matches
     * @return Network
     */
    static get(arg: string | number | Network, keys?: string | Array<any>): Network;

    /**
     * @function
     * @member Networks#add
     * Will add a custom Network
     * @param {Object} data
     * @param {string} data.name - The name of the network
     * @param {string} data.alias - The aliased name of the network
     * @param {Number} data.pubkeyhash - The publickey hash prefix
     * @param {Number} data.privatekey - The privatekey prefix
     * @param {Number} data.scripthash - The scripthash prefix
     * @param {Number} data.xpubkey - The extended public key magic
     * @param {Number} data.xprivkey - The extended private key magic
     * @param {Number} data.networkMagic - The network magic number
     * @param {Number} data.port - The network port
     * @param {Array}  data.dnsSeeds - An array of dns seeds
     * @return Network
     */
    static add(data: {
        name: string,
        alias: string,
        pubkeyhash: number,
        privatekey: number,
        scripthash: number,
        xpubkey: number,
        xprivkey: number,
        networkMagic: number,
        port: number,
        dnsSeeds: Array<any>
    }): Network;

    /**
     * @function
     * @member Networks#remove
     * Will remove a custom network
     * @param {Network} network
     */
    static remove(network: Network): void;

    /**
     * @instance
     * @member Networks#livenet
     */
    public static livenet: Network;

    /**
     * @instance
     * @member Networks#mainnet
     */
    public static mainnet: Network;

    /**
     * @instance
     * @member Networks#testnet
     */
    public static testnet: Network;

    /**
     * @function
     * @member Networks#enableRegtest
     * Will enable regtest features for testnet
     */
    static enableRegtest(): void;

    /**
     * @function
     * @member Networks#disableRegtest
     * Will disable regtest features for testnet
     */
    static disableRegtest(): void;

    public static defaultNetwork: Network;
}

/**
* The representation of an hierarchically derived public key.
*
* See https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki
*
*/
export class HDPublicKey {

    public publicKey: PublicKey;

    /**
     * The representation of an hierarchically derived public key.
     *
     * See https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki
     *
     * @constructor
     * @param {Object|string|Buffer} arg
     */
    constructor(arg: (Object | string | Buffer));

    /**
     * WARNING: This method will not be officially supported until v1.0.0.
     *
     *
     * Get a derivated child based on a string or number.
     *
     * If the first argument is a string, it's parsed as the full path of
     * derivation. Valid values for this argument include "m" (which returns the
     * same public key), "m/0/1/40/2/1000".
     *
     * Note that hardened keys can't be derived from a public extended key.
     *
     * If the first argument is a number, the child with that index will be
     * derived. See the example usage for clarification.
     *
     * @example
     * ```javascript
     * var parent = new HDPublicKey('xpub...');
     * var child_0_1_2 = parent.deriveChild(0).deriveChild(1).deriveChild(2);
     * var copy_of_child_0_1_2 = parent.deriveChild("m/0/1/2");
     * assert(child_0_1_2.xprivkey === copy_of_child_0_1_2);
     * ```
     *
     * @param {string|number} arg
     */
    deriveChild(arg: (string | number)): HDPublicKey;

    /**
     * Verifies that a given serialized public key in base58 with checksum format
     * is valid.
     *
     * @param {string|Buffer} data - the serialized public key
     * @param {string|Network=} network - optional, if present, checks that the
     *     network provided matches the network serialized.
     * @return {boolean}
     */
    static isValidSerialized(data: (string | Buffer), network: (string | Network)): boolean;
}

export class PublicKey {

    /**
     * Instantiate a PublicKey from a {@link PrivateKey}, {@link Point}, `string`, or `Buffer`.
     *
     * There are two internal properties, `network` and `compressed`, that deal with importing
     * a PublicKey from a PrivateKey in WIF format. More details described on {@link PrivateKey}
     *
     * @example
     * ```javascript
     * // instantiate from a private key
     * var key = PublicKey(privateKey, true);
     *
     * // export to as a DER hex encoded string
     * var exported = key.toString();
     *
     * // import the public key
     * var imported = PublicKey.fromString(exported);
     * ```
     *
     * @param {string} data - The encoded data in various formats
     * @param {Object} extra - additional options
     * @param {Network=} extra.network - Which network should the address for this public key be for
     * @param {String=} extra.compressed - If the public key is compressed
     * @returns {PublicKey} A new valid instance of an PublicKey
     * @constructor
     */
    constructor(data: string | Buffer, extra?: { network: Network, compressed: string });

    /**
     * Check if there would be any errors when initializing a PublicKey
     *
     * @param {string} data - The encoded data in various formats
     * @returns {null|Error} An error if exists
     */
    static getValidationError(data: string): null | Error;
}

export class Script {

    public toBuffer(): Buffer;

    /**
     * @param {Network=} network
     * @return {Address|boolean} the associated address for this script if possible, or false
     */
    toAddress(network: Network): Address;

    public chunks: Array<{ opcodenum?: number, buf?: Buffer }>;

    /**
     * @returns {Script} a new pay to script hash script that pays to this script
     */
    toScriptHashOut(): Script;

    /**
     * Adds a script element to the end of the script.
     *
     * @param {*} obj a string, number, Opcode, Buffer, or object to add
     * @returns {Script} this script instance
     *
     */
    public add(obj: any): Script;

    /**
     * Comes from bitcoind's script interpreter CheckMinimalPush function
     * 
     * @returns {boolean} if the chunk {i} is the smallest way to push that particular data.
     */
    public checkMinimalPush(i: number): boolean;

    /**
     * Compares a script with another script
     */
    public equals(script: Script): boolean;

    /**
     * Analogous to bitcoind's FindAndDelete. Find and delete equivalent chunks,
     * typically used with push data chunks.  Note that this will find and delete
     * not just the same data, but the same data with the same push data op as
     * produced by default. i.e., if a pushdata in a tx does not use the minimal
     * pushdata op, then when you try to remove the data it is pushing, it will not
     * be removed, because they do not use the same pushdata op.
     */
    public findAndDelete(script: Script): Script;

    /**
     * Will return the associated address information object
     * @return {Address|boolean}
     */
    public getAddressInfo(): Address | boolean;

    /**
     * Retrieve the associated data for this script.
     * In the case of a pay to public key hash or P2SH, return the hash.
     * In the case of a standard OP_RETURN, return the data
     * @returns {Buffer}
     */
    public getData(): Buffer;

    public getPublicKey(): Buffer;
    public getPublicKeyHash(): Buffer;

    /**
     * Comes from bitcoind's script GetSigOpCount(boolean) function
     * @param {boolean} use current (true) or pre-version-0.6 (false) logic
     * @returns {number} number of signature operations required by this script
     */
    public getSignatureOperationsCount(accurate: boolean): number;

    public hasCodeseparators(): boolean;

    /**
     * @returns {boolean} true if this is a valid standard OP_RETURN output
     */
    public isDataOut(): boolean;

    /**
     * @returns {boolean} if this is a multisig input script
     */
    public isMultisigIn(): boolean;

    /**
     * @returns {boolean} if this is a mutlsig output script
     */
    public isMultisigOut(): boolean;

    /**
     * @returns {boolean} if this is a pay to public key hash input script
     */
    public isPublicKeyHashIn(): boolean;

    /**
     * @returns {boolean} if this is a pay to pubkey hash output script
     */
    public isPublicKeyHashOut(): boolean;

    /**
     * @returns {boolean} if this is a pay to public key input script
     */
    public isPublicKeyIn(): boolean;

    /**
     * @returns {boolean} if this is a public key output script
     */
    public isPublicKeyOut(): boolean;

    /**
     * @returns {boolean} if the script is only composed of data pushing
     * opcodes or small int opcodes (OP_0, OP_1, ..., OP_16)
     */
    public isPushOnly(): boolean;

    /**
     * @returns {boolean} if this is a p2sh input script
     * Note that these are frequently indistinguishable from pubkeyhashin
     */
    public isScriptHashIn(): boolean;

    /**
     * @returns {boolean} if this is a p2sh output script
     */
    public isScriptHashOut(): boolean;

    public toHex(): string;

    /**
     * @returns {boolean} if script is one of the known types
     */
    public isStandard(): boolean;

    /**
     * @param {Object=} values - The return values
     * @param {Number} values.version - Set with the witness version
     * @param {Buffer} values.program - Set with the witness program
     * @returns {boolean} if this is a p2wpkh output script
     */
    public isWitnessProgram(values: { version: number, program: Buffer }): boolean;

    /**
     * @returns {boolean} if this is a p2wpkh output script
     */
    public isWitnessPublicKeyHashOut(): boolean;

    /**
     * @returns {boolean} if this is a p2wsh output script
     */
    public isWitnessScriptHashOut(): boolean;

    /**
     * Adds a script element at the start of the script.
     * @param {*} obj a string, number, Opcode, Buffer, or object to add
     * @returns {Script} this script instance
     */
    public prepend(obj: any): Script;


    /**
     * @returns {object} The Script type if it is a known form,
     * or Script.UNKNOWN if it isn't
     */
    public classify(): object;
}

export class BlockHeader {

    public toObject(): any;

    /**  The big endian hash buffer */
    public id: string;

    /**  The big endian hash buffer */
    public hash: string;

    /** Block version number */
    public version: number;

    /** 256-bit hash of the previous block header */
    public prevHash: Buffer;

    /** 256-bit hash based on all of the transactions in the block */
    public merkleRoot: Buffer;

    /** Current timestamp as seconds since 1970-01-01T00:00 UTC */
    public time: number;

    /** Current target in compact format */
    public bits: number;

    /** 32-bit number (starts at 0) */
    public nonce: number;

    /**
     * Instantiate a BlockHeader from a Buffer, JSON object, or Object with
     * the properties of the BlockHeader
     *
     * @param {*} - A Buffer, JSON string, or Object
     * @returns {BlockHeader} - An instance of block header
     * @constructor
     */
    constructor(arg: Buffer | string | object);

    /**
     * @param {Object} - A plain JavaScript object
     * @returns {BlockHeader} - An instance of block header
     */
    public static fromObject(obj: object): BlockHeader;

    /**
     * @param {Binary} - Raw block binary data or buffer
     * @returns {BlockHeader} - An instance of block header
     */
    public static fromRawBlock(data: string | Buffer): BlockHeader;

    /**
     * @param {Buffer} - A buffer of the block header
     * @returns {BlockHeader} - An instance of block header
     */
    public static fromBuffer(buf: Buffer): BlockHeader;

    /**
     * @param {string} - A hex encoded buffer of the block header
     * @returns {BlockHeader} - An instance of block header
     */
    public static fromString(str: string): BlockHeader;
}

/**
 * A Transaction contains a set of inputs and a set of outputs. Each input 
 * contains a reference to another transaction's output, and a signature that 
 * allows the value referenced in that output to be used in this transaction.
 */
export class Transaction {

    public inputs: Input[];

    public outputs: Output[];

    public outputAmount: number;

    /**
     * Represents a transaction, a set of inputs and outputs to change ownership of tokens
     *
     * @param {*} serialized
     * @constructor
     */
    constructor(serialize?: any);

    /** Retrieve the little endian hash of the transaction including witness data */
    public witnessHash: Buffer;

    /** Retrieve the little endian hash of the transaction (used for serialization) */
    public hash: string;

    public nLockTime: number;

    /**
     * Sets nLockTime so that transaction is not valid until the desired date(a
     * timestamp in seconds since UNIX epoch is also accepted)
     *
     * @param {Date | Number} time
     * @return {Transaction} this
     */
    public lockUntilDate(time: Date | number): Transaction;

    /**
     * Sets nLockTime so that transaction is not valid until the desired block
     * height.
     *
     * @param {Number} height
     * @return {Transaction} this
     */
    public lockUntilBlockHeight(height: number): Transaction;

    /**
     * Manually set the fee for this transaction. Beware that this resets all the signatures
     * for inputs (in further versions, SIGHASH_SINGLE or SIGHASH_NONE signatures will not
     * be reset).
     *
     * @param {number} amount satoshis to be sent
     * @return {Transaction} this, for chaining
     */
    public fee(amount: number): Transaction;

    /**
     * Manually set the fee per KB for this transaction. Beware that this resets all the signatures
     * for inputs (in further versions, SIGHASH_SINGLE or SIGHASH_NONE signatures will not
     * be reset).
     *
     * @param {number} amount satoshis per KB to be sent
     * @return {Transaction} this, for chaining
     */
    public feePerKb(amount: number): Transaction;

    /**
     * Set the change address for this transaction
     *
     * Beware that this resets all the signatures for inputs (in further versions,
     * SIGHASH_SINGLE or SIGHASH_NONE signatures will not be reset).
     *
     * @param {Address} address An address for change to be sent to.
     * @return {Transaction} this, for chaining
     */
    public change(address: Address): Transaction;

    public toString(): string;

    public hasWitnesses(): boolean;

    public fromBuffer(buffer: Buffer): Transaction;

    public fromString(str: string): Transaction;

    public toObject(): object;

    public toJSON(): string;

    public fromObject(arg: object | Transaction);

    /**
     * Calculates the fee of the transaction.
     * If there's a fixed fee set, return that.
     * If there is no change output set, the fee is the total value of the 
     * outputs minus inputs. Note that a serialized transaction only specifies 
     * the value of its outputs. (The value of inputs are recorded in the previous 
     * transaction outputs being spent.) This method therefore raises a 
     * "MissingPreviousOutput" error when called on a serialized transaction.
     * If there's no fee set and no change address, estimate the fee based on size.
     */
    public getFee(): number;

    /**
     * Analogous to bitcoind's IsCoinBase function in transaction.h
     */
    public isCoinbase(): boolean;

    public toBuffer(): Buffer;
}

/**
 * A Block instance represents the information of a block in the bitcoin network. 
 * Given a hexadecimal string representation of the serialization of a block with 
 * its transactions, you can instantiate a Block instance. Methods are provided 
 * to calculate and check the merkle root hash (if enough data is provided), but
 * transactions won't necessarily be valid spends, and this class won't validate
 * them. A binary representation as a Buffer instance is also valid input for a 
 * Block's constructor.
 */
export class Block {
    /**  The big endian hash buffer of the header */
    public id: string;

    /**  The big endian hash buffer of the header */
    public hash: string;

    /** Instance of block header */
    public header: BlockHeader;

    /** The set of transactions in a block  */
    public transactions: Array<Transaction>;

    public toObject(): object;

    public toJSON(): object;

    /**
     * Instantiate a Block from a Buffer, JSON object, or Object with
     * the properties of the Block
     *
     * @param {*} - A Buffer, JSON string, or Object
     * @returns {Block}
     * @constructor
     */
    constructor(arg: Buffer | string | object);

    /**
     * @param {Object} - A plain JavaScript object
     * @returns {Block} - An instance of block
     */
    public static fromObject(obj: object): Block;

    /**
     * @param {BufferReader} - A buffer reader of the block
     * @returns {Block} - An instance of block
     */
    public static fromBufferReader(br: any): Block;

    /**
     * @param {Buffer} - A buffer of the block
     * @returns {Block} - An instance of block
     */
    public static fromBuffer(buf: Buffer): Block;

    /**
     * @param {string} - str - A hex encoded string of the block
     * @returns {Block} - A hex encoded string of the block
     */
    public static fromString(str: string): Block;

    /**
     * @param {Binary} - Raw block binary data or buffer
     * @returns {Block} - An instance of block
     */
    public static fromRawBlock(data: Buffer | string): Block;

    public toBuffer(): Buffer;
}

export class Address {

    /**
     * Will return a buffer representation of the address
     *
     * @returns {Buffer} Bitcoin address buffer
     */
    public toBuffer(): Buffer;

    /**
     * Instantiate an address from an address string
     *
     * @param {string} str - An string of the bitcoin address
     * @param {String|Network=} network - either a Network instance, 'livenet', or 'testnet'
     * @param {string=} type - The type of address: 'script' or 'pubkey'
     * @returns {Address} A new valid and frozen instance of an Address
     */
    public static fromString(str: string, network: string | Network, type: string): Buffer;

    /**
     * Instantiate an address from an address String or Buffer, a public key or script hash Buffer,
     * or an instance of {@link PublicKey} or {@link Script}.
     *
     * This is an immutable class, and if the first parameter provided to this constructor is an
     * `Address` instance, the same argument will be returned.
     *
     * An address has two key properties: `network` and `type`. The type is either
     * `Address.PayToPublicKeyHash` (value is the `'pubkeyhash'` string)
     * or `Address.PayToScriptHash` (the string `'scripthash'`). The network is an instance of {@link Network}.
     * You can quickly check whether an address is of a given kind by using the methods
     * `isPayToPublicKeyHash` and `isPayToScriptHash`
     *
     * @example
     * ```javascript
     * // validate that an input field is valid
     * var error = Address.getValidationError(input, 'testnet');
     * if (!error) {
     *   var address = Address(input, 'testnet');
     * } else {
     *   // invalid network or checksum (typo?)
     *   var message = error.messsage;
     * }
     *
     * // get an address from a public key
     * var address = Address(publicKey, 'testnet').toString();
     * ```
     *
     * @param {*} data - The encoded data in various formats
     * @param {Network|String|number=} network - The network: 'livenet' or 'testnet'
     * @param {string=} type - The type of address: 'script' or 'pubkey'
     * @constructor
     */
    constructor(data: any, network?: (Network | string | number), type?: ('scripthash' | 'pubkeyhash'));

    /**
     * Will return a boolean if an address is valid
     *
     * @example
     * ```javascript
     * assert(Address.isValid('15vkcKf7gB23wLAnZLmbVuMiiVDc1Nm4a2', 'livenet'));
     * ```
     *
     * @param {string} data - The encoded data
     * @param {String|Network} network - either a Network instance, 'livenet', or 'testnet'
     * @param {string} type - The type of address: 'script' or 'pubkey'
     * @returns {boolean} The corresponding error message
     */
    static isValid(data: string, network: (string | Network), type?: ('scripthash' | 'pubkeyhash')): boolean;


    /**
     * Builds a p2sh address paying to script. This will hash the script and
     * use that to create the address.
     * If you want to extract an address associated with a script instead,
     * see {{Address#fromScript}}
     *
     * @param {Script} script - An instance of Script
     * @param {String|Network} network - either a Network instance, 'livenet', or 'testnet'
     * @returns {Address} A new valid and frozen instance of an Address
     */
    static payingTo(script: Script, network: string | Network): Address;
}

export class Opcode {
    public static map: {
        // push value
        OP_FALSE: 0,
        OP_0: 0,
        OP_PUSHDATA1: 76,
        OP_PUSHDATA2: 77,
        OP_PUSHDATA4: 78,
        OP_1NEGATE: 79,
        OP_RESERVED: 80,
        OP_TRUE: 81,
        OP_1: 81,
        OP_2: 82,
        OP_3: 83,
        OP_4: 84,
        OP_5: 85,
        OP_6: 86,
        OP_7: 87,
        OP_8: 88,
        OP_9: 89,
        OP_10: 90,
        OP_11: 91,
        OP_12: 92,
        OP_13: 93,
        OP_14: 94,
        OP_15: 95,
        OP_16: 96,

        // control
        OP_NOP: 97,
        OP_VER: 98,
        OP_IF: 99,
        OP_NOTIF: 100,
        OP_VERIF: 101,
        OP_VERNOTIF: 102,
        OP_ELSE: 103,
        OP_ENDIF: 104,
        OP_VERIFY: 105,
        OP_RETURN: 106,

        // stack ops
        OP_TOALTSTACK: 107,
        OP_FROMALTSTACK: 108,
        OP_2DROP: 109,
        OP_2DUP: 110,
        OP_3DUP: 111,
        OP_2OVER: 112,
        OP_2ROT: 113,
        OP_2SWAP: 114,
        OP_IFDUP: 115,
        OP_DEPTH: 116,
        OP_DROP: 117,
        OP_DUP: 118,
        OP_NIP: 119,
        OP_OVER: 120,
        OP_PICK: 121,
        OP_ROLL: 122,
        OP_ROT: 123,
        OP_SWAP: 124,
        OP_TUCK: 125,

        // splice ops
        OP_CAT: 126,
        OP_SUBSTR: 127,
        OP_LEFT: 128,
        OP_RIGHT: 129,
        OP_SIZE: 130,

        // bit logic
        OP_INVERT: 131,
        OP_AND: 132,
        OP_OR: 133,
        OP_XOR: 134,
        OP_EQUAL: 135,
        OP_EQUALVERIFY: 136,
        OP_RESERVED1: 137,
        OP_RESERVED2: 138,

        // numeric
        OP_1ADD: 139,
        OP_1SUB: 140,
        OP_2MUL: 141,
        OP_2DIV: 142,
        OP_NEGATE: 143,
        OP_ABS: 144,
        OP_NOT: 145,
        OP_0NOTEQUAL: 146,

        OP_ADD: 147,
        OP_SUB: 148,
        OP_MUL: 149,
        OP_DIV: 150,
        OP_MOD: 151,
        OP_LSHIFT: 152,
        OP_RSHIFT: 153,

        OP_BOOLAND: 154,
        OP_BOOLOR: 155,
        OP_NUMEQUAL: 156,
        OP_NUMEQUALVERIFY: 157,
        OP_NUMNOTEQUAL: 158,
        OP_LESSTHAN: 159,
        OP_GREATERTHAN: 160,
        OP_LESSTHANOREQUAL: 161,
        OP_GREATERTHANOREQUAL: 162,
        OP_MIN: 163,
        OP_MAX: 164,

        OP_WITHIN: 165,

        // crypto
        OP_RIPEMD160: 166,
        OP_SHA1: 167,
        OP_SHA256: 168,
        OP_HASH160: 169,
        OP_HASH256: 170,
        OP_CODESEPARATOR: 171,
        OP_CHECKSIG: 172,
        OP_CHECKSIGVERIFY: 173,
        OP_CHECKMULTISIG: 174,
        OP_CHECKMULTISIGVERIFY: 175,

        OP_CHECKLOCKTIMEVERIFY: 177,
        OP_CHECKSEQUENCEVERIFY: 178,

        // expansion
        OP_NOP1: 176,
        OP_NOP2: 177,
        OP_NOP3: 178,
        OP_NOP4: 179,
        OP_NOP5: 180,
        OP_NOP6: 181,
        OP_NOP7: 182,
        OP_NOP8: 183,
        OP_NOP9: 184,
        OP_NOP10: 185,

        // template matching params
        OP_PUBKEYHASH: 253,
        OP_PUBKEY: 254,
        OP_INVALIDOPCODE: 255
    };

}