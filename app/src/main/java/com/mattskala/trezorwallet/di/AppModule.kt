package com.mattskala.trezorwallet.di

import android.app.Application
import android.arch.persistence.room.Room
import com.mattskala.trezorwallet.TrezorApplication
import com.mattskala.trezorwallet.blockbook.BlockbookSocketService
import com.mattskala.trezorwallet.coinmarketcap.CoinMarketCapClient
import com.mattskala.trezorwallet.compose.CoinSelector
import com.mattskala.trezorwallet.compose.FeeEstimator
import com.mattskala.trezorwallet.compose.FifoCoinSelector
import com.mattskala.trezorwallet.compose.TransactionComposer
import com.mattskala.trezorwallet.data.AppDatabase
import com.mattskala.trezorwallet.data.PreferenceHelper
import com.mattskala.trezorwallet.data.repository.AccountRepository
import com.mattskala.trezorwallet.data.repository.AddressRepository
import com.mattskala.trezorwallet.data.repository.TransactionRepository
import com.mattskala.trezorwallet.discovery.AccountDiscoveryManager
import com.mattskala.trezorwallet.discovery.BalanceCalculator
import com.mattskala.trezorwallet.discovery.TransactionFetcher
import com.mattskala.trezorwallet.labeling.LabelingManager
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
            AccountDiscoveryManager(instance(), instance())
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

        bind<AccountRepository>() with singleton {
            AccountRepository(instance())
        }

        bind<AddressRepository>() with singleton {
            AddressRepository(instance())
        }

        bind<LabelingManager>() with singleton {
            LabelingManager(applicationContext, instance(), instance(), instance(), instance())
        }

        bind<BlockbookSocketService>() with singleton {
            BlockbookSocketService(instance())
        }

        bind<BalanceCalculator>() with singleton {
            BalanceCalculator()
        }
    }
}