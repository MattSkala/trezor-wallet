package cz.skala.trezorwallet.blockbook

import android.util.Log
import com.google.gson.Gson
import cz.skala.trezorwallet.TrezorApplication
import cz.skala.trezorwallet.blockbook.options.GetAddressHistoryOptions
import cz.skala.trezorwallet.blockbook.response.GetAddressHistoryResult
import cz.skala.trezorwallet.blockbook.response.GetInfoResult
import cz.skala.trezorwallet.blockbook.response.Tx
import cz.skala.trezorwallet.data.PreferenceHelper
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.coroutines.resume


/**
 * A service for communication with Socket.IO Blockbook API.
 */
class BlockbookSocketService(val prefs: PreferenceHelper) {
    companion object {
        private const val TAG = "BlockbookSocketService"
        private const val TRANSPORT = "websocket"

        private const val METHOD_GET_INFO = "getInfo"
        private const val METHOD_GET_ADDRESS_HISTORY = "getAddressHistory"
        private const val METHOD_ESTIMATE_SMART_FEE = "estimateSmartFee"
        private const val METHOD_SEND_TRANSACTION = "sendTransaction"
        private const val METHOD_GET_DETAILED_TRANSACTION = "getDetailedTransaction"

        private const val METHOD = "method"
        private const val PARAMS = "params"
        private const val RESULT = "result"
        private const val SUBSCRIBE = "subscribe"
    }

    private val gson = Gson()

    val suspendedCoroutines = mutableListOf<CancellableContinuation<Any>>()

    val socket: Socket by lazy {
        initLogger(IO::class.java)
        initLogger(Emitter::class.java)
        initLogger(Socket::class.java)
        initLogger(io.socket.engineio.client.Socket::class.java)

        val opts = IO.Options()
        opts.transports = arrayOf(TRANSPORT)
        val socket = IO.socket(TrezorApplication.BLOCKBOOK_API_URL, opts)

        socket.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "EVENT_CONNECT")

            GlobalScope.launch {
                val info = getInfo()
                prefs.blockHeight = info.blocks
            }
        }.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "EVENT_DISCONNECT: " + it.size)
        }.on(Socket.EVENT_MESSAGE) {
            Log.d(TAG, "EVENT_MESSAGE: " + it[0])
        }.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d(TAG, "EVENT_CONNECT_ERROR: " + it[0])

            while (suspendedCoroutines.isNotEmpty()) {
                val item = suspendedCoroutines.removeAt(0)
                item.cancel(it[0] as Exception)
            }
        }.on(Socket.EVENT_ERROR) {
            Log.d(TAG, "EVENT_ERROR: " + it[0])
        }.on("bitcoind/hashblock") {
            Log.d(TAG, "bitcoind/hashblock: " + it[0])
        }.on("bitcoind/addresstxid") {
            Log.d(TAG, "bitcoind/addresstxid: " + it[0])
        }

        socket
    }

    fun connect() {
        socket.connect()
    }

    fun disconnect() {
        socket.disconnect()
    }

    private suspend fun sendMessage(body: JSONObject) = suspendCancellableCoroutine<Any> { cont ->
        suspendedCoroutines.add(cont)

        socket.connect()
        Log.d(TAG, "send: $body")
        socket.send(body, Ack {
            Log.d(TAG, "ack: " + it[0])
            val response = it[0] as JSONObject
            val result = response.get(RESULT)

            suspendedCoroutines.remove(cont)
            cont.resume(result)
        })
    }

    /**
     * Calls a method with parameters and returns the result.
     */
    private suspend fun callMethod(method: String, params: JSONArray = JSONArray()): Any {
        val body = JSONObject()
        body.put(METHOD, method)
        body.put(PARAMS, params)
        return sendMessage(body)
    }

    fun subscribe(vararg params: Any) {
        socket.emit(SUBSCRIBE, params)
    }

    /**
     * Get the blockchain status information.
     */
    suspend fun getInfo(): GetInfoResult {
        val json = callMethod(METHOD_GET_INFO) as JSONObject
        return gson.fromJson(json.toString(), GetInfoResult::class.java)
    }

    /**
     * Estimates fee in BTC/kb for new tx to be included within the first x blocks.
     */
    suspend fun estimateSmartFee(blocks: Int, conservative: Boolean): Double {
        val params = JSONArray()
        params.put(blocks)
        params.put(conservative)
        return callMethod(METHOD_ESTIMATE_SMART_FEE, params) as Double
    }

    /**
     * Get transactions for multiple addresses.
     */
    suspend fun getAddressHistory(addresses: List<String>, options: GetAddressHistoryOptions):
            GetAddressHistoryResult {
        val params = JSONArray()
        params.put(toJsonArray(addresses))
        params.put(toJsonObject(options))
        val resultJson = callMethod(METHOD_GET_ADDRESS_HISTORY, params) as JSONObject
        return gson.fromJson(resultJson.toString(), GetAddressHistoryResult::class.java)
    }

    /**
     * Gets a transaction detail by TXID.
     */
    suspend fun getDetailedTransaction(txid: String): Tx {
        val params = JSONArray()
        params.put(txid)
        val resultJson = callMethod(METHOD_GET_DETAILED_TRANSACTION, params) as JSONObject
        return gson.fromJson(resultJson.toString(), Tx::class.java)
    }

    /**
     * Broadcasts a transaction.
     */
    suspend fun sendTransaction(hex: String): String {
        val params = JSONArray()
        params.put(hex)
        return callMethod(METHOD_SEND_TRANSACTION, params) as String
    }

    private fun toJsonArray(list: List<String>): JSONArray {
        val json = JSONArray()
        list.forEach { json.put(it) }
        return json
    }

    private fun toJsonObject(obj: Any): JSONObject {
        return JSONObject(gson.toJson(obj))
    }

    private fun initLogger(klass: Class<out Any>) {
        Logger.getLogger(klass.name).level = Level.ALL
        Logger.getLogger(klass.name).addHandler(object : Handler() {
            override fun publish(record: LogRecord?) {
                Log.d(record?.loggerName, record?.message)
            }

            override fun flush() {
            }

            override fun close() {
            }

        })
    }
}