package cz.skala.trezorwallet

import android.app.Application
import android.arch.persistence.room.Room
import android.util.Log
import cz.skala.trezorwallet.coinmarketcap.CoinMarketCapClient
import cz.skala.trezorwallet.compose.CoinSelector
import cz.skala.trezorwallet.compose.FeeEstimator
import cz.skala.trezorwallet.compose.FifoCoinSelector
import cz.skala.trezorwallet.compose.TransactionComposer
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.repository.TransactionRepository
import cz.skala.trezorwallet.discovery.AccountDiscoveryManager
import cz.skala.trezorwallet.discovery.TransactionFetcher
import cz.skala.trezorwallet.blockbook.BlockbookApiService
import cz.skala.trezorwallet.blockbook.BlockbookSocketService
import cz.skala.trezorwallet.discovery.BalanceCalculator
import cz.skala.trezorwallet.insight.InsightApiService
import cz.skala.trezorwallet.labeling.LabelingManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber


class TrezorApplication : Application(), KodeinAware {
    override val kodein = Kodein.lazy {
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

        bind<BlockbookApiService>() with eagerSingleton {
            val httpClient = OkHttpClient.Builder()

            // logging interceptor
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            httpClient.addInterceptor(logging)

            val retrofit = Retrofit.Builder()
                    .baseUrl(BLOCKBOOK_API_HOST + BLOCKBOOK_API_PATH)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build()

            retrofit.create(BlockbookApiService::class.java)
        }

        bind<AccountDiscoveryManager>() with singleton {
            AccountDiscoveryManager(instance())
        }

        bind<TransactionFetcher>() with singleton {
            TransactionFetcher(instance(), instance())
        }

        bind<CoinMarketCapClient>() with singleton {
            CoinMarketCapClient()
        }

        bind<PreferenceHelper>() with singleton {
            PreferenceHelper(applicationContext)
        }

        bind<FeeEstimator>() with singleton {
            FeeEstimator(instance())
        }

        bind<CoinSelector>() with singleton {
            FifoCoinSelector()
        }

        bind<TransactionComposer>() with singleton {
            TransactionComposer(instance(), instance())
        }

        bind<TransactionRepository>() with singleton {
            TransactionRepository(instance(), instance(), instance(), instance())
        }

        bind<LabelingManager>() with singleton {
            LabelingManager(applicationContext, instance(), instance())
        }

        bind<BlockbookSocketService>() with singleton {
            BlockbookSocketService(instance())
        }

        bind<BalanceCalculator>() with singleton {
            BalanceCalculator()
        }
    }

    companion object {
        const val DATABASE_NAME = "trezor-wallet"
        const val INSIGHT_API_URL = "https://insight.bitpay.com/api/"
        const val BLOCKBOOK_API_HOST = "https://btc1.trezor.io"
        const val BLOCKBOOK_API_PATH = "/api/"
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