package cz.skala.trezorwallet.labeling

import android.content.Context
import com.google.protobuf.ByteString
import com.satoshilabs.trezor.intents.hexToBytes
import com.satoshilabs.trezor.intents.toHex
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import cz.skala.trezorwallet.crypto.*
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.Account
import org.json.JSONObject
import java.io.File


class LabelingManager(
        private val context: Context,
        private val prefs: PreferenceHelper,
        private val database: AppDatabase
) {
    companion object {
        private const val CIPHER_KEY = "Enable labeling?"
        private const val CIPHER_VALUE = "fedcba98765432100123456789abcdeffedcba98765432100123456789abcdef"
        private const val CONSTANT = "0123456789abcdeffedcba9876543210"
        private const val METADATA_EXTENSION = ".mtdt"

        /**
         * Returns CipherKeyValue used for deriving the master key from the hardware device.
         */
        fun getCipherKeyValue(): TrezorMessage.CipherKeyValue {
            return TrezorMessage.CipherKeyValue.newBuilder()
                    .addAddressN(hardened(10015))
                    .addAddressN(hardened(0))
                    .setKey(CIPHER_KEY)
                    .setValue(ByteString.copyFrom(CIPHER_VALUE.hexToBytes()))
                    .setEncrypt(true)
                    .setAskOnEncrypt(true)
                    .setAskOnDecrypt(true)
                    .build()
        }

        /**
         * Derives the account key from the master key and xpub.
         */
        fun deriveAccountKey(masterKey: ByteArray, xpub: String): String {
            val accountKey = hmacSha256(masterKey, xpub.toByteArray())
            return encodeBase58Check(accountKey)
        }

        /**
         * Derives the filename and password from the account key.
         */
        fun deriveFilenameAndPassword(accountKey: String): Pair<String, ByteArray> {
            val result = hmacSha512(accountKey.toByteArray(), CONSTANT.hexToBytes())
            val left = result.copyOfRange(0, 32)
            val filename = left.toHex() + METADATA_EXTENSION
            val password = result.copyOfRange(32, 64)
            return Pair(filename, password)
        }

        /**
         * Encrypts the file content with a password.
         */
        fun encryptFile(content: String, password: ByteArray): ByteArray {
            val bytes = content.toByteArray()
            val (iv, tag, cipherText) = encryptAesGcm(bytes, password)
            return iv + tag + cipherText
        }

        /**
         * Decrypts the file content with a password.
         */
        fun decryptFile(bytes: ByteArray, password: ByteArray): String {
            val iv = bytes.copyOfRange(0, 12)
            val tag = bytes.copyOfRange(12, 28)
            val cipherText = bytes.copyOfRange(28, bytes.size)
            val plainText = decryptAesGcm(iv, tag, cipherText, password)
            return plainText.toString(Charsets.UTF_8)
        }
    }

    /**
     * Stores the master key.
     */
    fun setMasterKey(masterKey: ByteArray) {
        prefs.labelingMasterKey = masterKey
    }

    /**
     * Loads the previously stored master key.
     */
    fun getMasterKey(): ByteArray? {
        return prefs.labelingMasterKey
    }

    /**
     * Derives and stores account keys for all accounts in the database.
     */
    fun deriveAccountKeys(masterKey: ByteArray) {
        val accounts = database.accountDao().getAll()
        accounts.forEach {
            val accountKey = deriveAccountKey(masterKey, it.xpub)
            database.accountDao().updateLabelingKey(it.id, accountKey)
        }
    }

    /**
     * Decrypts the file with provided password and deserializes the metadata structure.
     */
    fun loadMetadataFromFile(filename: String, password: ByteArray): AccountMetadata {
        val file = File(context.filesDir, filename)
        val bytes = file.readBytes()
        val content = decryptFile(bytes, password)
        val json = JSONObject(content)
        return AccountMetadata.fromJson(json)
    }

    /**
     * Serializes the structure into JSON, encrypts it with [password] and saves it to [filename].
     */
    fun saveMetadataToFile(metadata: AccountMetadata, filename: String, password: ByteArray) {
        val file = File(context.filesDir, filename)
        val content = metadata.toJson().toString()
        val data = encryptFile(content, password)
        file.writeBytes(data)
    }

    /**
     * Fetches metadata for all accounts from Dropbox and updates labels in the database.
     */
    fun fetchAccountsMetadata() {
        val accounts = database.accountDao().getAll()
        accounts.forEach {
            fetchAccountMetadata(it)
        }
    }

    /**
     * Fetches account metadata from Dropbox and updates labels in the database.
     */
    private fun fetchAccountMetadata(account: Account) {
        // TODO
    }

    /**
     * Uploads account metadata file to Dropbox.
     */
    fun uploadAccountMetadata(account: Account) {
        // TODO
    }
}