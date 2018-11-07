package cz.skala.trezorwallet.ui.addressdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import com.satoshilabs.trezor.intents.ui.data.CheckAddressRequest
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.data.entity.Address
import cz.skala.trezorwallet.labeling.LabelingManager
import cz.skala.trezorwallet.ui.LabelDialogFragment
import kotlinx.android.synthetic.main.activity_address_detail.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.glxn.qrgen.android.QRCode
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance


/**
 * An activity for address detail.
 */
class AddressDetailActivity : AppCompatActivity(), KodeinAware, LabelDialogFragment.EditTextDialogListener {
    companion object {
        const val EXTRA_ADDRESS = "address"
    }

    override val kodein by closestKodein()
    private val database: AppDatabase by instance()
    private val labeling: LabelingManager by instance()
    private val prefs: PreferenceHelper by instance()

    private var account: Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_address_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val address = intent.getParcelableExtra<Address>(EXTRA_ADDRESS)
        txtAddress.text = address.address

        updateActionBarTitle(address)

        btnCopy.setOnClickListener {
            copyToClipboard(address)
        }

        btnShow.setOnClickListener {
            account?.let { account ->
                showOnTrezor(address, account)
            }
        }

        loadAccount(address)

        showQrCode(address)
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
        val address = intent.getParcelableExtra<Address>(EXTRA_ADDRESS)
        GlobalScope.launch(Dispatchers.Main) {
            labeling.setAddressLabel(address, text)
            updateActionBarTitle(address)
        }
    }

    private fun updateActionBarTitle(address: Address) {
        supportActionBar?.title = if (address.label.isNullOrEmpty())
            resources.getString(R.string.address) else address.label
    }

    private fun loadAccount(address: Address) {
        GlobalScope.launch(Dispatchers.Main) {
            val acc = GlobalScope.async(Dispatchers.Default) {
                database.accountDao().getById(address.account)
            }.await()
            account = acc
            txtAccountLabel.text = acc.getDisplayLabel(resources)
            txtAddressPath.text = address.getPathString(acc)
        }
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