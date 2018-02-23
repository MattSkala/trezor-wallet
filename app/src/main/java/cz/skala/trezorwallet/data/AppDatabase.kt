package cz.skala.trezorwallet.data

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import cz.skala.trezorwallet.data.dao.AccountDao
import cz.skala.trezorwallet.data.entity.Account

@Database(entities = [Account::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}