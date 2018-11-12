package com.mattskala.trezorwallet.data

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.google.protobuf.ByteString
import com.satoshilabs.trezor.intents.hexToBytes
import com.satoshilabs.trezor.intents.toHex

/**
 * A helper for accessing SharedPreferences.
 */
class PreferenceHelper(context: Context) {
    companion object {
        private const val INITIALIZED = "initialized"
        private const val RATE = "rate"
        private const val CURRENCY_CODE = "currency_code"
        private const val FEE_HIGH = "fee_high"
        private const val FEE_NORMAL = "fee_normal"
        private const val FEE_ECONOMY = "fee_economy"
        private const val FEE_LOW = "fee_low"
        private const val LABELING_MASTER_KEY = "labeling_master_key"
        private const val DROPBOX_TOKEN = "dropbox_token"
        private const val DEVICE_STATE = "device_state"
        private const val BLOCK_HEIGHT = "block_height"
        private const val SYNC_HEIGHT = "sync_height"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * TREZOR has been connected and account discovery performed.
     */
    var initialized: Boolean
        get() = prefs.getBoolean(INITIALIZED, false)
        set(value) = prefs.edit().putBoolean(INITIALIZED, value).apply()

    /**
     * BTC/fiat exchange rate.
     */
    var rate: Float
        get() = prefs.getFloat(RATE, 0f)
        set(value) = prefs.edit().putFloat(RATE, value).apply()

    /**
     * Fiat currency code.
     */
    var currencyCode: String
        get() = prefs.getString(CURRENCY_CODE, "USD")!!
        set(value) = prefs.edit().putString(CURRENCY_CODE, value).apply()

    /**
     * The recommended high fee.
     */
    var feeHigh: Int
        get() = prefs.getInt(FEE_HIGH, 200)
        set(value) = prefs.edit().putInt(FEE_HIGH, value).apply()

    /**
     * The recommended normal fee.
     */
    var feeNormal: Int
        get() = prefs.getInt(FEE_NORMAL, 140)
        set(value) = prefs.edit().putInt(FEE_NORMAL, value).apply()

    /**
     * The recommended economy fee.
     */
    var feeEconomy: Int
        get() = prefs.getInt(FEE_ECONOMY, 70)
        set(value) = prefs.edit().putInt(FEE_ECONOMY, value).apply()

    /**
     * The recommended low fee.
     */
    var feeLow: Int
        get() = prefs.getInt(FEE_LOW, 10)
        set(value) = prefs.edit().putInt(FEE_LOW, value).apply()

    /**
     * The master key user for labeling.
     */
    var labelingMasterKey: ByteArray?
        get() = prefs.getString(LABELING_MASTER_KEY, null)?.hexToBytes()
        set(value) = prefs.edit().putString(LABELING_MASTER_KEY, value?.toHex()).apply()

    /**
     * Dropbox OAuth2 token.
     */
    var dropboxToken: String?
        get() = prefs.getString(DROPBOX_TOKEN, null)
        set(value) = prefs.edit().putString(DROPBOX_TOKEN, value).apply()

    /**
     * TREZOR device state.
     */
    var deviceState: ByteString?
        get() {
            val string = prefs.getString(DEVICE_STATE, null)
            Log.d("TrezorPrefs", "get device state = $string")
            return if (string != null) ByteString.copyFrom(string.hexToBytes()) else null
        }
        set(value) {
            value?.toByteArray()
            val hex = value?.toByteArray()?.toHex()
            Log.d("TrezorPrefs", "set device state = $hex")
            prefs.edit().putString(DEVICE_STATE, hex).apply()
        }

    /**
     * Current blockchain height.
     */
    var blockHeight: Int
        get() = prefs.getInt(BLOCK_HEIGHT, 0)
        set(value) = prefs.edit().putInt(BLOCK_HEIGHT, value).apply()

    /**
     * The last synchronized block.
     */
    var syncHeight: Int
        get() = prefs.getInt(SYNC_HEIGHT, 0)
        set(value) = prefs.edit().putInt(SYNC_HEIGHT, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}