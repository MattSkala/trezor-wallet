package cz.skala.trezorwallet.ui.addresses

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.SupportFragmentInjector
import cz.skala.trezorwallet.R
import kotlinx.android.synthetic.main.fragment_addresses.*


/**
 * A fragment for addresses list.
 */
class AddressesFragment : Fragment(), SupportFragmentInjector {
    companion object {
        val ARG_ACCOUNT_ID = "account_id"
    }

    override val injector = KodeinInjector()
    private val viewModel: AddressesViewModel by injector.instance()

    val adapter = AddressesAdapter()

    override fun provideOverridingModule() = Kodein.Module {
        bind<AddressesViewModel>() with provider {
            val factory = AddressesViewModel.Factory(instance())
            ViewModelProviders.of(this@AddressesFragment, factory)[AddressesViewModel::class.java]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeInjector()

        val args = arguments ?: return
        viewModel.start(args.getString(ARG_ACCOUNT_ID))

        viewModel.items.observe(this, Observer {
            if (it != null) {
                adapter.updateItems(it)
            }
        })

        adapter.onPreviousAddressesClickListener = {
            viewModel.togglePreviousAddresses()
        }

        adapter.onMoreAddressesClickListener = {
            viewModel.addFreshAddress()
            recyclerView.scrollToPosition(adapter.itemCount - 1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_addresses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    override fun onDestroy() {
        destroyInjector()
        super.onDestroy()
    }
}