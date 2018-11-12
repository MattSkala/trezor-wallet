package com.mattskala.trezorwallet

import android.app.Application
import android.util.Log
import com.mattskala.trezorwallet.di.appModule
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import timber.log.Timber
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import com.crashlytics.android.core.CrashlyticsCore




class TrezorApplication : Application(), KodeinAware {
    override val kodein = Kodein.lazy {
        import(appModule(this@TrezorApplication))
    }

    companion object {
        const val DATABASE_NAME = "trezor-wallet"
        const val BLOCKBOOK_API_URL = BuildConfig.BLOCKBOOK_URL

        fun isTestnet(): Boolean {
            return BuildConfig.FLAVOR == "btcTestnet"
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val crashlyticsCore = CrashlyticsCore.Builder()
                .disabled(BuildConfig.DEBUG)
                .build()
        Fabric.with(this, Crashlytics.Builder().core(crashlyticsCore).build())

        // Log exceptions from coroutines
        val currentUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.println(Log.ERROR, thread.name, Log.getStackTraceString(exception))
            currentUncaughtExceptionHandler.uncaughtException(thread, exception)
        }
    }
}