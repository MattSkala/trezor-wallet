package com.mattskala.trezorwallet.blockbook.response

class Tx(
        val blockTimestamp: Long?,
        val feeSatoshis: Long,
        val hash: String,
        val height: Int,
        val hex: String,
        val inputs: List<TxIn>,
        val inputSatoshis: Long,
        val locktime: Int,
        val outputs: List<TxOut>,
        val outputSatoshis: Long,
        val version: Int
)
