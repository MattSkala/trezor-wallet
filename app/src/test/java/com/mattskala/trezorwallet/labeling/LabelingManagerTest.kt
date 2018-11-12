package com.mattskala.trezorwallet.labeling

import com.satoshilabs.trezor.intents.hexToBytes
import org.junit.Assert
import org.junit.Test

class LabelingManagerTest {
    @Test
    fun deriveAccountKey() {
        var masterKey = "20c8bf0701213cdcf4c2f56fd0096c1772322d42fb9c4d0ddf6bb122d713d2f3".hexToBytes()
        var xpub = "xpub6BiVtCpG9fQPxnPmHXG8PhtzQdWC2Su4qWu6XW9tpWFYhxydCLJGrWBJZ5H6qTAHdPQ7pQhtpjiYZVZARo14qHiay2fvrX996oEP42u8wZy"
        var accountKey = LabelingManager.deriveAccountKey(masterKey, xpub)
        Assert.assertEquals("v5kCxSKLTsnwmgPBeaRyFDWeG9zXouF34L72763zjLrS4LWy8", accountKey)

        masterKey = "3ae68b955e2a72c6f4f0fd5c63ea45dc3e52d22f479972f6c07778a477f837e9".hexToBytes()
        xpub = "xpub6CcYFYTZ1HLFnqdxZVyQD2E3rsFtFrtpkyGTcsrSkfv34fBT2ReZuTaVmt2m98XTQJ3EviWw9w4XGeSdfTefSSPrxh3y9kVnDYv2bxrZykv"
        accountKey = LabelingManager.deriveAccountKey(masterKey, xpub)
        Assert.assertEquals("2BbfdJHs7ESiguV86hDsG1pmdVC8mkV4HP1TxLSpRGeRxngLD6", accountKey)
    }

    @Test
    fun deriveFilename() {
        val accountKey = "v5kCxSKLTsnwmgPBeaRyFDWeG9zXouF34L72763zjLrS4LWy8"
        val (filename, _) = LabelingManager.deriveFilenameAndPassword(accountKey)
        Assert.assertEquals("08108c3a46882bb71a5df59f4962e02f89a63efb1cf5f32ded94694528be6cec.mtdt", filename)
    }

    @Test
    fun decryptFile() {
        val data = "d32a5831b74ba04cdf44309fbb96a1b464fe5d4a27d1e753c30602ba1947" +
                "3cca7d8734e8b9442dbd41d530c42e03fea59a5d38b21392f3e4a135eb07" +
                "009d5a8b9996055b7aff076918c4ed63ee49db56c5a6b069cac7f221f704" +
                "5af7197cdbb562ba004d7a6f06eb7cffd1dfb177fd652e66c2d05d944b58" +
                "85d6a104853a0d07e4cebff3513a2f6a1c8ff6f4f98ce222f3d601f1c796" +
                "d070b7523649e10242dfe78cb2db50e826dd18b1f65213f5c0748577ecc9" +
                "7b8e13ab9cd0c5fe7b76635717c64ad352064a3321df6bbfa2db8ef8c692" +
                "55ef9d8a8dfbce9c6ad3029bbdcf1b2bb04795fd96aa95d27e6ca1ed2658" +
                "bfb108b44dac2159184d6e3cabe341e2ec5d83756aeb8c408e92fe6ca3e6" +
                "3d4c0d644aa2648341506324574d205934c65f54979b1d684f7a2442e8d5" +
                "2149ed67449019e6091aa182afcaf5aa1fa8bf3114ee7b46e47b4c6648d1" +
                "d1355cefd10081be6e8c7bdf1b2ff14d8896b1ede811fa1aa2c024a6ebf3" +
                "6baf0a8d6afa2975bf551e8bc3f03117b42dc4cbe2a6bd700f2fda40c78a" +
                "48627ebc130286ba98"
        val accountKey = "v5kCxSKLTsnwmgPBeaRyFDWeG9zXouF34L72763zjLrS4LWy8"
        val (_, password) = LabelingManager.deriveFilenameAndPassword(accountKey)
        val content = LabelingManager.decryptData(data.hexToBytes(), password)
        Assert.assertEquals("{\"accountLabel\":\"Saving account\",\"addressLabels\":{\"1JAd7XCBzGudGpJQSDSfpmJhiygtLQWaGL\":\"My receiving address\",\"1GWFxtwWmNVqotUPXLcKVL2mUKpshuJYo\":\"\"},\"version\":\"1.0.0\",\"outputLabels\":{\"350eebc1012ce2339b71b5fca317a0d174abc3a633684bc65a71845deb596539\":{\"0\":\"Money to Adam\"},\"ebbd138134e2c8acfee4fd4edb6f7f9175ee7b4020bcc82aba9a13ce06fae85b\":{\"0\":\"Feeding bitcoin eater\"}}}", content)
    }
}