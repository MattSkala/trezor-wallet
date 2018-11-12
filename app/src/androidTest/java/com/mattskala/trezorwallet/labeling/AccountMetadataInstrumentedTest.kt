package com.mattskala.trezorwallet.labeling

import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class AccountMetadataInstrumentedTest {
    @Test
    fun toJson() {
        val metadata = AccountMetadata()
        metadata.version = "1.0.0"
        metadata.accountLabel = "Saving account"
        metadata.setAddressLabel("1JAd7XCBzGudGpJQSDSfpmJhiygtLQWaGL", "My receiving address")
        metadata.setAddressLabel("1GWFxtwWmNVqotUPXLcKVL2mUKpshuJYo", "")
        metadata.setOutputLabel("350eebc1012ce2339b71b5fca317a0d174abc3a633684bc65a71845deb596539", 0, "Money to Adam")
        metadata.setOutputLabel("350eebc1012ce2339b71b5fca317a0d174abc3a633684bc65a71845deb596539", 1, "")
        metadata.setOutputLabel("ebbd138134e2c8acfee4fd4edb6f7f9175ee7b4020bcc82aba9a13ce06fae85b", 0, "Feeding bitcoin eater")
        val json = metadata.toJson()
        Assert.assertEquals(metadata.version, json.getString("version"))
        Assert.assertEquals(metadata.accountLabel, json.getString("accountLabel"))
        Assert.assertEquals("My receiving address", json.
                getJSONObject("addressLabels").
                getString("1JAd7XCBzGudGpJQSDSfpmJhiygtLQWaGL"))
        Assert.assertEquals("", json.
                getJSONObject("addressLabels").
                getString("1GWFxtwWmNVqotUPXLcKVL2mUKpshuJYo"))
        Assert.assertEquals("Money to Adam", json.
                getJSONObject("outputLabels").
                getJSONObject("350eebc1012ce2339b71b5fca317a0d174abc3a633684bc65a71845deb596539").
                getString("0"))
        Assert.assertEquals("", json.
                getJSONObject("outputLabels").
                getJSONObject("350eebc1012ce2339b71b5fca317a0d174abc3a633684bc65a71845deb596539").
                getString("1"))
        Assert.assertEquals("Feeding bitcoin eater", json.
                getJSONObject("outputLabels").
                getJSONObject("ebbd138134e2c8acfee4fd4edb6f7f9175ee7b4020bcc82aba9a13ce06fae85b").
                getString("0"))
    }

    @Test
    fun fromJson() {
        val json = "{\"accountLabel\":\"Saving account\",\"addressLabels\":{\"1JAd7XCBzGudGpJQSDSfpmJhiygtLQWaGL\":\"My receiving address\",\"1GWFxtwWmNVqotUPXLcKVL2mUKpshuJYo\":\"\"},\"version\":\"1.0.0\",\"outputLabels\":{\"350eebc1012ce2339b71b5fca317a0d174abc3a633684bc65a71845deb596539\":{\"0\":\"Money to Adam\"},\"ebbd138134e2c8acfee4fd4edb6f7f9175ee7b4020bcc82aba9a13ce06fae85b\":{\"0\":\"Feeding bitcoin eater\"}}}"
        val metadata = AccountMetadata.fromJson(JSONObject(json))
        Assert.assertEquals("1.0.0", metadata.version)
        Assert.assertEquals("Saving account", metadata.accountLabel)
        Assert.assertEquals("My receiving address", metadata.getAddressLabel("1JAd7XCBzGudGpJQSDSfpmJhiygtLQWaGL"))
        Assert.assertEquals("", metadata.getAddressLabel("1GWFxtwWmNVqotUPXLcKVL2mUKpshuJYo"))
        Assert.assertEquals("Money to Adam", metadata.getOutputLabel("350eebc1012ce2339b71b5fca317a0d174abc3a633684bc65a71845deb596539", 0))
        Assert.assertEquals(null, metadata.getOutputLabel("350eebc1012ce2339b71b5fca317a0d174abc3a633684bc65a71845deb596539", 1))
        Assert.assertEquals("Feeding bitcoin eater", metadata.getOutputLabel("ebbd138134e2c8acfee4fd4edb6f7f9175ee7b4020bcc82aba9a13ce06fae85b", 0))
    }
}