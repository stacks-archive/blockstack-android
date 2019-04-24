package org.blockstack.android


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.android.synthetic.main.content_account.*
import org.blockstack.android.sdk.BlockstackSession


class AccountActivity : AppCompatActivity() {
    private val TAG = AccountActivity::class.java.simpleName

    private var _blockstackSession: BlockstackSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        signInButton.isEnabled = false
        signOutButton.isEnabled = false

        _blockstackSession = BlockstackSession(this, defaultConfig)
        if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }
        onLoaded()
        signInButton.setOnClickListener { _ ->
            blockstackSession().redirectUserToSignIn { _ ->
                Log.d(TAG, "signed in error!")
            }
        }

        signOutButton.setOnClickListener { _ ->
            blockstackSession().signUserOut()
            Log.d(TAG, "signed out!")
            finish()
        }
    }

    private fun onLoaded() {
        signInButton.isEnabled = true
        signOutButton.isEnabled = true
        val signedIn = blockstackSession().isUserSignedIn()
        if (signedIn) {
            signInButton.visibility = View.GONE
            signOutButton.visibility = View.VISIBLE
        } else {
            signInButton.visibility = View.VISIBLE
            signOutButton.visibility = View.GONE
        }

    }

    private fun onSignIn() {
        blockstackSession().loadUserData()
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")

        if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }

    }

    private fun handleAuthResponse(intent: Intent?) {
        val response = intent?.data?.query
        Log.d(TAG, "response ${response}")
        if (response != null) {
            val authResponseTokens = response.split('=')

            if (authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]
                Log.d(TAG, "authResponse: ${authResponse}")
                blockstackSession().handlePendingSignIn(authResponse, {
                    if (it.hasErrors) {
                        Toast.makeText(this, it.error, Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d(TAG, "signed in!")
                        runOnUiThread {
                            onSignIn()
                        }
                    }
                })
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
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


