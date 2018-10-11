package org.blockstack.android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.android.synthetic.main.content_cipher.*
import org.blockstack.android.sdk.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream

val TAG = CipherActivity::class.java.simpleName

class CipherActivity : AppCompatActivity() {

    private var _blockstackSession: BlockstackSession2? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cipher)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        Log.d(TAG, "json " + intent.getStringExtra("json"))
        val userData = UserData(JSONObject(intent.getStringExtra("json")))
        _blockstackSession = BlockstackSession2(this, defaultConfig)
    }

    override fun onResume() {
        super.onResume()
        if (_blockstackSession?.loaded == true) {
            checkLogin()
        }
    }

    fun checkLogin() {
        if (blockstackSession().isUserSignedIn()) {
            encryptDecryptString()
        } else {
            navigateToAccount()
        }
    }


    private fun navigateToAccount() {
        startActivity(Intent(this, AccountActivity::class.java))
    }

    fun encryptDecryptString() {
        val options = CryptoOptions()
        val cipherResult = blockstackSession().encryptContent("Hello Android", options)
        Log.d(TAG, "result encryptDecryptString " + cipherResult.toString())
        if (cipherResult.hasValue) {
            val cipher = cipherResult.value!!
            Log.d(TAG, "cipher:" + cipherResult.value.toString())
        } else {
            Toast.makeText(this, "error: " + cipherResult.error, Toast.LENGTH_SHORT).show()
        }
    }

    fun blockstackSession(): BlockstackSession2 {
        val session = _blockstackSession
        if (session != null) {
            return session
        } else {
            throw IllegalStateException("No session.")
        }
    }
}