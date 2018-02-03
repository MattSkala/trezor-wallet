package cz.skala.trezorwallet.crypto

import org.spongycastle.crypto.ec.CustomNamedCurves
import org.spongycastle.math.ec.ECPoint
import org.spongycastle.math.ec.FixedPointCombMultiplier
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.InvalidKeyException
import java.util.*

/**
 * A pair of the public key and the chain code.
 */
class ExtendedPublicKey(val publicKey: ECPoint, val chainCode: ByteArray) {
    companion object {
        const val HARDENED_IDX = 1L shl 31

        fun decodePublicKey(publicKeyEncoded: ByteArray): ECPoint {
            val curveParams = CustomNamedCurves.getByName(SECP256K1)
            return curveParams.curve.decodePoint(publicKeyEncoded)
        }

        /**
         * Checks whether the child key is hardened. Each extended key has 2^31 normal child keys
         * and 2^31 hardened child keys. Normal child keys use indices 0 .. 2^31 - 1.
         */
        fun isHardened(i: Long): Boolean {
            return i >= HARDENED_IDX
        }
    }

    /**
     * Computes a child extended public key from the parent extended public key. It is only defined
     * for non-hardened child keys.
     */
    fun deriveChildKey(index: Int): ExtendedPublicKey {
        val parentNode = this

        if (isHardened(index.toLong())) {
            throw IllegalArgumentException("Derivation defined only for non-hardened child keys")
        }

        val data = ByteBuffer.allocate(37)
        data.put(parentNode.publicKey.getEncoded(true)) // 33 bytes
        data.putInt(index) // 4 bytes
        val i = hmacSha512(parentNode.chainCode, data.array()) // 64 bytes
        val il = Arrays.copyOfRange(i, 0, 32)
        val ir = Arrays.copyOfRange(i, 32, 64)

        val ilInt = BigInteger(1, il)

        val curveParams = CustomNamedCurves.getByName(SECP256K1)

        if (ilInt > curveParams.n) {
            throw InvalidKeyException("il is larger that the curve order")
        }

        val childPublicKey = FixedPointCombMultiplier()
                .multiply(curveParams.g, ilInt)
                .add(parentNode.publicKey)

        if (childPublicKey.isInfinity) {
            throw InvalidKeyException("The child key public key point is at infinity")
        }

        return ExtendedPublicKey(childPublicKey, ir)
    }
}