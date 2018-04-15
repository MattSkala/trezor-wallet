package cz.skala.trezorwallet.labeling

import org.json.JSONObject

/**
 * Account metadata structure as defined in SLIP-0015.
 */
class AccountMetadata {
    companion object {
        const val CURRENT_VERSION = "1.0.0"

        private const val KEY_VERSION = "version"
        private const val KEY_ACCOUNT_LABEL = "accountLabel"
        private const val KEY_ADDRESS_LABELS = "addressLabels"
        private const val KEY_OUTPUT_LABELS = "outputLabels"

        fun fromJson(json: JSONObject): AccountMetadata {
            val metadata = AccountMetadata()
            metadata.version = json.getString(KEY_VERSION)
            if (metadata.version != CURRENT_VERSION) {
                throw Exception("Unsupported metadata version")
            }
            metadata.accountLabel = if (json.has(KEY_ACCOUNT_LABEL))
                json.getString(KEY_ACCOUNT_LABEL) else null
            metadata.addressLabels = if (json.has(KEY_ADDRESS_LABELS))
                parseAddressLabels(json.getJSONObject(KEY_ADDRESS_LABELS)) else mutableMapOf()
            metadata.outputLabels = if (json.has(KEY_OUTPUT_LABELS))
                parseOutputLabels(json.getJSONObject(KEY_OUTPUT_LABELS)) else mutableMapOf()
            return metadata
        }

        private fun parseAddressLabels(json: JSONObject): MutableMap<String, String?> {
            val addressLabels = mutableMapOf<String, String?>()
            for (key in json.keys()) {
                addressLabels[key] = json.getString(key)
            }
            return addressLabels
        }

        private fun parseOutputLabels(json: JSONObject): MutableMap<String, MutableMap<String, String?>?> {
            val outputLabels = mutableMapOf<String, MutableMap<String, String?>?>()
            for (key in json.keys()) {
                val labels = mutableMapOf<String, String?>()
                val labelsJson = json.getJSONObject(key)
                for (index in labelsJson.keys()) {
                    labels[index] = labelsJson.getString(index)
                }
                outputLabels[key] = labels
            }
            return outputLabels
        }
    }

    var version: String = CURRENT_VERSION
    var accountLabel: String? = null
    var addressLabels: MutableMap<String, String?> = mutableMapOf()
    var outputLabels: MutableMap<String, MutableMap<String, String?>?> = mutableMapOf()

    fun setAddressLabel(address: String, label: String?) {
        addressLabels[address] = label
    }

    fun getAddressLabel(address: String): String? {
        return addressLabels[address]
    }

    fun setOutputLabel(txid: String, index: Int, label: String) {
        val tx = outputLabels[txid] ?: mutableMapOf()
        outputLabels[txid] = tx
        tx[index.toString()] = label
    }

    fun getOutputLabel(txid: String, index: Int): String? {
        val tx = outputLabels[txid] ?: mutableMapOf()
        return tx[index.toString()]
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put(KEY_VERSION, CURRENT_VERSION)
        json.put(KEY_ACCOUNT_LABEL, accountLabel)
        val addressLabelsJson = JSONObject()
        addressLabels.forEach { key, value ->
            addressLabelsJson.put(key, value)
        }
        json.put(KEY_ADDRESS_LABELS, addressLabelsJson)
        val outputLabelsJson = JSONObject()
        outputLabels.forEach { txid, outputs ->
            val labelsJson = JSONObject()
            outputs?.forEach { index, label ->
                labelsJson.put(index, label)
            }
            outputLabelsJson.put(txid, labelsJson)
        }
        json.put(KEY_OUTPUT_LABELS, outputLabelsJson)
        return json
    }
}