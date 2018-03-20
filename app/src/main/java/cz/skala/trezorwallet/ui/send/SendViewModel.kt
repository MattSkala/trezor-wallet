package cz.skala.trezorwallet.ui.send

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.util.Log
import com.google.protobuf.ByteString
import com.satoshilabs.trezor.intents.ui.data.SignTxRequest
import com.satoshilabs.trezor.intents.ui.data.TrezorRequest
import com.satoshilabs.trezor.intents.ui.hexToBytes
import com.satoshilabs.trezor.intents.ui.toHex
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.ui.BTC_TO_SATOSHI
import cz.skala.trezorwallet.ui.SingleLiveEvent
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import org.spongycastle.util.encoders.Hex

/**
 * A ViewModel for SendFragment.
 */
class SendViewModel(val database: AppDatabase) : ViewModel() {
    companion object {
        private const val TAG = "SendViewModel"
    }

    val trezorRequest = SingleLiveEvent<TrezorRequest>()

    fun composeTransaction(accountId: String, address: String, amount: Double, fee: Int) {
        launch(UI) {
            val (tx, inputTransactions) = bg {
                val account = database.accountDao().getById(accountId)

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

                //val outputs = mutableListOf<TransactionOutput>()
                val trezorOutputs = mutableListOf<TrezorType.TxOutputType>()
                var outputsValue = 0.0

                // TODO: get fresh change address
                val changeAddress = database.addressDao().getByAccount(account.id, true).first()

                // TODO: calculate fee
                var feeValue = 0.0
                var changeValue = inputsValue - amount - feeValue

                Log.d(TAG, "inputs: " + selectedUtxo.size + " " + inputsValue)
                selectedUtxo.forEachIndexed { index, input ->
                    Log.d(TAG, "#$index ${input.txid}:${input.n} ${input.value}")
                }

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

                Log.d(TAG, "outputs:")
                trezorOutputs.forEachIndexed { index, output ->
                    Log.d(TAG, "#$index ${output.address} ${output.amount}")
                }

                Log.d(TAG, "fee: $feeValue")

                // TODO: change key to string
                val inputTransactions = mutableMapOf<ByteArray, TrezorType.TransactionType>()

                val trezorInputs = selectedUtxo.map {
                    val addr = database.addressDao().getByAddress(account.id, it.addr!!)

                    val txidBytes = Hex.decode(it.txid)
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


                selectedUtxo.forEach {
                    val tx = database.transactionDao().getByTxid(it.txid)
                    val txType = toTrezorTransactionType(tx, account)
                    val txidBytes = Hex.decode(it.txid)

                    Log.d(TAG, "input tx: " + it.txid)
                    Log.d(TAG, "input hex: " + it.txid.hexToBytes().toHex())

                    inputTransactions[txidBytes] = txType
                }

                val trezorTransaction = TrezorType.TransactionType.newBuilder()
                        .addAllInputs(trezorInputs)
                        .addAllOutputs(trezorOutputs)
                        .setInputsCnt(trezorInputs.size)
                        .setOutputsCnt(trezorOutputs.size)
                        .build()

                Pair(trezorTransaction, inputTransactions)
            }.await()

            val signRequest = SignTxRequest(tx, inputTransactions)
            trezorRequest.value = signRequest
        }
    }

    fun validateAddress(address: String): Boolean {
        // TODO
        return true
    }

    fun validateAmount(amount: Double): Boolean {
        // TODO
        return true
    }

    fun validateFee(fee: Int): Boolean {
        // TODO
        return true
    }

    private fun toTrezorTransactionType(tx: TransactionWithInOut, account: Account): TrezorType.TransactionType {
        val trezorInputs = tx.vin.map {
            //val address = database.addressDao().getByAddress(it.addr)
            val prevHash = ByteString.copyFrom(it.txid.hexToBytes())
            val scriptSig = ByteString.copyFrom(it.scriptSig.hexToBytes())

            val builder = TrezorType.TxInputType.newBuilder()
                    //.addAllAddressN(address.getPath(account).toList())
                    .setPrevHash(prevHash)
                    .setPrevIndex(it.n)
                    .setScriptSig(scriptSig)

            if (account.legacy) {
                builder.scriptType = TrezorType.InputScriptType.SPENDADDRESS
            } else {
                builder.scriptType = TrezorType.InputScriptType.SPENDP2SHWITNESS
                builder.amount = (it.value * BTC_TO_SATOSHI).toLong()
            }

            builder.build()
        }

        val trezorOutputs = tx.vout.map {
            // TODO: set script type for change outputs
            TrezorType.TxOutputType.newBuilder()
                    .setAddress(it.addr)
                    .setAmount((it.value * BTC_TO_SATOSHI).toLong())
                    .setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS)
                    .build()
        }

        return TrezorType.TransactionType.newBuilder()
                .setVersion(tx.tx.version)
                .addAllInputs(trezorInputs)
                .addAllOutputs(trezorOutputs)
                .setInputsCnt(trezorInputs.size)
                .setOutputsCnt(trezorOutputs.size)
                .build()
    }

    class Factory(val database: AppDatabase) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SendViewModel(database) as T
        }
    }
}