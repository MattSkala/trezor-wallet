package cz.skala.trezorwallet.blockbook

import android.util.Log
import io.socket.parser.Packet
import io.socket.parser.Parser


/**
 * A packet encoder that does not escape slashes as opposed to the default implementation.
 */
class BlockbookPacketEncoder : Parser.Encoder {

    override fun encode(obj: Packet<*>, callback: Parser.Encoder.Callback) {
        val encoding = encodeAsString(obj)
        callback.call(arrayOf(encoding))
    }

    private fun encodeAsString(obj: Packet<*>): String {
        val str = StringBuilder("" + obj.type)

        if (Parser.BINARY_EVENT == obj.type || Parser.BINARY_ACK == obj.type) {
            str.append(obj.attachments)
            str.append("-")
        }

        if (obj.nsp != null && obj.nsp.length != 0 && "/" != obj.nsp) {
            str.append(obj.nsp)
            str.append(",")
        }

        if (obj.id >= 0) {
            str.append(obj.id)
        }

        if (obj.data != null) {
            var encodedData = obj.data.toString()

            // Unescape slashes
            encodedData = encodedData.replace("\\/", "/")

            str.append(encodedData)
        }

        Log.d("BlockbookPacketEncoder", str.toString())

        return str.toString()
    }
}