package com.mattskala.trezorwallet.labeling

import android.content.Context
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.NetworkIOException
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.DownloadErrorException
import com.dropbox.core.v2.files.GetMetadataErrorException
import com.dropbox.core.v2.files.UploadErrorException
import com.dropbox.core.v2.files.WriteMode
import com.google.protobuf.ByteString
import com.satoshilabs.trezor.intents.hexToBytes
import com.satoshilabs.trezor.intents.toHex
import com.satoshilabs.trezor.intents.ui.data.GenericRequest
import com.satoshilabs.trezor.intents.ui.data.TrezorRequest
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import com.mattskala.trezorwallet.crypto.*
import com.mattskala.trezorwallet.data.AppDatabase
import com.mattskala.trezorwallet.data.PreferenceHelper
import com.mattskala.trezorwallet.data.entity.Account
import com.mattskala.trezorwallet.data.entity.Address
import com.mattskala.trezorwallet.data.entity.TransactionOutput
import com.mattskala.trezorwallet.data.repository.AccountRepository
import com.mattskala.trezorwallet.data.repository.AddressRepository
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.InvalidKeyException


/**
 * A manager that handles encryption/decryption and Dropbox synchronization of account metadata.
 */
class LabelingManager(
        private val context: Context,
        private val prefs: PreferenceHelper,
        private val database: AppDatabase,
        private val accountRepository: AccountRepository,
        private val addressRepository: AddressRepository
) {
    companion object {
        private const val CIPHER_KEY = "Enable labeling?"
        private const val CIPHER_VALUE = "fedcba98765432100123456789abcdeffedcba98765432100123456789abcdef"
        private const val CONSTANT = "0123456789abcdeffedcba9876543210"
        private const val METADATA_EXTENSION = ".mtdt"

        /**
         * Returns a TREZOR request for deriving the master key.
         */
        fun createCipherKeyValueRequest(): TrezorRequest {
            val message = TrezorMessage.CipherKeyValue.newBuilder()
                    .addAddressN(hardened(10015))
                    .addAddressN(hardened(0))
                    .setKey(CIPHER_KEY)
                    .setValue(ByteString.copyFrom(CIPHER_VALUE.hexToBytes()))
                    .setEncrypt(true)
                    .setAskOnEncrypt(true)
                    .setAskOnDecrypt(true)
                    .build()
            return GenericRequest(message)
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
        fun encryptData(content: String, password: ByteArray): ByteArray {
            val bytes = content.toByteArray()
            val (iv, tag, cipherText) = encryptAesGcm(bytes, password)
            return iv + tag + cipherText
        }

        /**
         * Decrypts the file content with a password.
         */
        fun decryptData(bytes: ByteArray, password: ByteArray): String {
            val iv = bytes.copyOfRange(0, 12)
            val tag = bytes.copyOfRange(12, 28)
            val cipherText = bytes.copyOfRange(28, bytes.size)
            val plainText = decryptAesGcm(iv, tag, cipherText, password)
            return plainText.toString(Charsets.UTF_8)
        }
    }

    /**
     * Returns whetter the labeling is enabled.
     */
    fun isEnabled(): Boolean {
        return getMasterKey() != null
    }

    /**
     * Stores Dropbox OAuth token.
     */
    fun setDropboxToken(token: String) {
        prefs.dropboxToken = token
    }

    /**
     * Enables labeling by setting the master key.
     */
    suspend fun enableLabeling(masterKey: ByteArray) {
        setMasterKey(masterKey)
        downloadAccountsMetadata()
    }

    /**
     * Deletes all metadata files, clears labels in the database and removes the master key.
     */
    suspend fun disableLabeling() {
        removeMetadataFiles()
        clearDatabaseLabels()
        setMasterKey(null)
    }

    /**
     * Updates an account label.
     */
    suspend fun setAccountLabel(account: Account, label: String) {
        val resources = context.resources
        val defaultLabel = account.getDefaultLabel(resources)
        val savedLabel = if (label != defaultLabel) label else null
        account.label = savedLabel

        accountRepository.insert(account)

        val metadata = loadMetadata(account)
        if (metadata != null) {
            metadata.accountLabel = label
            saveMetadata(account, metadata)
            uploadAccountMetadata(account)
        }
    }

    /**
     * Updates an address label.
     */
    suspend fun setAddressLabel(address: Address, label: String) {
        address.label = label

        addressRepository.insert(address)

        val account = accountRepository.getById(address.account)
        val metadata = loadMetadata(account)
        if (metadata != null) {
            metadata.setAddressLabel(address.address, label)
            saveMetadata(account, metadata)
            uploadAccountMetadata(account)
        }
    }

    /**
     * Updates a transaction output label.
     */
    suspend fun setOutputLabel(output: TransactionOutput, label: String) {
        output.label = label

        withContext(Dispatchers.IO) {
            database.transactionDao().insert(output)
        }

        val account = accountRepository.getById(output.account)
        val metadata = loadMetadata(account)
        if (metadata != null) {
            metadata.setOutputLabel(output.txid, output.n, label)
            saveMetadata(account, metadata)
            uploadAccountMetadata(account)
        }
    }

    /**
     * Loads metadata from file for the specified account.
     */
    suspend fun loadMetadata(account: Account): AccountMetadata? = withContext(Dispatchers.IO) {
        val masterKey = getMasterKey() ?: throw InvalidKeyException("Master key is null")
        val accountKey = deriveAccountKey(masterKey, account.xpub)
        val (filename, password) = deriveFilenameAndPassword(accountKey)
        loadMetadataFromFile(filename, password) ?: AccountMetadata()
    }

    /**
     * Stores the master key.
     */
    private fun setMasterKey(masterKey: ByteArray?) {
        prefs.labelingMasterKey = masterKey
    }

    /**
     * Loads the previously stored master key.
     */
    private fun getMasterKey(): ByteArray? {
        return prefs.labelingMasterKey
    }

    /**
     * Save metadata to file.
     */
    private suspend fun saveMetadata(account: Account, metadata: AccountMetadata) =
            withContext(Dispatchers.IO) {
        val masterKey = getMasterKey() ?: throw InvalidKeyException("Master key is null")
        val accountKey = deriveAccountKey(masterKey, account.xpub)
        val (filename, password) = deriveFilenameAndPassword(accountKey)
        saveMetadataToFile(metadata, filename, password)
    }

    /**
     * Decrypts the file with provided password and deserializes the metadata structure.
     */
    private fun loadMetadataFromFile(filename: String, password: ByteArray): AccountMetadata? {
        val file = File(context.filesDir, filename)
        if (!file.exists()) return null
        val bytes = file.readBytes()
        if (bytes.isEmpty()) return null
        val content = decryptData(bytes, password)
        val json = JSONObject(content)
        return AccountMetadata.fromJson(json)
    }

    /**
     * Serializes the structure into JSON, encrypts it with [password] and saves it to [filename].
     */
    private fun saveMetadataToFile(metadata: AccountMetadata, filename: String, password: ByteArray) {
        val file = File(context.filesDir, filename)
        val content = metadata.toJson().toString()
        val data = encryptData(content, password)
        file.writeBytes(data)
    }

    /**
     * Removes metadata files for all accounts. Should be called before disabling labeling or
     * forgetting the device.
     */
    private suspend fun removeMetadataFiles() = withContext(Dispatchers.IO) {
        val masterKey = getMasterKey() ?: throw InvalidKeyException("Master key is null")
        val accounts = database.accountDao().getAll()
        accounts.forEach {
            val accountKey = deriveAccountKey(masterKey, it.xpub)
            val (filename, _) = deriveFilenameAndPassword(accountKey)
            val file = File(context.filesDir, filename)
            file.delete()
        }
    }

    /**
     * Removes all labels from the database.
     */
    private suspend fun clearDatabaseLabels() = withContext(Dispatchers.IO) {
        database.accountDao().clearLabels()
        database.addressDao().clearLabels()
        database.transactionDao().clearLabels()
    }

    /**
     * Fetches metadata for all accounts from Dropbox and updates labels in the database.
     */
    suspend fun downloadAccountsMetadata() {
        val accounts = accountRepository.getAll()
        accounts.forEach {
            downloadAccountMetadata(it)
            updateDatabaseLabels(it)
        }
    }

    /**
     * Creates a Dropbox client with previously stored OAuth token.
     */
    private fun getDropboxClient(): DbxClientV2 {
        val accessToken = prefs.dropboxToken
        val requestConfig = DbxRequestConfig.newBuilder("TrezorWalletAndroid/1.0")
                .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                .build()
        return DbxClientV2(requestConfig, accessToken)
    }

    /**
     * Fetches account metadata from Dropbox.
     */
    suspend fun downloadAccountMetadata(account: Account) = withContext(Dispatchers.IO) {
        val masterKey = getMasterKey() ?: throw InvalidKeyException("Master key is null")
        val accountKey = deriveAccountKey(masterKey, account.xpub)
        val (filename, _) = deriveFilenameAndPassword(accountKey)

        try {
            val path = "/$filename"

            // check if file exists
            getDropboxClient().files().getMetadata(path)

            // download file
            val file = File(context.filesDir, filename)
            val outputStream = FileOutputStream(file)
            getDropboxClient().files().download(path).download(outputStream)
        } catch (e: GetMetadataErrorException) {
            e.printStackTrace()
        } catch (e: DownloadErrorException) {
            e.printStackTrace()
        } catch (e: NetworkIOException) {
            e.printStackTrace()
        }
    }

    /**
     * Updates database labels from the account metadata file.
     */
    private suspend fun updateDatabaseLabels(account: Account) = withContext(Dispatchers.IO) {
        val masterKey = getMasterKey() ?: throw InvalidKeyException("Master key is null")
        val accountKey = deriveAccountKey(masterKey, account.xpub)
        val (filename, password) = deriveFilenameAndPassword(accountKey)

        val metadata = loadMetadataFromFile(filename, password)
        if (metadata != null) {
            account.label = metadata.accountLabel
            database.accountDao().insert(account)

            metadata.addressLabels.forEach { entry ->
                val address = entry.key
                val label = entry.value
                database.addressDao().updateLabel(account.id, address, label)
            }

            metadata.outputLabels.forEach { entry ->
                val txid = entry.key
                val outputs = entry.value
                outputs?.forEach { outputEntry ->
                    val index = outputEntry.key
                    val label = outputEntry.value
                    database.transactionDao().updateLabel(account.id, txid, index.toInt(), label)
                }
            }
        }
    }

    /**
     * Uploads account metadata file to Dropbox.
     */
    private suspend fun uploadAccountMetadata(account: Account) = withContext(Dispatchers.IO) {
        val masterKey = getMasterKey() ?: throw InvalidKeyException("Master key is null")
        val accountKey = deriveAccountKey(masterKey, account.xpub)
        val (filename, _) = deriveFilenameAndPassword(accountKey)
        val file = File(context.filesDir, filename)
        val inputStream = FileInputStream(file)

        try {
            val path = "/$filename"
            getDropboxClient().files()
                    .uploadBuilder(path)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(inputStream)
        } catch (e: UploadErrorException) {
            e.printStackTrace()
        } catch (e: NetworkIOException) {
            e.printStackTrace()
        }
    }
}