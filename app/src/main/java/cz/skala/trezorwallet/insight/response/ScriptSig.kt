package cz.skala.trezorwallet.insight.response

/**
 * A script signature in transaction input.
 */
class ScriptSig(
        val asm: String,
        val hex: String
)