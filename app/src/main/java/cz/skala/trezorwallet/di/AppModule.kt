package cz.skala.trezorwallet.di

import android.app.Application
import android.arch.persistence.room.Room
import cz.skala.trezorwallet.TrezorApplication
import cz.skala.trezorwallet.blockbook.BlockbookSocketService
import cz.skala.trezorwallet.coinmarketcap.CoinMarketCapClient
import cz.skala.trezorwallet.compose.CoinSelector
import cz.skala.trezorwallet.compose.FeeEstimator
import cz.skala.trezorwallet.compose.FifoCoinSelector
import cz.skala.trezorwallet.compose.TransactionComposer
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.repository.TransactionRepository
import cz.skala.trezorwallet.discovery.AccountDiscoveryManager
import cz.skala.trezorwallet.discovery.BalanceCalculator
import cz.skala.trezorwallet.discovery.TransactionFetcher
import cz.skala.trezorwallet.labeling.LabelingManager
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton


fun appModule(applicationContext: Application): Kodein.Module {
    return Kodein.Module("AppModule") {
        bind<AppDatabase>() with eagerSingleton {
            Room.databaseBuilder(applicationContext, AppDatabase::class.java, TrezorApplication.DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
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
}