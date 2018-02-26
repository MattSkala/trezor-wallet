package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * An address entity.
 */
@Entity(tableName = "addresses")
class Address(
        @PrimaryKey val address: String,
        val account: Int,
        val change: Boolean,
        val index: Int,
        val label: String?
)