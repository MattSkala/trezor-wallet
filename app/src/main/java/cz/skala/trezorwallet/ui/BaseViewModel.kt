package cz.skala.trezorwallet.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein


abstract class BaseViewModel(app: Application) : AndroidViewModel(app), KodeinAware {
    override val kodein by closestKodein(app)
}