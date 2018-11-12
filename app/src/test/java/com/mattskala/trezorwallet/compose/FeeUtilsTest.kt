package com.mattskala.trezorwallet.compose

import com.satoshilabs.trezor.lib.protobuf.TrezorType
import org.junit.Assert
import org.junit.Test

class FeeUtilsTest {
    @Test
    fun scriptLength() {
        val p2shwitness = TrezorType.TxOutputType.newBuilder()
                .setScriptType(TrezorType.OutputScriptType.PAYTOP2SHWITNESS).buildPartial()
        val witness = TrezorType.TxOutputType.newBuilder()
                .setScriptType(TrezorType.OutputScriptType.PAYTOWITNESS).buildPartial()
        val p2pkh = TrezorType.TxOutputType.newBuilder()
                .setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS)
                .setAddress("1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2").buildPartial()
        val p2sh = TrezorType.TxOutputType.newBuilder()
                .setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS)
                .setAddress("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy").buildPartial()
        val p2wpkh = TrezorType.TxOutputType.newBuilder()
                .setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS)
                .setAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4").buildPartial()
        val p2wsh = TrezorType.TxOutputType.newBuilder()
                .setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS)
                .setAddress("bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3")
                .buildPartial()

        Assert.assertEquals(P2SH_OUTPUT_SCRIPT_LENGTH,
                scriptLength(p2shwitness))
        Assert.assertEquals(P2WPKH_OUTPUT_SCRIPT_LENGTH,
                scriptLength(witness))
        Assert.assertEquals(P2PKH_OUTPUT_SCRIPT_LENGTH,
                scriptLength(p2pkh))
        Assert.assertEquals(P2SH_OUTPUT_SCRIPT_LENGTH,
                scriptLength(p2sh))
        Assert.assertEquals(P2WPKH_OUTPUT_SCRIPT_LENGTH,
                scriptLength(p2wpkh))
        Assert.assertEquals(P2WSH_OUTPUT_SCRIPT_LENGTH,
                scriptLength(p2wsh))
    }

    @Test
    fun changeOutputBytes() {
        Assert.assertEquals(32, changeOutputBytes(true))
        Assert.assertEquals(34, changeOutputBytes(false))
    }

    @Test
    fun estimateFee() {
        val outputs = mutableListOf<TrezorType.TxOutputType>()
        outputs += TrezorType.TxOutputType.newBuilder()
                .setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS)
                .setAddress("1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2")
                .buildPartial()
        val feeSegwit = calculateFee(2, outputs, 1, true)
        val feeLegacy = calculateFee(2, outputs, 1, false)
        Assert.assertEquals(11 + 2 * (41 + 51) + (9 + 25), feeSegwit)
        Assert.assertEquals(11 + 2 * (41 + 109) + (9 + 25), feeLegacy)
    }
}