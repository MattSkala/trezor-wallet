package com.mattskala.trezorwallet.data.entity

import com.mattskala.trezorwallet.exception.InvalidBitcoinURIException
import org.junit.Assert
import org.junit.Test

class BitcoinURITest {
    @Test(expected = InvalidBitcoinURIException::class)
    fun parse_invalid() {
        BitcoinURI.parse("asdf")
    }

    @Test
    fun parse_address() {
        val address = "175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W"
        val uri = BitcoinURI.parse("bitcoin:$address")
        Assert.assertEquals(address, uri.address)
    }

    @Test
    fun parse_parameters() {
        val address = "175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W"
        val amount = "50"
        val label = "Luke-Jr"
        val message = "Donation%20for%20project%20xyz"
        val uri = BitcoinURI.parse("bitcoin:$address?amount=$amount&label=$label&message=$message")
        Assert.assertEquals(address, uri.address)
        Assert.assertEquals(50.0, uri.amount, 0.0)
        Assert.assertEquals(label, uri.label)
        Assert.assertEquals("Donation for project xyz", uri.message)
    }
}