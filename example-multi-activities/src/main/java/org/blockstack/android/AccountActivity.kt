package org.blockstack.android


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.android.synthetic.main.content_account.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.BlockstackSignIn
import org.blockstack.android.sdk.ui.SignInProvider
import org.blockstack.android.sdk.ui.showBlockstackConnect


class AccountActivity : AppCompatActivity(), SignInProvider {
    private val TAG = AccountActivity::class.java.simpleName

    private lateinit var blockstackSignIn: BlockstackSignIn
    private lateinit var blockstackSession: BlockstackSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        signInButton.isEnabled = false
        signOutButton.isEnabled = false

        val sessionStore = SessionStoreProvider.getInstance(this)
        blockstackSignIn = BlockstackSignIn(sessionStore, defaultConfig, defaultAppDetails)
        blockstackSession = BlockstackSession(sessionStore, defaultConfig)

        if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }
        onLoaded()
        signInButton.setOnClickListener {
            showBlockstackConnect()
        }

        signOutButton.setOnClickListener { _ ->
            blockstackSession.signUserOut()
            Log.d(TAG, "signed out!")
            finish()
        }
    }

    private fun onLoaded() {
        signInButton.isEnabled = true
        signOutButton.isEnabled = true
        val signedIn = blockstackSession.isUserSignedIn()
        if (signedIn) {
            signInButton.visibility = View.GONE
            signOutButton.visibility = View.VISIBLE
        } else {
            signInButton.visibility = View.VISIBLE
            signOutButton.visibility = View.GONE
        }

    }

    private fun onSignIn() {
        blockstackSession.loadUserData()
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
                lifecycleScope.launch(Dispatchers.IO) {
                    val it = blockstackSession.handlePendingSignIn(authResponse)
                    if (it.hasErrors) {
                        runOnUiThread {
                            Toast.makeText(this@AccountActivity, "error: ${it.error}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.d(TAG, "signed in!")
                        runOnUiThread {
                            onSignIn()
                        }
                    }
                }
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

    override fun provideBlockstackSignIn(): BlockstackSignIn {
        return blockstackSignIn
    }
}


