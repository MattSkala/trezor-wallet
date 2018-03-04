package cz.skala.trezorwallet.ui.addressdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.AppCompatActivityInjector
import com.github.salomonbrys.kodein.instance
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import com.satoshilabs.trezor.intents.ui.data.CheckAddressRequest
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.data.entity.Address
import kotlinx.android.synthetic.main.activity_address_detail.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import net.glxn.qrgen.android.QRCode
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.imageBitmap


/**
 * An activity for address detail.
 */
class AddressDetailActivity : AppCompatActivity(), AppCompatActivityInjector {
    companion object {
        const val EXTRA_ADDRESS = "address"
    }

    override val injector = KodeinInjector()
    private val database: AppDatabase by injector.instance()

    private var account: Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeInjector()

        setContentView(R.layout.activity_address_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val address = intent.getParcelableExtra<Address>(EXTRA_ADDRESS)
        txtAddress.text = address.address

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

    override fun onDestroy() {
        destroyInjector()
        super.onDestroy()
    }

    private fun loadAccount(address: Address) {
        launch(UI) {
            val acc = bg {
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
        imgQrCode.imageBitmap = qrCodeBitmap
    }

    private fun showOnTrezor(address: Address, account: Account) {
        val message = TrezorMessage.GetAddress.newBuilder()
                .addAllAddressN(address.getPath(account).asList())
                .setShowDisplay(true)
                .build()
        val intent = TrezorActivity.createIntent(this, CheckAddressRequest(message, address.address))
        startActivity(intent)
    }
}