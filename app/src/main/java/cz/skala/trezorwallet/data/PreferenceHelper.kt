package cz.skala.trezorwallet.data

import android.content.Context
import org.jetbrains.anko.defaultSharedPreferences

/**
 * A helper for accessing SharedPreferences.
 */
class PreferenceHelper(context: Context) {
    companion object {
        private const val INITIALIZED = "initialized"
        private const val RATE = "rate"
        private const val CURRENCY_CODE = "currency_code"
    }

    private val prefs = context.defaultSharedPreferences

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
        get() = prefs.getString(CURRENCY_CODE, "USD")
        set(value) = prefs.edit().putString(CURRENCY_CODE, value).apply()
}