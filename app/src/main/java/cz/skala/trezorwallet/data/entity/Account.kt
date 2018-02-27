package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * An account entity.
 */
@Entity(tableName = "accounts")
class Account(
        @PrimaryKey val id: String,
        @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val publicKey: ByteArray,
        @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val chainCode: ByteArray,
        val index: Int,
        val legacy: Boolean,
        val label: String?,
        val balance: Int
)