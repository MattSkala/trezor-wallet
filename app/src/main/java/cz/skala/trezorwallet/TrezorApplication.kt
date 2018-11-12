package cz.skala.trezorwallet

import android.app.Application
import android.util.Log
import cz.skala.trezorwallet.di.appModule
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import timber.log.Timber


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

        // Log exceptions from coroutines
        val currentUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.println(Log.ERROR, thread.name, Log.getStackTraceString(exception))
            currentUncaughtExceptionHandler.uncaughtException(thread, exception)
        }
    }
}