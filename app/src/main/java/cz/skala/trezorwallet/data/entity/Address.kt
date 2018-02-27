package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * An address entity.
 */
@Entity(tableName = "addresses")
class Address(
        @PrimaryKey val address: String,
        val account: String,
        val change: Boolean,
        val index: Int,
        val label: String?,
        var totalReceived: Double
)