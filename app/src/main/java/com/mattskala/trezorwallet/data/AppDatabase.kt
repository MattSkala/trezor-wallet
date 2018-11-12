package com.mattskala.trezorwallet.data

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import com.mattskala.trezorwallet.data.dao.AccountDao
import com.mattskala.trezorwallet.data.dao.AddressDao
import com.mattskala.trezorwallet.data.dao.TransactionDao
import com.mattskala.trezorwallet.data.entity.*

@Database(entities = [Account::class, Transaction::class, TransactionInput::class,
    TransactionOutput::class, Address::class], version = 29, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun addressDao(): AddressDao
}