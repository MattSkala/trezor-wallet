package com.mattskala.trezorwallet.blockbook

import android.util.Log
import com.google.gson.Gson
import com.mattskala.trezorwallet.TrezorApplication
import com.mattskala.trezorwallet.blockbook.options.GetAddressHistoryOptions
import com.mattskala.trezorwallet.blockbook.response.AddressTxidResult
import com.mattskala.trezorwallet.blockbook.response.GetAddressHistoryResult
import com.mattskala.trezorwallet.blockbook.response.GetInfoResult
import com.mattskala.trezorwallet.blockbook.response.Tx
import com.mattskala.trezorwallet.data.PreferenceHelper
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


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
        private const val ERROR = "error"
        private const val MESSAGE = "message"
        private const val SUBSCRIBE = "subscribe"

        private const val BITCOIND_HASHBLOCK = "bitcoind/hashblock"
        private const val BITCOIND_ADDRESSTXID = "bitcoind/addresstxid"
    }


    private val gson = Gson()
    private val suspendedCoroutines = mutableListOf<CancellableContinuation<Any>>()

    private val connectionListeners = mutableListOf<ConnectionListener>()
    private val subscriptionListeners = mutableListOf<SubscriptionListener>()

    private val socket: Socket by lazy {
        val opts = IO.Options()
        opts.transports = arrayOf(TRANSPORT)
        opts.encoder = BlockbookPacketEncoder()
        val socket = IO.socket(TrezorApplication.BLOCKBOOK_API_URL, opts)

        socket.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "EVENT_CONNECT")

            connectionListeners.forEach { listener ->
                listener.onConnect()
            }
        }.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "EVENT_DISCONNECT")

            connectionListeners.forEach { listener ->
                listener.onDisconnect()
            }
        }.on(Socket.EVENT_MESSAGE) {
            Log.d(TAG, "EVENT_MESSAGE: " + it[0])
        }.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d(TAG, "EVENT_CONNECT_ERROR: " + it[0])

            while (suspendedCoroutines.isNotEmpty()) {
                val item = suspendedCoroutines.removeAt(0)
                item.cancel(BlockbookException("Connection error", it[0] as Exception))
            }
        }.on(Socket.EVENT_ERROR) {
            Log.d(TAG, "EVENT_ERROR: " + it[0])
        }.on(BITCOIND_HASHBLOCK) {
            Log.d(TAG, "bitcoind/hashblock: " + it[0])

            val hash = it[0] as String

            subscriptionListeners.forEach { listener ->
                listener.onHashblock(hash)
            }
        }.on(BITCOIND_ADDRESSTXID) {
            Log.d(TAG, "bitcoind/addresstxid: " + it[0])

            val result = gson.fromJson((it[0] as JSONObject).toString(),
                    AddressTxidResult::class.java)

            subscriptionListeners.forEach { listener ->
                listener.onAddressTxid(result.address, result.txid)
            }
        }

        socket
    }

    fun isConnected(): Boolean {
        return socket.connected()
    }

    fun connect() {
        socket.connect()
    }

    fun disconnect() {
        socket.disconnect()
    }

    @Throws(BlockbookException::class)
    private suspend fun sendMessage(body: JSONObject) = suspendCancellableCoroutine<Any> { cont ->
        suspendedCoroutines.add(cont)

        socket.connect()
        Log.d(TAG, "send: $body")
        socket.send(body, Ack {
            Log.d(TAG, "ack: " + it[0])
            val response = it[0] as JSONObject
            when {
                response.has(RESULT) -> {
                    val result = response.get(RESULT)
                    cont.resume(result)
                }
                response.has(ERROR) -> {
                    val errorJson = response.getJSONObject(ERROR)
                    val message = errorJson.getString(MESSAGE)
                    cont.resumeWithException(BlockbookException(message))
                }
                else -> cont.resumeWithException(BlockbookException("Invalid response"))
            }
            suspendedCoroutines.remove(cont)
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

    fun subscribe(topic: String) {
        Log.d(TAG, "subscribe: $topic")

        socket.connect()
        socket.emit(SUBSCRIBE, topic)
    }

    fun subscribeHashblock() {
        subscribe(BITCOIND_HASHBLOCK)
    }

    fun subscribeAddressTxid(addresses: List<String>) {
        socket.connect()

        val addressesJson = JSONArray()
        addresses.forEach {
            addressesJson.put(it)
        }
        socket.emit(SUBSCRIBE, BITCOIND_ADDRESSTXID, addressesJson)
    }

    fun addConnectionListener(listener: ConnectionListener) {
        if (!connectionListeners.contains(listener)) {
            connectionListeners.add(listener)
        }
    }

    fun removeConnectionListener(listener: ConnectionListener) {
        connectionListeners.remove(listener)
    }

    fun addSubscriptionListener(listener: SubscriptionListener) {
        if (!subscriptionListeners.contains(listener)) {
            subscriptionListeners.add(listener)
        }
    }

    fun removeSubscriptionListener(listener: SubscriptionListener) {
        subscriptionListeners.remove(listener)
    }

    private fun toJsonArray(list: List<String>): JSONArray {
        val json = JSONArray()
        list.forEach { json.put(it) }
        return json
    }

    private fun toJsonObject(obj: Any): JSONObject {
        return JSONObject(gson.toJson(obj))
    }

    interface SubscriptionListener {
        fun onHashblock(hash: String)
        fun onAddressTxid(address: String, txid: String)
    }

    interface ConnectionListener {
        fun onConnect()
        fun onDisconnect()
    }
}