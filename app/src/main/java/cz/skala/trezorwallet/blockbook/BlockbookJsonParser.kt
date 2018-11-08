package cz.skala.trezorwallet.blockbook

import cz.skala.trezorwallet.insight.response.*
import org.json.JSONObject

class BlockbookJsonParser {
    companion object {
        private const val TAG = "BlockbookJsonParser"

        private const val INPUTS = "inputs"
        private const val OUTPUTS = "outputs"
        private const val ADDRESS = "address"
        private const val TXID = "txid"
    }

    fun parseTxJson(txJson: JSONObject): Tx {
        val inputsJson = txJson.getJSONArray(INPUTS)
        val outputsJson = txJson.getJSONArray(OUTPUTS)
        val inputs = mutableListOf<TxIn>()
        val outputs = mutableListOf<TxOut>()

        for (n in 0 until inputsJson.length()) {
            val inputJson = inputsJson.getJSONObject(n)
            inputs += TxIn(
                    n,
                    inputJson.getString(ADDRESS),
                    inputJson.getString(TXID),
                    inputJson.getInt("outputIndex"),
                    inputJson.getLong("satoshis"),
                    ScriptSig(inputJson.getString("script")),
                    inputJson.getLong("sequence")
            )
        }

        for (n in 0 until outputsJson.length()) {
            val outputJson = outputsJson.getJSONObject(n)
            val outputAddresses = if (!outputJson.isNull("address"))
                listOf(outputJson.getString("address")) else listOf()
            outputs += TxOut(
                    n,
                    outputJson.getLong("satoshis"),
                    ScriptPubKey(outputJson.getString("script"), null,
                            outputAddresses),
                    null,
                    null
            )
        }

        return Tx(
                txJson.getString("hash"),
                txJson.getInt("version"),
                if (txJson.has("locktime")) txJson.getInt("locktime") else 0,
                txJson.getString("hex").length / 2,
                txJson.getInt("height"),
                txJson.getLong("blockTimestamp"),
                txJson.getLong("inputSatoshis"),
                txJson.getLong("outputSatoshis"),
                txJson.getLong("feeSatoshis"),
                inputs,
                outputs
        )
    }
}
