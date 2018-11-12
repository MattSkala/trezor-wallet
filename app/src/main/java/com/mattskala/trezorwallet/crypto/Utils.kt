package com.mattskala.trezorwallet.crypto


import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


const val HMAC_SHA512 = "HmacSHA512"
const val HMAC_SHA256 = "HmacSHA256"
const val SECP256K1 = "secp256k1"

private const val GCM_NONCE_LENGTH = 12 // bytes
private const val GCM_TAG_LENGTH = 16 // bytes

/**
 * Calculates HMAC-SHA512 of [data] with provided [key].
 */
fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
    val sha512hmac = Mac.getInstance(HMAC_SHA512)
    val keySpec = SecretKeySpec(key, HMAC_SHA512)
    sha512hmac.init(keySpec)
    return sha512hmac.doFinal(data)
}

/**
 * Calculates HMAC-SHA256 of [data] with provided [key].
 */
fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val sha256hmac = Mac.getInstance(HMAC_SHA256)
    val keySpec = SecretKeySpec(key, HMAC_SHA256)
    sha256hmac.init(keySpec)
    return sha256hmac.doFinal(data)
}

/**
 * Calculates SHA256 hash.
 */
fun sha256(data: ByteArray): ByteArray {
    return sha256(data, 0, data.size)
}

/**
 * Calculates SHA256 hash of [data] with specified offset and data length.
 */
fun sha256(data: ByteArray, offset: Int, length: Int): ByteArray {
    val digest = SHA256Digest()
    digest.update(data, offset, length)
    val out = ByteArray(digest.digestSize)
    digest.doFinal(out, 0)
    return out
}

/**
 * Calculates RIPEMD160 hash.
 */
fun ripemd160(data: ByteArray): ByteArray {
    val digest = RIPEMD160Digest()
    digest.update(data, 0, data.size)
    val out = ByteArray(digest.digestSize)
    digest.doFinal(out, 0)
    return out
}

/**
 * Calculates RIPEMD160(SHA256(data)) hash.
 */
fun hash160(data: ByteArray): ByteArray {
    return ripemd160(sha256(data))
}

/**
 * Gets a hardened path index.
 */
fun hardened(index: Int): Int {
    return (index + ExtendedPublicKey.HARDENED_IDX).toInt()
}

/**
 * Encrypts [plainText] with AES/GCM using [password] as a key. Returns a triple of
 * an initialization vector, a GCM tag and a cipher text.
 */
fun encryptAesGcm(plainText: ByteArray, password: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
    val cipher = GCMBlockCipher(AESEngine())

    val key = KeyParameter(password)

    val iv = ByteArray(GCM_NONCE_LENGTH)
    SecureRandom().nextBytes(iv)

    val params = AEADParameters(key, GCM_TAG_LENGTH * 8, iv)

    cipher.init(true, params)

    val cipherTextWithTag = ByteArray(cipher.getOutputSize(plainText.size))
    val len = cipher.processBytes(plainText, 0, plainText.size, cipherTextWithTag, 0)

    cipher.doFinal(cipherTextWithTag, len)

    val tag = cipher.mac

    val cipherText = cipherTextWithTag.copyOfRange(0, cipherTextWithTag.size - GCM_TAG_LENGTH)
    return Triple(iv, tag, cipherText)
}

/**
 * Decrypts [cipherText] encrypted with AES/GCM using [password] as a key. It uses [iv] as the
 * initialization vector and checks if [tag] matches the MAC.
 */
fun decryptAesGcm(iv: ByteArray, tag: ByteArray, cipherText: ByteArray, password: ByteArray): ByteArray {
    val cipher = GCMBlockCipher(AESEngine())

    val key = KeyParameter(password)

    val params = AEADParameters(key, GCM_TAG_LENGTH * 8, iv)

    cipher.init(false, params)

    val cipherTextWithTag = cipherText + tag

    val plainText = ByteArray(cipher.getOutputSize(cipherTextWithTag.size))
    val len = cipher.processBytes(cipherTextWithTag, 0, cipherTextWithTag.size, plainText, 0)

    cipher.doFinal(plainText, len)

    return plainText
}
