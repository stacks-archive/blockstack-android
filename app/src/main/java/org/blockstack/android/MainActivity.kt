package org.blockstack.android

import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView


import kotlinx.android.synthetic.main.activity_main.*
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.GetFileOptions
import org.blockstack.android.sdk.PutFileOptions
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URI

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.qualifiedName

    private var _blockstackSession: BlockstackSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val appDomain = URI("https://flamboyant-darwin-d11c17.netlify.com")
        val redirectURI = URI("${appDomain}/redirect")
        val manifestURI = URI("${appDomain}/manifest.json")
        val scopes = arrayOf("store_write")
        _blockstackSession = BlockstackSession(this, appDomain, redirectURI, manifestURI, scopes)
        
        val signInButton: Button = findViewById<Button>(R.id.button) as Button

        val getStringFileButton: Button = findViewById<Button>(R.id.getStringFileButton) as Button
        getStringFileButton.isEnabled = false
        val putStringFileButton: Button = findViewById<Button>(R.id.putStringFileButton) as Button
        putStringFileButton.isEnabled = false

        val getImageFileButton: Button = findViewById<Button>(R.id.getImageFileButton) as Button
        getImageFileButton.isEnabled = false
        val putImageFileButton: Button = findViewById<Button>(R.id.putImageFileButton) as Button
        putImageFileButton.isEnabled = false

        val userDataTextView: TextView = findViewById<TextView>(R.id.userDataTextView) as TextView
        val readURLTextView: TextView = findViewById<TextView>(R.id.readURLTextView) as TextView
        val fileContentsTextView: TextView = findViewById<TextView>(R.id.fileContentsTextView) as TextView
        val imageFileTextView: TextView = findViewById<TextView>(R.id.imageFileTextView) as TextView

        signInButton.setOnClickListener { view: View ->
            blockstackSession().redirectUserToSignIn({ userData: JSONObject ->
                Log.d(TAG, "signed in!")
                runOnUiThread {
                    userDataTextView.text = "Signed in as ${userData.get("did")}"
                    signInButton.isEnabled = false
                    getStringFileButton.isEnabled = true
                    putStringFileButton.isEnabled = true
                    putImageFileButton.isEnabled = true
                    getImageFileButton.isEnabled = true
                }

            })
        }

        getStringFileButton.setOnClickListener { view: View ->
            fileContentsTextView.text = "Downloading..."

            val options = GetFileOptions()
            blockstackSession().getFile("message.txt", options, {contents: String ->
                Log.d(TAG, "File contents: ${contents}")
                runOnUiThread {
                    fileContentsTextView.text = contents
                }
            })
        }

        putStringFileButton.setOnClickListener { view: View ->
            readURLTextView.text = "Uploading..."
            val options = PutFileOptions()
            blockstackSession().putFile("message.txt", "Hello Android!", options,
                    {readURL: String ->
                Log.d(TAG, "File stored at: ${readURL}")
                        runOnUiThread {
                            readURLTextView.text = "File stored at: ${readURL}"
                        }
            })
        }

        putImageFileButton.setOnClickListener { view: View ->
            imageFileTextView.text = "Uploading..."

            val drawable: BitmapDrawable = resources.getDrawable(R.drawable.blockstackteam) as BitmapDrawable

            val bitmap = drawable.getBitmap()
            val stream = ByteArrayOutputStream()

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val bitMapData = stream.toByteArray()

            val options = PutFileOptions(false)
            blockstackSession().putFile("team2.jpg", bitMapData, options,
                    {readURL: String ->
                        Log.d(TAG, "File stored at: ${readURL}")
                        runOnUiThread {
                            imageFileTextView.text = "File stored at: ${readURL}"
                        }
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
        val session = _blockstackSession
        if(session != null) {
            return session
        } else {
            throw IllegalStateException("No session.")
        }
    }

}
