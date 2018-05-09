package cz.skala.trezorwallet.ui

import android.support.v7.app.AppCompatActivity
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein


abstract class BaseActivity : AppCompatActivity(), KodeinAware {
    private val parentKodein by closestKodein()
    override val kodein = Kodein.lazy {
        extend(parentKodein)
        import(provideOverridingModule())
    }

    open fun provideOverridingModule() = Kodein.Module {}
}