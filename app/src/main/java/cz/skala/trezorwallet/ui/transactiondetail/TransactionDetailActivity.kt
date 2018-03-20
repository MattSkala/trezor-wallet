package cz.skala.trezorwallet.ui.transactiondetail

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.AppCompatActivityInjector
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.data.entity.TransactionInput
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.ui.BTC_TO_SATOSHI
import cz.skala.trezorwallet.ui.formatBtcValue
import kotlinx.android.synthetic.main.activity_transaction_detail.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


/**
 * A transaction detail activity.
 */
class TransactionDetailActivity : AppCompatActivity(), AppCompatActivityInjector {
    companion object {
        const val EXTRA_TXID = "txid"
    }

    override val injector = KodeinInjector()
    private val viewModel: TransactionDetailViewModel by injector.instance()

    override fun provideOverridingModule() = Kodein.Module {
        bind<TransactionDetailViewModel>() with provider {
            val factory = TransactionDetailViewModel.Factory(instance())
            ViewModelProviders.of(this@TransactionDetailActivity, factory)[TransactionDetailViewModel::class.java]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initializeInjector()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_transaction_detail)

        val txid = intent.getStringExtra(EXTRA_TXID)
        txtHash.text = txid

        viewModel.start(intent.getStringExtra(EXTRA_TXID))

        viewModel.transaction.observe(this, Observer {
            if (it != null) {
                showTransaction(it)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.transaction_detail, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.web -> {
                showTransactionOnWeb()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        destroyInjector()
        super.onDestroy()
    }

    private fun showTransaction(transaction: TransactionWithInOut) {
        val blocktime = transaction.tx.blocktime
        if (blocktime != null && blocktime > 0) {
            txtConfirmed.setText(R.string.confirmed)
            txtBlockTime.visibility = View.VISIBLE
            txtBlockHeight.visibility = View.VISIBLE
            val date = Date(blocktime * 1000)
            txtBlockTime.text = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG,
                    SimpleDateFormat.MEDIUM).format(date)
            txtBlockHeight.text = getString(R.string.in_block_x, transaction.tx.blockheight.toString())
        } else {
            txtConfirmed.setText(R.string.tx_unconfirmed)
            txtBlockTime.visibility = View.GONE
            txtBlockHeight.visibility = View.GONE
        }

        transaction.vin.forEach {
            addInputView(it)
        }

        transaction.vout.forEach {
            addOutputView(it)
        }

        txtFee.text = formatBtcValue(transaction.tx.fee)
        val feePerByte = (transaction.tx.fee * BTC_TO_SATOSHI /transaction.tx.size).roundToInt()
        txtFeePerByte.text = getString(R.string.sat_per_byte, feePerByte.toString())
    }

    private fun addInputView(input: TransactionInput) {
        val view = TransactionInOutView(this)
        view.setValue(input.value)
        view.setAddress(input.addr)
        view.setOnClickListener {
            showAddressOnWeb(input.addr)
        }
        inputs.addView(view)
    }

    private fun addOutputView(output: TransactionOutput) {
        val view = TransactionInOutView(this)
        view.setValue(output.value)
        view.setAddress(output.addr ?: "Unknown Address")
        view.setOnClickListener {
            output.addr?.let {
                showAddressOnWeb(it)
            }
        }
        outputs.addView(view)
    }

    private fun showTransactionOnWeb() {
        val txid = viewModel.transaction.value!!.tx.txid
        val url = "https://blockchain.info/tx/$txid"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    private fun showAddressOnWeb(address: String) {
        val url = "https://blockchain.info/address/$address"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }
}