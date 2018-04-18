package cz.skala.trezorwallet.compose

import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.exception.InsufficientFundsException
import junit.framework.Assert
import org.junit.Test

class FifoCoinSelectorTest {
    private val selector = FifoCoinSelector()

    private fun createUtxo(value: Long): TransactionOutput {
        return TransactionOutput("", "", "", 0, "",
                value, null, true, false, "",
                null)
    }

    @Test
    fun select_singleInput() {
        val utxoSet = mutableListOf<TransactionOutput>()
        utxoSet += createUtxo(10000L)
        utxoSet += createUtxo(2000L)

        val outputs = mutableListOf<TrezorType.TxOutputType>()
        outputs += TrezorType.TxOutputType.newBuilder()
                .setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS)
                .setAddress("1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2")
                .setAmount(5000L)
                .buildPartial()

        val (inputs, _) = selector.select(utxoSet, outputs, 1, true)

        Assert.assertEquals(1, inputs.size)
        Assert.assertEquals(10000L, inputs[0].value)
    }

    @Test
    fun select_multipleInputs() {
        val utxoSet = mutableListOf<TransactionOutput>()
        utxoSet += createUtxo(1000L)
        utxoSet += createUtxo(2000L)
        utxoSet += createUtxo(3000L)

        val outputs = mutableListOf<TrezorType.TxOutputType>()
        outputs += TrezorType.TxOutputType.newBuilder()
                .setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS)
                .setAddress("1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2")
                .setAmount(5000L)
                .buildPartial()

        val (inputs, _) = selector.select(utxoSet, outputs, 1, true)

        Assert.assertEquals(3, inputs.size)
        Assert.assertEquals(1000L, inputs[0].value)
        Assert.assertEquals(2000L, inputs[1].value)
        Assert.assertEquals(3000L, inputs[2].value)
    }

    @Test(expected = InsufficientFundsException::class)
    fun select_insufficientFunds() {
        val utxoSet = mutableListOf<TransactionOutput>()
        utxoSet += createUtxo(1000L)
        utxoSet += createUtxo(2000L)

        val outputs = mutableListOf<TrezorType.TxOutputType>()
        outputs += TrezorType.TxOutputType.newBuilder()
                .setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS)
                .setAddress("1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2")
                .setAmount(10000L)
                .buildPartial()

        selector.select(utxoSet, outputs, 1, true)
    }
}