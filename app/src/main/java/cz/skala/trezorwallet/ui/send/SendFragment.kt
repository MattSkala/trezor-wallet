package cz.skala.trezorwallet.ui.send

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.SupportFragmentInjector
import cz.skala.trezorwallet.R


/**
 * A fragment for composing a transaction.
 */
class SendFragment : Fragment(), SupportFragmentInjector {
    companion object {
        const val ARG_ACCOUNT_ID = "account_id"
    }

    override val injector = KodeinInjector()
    private val viewModel: SendViewModel by injector.instance()

    override fun provideOverridingModule() = Kodein.Module {
        bind<SendViewModel>() with provider {
            val factory = SendViewModel.Factory(instance())
            ViewModelProviders.of(this@SendFragment, factory)[SendViewModel::class.java]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeInjector()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroy() {
        destroyInjector()
        super.onDestroy()
    }
}