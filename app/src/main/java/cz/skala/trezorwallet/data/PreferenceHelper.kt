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
        private const val FEE_HIGH = "fee_high"
        private const val FEE_NORMAL = "fee_normal"
        private const val FEE_ECONOMY = "fee_economy"
        private const val FEE_LOW = "fee_low"
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
}