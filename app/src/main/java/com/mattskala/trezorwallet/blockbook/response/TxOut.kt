package com.mattskala.trezorwallet.blockbook.response

class TxOut(
        val satoshis: Long,
        val script: String,
        val address: String?
)