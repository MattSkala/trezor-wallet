package com.mattskala.trezorwallet.crypto

import java.math.BigInteger

private val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray()

fun encodeBase58(input: ByteArray): String {
    if (input.isEmpty()) return ""

    // Count leading zeros
    var zeros = 0
    while (zeros < input.size && input[zeros] == 0.toByte()) {
        zeros++
    }

    var x = BigInteger(1, input)
    var output = String()

    while (x > 0.toBigInteger()) {
        val result = x.divideAndRemainder(58.toBigInteger())
        x = result[0]
        val remainder = result[1].toInt()
        output += ALPHABET[remainder]
    }

    while (zeros > 0) {
        zeros--
        output += ALPHABET[0]
    }

    return output.reversed()
}

fun encodeBase58Check(payload: ByteArray, version: Byte): String {
    // 1 byte version + data bytes + 4 bytes check code
    val addressBytes = ByteArray(1 + payload.size + 4)
    addressBytes[0] = version
    System.arraycopy(payload, 0, addressBytes, 1, payload.size)
    val checksum: ByteArray = sha256(sha256(addressBytes, 0, payload.size + 1))
    System.arraycopy(checksum, 0, addressBytes, payload.size + 1, 4)
    return encodeBase58(addressBytes)
}

fun encodeBase58Check(payload: ByteArray): String {
    // data bytes + 4 bytes check code
    val addressBytes = ByteArray(payload.size + 4)
    System.arraycopy(payload, 0, addressBytes, 0, payload.size)
    val checksum: ByteArray = sha256(sha256(addressBytes, 0, payload.size))
    System.arraycopy(checksum, 0, addressBytes, payload.size, 4)
    return encodeBase58(addressBytes)
}
