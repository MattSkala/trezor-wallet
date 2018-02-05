package cz.skala.trezorwallet.crypto

import org.junit.Assert
import org.junit.Test

class ExtendedPublicKeyTest {
    @Test
    fun isHardened() {
        Assert.assertEquals(false, ExtendedPublicKey.isHardened(0))
        Assert.assertEquals(false, ExtendedPublicKey.isHardened(100))
        Assert.assertEquals(false, ExtendedPublicKey.isHardened(1 shl 31 - 1))
        Assert.assertEquals(true, ExtendedPublicKey.isHardened(1L shl 31))
        Assert.assertEquals(true, ExtendedPublicKey.isHardened((1L shl 31) + 100))
        Assert.assertEquals(true, ExtendedPublicKey.isHardened((1L shl 32) - 1))
    }
}
