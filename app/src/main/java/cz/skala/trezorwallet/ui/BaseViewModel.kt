package cz.skala.trezorwallet.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.os.Bundle
import android.support.annotation.CallSuper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein


abstract class BaseViewModel(app: Application) : AndroidViewModel(app), KodeinAware {
    override val kodein by closestKodein(app)

    protected val job = Job()
    protected val viewModelScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}