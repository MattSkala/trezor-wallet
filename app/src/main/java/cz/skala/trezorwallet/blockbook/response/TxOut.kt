package cz.skala.trezorwallet.blockbook.response

class TxOut(
        val satoshis: Long,
        val script: String,
        val address: String?
)