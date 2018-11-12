package com.mattskala.trezorwallet.ui.transactions


class AccountSummary(val received: Long, val sent: Long) {
    val balance = received - sent
}