package org.blockstack.android

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView


import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.blockstack.android.sdk.BlockstackSession
import org.json.JSONObject
import java.net.URI

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName

    private var _blockstackSession: BlockstackSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        signInButton.isEnabled = false
        getFileButton.isEnabled = false
        putFileButton.isEnabled = false

        val appDomain = URI("https://flamboyant-darwin-d11c17.netlify.com")
        val redirectURI = URI("${appDomain}/redirect")
        val manifestURI = URI("${appDomain}/manifest.json")
        val scopes = arrayOf("store_write")

        _blockstackSession = BlockstackSession(this, appDomain, redirectURI, manifestURI, scopes,
                onLoadedCallback = {signInButton.isEnabled = true})

        signInButton.setOnClickListener { view: View ->
            blockstackSession().redirectUserToSignIn({ userData: JSONObject ->
                Log.d(TAG, "signed in!")
                runOnUiThread {
                    userDataTextView.text = "Signed in as ${userData.get("did")}"
                    signInButton.isEnabled = false
                    getFileButton.isEnabled = true
                    putFileButton.isEnabled = true
                }

            })
        }

        getFileButton.setOnClickListener { view: View ->
            fileContentsTextView.text = "Downloading..."
            blockstackSession().getFile("message.txt", {contents: String ->
                Log.d(TAG, "File contents: ${contents}")
                runOnUiThread {
                    fileContentsTextView.text = contents
                }
            })
        }

        putFileButton.setOnClickListener { view: View ->
            readURLTextView.text = "Uploading..."
            blockstackSession().putFile("message.txt", "Hello Android!", {readURL: String ->
                Log.d(TAG, "File stored at: ${readURL}")
                runOnUiThread {
                    readURLTextView.text = "File stored at: ${readURL}"
                }
            })
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")
        val response = intent?.dataString
        Log.d(TAG, response)
        if(response != null) {
            val authResponseTokens = response.split(':')

            if(authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]
                Log.d(TAG, "authResponse: ${authResponse}")
                blockstackSession().handlePendingSignIn(authResponse)
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
