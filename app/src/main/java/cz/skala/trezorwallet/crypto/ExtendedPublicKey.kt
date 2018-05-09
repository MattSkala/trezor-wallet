package cz.skala.trezorwallet.crypto

import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.math.ec.FixedPointCombMultiplier
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
    @Throws(InvalidKeyException::class)
    fun deriveChildKey(index: Int): ExtendedPublicKey {
        val parentNode = this

        if (isHardened(index.toLong())) {
            throw IllegalArgumentException("Derivation defined only for non-hardened child keys")
        }

        val data = ByteBuffer.allocate(37)
        data.put(parentNode.publicKey.getEncoded(true)) // 33 bytes
        data.putInt(index) // 4 bytes
        val i = hmacSha512(parentNode.chainCode, data.array()) // 64 bytes
        val il = BigInteger(1, Arrays.copyOfRange(i, 0, 32))
        val ir = Arrays.copyOfRange(i, 32, 64)

        val curveParams = CustomNamedCurves.getByName(SECP256K1)

        if (il > curveParams.n) {
            throw InvalidKeyException("I_L is larger that the curve order")
        }

        val childPublicKey = FixedPointCombMultiplier()
                .multiply(curveParams.g, il)
                .add(parentNode.publicKey)

        if (childPublicKey.isInfinity) {
            throw InvalidKeyException("The child public key point is at infinity")
        }

        return ExtendedPublicKey(childPublicKey, ir)
    }

    /**
     * Encodes the public key as a Bitcoin address in Base58Check format.
     */
    fun getAddress(): String {
        val publicKeyEncoded = publicKey.getEncoded(true)
        val hash160 = hash160(publicKeyEncoded)
        return encodeBase58Check(hash160, 0)
    }

    /**
     * Encodes the public key as a P2SH(P2WPKH) address.
     */
    fun getSegwitAddress(): String {
        val publicKeyEncoded = publicKey.getEncoded(true)
        val publicKeyHash = hash160(publicKeyEncoded)
        val scriptSig = ByteArray(publicKeyHash.size + 2)
        scriptSig[0] = 0x00 // version 0
        scriptSig[1] = 0x14 // push 20 bytes
        System.arraycopy(publicKeyHash, 0, scriptSig, 2, publicKeyHash.size)
        val scriptSigHash = hash160(scriptSig)
        return encodeBase58Check(scriptSigHash, 5)
    }
}