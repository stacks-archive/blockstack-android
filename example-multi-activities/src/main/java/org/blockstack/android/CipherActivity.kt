package org.blockstack.android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.android.synthetic.main.content_cipher.*
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.CryptoOptions
import java.io.ByteArrayOutputStream

class CipherActivity : AppCompatActivity() {

    private var _blockstackSession: BlockstackSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cipher)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        _blockstackSession = BlockstackSession(this, defaultConfig,
                onLoadedCallback = { checkLogin() })
    }

    override fun onResume() {
        super.onResume()
        if (_blockstackSession?.loaded == true) {
            checkLogin()
        }
    }

    fun checkLogin() {
        blockstackSession().isUserSignedIn({ signedIn ->
            progressBar.visibility = View.GONE
            if (signedIn) {
                encryptDecryptString()
                encryptDecryptImage()
            } else {
                navigateToAccount()
            }
        })
    }

    private fun navigateToAccount() {
        startActivity(Intent(this, AccountActivity::class.java))
    }

    fun encryptDecryptString() {
        val options = CryptoOptions(null)
        blockstackSession().encryptContent("Hello Android", options) { cipherResult ->
            if (cipherResult.hasValue) {
                val cipher = cipherResult.value!!
                blockstackSession().decryptContent(cipher.json.toString(), options) { plainContentResult ->
                    if (plainContentResult.hasValue) {
                        val plainContent:String = plainContentResult.value as String
                        runOnUiThread {
                            textView.setText(plainContent)
                        }
                    } else {
                        Toast.makeText(this, "error: " + plainContentResult.error, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "error: " + cipherResult.error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun encryptDecryptImage() {

        val drawable: BitmapDrawable = resources.getDrawable(R.drawable.default_avatar) as BitmapDrawable

        val bitmap = drawable.getBitmap()
        val stream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bitMapData = stream.toByteArray()

        val options = CryptoOptions(null)
        blockstackSession().encryptContent(bitMapData, options) { cipherResult ->
            if (cipherResult.hasValue) {
                val cipher = cipherResult.value!!
                blockstackSession().decryptContent(cipher.json.toString(), options) { plainContentResult ->
                    if (plainContentResult.hasValue) {
                        val plainContent: ByteArray = plainContentResult.value as ByteArray
                        val imageByteArray = plainContent
                        val bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)
                        runOnUiThread {
                            imageView.setImageBitmap(bitmap)
                        }
                    } else {
                        Toast.makeText(this, "error: " + plainContentResult.error, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "error: " + cipherResult.error, Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun blockstackSession(): BlockstackSession {
        val session = _blockstackSession
        if (session != null) {
            return session
        } else {
            throw IllegalStateException("No session.")
        }
    }
}