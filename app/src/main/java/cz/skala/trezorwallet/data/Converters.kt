package cz.skala.trezorwallet.data

import android.arch.persistence.room.TypeConverter
import cz.skala.trezorwallet.data.entity.Transaction


class Converters {
    @TypeConverter
    fun fromTransactionType(value: Transaction.Type): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): Transaction.Type {
        return Transaction.Type.valueOf(value)
    }
}