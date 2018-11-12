package com.mattskala.trezorwallet.compose

import com.satoshilabs.trezor.lib.protobuf.TrezorType

/**
 * Helper functions for caculating the fee based on the transaction structure.
 */

// 4 bytes version, 1 byte in count, 1 byte out count, 4 bytes locktime, 1 byte for segwit start
private const val TX_EMPTY_SIZE = 4 + 1 + 1 + 4 + 1

// 32 bytes tx hash, 4 bytes output index, 1 byte script size, 4 bytes sequence
private const val TX_INPUT_BASE = 32 + 4 + 1 + 4

// 8 bytes amount, 1 byte script size
private const val TX_OUTPUT_BASE = 8 + 1

private const val SEGWIT_INPUT_SCRIPT_LENGTH = 51

private const val INPUT_SCRIPT_LENGTH = 109

// 4 bytes ops, 1 byte hash length, 20 bytes public key hash
internal const val P2PKH_OUTPUT_SCRIPT_LENGTH = 25

// 2 bytes ops, 1 byte hash length, 20 bytes script hash
internal const val P2SH_OUTPUT_SCRIPT_LENGTH = 23

// 1 byte version, 1 byte hash length, 20 bytes public key hash
internal const val P2WPKH_OUTPUT_SCRIPT_LENGTH = 22

// 1 byte version, 1 byte hash length, 32 bytes script hash
internal const val P2WSH_OUTPUT_SCRIPT_LENGTH = 34

/**
 * The length of the Bech32 P2WPKH address.
 */
private const val P2WPKH_ADDRESS_LENGTH = 42

/**
 * The length of the Bech32 P2SH address.
 */
private const val P2WSH_ADDRESS_LENGTH = 62

/**
 * Estimates the fee of a composed transaction, based on the number and type of inputs,
 * outputs and a desired fee per byte.
 *
 * @param inputsCount Number of transaction inputs.
 * @param outputs Transaction outputs.
 * @param feeRate Fee rate in satoshis per byte.
 * @param segwit True for segwit, false for legacy accounts.
 */
fun calculateFee(inputsCount: Int, outputs: List<TrezorType.TxOutputType>,
                feeRate: Int, segwit: Boolean): Int {
    return estimateTransactionSize(inputsCount, outputs, segwit) * feeRate
}

/**
 * Estimates the transaction size in bytes.
 */
private fun estimateTransactionSize(inputsCount: Int,
                                    outputs: List<TrezorType.TxOutputType>,
                                    segwit: Boolean): Int {
    val inputsBytes = inputsCount * inputBytes(segwit)
    val outputsBytes: Int = outputs.map { outputBytes(it) }.sum()
    return TX_EMPTY_SIZE + inputsBytes + outputsBytes
}

/**
 * Returns an input size in bytes.
 *
 * @param segwit True for segwit, false for legacy accounts.
 */
private fun inputBytes(segwit: Boolean): Int {
    val scriptLength = if (segwit) SEGWIT_INPUT_SCRIPT_LENGTH else INPUT_SCRIPT_LENGTH
    return TX_INPUT_BASE + scriptLength
}

/**
 * Returns a change output size in bytes.
 *
 * @param segwit True for segwit, false for legacy accounts.
 */
fun changeOutputBytes(segwit: Boolean): Int {
    val scriptLength = if (segwit) P2SH_OUTPUT_SCRIPT_LENGTH else P2PKH_OUTPUT_SCRIPT_LENGTH
    return TX_OUTPUT_BASE + scriptLength
}

/**
 * Returns and output size in bytes.
 */
private fun outputBytes(output: TrezorType.TxOutputType): Int {
    return TX_OUTPUT_BASE + scriptLength(output)
}

/**
 * Returns the unlocking script length in bytes according to the output type.
 */
fun scriptLength(output: TrezorType.TxOutputType): Int {
    return when {
        output.scriptType == TrezorType.OutputScriptType.PAYTOP2SHWITNESS ->
            P2SH_OUTPUT_SCRIPT_LENGTH
        output.scriptType == TrezorType.OutputScriptType.PAYTOWITNESS ->
            P2WPKH_OUTPUT_SCRIPT_LENGTH
        output.scriptType == TrezorType.OutputScriptType.PAYTOADDRESS -> {
            when {
                output.address.startsWith("1") -> P2PKH_OUTPUT_SCRIPT_LENGTH
                output.address.startsWith("3") -> P2SH_OUTPUT_SCRIPT_LENGTH
                output.address.startsWith("bc1") -> {
                    when (output.address.length) {
                        P2WPKH_ADDRESS_LENGTH -> P2WPKH_OUTPUT_SCRIPT_LENGTH
                        P2WSH_ADDRESS_LENGTH -> P2WSH_OUTPUT_SCRIPT_LENGTH
                        else -> 0
                    }
                }
                else -> 0
            }
        }
        else -> 0
    }
}
