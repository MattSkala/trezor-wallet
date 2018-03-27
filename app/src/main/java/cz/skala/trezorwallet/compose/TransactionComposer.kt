package cz.skala.trezorwallet.compose

import com.google.protobuf.ByteString
import com.satoshilabs.trezor.intents.hexToBytes
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.ui.BTC_TO_SATOSHI

class TransactionComposer(val database: AppDatabase) {
    /**
     * Composes a new transaction.
     *
     * @param [accountId] An account to spend UTXOs from.
     * @param [address] Target Bitcoin address encoded as Base58Check.
     * @param [fee] Mining fee in satoshis per byte.
     */
    fun composeTransaction(accountId: String, address: String, amount: Double, fee: Int):
            Pair<TrezorType.TransactionType, Map<String, TrezorType.TransactionType>> {
        val account = database.accountDao().getById(accountId)

        val utxo = findUtxo(account, amount)

        val trezorInputs = createTrezorInputs(account, utxo)
        val trezorOutputs = createTrezorOutputs(account, utxo, address, amount, fee)

        val trezorTransaction = TrezorType.TransactionType.newBuilder()
                .addAllInputs(trezorInputs)
                .addAllOutputs(trezorOutputs)
                .setInputsCnt(trezorInputs.size)
                .setOutputsCnt(trezorOutputs.size)
                .build()

        val inputTransactions = mutableMapOf<String, TrezorType.TransactionType>()

        utxo.forEach {
            val tx = database.transactionDao().getByTxid(it.txid)
            val txType = toTrezorTransactionType(tx, account)
            inputTransactions[it.txid] = txType
        }

        return Pair(trezorTransaction, inputTransactions)
    }

    /**
     * Finds unspent transaction outputs with total value greater than the specified amount.
     *
     * @param account An account to find outputs for.
     * @param amount Total outputs value in BTC.
     */
    private fun findUtxo(account: Account, amount: Double): List<TransactionOutput> {
        val utxoSet = database.transactionDao().getUnspentOutputs(account.id)
        val selectedUtxo = mutableListOf<TransactionOutput>()
        var inputsValue = 0.0

        for (utxo in utxoSet) {
            if (inputsValue < amount) {
                selectedUtxo += utxo
                inputsValue += utxo.value
            } else {
                break
            }
        }

        return selectedUtxo
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
                builder.amount = (it.value * BTC_TO_SATOSHI).toLong()
            }

            builder.build()
        }
    }

    private fun createTrezorOutputs(account: Account, inputs: List<TransactionOutput>, address: String,
                                    amount: Double, fee: Int): List<TrezorType.TxOutputType> {
        val trezorOutputs = mutableListOf<TrezorType.TxOutputType>()

        // TODO: get fresh change address
        val changeAddress = database.addressDao().getByAccount(account.id, true).first()

        var inputsValue = 0.0
        inputs.forEach { inputsValue += it.value }
        // TODO: better fee estimation
        val feeValue = estimateFee(inputs.size, 2, fee)
        val changeValue = inputsValue - amount - feeValue

        trezorOutputs += TrezorType.TxOutputType.newBuilder()
                .setAddress(address)
                .setAmount((amount * BTC_TO_SATOSHI).toLong())
                .setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS)
                .build()

        val changeScriptType = if (account.legacy) {
            TrezorType.OutputScriptType.PAYTOADDRESS
        } else {
            TrezorType.OutputScriptType.PAYTOP2SHWITNESS
        }
        trezorOutputs += TrezorType.TxOutputType.newBuilder()
                .addAllAddressN(changeAddress.getPath(account).toList())
                .setAmount((changeValue * BTC_TO_SATOSHI).toLong())
                .setScriptType(changeScriptType)
                .build()

        return trezorOutputs
    }

    /**
     * Converts an existing transaction from db into TransactionType for usage in TREZOR request.
     */
    private fun toTrezorTransactionType(tx: TransactionWithInOut, account: Account): TrezorType.TransactionType {
        val trezorInputs = tx.vin.map {
            val prevHash = ByteString.copyFrom(it.txid.hexToBytes())
            val scriptSig = ByteString.copyFrom(it.scriptSig.hexToBytes())

            val builder = TrezorType.TxInputType.newBuilder()
                    .setPrevHash(prevHash)
                    .setPrevIndex(it.vout)
                    .setScriptSig(scriptSig)
                    .setSequence(it.sequence.toInt())

            if (account.legacy) {
                builder.scriptType = TrezorType.InputScriptType.SPENDADDRESS
            } else {
                builder.scriptType = TrezorType.InputScriptType.SPENDP2SHWITNESS
                builder.amount = (it.value * BTC_TO_SATOSHI).toLong()
            }

            builder.build()
        }

        val trezorOutputs = tx.vout.map {
            val scriptPubKey = ByteString.copyFrom(it.scriptPubKey.hexToBytes())
            TrezorType.TxOutputBinType.newBuilder()
                    .setAmount((it.value * BTC_TO_SATOSHI).toLong())
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

    /**
     * Estimates a total fee based on the number of inputs, outputs and desired fee per byte.
     */
    private fun estimateFee(inputs: Int, outputs: Int, fee: Int): Double {
        return estimateTransactionSize(inputs, outputs) * fee / BTC_TO_SATOSHI.toDouble()
    }

    /**
     * Estimates a transaction size in bytes.
     */
    private fun estimateTransactionSize(inputs: Int, outputs: Int): Int {
        return inputs * 180 + outputs * 34 + 10
    }
}