package org.blockstack.android


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.GetFileOptions
import org.blockstack.android.sdk.PutFileOptions
import java.io.ByteArrayOutputStream
import java.net.URI
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.blockstack.android.sdk.UserData
import org.jetbrains.anko.coroutines.experimental.bg
import java.net.URL


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

        signInButton.setOnClickListener { view: View ->
            blockstackSession().redirectUserToSignIn { userData ->
                Log.d(TAG, "signed in!")
                runOnUiThread {
                    onSignIn(userData)
                }
            }
        }

        getStringFileButton.setOnClickListener { _ ->
            fileContentsTextView.text = "Downloading..."

            val options = GetFileOptions()
            blockstackSession().getFile(textFileName, options, {content: Any ->
                Log.d(TAG, "File contents: ${content as String}")
                runOnUiThread {
                    fileContentsTextView.text = content as String
                }
            })
        }

        putStringFileButton.setOnClickListener { _ ->
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

        putImageFileButton.setOnClickListener { _ ->
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

        getImageFileButton.setOnClickListener { _ ->
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

    private fun onSignIn(userData: UserData) {
        userDataTextView.text = "Signed in as ${userData.getDid()}"
        showUserAvatar(userData.getAvatarImage())
        signInButton.isEnabled = false

        getStringFileButton.isEnabled = true
        putStringFileButton.isEnabled = true
        putImageFileButton.isEnabled = true
        getImageFileButton.isEnabled = true
    }

    private fun showUserAvatar(avatarImage: String?) {
        if (avatarImage != null) {
            // use whatever suits your app architecture best to asynchronously load the avatar
            // better use a image loading library than the code below
            async(UI) {
                val avatar = bg {
                    try {
                        BitmapDrawable.createFromStream(URL(avatarImage).openStream(), "src")
                    } catch (e: Exception) {
                        Log.d(TAG, e.toString())
                        null
                    }
                }.await()
                avatarView.setImageDrawable(avatar)
            }
        } else {
            avatarView.setImageResource(R.drawable.default_avatar)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")

        if (intent?.action == Intent.ACTION_MAIN) {
            blockstackSession().loadUserData {
                userData -> runOnUiThread {onSignIn(userData)}
            }
        } else if (intent?.action == Intent.ACTION_VIEW) {
            val response = intent?.dataString
            Log.d(TAG, "response ${response}")
            if (response != null) {
                val authResponseTokens = response.split(':')

                if (authResponseTokens.size > 1) {
                    val authResponse = authResponseTokens[1]
                    Log.d(TAG, "authResponse: ${authResponse}")
                    blockstackSession().handlePendingSignIn(authResponse)
                }
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
