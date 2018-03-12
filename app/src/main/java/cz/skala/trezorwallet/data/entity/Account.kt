package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.content.res.Resources
import cz.skala.trezorwallet.R

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
        val balance: Double
) {
    fun getDisplayLabel(resources: Resources): String {
        return when {
            label != null -> label
            legacy -> resources.getString(R.string.legacy_account_x, index + 1)
            else -> resources.getString(R.string.account_x, index + 1)
        }
    }
}