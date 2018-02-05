package cz.skala.trezorwallet.crypto

import org.spongycastle.crypto.digests.RIPEMD160Digest
import org.spongycastle.crypto.digests.SHA256Digest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


const val HMAC_SHA512 = "HmacSHA512"
const val SECP256K1 = "secp256k1"


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
