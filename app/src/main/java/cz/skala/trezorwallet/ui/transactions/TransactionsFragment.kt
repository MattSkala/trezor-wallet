package cz.skala.trezorwallet.ui.transactions

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.ui.BaseFragment
import cz.skala.trezorwallet.ui.transactiondetail.TransactionDetailActivity
import kotlinx.android.synthetic.main.fragment_transactions.*
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider


/**
 * A fragment for transactions list.
 */
class TransactionsFragment : BaseFragment() {
    companion object {
        const val ARG_ACCOUNT_ID = "account_id"
    }

    private val viewModel: TransactionsViewModel by instance()

    private val adapter = TransactionsAdapter()

    override fun provideOverridingModule() = Kodein.Module {
        bind<TransactionsViewModel>() with provider {
            ViewModelProviders.of(this@TransactionsFragment)[TransactionsViewModel::class.java]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments ?: return
        viewModel.start(args.getString(ARG_ACCOUNT_ID))

        viewModel.items.observe(this, Observer {
            if (it != null) {
                adapter.items = it
                adapter.notifyDataSetChanged()
            }
        })

        viewModel.refreshing.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = (it == true)
        })

        viewModel.empty.observe(this, Observer {
            empty.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        adapter.onTransactionClickListener = {
            startTransactionDetailActivity(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchTransactions()
        }

        btnHideAccount.setOnClickListener {
            viewModel.removeAccount()
        }
    }

    private fun startTransactionDetailActivity(transaction: TransactionWithInOut) {
        val intent = Intent(activity, TransactionDetailActivity::class.java)
        intent.putExtra(TransactionDetailActivity.EXTRA_ACCOUNT_ID, transaction.tx.account)
        intent.putExtra(TransactionDetailActivity.EXTRA_TXID, transaction.tx.txid)
        startActivity(intent)
    }
}