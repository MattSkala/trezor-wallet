package cz.skala.trezorwallet.data

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import cz.skala.trezorwallet.data.dao.AccountDao
import cz.skala.trezorwallet.data.dao.AddressDao
import cz.skala.trezorwallet.data.dao.TransactionDao
import cz.skala.trezorwallet.data.entity.*

@Database(entities = [Account::class, Transaction::class, TransactionInput::class,
    TransactionOutput::class, Address::class], version = 12, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun addressDao(): AddressDao
}