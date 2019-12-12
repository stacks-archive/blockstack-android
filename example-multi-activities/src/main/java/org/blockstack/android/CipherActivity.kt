package org.blockstack.android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.android.synthetic.main.content_cipher.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.blockstack.android.sdk.Blockstack
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.model.CryptoOptions
import org.blockstack.android.sdk.model.GetFileOptions
import org.blockstack.android.sdk.model.PutFileOptions
import java.io.ByteArrayOutputStream

val TAG = CipherActivity::class.java.simpleName

class CipherActivity : AppCompatActivity() {

    private lateinit var blockstack: Blockstack
    private var _blockstackSession: BlockstackSession? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cipher)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val sessionStore = SessionStoreProvider.getInstance(this)
        blockstack = Blockstack()
        _blockstackSession = BlockstackSession(sessionStore, defaultConfig, blockstack = blockstack)
    }

    override fun onResume() {
        super.onResume()
        checkLogin()
    }

    private fun checkLogin() {
        if (blockstackSession().isUserSignedIn()) {
            encryptDecryptString()
            encryptDecryptImage()
            //putFileGetFile()
            //putFileGetFileImage()
        } else {
            navigateToAccount()
        }
    }

    private fun putFileGetFile() {
        // works
        lifecycleScope.launch(Dispatchers.IO) {
            val it = blockstackSession().putFile("try.txt", "Hello from Blockstack2", PutFileOptions(encrypt = true))
            Log.d(TAG, "result: " + it.value)
            // does not yet work
            lifecycleScope.launch(Dispatchers.IO) {
                val it2 = blockstackSession().getFile("try.txt", GetFileOptions(true))
                Log.d(TAG, "content " + it2.value)
            }
        }
    }

    private fun putFileGetFileImage() {
        val drawable: BitmapDrawable = ContextCompat.getDrawable(this, R.drawable.default_avatar) as BitmapDrawable

        val bitmap = drawable.bitmap
        val stream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bitMapData = stream.toByteArray()

        // works
        lifecycleScope.launch(Dispatchers.IO) {
            val it = blockstackSession().putFile("try.txt", bitMapData, PutFileOptions(encrypt = true))
            Log.d(TAG, "result: " + it.value)
            // does not yet work
            lifecycleScope.launch(Dispatchers.IO) {

                val it2 = blockstackSession().getFile("try.txt", GetFileOptions(true))
                val plainContent: ByteArray = it2.value as ByteArray
                val imageByteArray = plainContent
                val receivedBitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)
                runOnUiThread {
                    imageView.setImageBitmap(receivedBitmap)
                }
            }
        }
    }

    private fun navigateToAccount() {
        startActivity(Intent(this, AccountActivity::class.java))
    }

    fun encryptDecryptString() {
        val options = CryptoOptions()
        val cipherResult = blockstackSession().encryptContent("Hello Android", options)
        if (cipherResult.hasValue) {
            val cipher = cipherResult.value!!
            val plainContentResult = blockstackSession().decryptContent(cipher.json.toString(), false, options)
            if (plainContentResult.hasValue) {
                val plainContent: String = plainContentResult.value as String
                runOnUiThread {
                    textView.text = plainContent
                }
            } else {
                Toast.makeText(this, "error: ${plainContentResult.error}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "error: ${cipherResult.error}", Toast.LENGTH_SHORT).show()
        }
    }

    fun encryptDecryptImage() {

        val drawable: BitmapDrawable = ContextCompat.getDrawable(this, R.drawable.default_avatar) as BitmapDrawable

        val bitmap = drawable.bitmap
        val stream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bitMapData = stream.toByteArray()

        val options = CryptoOptions()

        val cipherResult = blockstackSession().encryptContent(bitMapData, options)

        if (cipherResult.hasValue) {
            val cipher = cipherResult.value!!
            val plainContentResult = blockstackSession().decryptContent(cipher.json.toString(), true, options)
            if (plainContentResult.hasValue) {
                val plainContent: ByteArray = plainContentResult.value as ByteArray
                val imageByteArray = plainContent
                val receivedBitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)
                runOnUiThread {
                    imageView.setImageBitmap(receivedBitmap)
                }
            } else {
                Toast.makeText(this, "error: ${plainContentResult.error}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "error: ${cipherResult.error} ", Toast.LENGTH_SHORT).show()
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