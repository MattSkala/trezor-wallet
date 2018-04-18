package cz.skala.trezorwallet.compose

import com.google.protobuf.ByteString
import com.satoshilabs.trezor.intents.hexToBytes
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.exception.InsufficientFundsException

/**
 * The transaction composer builds an unsigned transaction using UTXOs on a specified account.
 * At the moment, it can build only transactions with a single target address. It allows specifying
 * a custom fee rate.
 */
class TransactionComposer(
        private val database: AppDatabase,
        private val coinSelector: CoinSelector
) {
    /**
     * Composes a new transaction.
     *
     * @param accountId An account to spend UTXOs from.
     * @param address Target Bitcoin address encoded as Base58Check.
     * @param amount Amount in satoshis to be sent to the target address.
     * @param feeRate Mining fee in satoshis per byte.
     * @return A pair of the constructed transaction and a map of referenced non-SegWit
     * transactions with TXIDs as keys.
     */
    @Throws(InsufficientFundsException::class)
    fun composeTransaction(accountId: String, address: String, amount: Long, feeRate: Int):
            Pair<TrezorType.TransactionType, Map<String, TrezorType.TransactionType>> {
        val account = database.accountDao().getById(accountId)
        val utxoSet = database.transactionDao().getUnspentOutputs(accountId)

        val outputs = mutableListOf<TrezorType.TxOutputType>()
        outputs += TrezorType.TxOutputType.newBuilder()
                .setAddress(address)
                .setAmount(amount)
                .setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS)
                .build()

        val (utxo, fee) = coinSelector.select(utxoSet, outputs, feeRate, !account.legacy)

        addChangeOutput(account, utxo, outputs, fee)

        val inputs = createTrezorInputs(account, utxo)

        val transaction = TrezorType.TransactionType.newBuilder()
                .addAllInputs(inputs)
                .addAllOutputs(outputs)
                .setInputsCnt(inputs.size)
                .setOutputsCnt(outputs.size)
                .build()

        // Provide a list of referenced transactions for non-SegWit inputs
        val referencedTransactions = mutableMapOf<String, TrezorType.TransactionType>()
        if (account.legacy) {
            utxo.forEach {
                val tx = database.transactionDao().getByTxid(accountId, it.txid)
                referencedTransactions[it.txid] = toTrezorTransactionType(tx)
            }
        }

        return Pair(transaction, referencedTransactions)
    }

    /**
     * Maps TransctionOutput entities to TxInputType objects.
     */
    private fun createTrezorInputs(account: Account, utxo: List<TransactionOutput>):
            List<TrezorType.TxInputType> {
        return utxo.map {
            val addr = database.addressDao().getByAddress(account.id, it.addr!!)

            val txidBytes = it.txid.hexToBytes()
            val prevHash = ByteString.copyFrom(txidBytes)

            val builder = TrezorType.TxInputType.newBuilder()
                    .addAllAddressN(addr.getPath(account).toList())
                    .setPrevHash(prevHash)
                    .setPrevIndex(it.n)

            if (account.legacy) {
                builder.scriptType = TrezorType.InputScriptType.SPENDADDRESS
            } else {
                builder.scriptType = TrezorType.InputScriptType.SPENDP2SHWITNESS
                builder.amount = it.value
            }

            builder.build()
        }
    }

    /**
     * Adds a change output if the output value is greater than the minimum output value.
     */
    private fun addChangeOutput(account: Account, inputs: List<TransactionOutput>,
                                outputs: MutableList<TrezorType.TxOutputType>, fee: Int) {
        // Get fresh change address
        val changeAddress = database.addressDao().getByAccount(account.id, true).first {
            it.totalReceived == 0.0
        }

        val changeScriptType = if (account.legacy) {
            TrezorType.OutputScriptType.PAYTOADDRESS
        } else {
            TrezorType.OutputScriptType.PAYTOP2SHWITNESS
        }

        val inputsValue = inputs.sumBy { it.value.toInt() }
        val outputsValue = outputs.sumBy { it.amount.toInt() }
        val changeValue = inputsValue - outputsValue - fee

        if (changeValue >= CoinSelector.DUST_THRESHOLD) {
            outputs += TrezorType.TxOutputType.newBuilder()
                    .addAllAddressN(changeAddress.getPath(account).toList())
                    .setAmount(changeValue.toLong())
                    .setScriptType(changeScriptType)
                    .build()
        }
    }

    /**
     * Converts an existing transaction entity into TransactionType for usage in TREZOR request.
     */
    private fun toTrezorTransactionType(tx: TransactionWithInOut): TrezorType.TransactionType {
        val trezorInputs = tx.vin.map {
            val prevHash = ByteString.copyFrom(it.txid.hexToBytes())
            val scriptSig = ByteString.copyFrom(it.scriptSig.hexToBytes())

            TrezorType.TxInputType.newBuilder()
                    .setPrevHash(prevHash)
                    .setPrevIndex(it.vout)
                    .setScriptSig(scriptSig)
                    .setSequence(it.sequence.toInt())
                    .build()
        }

        val trezorOutputs = tx.vout.map {
            val scriptPubKey = ByteString.copyFrom(it.scriptPubKey.hexToBytes())
            TrezorType.TxOutputBinType.newBuilder()
                    .setAmount(it.value)
                    .setScriptPubkey(scriptPubKey)
                    .build()
        }

        return TrezorType.TransactionType.newBuilder()
                .setVersion(tx.tx.version)
                .setLockTime(tx.tx.locktime)
                .addAllInputs(trezorInputs)
                .addAllBinOutputs(trezorOutputs)
                .setInputsCnt(trezorInputs.size)
                .setOutputsCnt(trezorOutputs.size)
                .build()
    }
}