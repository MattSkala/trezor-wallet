package com.mattskala.trezorwallet.blockbook.response

class TxIn(
        val txid: String,
        val outputIndex: Int,
        val script: String,
        val sequence: Long,
        val address: String,
        val satoshis: Long
)