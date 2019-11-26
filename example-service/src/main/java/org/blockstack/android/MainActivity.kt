package org.blockstack.android


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.BlockstackSignIn
import org.blockstack.android.sdk.SessionStore


class MainActivity : AppCompatActivity() {
    private lateinit var blockstackSignIn: BlockstackSignIn
    private val TAG = MainActivity::class.java.simpleName

    private var _blockstackSession: BlockstackSession? = null

    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        signInButton.isEnabled = false
        startServiceButton.isEnabled = false
        signOutButton.isEnabled = false

        val sessionStore = SessionStore(PreferenceManager.getDefaultSharedPreferences(this))
        blockstackSignIn = BlockstackSignIn(sessionStore, defaultConfig)
        _blockstackSession = BlockstackSession(sessionStore, defaultConfig)

        val signedIn = _blockstackSession?.isUserSignedIn()
        if (signedIn!!) {
            val userData = _blockstackSession?.loadUserData()
            userData?.let { onSignIn() }
        } else {
            signInButton.isEnabled = true
            startServiceButton.isEnabled = false
            signOutButton.isEnabled = false
        }


        signInButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                blockstackSignIn.redirectUserToSignIn(this@MainActivity)
            }
        }

        signOutButton.setOnClickListener {
            signOutButton.isEnabled = false
            blockstackSession().signUserOut()
            signInButton.isEnabled = true
            startServiceButton.isEnabled = false

        }

        startServiceButton.setOnClickListener {
            startServiceButton.isEnabled = false
            startService(Intent(this, BlockstackService::class.java))
        }

        if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                onUploadDone()
            }

        }
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, IntentFilter(BlockstackService.ACTION_DONE))
    }

    private fun onUploadDone() {
        startServiceButton.isEnabled = true
    }

    private fun onSignIn() {
        signInButton.isEnabled = false
        startServiceButton.isEnabled = true
        signOutButton.isEnabled = true
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")

        if (intent?.action == Intent.ACTION_MAIN) {
            runOnUiThread {
                onSignIn()
            }
        } else if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }
    }

    private fun handleAuthResponse(intent: Intent) {
        val response = intent.data?.query
        Log.d(TAG, "response $response")
        if (response != null) {
            val authResponseTokens = response.split('=')

            if (authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]
                Log.d(TAG, "authResponse: $authResponse")
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = blockstackSession().handlePendingSignIn(authResponse)
                    Log.d(TAG, "signed in?" + result.hasValue)
                    if (result.hasValue) {
                        runOnUiThread {
                            onSignIn()

                        }
                    }
                }
            }
        }
    }

    private fun blockstackSession(): BlockstackSession {
        val session = _blockstackSession
        if (session != null) {
            return session
        } else {
            throw IllegalStateException("No session.")
        }
    }
}
