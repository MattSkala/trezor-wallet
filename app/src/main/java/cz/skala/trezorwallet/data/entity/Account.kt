package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.content.res.Resources
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.crypto.ExtendedPublicKey

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
    companion object {
        fun fromNode(node: TrezorType.HDNodeType, legacy: Boolean): Account {
            val publicKey = node.publicKey.toByteArray()
            val chainCode = node.chainCode.toByteArray()
            val index = node.childNum - ExtendedPublicKey.HARDENED_IDX.toInt()
            val accountNode = ExtendedPublicKey(ExtendedPublicKey.decodePublicKey(publicKey), chainCode)
            return Account(accountNode.getAddress(), publicKey, chainCode, index,
                    legacy, null, 0.0)
        }
    }

    fun getDisplayLabel(resources: Resources): String {
        return when {
            label != null -> label
            legacy -> resources.getString(R.string.legacy_account_x, index + 1)
            else -> resources.getString(R.string.account_x, index + 1)
        }
    }
}