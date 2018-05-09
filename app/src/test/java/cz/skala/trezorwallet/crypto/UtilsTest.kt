package cz.skala.trezorwallet.crypto

import com.satoshilabs.trezor.intents.toHex
import org.junit.Assert
import org.junit.Test
import java.util.*


class UtilsTest {
    @Test
    fun encryptDecrypt() {
        val plainText = "Hello world!".toByteArray()
        val password = ByteArray(32)
        Random().nextBytes(password)
        val (iv, tag, cipherText) = encryptAesGcm(plainText, password)
        val decrypted = decryptAesGcm(iv, tag, cipherText, password)
        Assert.assertEquals(plainText.toString(Charsets.UTF_8), decrypted.toString(Charsets.UTF_8))
        Assert.assertEquals(plainText.toHex(), decrypted.toHex())
    }
}