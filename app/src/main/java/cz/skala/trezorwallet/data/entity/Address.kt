package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * An address entity.
 */
@Entity
class Address(
        @PrimaryKey
        val address: String,
        val label: String?
)