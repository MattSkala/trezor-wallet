package cz.skala.trezorwallet.crypto

import com.satoshilabs.trezor.intents.hexToBytes
import org.junit.Assert
import org.junit.Test

class Base58Test {
    @Test
    fun encodeBase58_success() {
        Assert.assertEquals("3WyEDWjcVB", encodeBase58("Bitcoin".toByteArray()))
        Assert.assertEquals("JxF12TrwUP45BMd", encodeBase58("Hello World".toByteArray()))
    }

    @Test
    fun encodeBase58Check_success() {
        Assert.assertEquals("2BbfdJHs7ESiguV86hDsG1pmdVC8mkV4HP1TxLSpRGeRxngLD6", encodeBase58Check("9bc26af71dec8e58c541f71c75b182cea08f2448fa0f0adc10c0d9d3c51e3b57".hexToBytes()))
    }
}