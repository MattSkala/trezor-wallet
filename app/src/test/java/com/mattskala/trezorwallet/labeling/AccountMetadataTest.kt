package com.mattskala.trezorwallet.labeling

import org.junit.Assert
import org.junit.Test

class AccountMetadataTest {
    @Test
    fun addressLabel() {
        val metadata = AccountMetadata()
        metadata.setAddressLabel("address1", "label1")
        metadata.setAddressLabel("address2", "")
        Assert.assertEquals("label1", metadata.getAddressLabel("address1"))
        Assert.assertEquals("", metadata.getAddressLabel("address2"))
    }

    @Test
    fun outputLabel() {
        val metadata = AccountMetadata()
        metadata.setOutputLabel("tx1", 0, "output0")
        metadata.setOutputLabel("tx1", 2, "output2")
        metadata.setOutputLabel("tx2", 1, "output1")
        Assert.assertEquals("output0", metadata.getOutputLabel("tx1", 0))
        Assert.assertEquals(null, metadata.getOutputLabel("tx1", 1))
        Assert.assertEquals("output2", metadata.getOutputLabel("tx1", 2))
        Assert.assertEquals(null, metadata.getOutputLabel("tx2", 0))
        Assert.assertEquals("output1", metadata.getOutputLabel("tx2", 1))
    }
}