package cz.skala.trezorwallet.data.entity

import android.annotation.SuppressLint
import android.arch.persistence.room.Entity
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * An address entity.
 */
@SuppressLint("ParcelCreator")
@Entity(tableName = "addresses", primaryKeys = ["address", "account"])
@Parcelize
class Address(
        val address: String,
        val account: String,
        val change: Boolean,
        val index: Int,
        val label: String?,
        var totalReceived: Double
) : Parcelable {
    fun getPath(account: Account): String {
        var path = "m"
        path += if (account.legacy) "/44'" else "/49'"
        path += "/0'"
        path += "/" + account.index + "'"
        path += if (change) "/1" else "/0"
        path += "/" + index
        return path
    }
}