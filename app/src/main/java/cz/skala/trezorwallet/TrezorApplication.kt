package cz.skala.trezorwallet

import android.app.Application
import android.arch.persistence.room.Room
import com.github.salomonbrys.kodein.*
import cz.skala.trezorwallet.coinmarketcap.CoinMarketCapClient
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.discovery.AccountDiscoveryManager
import cz.skala.trezorwallet.discovery.TransactionFetcher
import cz.skala.trezorwallet.insight.InsightApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber


class TrezorApplication : Application(), KodeinAware {
    override val kodein by Kodein.lazy {
        bind<AppDatabase>() with eagerSingleton {
            Room.databaseBuilder(applicationContext, AppDatabase::class.java, DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
        }

        bind<InsightApiService>() with eagerSingleton {
            val httpClient = OkHttpClient.Builder()

            // logging interceptor
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            httpClient.addInterceptor(logging)

            val retrofit = Retrofit.Builder()
                    .baseUrl(INSIGHT_API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build()

            retrofit.create(InsightApiService::class.java)
        }

        bind<AccountDiscoveryManager>() with singleton {
            AccountDiscoveryManager(instance())
        }

        bind<TransactionFetcher>() with singleton {
            TransactionFetcher(instance())
        }

        bind<CoinMarketCapClient>() with singleton {
            CoinMarketCapClient()
        }

        bind<PreferenceHelper>() with singleton {
            PreferenceHelper(applicationContext)
        }
    }

    companion object {
        const val PREF_INITIALIZED = "initialized"

        const val DATABASE_NAME = "trezor-wallet"
        const val INSIGHT_API_URL = "https://btc-bitcore1.trezor.io/api/"
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}