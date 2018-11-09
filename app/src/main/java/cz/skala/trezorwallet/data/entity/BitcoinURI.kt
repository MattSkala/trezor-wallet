package cz.skala.trezorwallet.data.entity

import cz.skala.trezorwallet.exception.InvalidBitcoinURIException
import java.net.URI
import java.net.URISyntaxException

/**
 * Bitcoin URI scheme parsing as defined by BIP 21.
 */
class BitcoinURI(
        val address: String,
        val amount: Double,
        val label: String?,
        val message: String?
) {
    companion object {
        @Throws(InvalidBitcoinURIException::class)
        fun parse(url: String): BitcoinURI {
            val uri: URI

            try {
                uri = URI(url)
            } catch (e: URISyntaxException) {
                throw InvalidBitcoinURIException("Invalid URI", e)
            }

            if (uri.scheme != "bitcoin") {
                throw InvalidBitcoinURIException("Invalid URI scheme: " + uri.scheme)
            }

            val schemeSpecificPart = uri.schemeSpecificPart
            val parts = schemeSpecificPart.split("?")
            val address = parts[0]

            var amount = 0.0
            var label: String? = null
            var message: String? = null

            if (parts.size == 2) {
                val parametersPart = parts[1]
                val parameters = parseParameters(parametersPart)

                amount = parameters["amount"]?.toDoubleOrNull() ?: 0.0
                label = parameters["label"]
                message = parameters["message"]
            }

            return BitcoinURI(address, amount, label, message)
        }

        private fun parseParameters(parametersPart: String): Map<String, String> {
            val parameters = parametersPart.split("&")
            val map = mutableMapOf<String, String>()
            parameters.forEach {
                val parameter = it.split("=")
                val name = parameter[0]
                val value = parameter[1]
                map[name] = value
            }
            return map
        }
    }

    fun getUrl(): String {
        return StringBuilder().apply {
            append("bitcoin:")
            append(address)

            val params = mutableMapOf<String, String>()
            if (amount > 0) {
                params["amount"] = amount.toString()
            }
            if (label != null) {
                params["label"] = label
            }
            if (message != null) {
                params["message"] = message
            }

            var isFirst = true
            params.forEach {
                append (if (isFirst) "?" else "&")
                append("${it.key}=${it.value}")
                isFirst = false
            }
        }.toString()
    }
}