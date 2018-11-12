package com.mattskala.trezorwallet.ui.addressdetail

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import com.satoshilabs.trezor.intents.ui.data.CheckAddressRequest
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import com.mattskala.trezorwallet.R
import com.mattskala.trezorwallet.data.PreferenceHelper
import com.mattskala.trezorwallet.data.entity.Account
import com.mattskala.trezorwallet.data.entity.Address
import com.mattskala.trezorwallet.labeling.LabelingManager
import com.mattskala.trezorwallet.ui.BaseActivity
import com.mattskala.trezorwallet.ui.LabelDialogFragment
import kotlinx.android.synthetic.main.activity_address_detail.*
import net.glxn.qrgen.android.QRCode
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider


/**
 * An activity for address detail.
 */
class AddressDetailActivity : BaseActivity(), LabelDialogFragment.EditTextDialogListener {
    companion object {
        const val EXTRA_ADDRESS = "address"
    }

    private val viewModel: AddressDetailViewModel by instance()

    private val labeling: LabelingManager by instance()
    private val prefs: PreferenceHelper by instance()

    override fun provideOverridingModule() = Kodein.Module("AddressDetail") {
        bind<AddressDetailViewModel>() with provider {
            ViewModelProviders.of(this@AddressDetailActivity)[AddressDetailViewModel::class.java]
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val address = intent.getParcelableExtra<Address>(EXTRA_ADDRESS)
        viewModel.start(address)

        setContentView(R.layout.activity_address_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        txtAddress.text = address.address

        btnCopy.setOnClickListener {
            copyToClipboard(address)
        }

        btnShow.setOnClickListener {
            viewModel.showOnTrezor()
        }

        showQrCode(address)

        viewModel.addressLabel.observe(this, Observer {
            supportActionBar?.title = it
        })

        viewModel.accountLabel.observe(this, Observer {
            txtAccountLabel.text = it
        })

        viewModel.derivationPath.observe(this, Observer {
            txtAddressPath.text = it
        })

        viewModel.showOnTrezorRequest.observe(this, Observer {
            if (it != null) {
                showOnTrezor(it.first, it.second)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.address_detail, menu)
        val label = menu.findItem(R.id.label)
        label.isVisible = labeling.isEnabled()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.web -> {
                showAddressOnWeb()
                true
            }
            R.id.label -> {
                showLabelDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onTextChanged(text: String) {
        viewModel.setAddressLabel(text)
    }

    private fun copyToClipboard(address: Address) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Bitcoin Address", address.address)
        clipboard.primaryClip = clip

        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun showQrCode(address: Address) {
        val qrCodeBitmap = QRCode.from("bitcoin:" + address.address)
                .withSize(500, 500)
                .bitmap()
        imgQrCode.setImageBitmap(qrCodeBitmap)
    }

    private fun showOnTrezor(address: Address, account: Account) {
        val path = address.getPath(account)
        val message = TrezorMessage.GetAddress.newBuilder()
                .addAllAddressN(path.asList())
                .setShowDisplay(true)
                .setScriptType(if (account.legacy) TrezorType.InputScriptType.SPENDADDRESS else
                    TrezorType.InputScriptType.SPENDP2SHWITNESS)
                .build()

        val intent = TrezorActivity.createIntent(this,
                CheckAddressRequest(message, address.address, prefs.deviceState))
        startActivity(intent)
    }

    private fun showAddressOnWeb() {
        val address = intent.getParcelableExtra<Address>(EXTRA_ADDRESS).address
        val url = "https://blockchain.info/address/$address"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    private fun showLabelDialog() {
        val fragment = LabelDialogFragment()
        val title = resources.getString(R.string.address_label)
        val address = intent.getParcelableExtra<Address>(EXTRA_ADDRESS)
        val label = address.label ?: ""
        val args = Bundle()
        args.putString(LabelDialogFragment.ARG_TITLE, title)
        args.putString(LabelDialogFragment.ARG_TEXT, label)
        fragment.arguments = args
        fragment.show(supportFragmentManager, "dialog")
    }
}