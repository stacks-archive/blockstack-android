package org.blockstack.android


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.GetFileOptions
import org.blockstack.android.sdk.PutFileOptions
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URI

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName
    private val textFileName = "message.txt"
    private val imageFileName = "team.jpg"

    private var _blockstackSession: BlockstackSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        signInButton.isEnabled = false
        getStringFileButton.isEnabled = false
        putStringFileButton.isEnabled = false

        val appDomain = URI("https://flamboyant-darwin-d11c17.netlify.com")
        val redirectURI = URI("${appDomain}/redirect")
        val manifestURI = URI("${appDomain}/manifest.json")
        val scopes = arrayOf("store_write")

        _blockstackSession = BlockstackSession(this, appDomain, redirectURI, manifestURI, scopes,
                onLoadedCallback = {signInButton.isEnabled = true})

        getStringFileButton.isEnabled = false
        putStringFileButton.isEnabled = false
        getImageFileButton.isEnabled = false
        putImageFileButton.isEnabled = false

        val imageView: ImageView = findViewById<ImageView>(R.id.imageView) as ImageView

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
            blockstackSession().getFile(textFileName, options, {content: Any ->
                Log.d(TAG, "File contents: ${content as String}")
                runOnUiThread {
                    fileContentsTextView.text = content as String
                }
            })
        }

        putStringFileButton.setOnClickListener { view: View ->
            readURLTextView.text = "Uploading..."
            val options = PutFileOptions()
            blockstackSession().putFile(textFileName, "Hello Android!", options,
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
            blockstackSession().putFile(imageFileName, bitMapData, options,
                    {readURL: String ->
                        Log.d(TAG, "File stored at: ${readURL}")
                        runOnUiThread {
                            imageFileTextView.text = "File stored at: ${readURL}"
                        }
                    })
        }

        getImageFileButton.setOnClickListener { view: View ->
            val options = GetFileOptions(decrypt = false)
            blockstackSession().getFile(imageFileName, options, {contents: Any ->

                val imageByteArray = contents as ByteArray
                val bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)
                runOnUiThread {
                    imageView.setImageBitmap(bitmap)
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
