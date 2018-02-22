package cz.skala.trezorwallet.insight.response

class ScriptPubKey(
        val asm: String,
        val hex: String,
        val type: String?,
        val addresses: List<String>?
)