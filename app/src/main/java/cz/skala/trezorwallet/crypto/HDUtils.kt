package cz.skala.trezorwallet.crypto

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