package cz.skala.trezorwallet.crypto

import junit.framework.Assert
import org.junit.Test

class Base58Test {
    @Test
    fun encodeBase58_success() {
        Assert.assertEquals("3WyEDWjcVB", encodeBase58("Bitcoin".toByteArray()))
        Assert.assertEquals("JxF12TrwUP45BMd", encodeBase58("Hello World".toByteArray()))
    }
}