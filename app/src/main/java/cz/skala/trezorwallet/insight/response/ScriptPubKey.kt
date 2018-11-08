package cz.skala.trezorwallet.insight.response

class ScriptPubKey(
        val hex: String,
        val type: String?,
        val addresses: List<String>?
)