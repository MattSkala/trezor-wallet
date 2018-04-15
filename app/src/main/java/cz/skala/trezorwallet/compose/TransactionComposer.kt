package cz.skala.trezorwallet.compose

import android.util.Log
import com.google.protobuf.ByteString
import com.satoshilabs.trezor.intents.hexToBytes
import com.satoshilabs.trezor.intents.toHex
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.exception.InsufficientFundsException

class TransactionComposer(val database: AppDatabase, val coinSelector: CoinSelector) {
    /**
     * Composes a new transaction.
     *
     * @param [accountId] An account to spend UTXOs from.
     * @param [address] Target Bitcoin address encoded as Base58Check.
     * @param [amount] Amount in satoshis to be sent to the target address.
     * @param [feeRate] Mining fee in satoshis per byte.
     */
    @Throws(InsufficientFundsException::class)
    fun composeTransaction(accountId: String, address: String, amount: Long, feeRate: Int):
            Pair<TrezorType.TransactionType, Map<String, TrezorType.TransactionType>> {
        val account = database.accountDao().getById(accountId)

        val utxoSet = database.transactionDao().getUnspentOutputs(account.id)

        val outputs = mutableListOf<TrezorType.TxOutputType>()
        outputs += TrezorType.TxOutputType.newBuilder()
                .setAddress(address)
                .setAmount(amount)
                .setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS)
                .build()

        val (utxo, fee) = coinSelector.select(utxoSet, outputs, feeRate, !account.legacy)

        addChangeOutput(account, utxo, outputs, fee)

        val trezorInputs = createTrezorInputs(account, utxo)
        val trezorOutputs = outputs

        val trezorTransaction = TrezorType.TransactionType.newBuilder()
                .addAllInputs(trezorInputs)
                .addAllOutputs(trezorOutputs)
                .setInputsCnt(trezorInputs.size)
                .setOutputsCnt(trezorOutputs.size)
                .build()

        val inputTransactions = mutableMapOf<String, TrezorType.TransactionType>()

        utxo.forEach {
            val tx = database.transactionDao().getByTxid(accountId, it.txid)
            val txType = toTrezorTransactionType(tx)
            inputTransactions[it.txid] = txType
        }

        Log.d("TransactionComposer", "TREZOR Transaction")
        Log.d("TransactionComposer", trezorTransaction.toString())
        Log.d("TransactionComposer", "Referenced Transactions")
        inputTransactions.forEach { key, value ->
            Log.d("TransactionComposer", "txid: $key")
            Log.d("TransactionComposer", value.toString())
            value.inputsList.forEachIndexed { i, input ->
                Log.d("TransactionComposer", "input #$i " + input.prevHash.toByteArray().toHex() + " " + input.prevIndex + " " + input.scriptSig.toByteArray().toHex() + " " + input.sequence)
            }
            value.binOutputsList.forEachIndexed { i, output ->
                Log.d("TransactionComposer", "output #$i " + output.amount + " " + output.scriptPubkey.toByteArray().toHex())
            }
        }

        return Pair(trezorTransaction, inputTransactions)
    }

    private fun createTrezorInputs(account: Account, utxo: List<TransactionOutput>): List<TrezorType.TxInputType> {
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
        // TODO: get fresh change address
        val changeAddress = database.addressDao().getByAccount(account.id, true).first()

        val changeScriptType = if (account.legacy) {
            TrezorType.OutputScriptType.PAYTOADDRESS
        } else {
            TrezorType.OutputScriptType.PAYTOP2SHWITNESS
        }

        var inputsValue = 0L
        inputs.forEach { inputsValue += it.value }

        var outputsValue = 0L
        outputs.forEach { outputsValue += it.amount }

        val changeValue = inputsValue - outputsValue - fee

        Log.d("TransactionComposer", "inputs: $inputsValue outputs: $outputsValue fee: $fee change: $changeValue")

        if (changeValue >= CoinSelector.MINIMUM_OUTPUT_VALUE) {
            outputs += TrezorType.TxOutputType.newBuilder()
                    .addAllAddressN(changeAddress.getPath(account).toList())
                    .setAmount(changeValue)
                    .setScriptType(changeScriptType)
                    .build()
        }
    }

    /**
     * Converts an existing transaction from db into TransactionType for usage in TREZOR request.
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