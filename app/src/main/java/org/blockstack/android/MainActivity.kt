package org.blockstack.android

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button


import kotlinx.android.synthetic.main.activity_main.*
import org.blockstack.android.sdk.BlockstackSession
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.qualifiedName
    private var _blockstackSession: BlockstackSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        blockstackSession() // initialize blockstack session

        val btn: Button = findViewById<Button>(R.id.button) as Button
        btn.setOnClickListener { view: View ->
            blockstackSession().redirectUserToSignIn({ userData: JSONObject ->
                Log.d(TAG, "signed in!")
                runOnUiThread {
                    btn.visibility = View.GONE
                }

            })
        }

        val putFileButton: Button = findViewById<Button>(R.id.putFileButton) as Button
        putFileButton.setOnClickListener { view: View ->
            blockstackSession().putFile("/test.json", "hello", {readURL: String ->
                Log.d(TAG, "File stored at: ${readURL}")
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun blockstackSession() : BlockstackSession {
        val localSession = _blockstackSession
        if(localSession != null) {
            return localSession
        } else {
            val session = BlockstackSession(this)
            _blockstackSession = session
            return session
        }
    }
}
