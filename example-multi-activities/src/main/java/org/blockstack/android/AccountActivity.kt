package org.blockstack.android


import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.android.synthetic.main.content_account.*
import org.blockstack.android.sdk.BlockstackSession
import java.net.URI


class AccountActivity : AppCompatActivity() {
    private val TAG = AccountActivity::class.java.simpleName

    private var _blockstackSession: BlockstackSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)
        setSupportActionBar(toolbar)

        signInButton.isEnabled = false
        signOutButton.isEnabled = false

        val appDomain = URI("https://flamboyant-darwin-d11c17.netlify.com")
        val redirectURI = URI("${appDomain}/redirect")
        val manifestURI = URI("${appDomain}/manifest.json")
        val scopes = arrayOf("store_write")

        _blockstackSession = BlockstackSession(this, appDomain, redirectURI, manifestURI, scopes,
                onLoadedCallback = {
                    if (intent?.action == Intent.ACTION_VIEW) {
                        handleAuthResponse(intent)
                    }
                    onLoaded()})

        signInButton.setOnClickListener { view: View ->
            blockstackSession().redirectUserToSignIn { userData ->
                Log.d(TAG, "signed in!")
                runOnUiThread {
                    onSignIn()
                }
            }
        }

        signOutButton.setOnClickListener { _ ->
            blockstackSession().signUserOut {
                Log.d(TAG, "signed out!")
                finish()
            }
        }
    }

    private fun onLoaded() {
        signInButton.isEnabled = true
        signOutButton.isEnabled = true
        blockstackSession().isUserSignedIn { signedIn ->
            if (signedIn) {
                signInButton.visibility = View.GONE
                signOutButton.visibility = View.VISIBLE
            } else {
                signInButton.visibility = View.VISIBLE
                signOutButton.visibility = View.GONE
            }
        }
    }

    private fun onSignIn() {
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
        val response = intent?.dataString
        Log.d(TAG, "response ${response}")
        if (response != null) {
            val authResponseTokens = response.split(':')

            if (authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]
                Log.d(TAG, "authResponse: ${authResponse}")
                blockstackSession().handlePendingSignIn(authResponse, {
                    Log.d(TAG, "signed in!")
                    runOnUiThread {
                        onSignIn()
                    }
                })
            }
        }
    }

    fun blockstackSession() : BlockstackSession {
        val session = _blockstackSession
        if(session != null) {
            return session
        } else {
            throw IllegalStateException("No session.")
        }
    }

}
