package org.blockstack.android


import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.blockstack.android.sdk.*
import org.blockstack.android.sdk.model.*
import org.blockstack.android.sdk.ecies.signECDSA
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.net.URL
import java.util.*

private const val username = "dev_android_sdk.id.blockstack"
private const val FILE_PREFIX = "file://"

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {
    private lateinit var network: Network
    private lateinit var blockstack: Blockstack
    private lateinit var blockstackSignIn: BlockstackSignIn

    private val TAG = MainActivity::class.java.simpleName

    private val textFileName = "message.txt"
    private val imageFileName = "team.jpg"

    private var _blockstackSession: BlockstackSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val config = BlockstackConfig(
                URI("https://flamboyant-darwin-d11c17.netlify.app"),
                "/redirect",
                "/manifest.json",
                arrayOf(BaseScope.StoreWrite.scope))

        val appDetails = AppDetails(getString(R.string.app_name), "https://helloblockstack.com/icon-192x192.png")

        val sessionStore = SessionStore(getBlockstackSharedPreferences())
        blockstack = Blockstack()
        _blockstackSession = BlockstackSession(sessionStore, config, blockstack = blockstack)
        blockstackSignIn = BlockstackSignIn(sessionStore, config, appDetails)
        network = Network("https://core.blockstack.org")
        signInButton.isEnabled = true
        getUserAppFileUrlButton.isEnabled = true

        validateProofsButton.isEnabled = false
        getStringFileButton.isEnabled = false
        putStringFileButton.isEnabled = false
        deleteStringFileButton.isEnabled = false
        getImageFileButton.isEnabled = false
        putImageFileButton.isEnabled = false
        deleteImageFileButton.isEnabled = false
        getStringFileFromUserButton.isEnabled = false
        getAppBucketUrlButton.isEnabled = false
        listFilesButton.isEnabled = false
        putLocalFileButton.isEnabled = false
        getLocalFileButton.isEnabled = false

        BlockstackConnect
                .config(config, sessionStore, appDetails)

        signInButton.setOnClickListener {
            BlockstackConnect.connect(this@MainActivity)
        }

        signInButtonWithGaia.setOnClickListener {
            val key = blockstackSignIn.generateAndStoreTransitKey()
            lifecycleScope.launch {
                val authRequest = blockstackSignIn.makeAuthRequest(key, Date(System.currentTimeMillis() + 3600000).time, extraParams = mapOf(Pair("solicitGaiaHubUrl", true)))
                blockstackSignIn.redirectToSignInWithAuthRequest(this@MainActivity, authRequest)
            }
        }

        /*
        validateProofsButton.setOnClickListener { _ ->
            validateProofsText.text = "Validating..."
            val it = blockstackSession().loadUserData()
            val ownerAddress = it.decentralizedID.split(":")[2]
            val result = blockstackSession().validateProofs(it.profile!!, ownerAddress, it.json.optString("username"))
            validateProofsText.text = "${result.value?.size} proofs found."
        }
         */

        getStringFileButton.setOnClickListener {
            fileContentsTextView.text = "Downloading..."
            val options = GetFileOptions(decrypt = false)
            lifecycleScope.launch {
                val contentResult = blockstackSession().getFile(textFileName, options)
                if (contentResult.hasValue) {
                    val content = contentResult.value!!
                    Log.d(TAG, "File contents: $content")
                    fileContentsTextView.text = content as String

                } else {
                    Toast.makeText(this@MainActivity, "error: ${contentResult.error}", Toast.LENGTH_SHORT).show()
                }

            }
        }

        deleteStringFileButton.setOnClickListener {
            deleteFileMessageTextView.text = "Deleting..."
            lifecycleScope.launch {
                val deleteResult = blockstackSession().deleteFile(textFileName, DeleteFileOptions())
                if (deleteResult.hasErrors) {
                    Toast.makeText(this@MainActivity, "error " + deleteResult.error, Toast.LENGTH_SHORT).show()
                } else {
                    deleteFileMessageTextView.text = "File $textFileName deleted."
                }
            }
        }

        putStringFileButton.setOnClickListener {
            readURLTextView.text = "Uploading..."
            val options = PutFileOptions(encrypt = false)
            lifecycleScope.launch {

                val readURLResult = blockstackSession().putFile(textFileName, "Hello Android!", options)
                if (readURLResult.hasValue) {
                    val readURL = readURLResult.value!!
                    Log.d(TAG, "File stored at: $readURL")
                    readURLTextView.text = "File stored at: $readURL"

                } else {
                    Toast.makeText(this@MainActivity, "error: ${readURLResult.error}", Toast.LENGTH_SHORT).show()
                }

            }
        }

        putImageFileButton.setOnClickListener {
            imageFileTextView.text = "Uploading..."

            val drawable: BitmapDrawable = ContextCompat.getDrawable(this, R.drawable.blockstackteam) as BitmapDrawable

            val bitmap = drawable.bitmap
            val stream = ByteArrayOutputStream()

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val bitMapData = stream.toByteArray()

            val options = PutFileOptions(true)
            lifecycleScope.launch {
                val readURLResult = blockstackSession().putFile(imageFileName, bitMapData, options)
                if (readURLResult.hasValue) {
                    val readURL = readURLResult.value!!
                    Log.d(TAG, "File stored at: $readURL")
                    imageFileTextView.text = "File stored at: $readURL"

                } else {
                    Toast.makeText(this@MainActivity, "error: ${readURLResult.error}", Toast.LENGTH_SHORT).show()
                }

            }
        }

        getImageFileButton.setOnClickListener {
            val options = GetFileOptions(decrypt = true)
            lifecycleScope.launch {
                val contentsResult = blockstackSession().getFile(imageFileName, options)
                if (contentsResult.hasValue) {
                    val contents = contentsResult.value!!
                    val imageByteArray = contents as ByteArray
                    val bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)
                    imageView.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(this@MainActivity, "error: ${contentsResult.error}", Toast.LENGTH_SHORT).show()
                }

            }
        }

        deleteImageFileButton.setOnClickListener {
            Log.d(TAG, "Deleting...")
            lifecycleScope.launch {
                val deleteResult = blockstackSession().deleteFile(imageFileName, DeleteFileOptions())
                if (deleteResult.hasErrors) {
                    Toast.makeText(this@MainActivity, "error " + deleteResult.error, Toast.LENGTH_SHORT).show()
                } else {
                    imageView.setImageBitmap(null)
                    Log.d(TAG, "File $imageFileName deleted.")
                }
            }
        }

        getStringFileFromUserButton.setOnClickListener {

            val zoneFileLookupUrl = URL("https://core.blockstack.org/v1/names")
            fileFromUserContentsTextView.text = "Downloading file from other user..."
            lifecycleScope.launch {
                val profile = blockstack.lookupProfile(username, zoneFileLookupURL = zoneFileLookupUrl)

                val options = GetFileOptions(username = username,
                        zoneFileLookupURL = zoneFileLookupUrl,
                        app = "https://flamboyant-darwin-d11c17.netlify.app",
                        decrypt = false)
                val contentResult = blockstackSession().getFile(textFileName, options)
                if (contentResult.hasValue) {
                    val content = contentResult.value!!
                    lifecycleScope.launch(Dispatchers.Main) {
                        fileFromUserContentsTextView.text = "from ${profile.name}($username):\n ${content as String}"
                    }
                } else {
                    val errorMsg = "error: ${contentResult.error}"
                    lifecycleScope.launch(Dispatchers.Main) {
                        fileFromUserContentsTextView.text = errorMsg
                        Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        getAppBucketUrlButton.setOnClickListener {
            getAppBucketUrlText.text = "Getting url ..."
            val userData = blockstackSession().loadUserData()
            lifecycleScope.launch {
                getAppBucketUrlText.text =
                        blockstack.getAppBucketUrl(userData.hubUrl, userData.appPrivateKey)
                                ?: "error"
            }
        }


        getUserAppFileUrlButton.setOnClickListener { _ ->
            getUserAppFileUrlText.text = "Getting url ..."
            val zoneFileLookupUrl = "https://core.blockstack.org/v1/names"
            lifecycleScope.launch {
                val it = blockstack.getUserAppFileUrl(textFileName, username, "https://flamboyant-darwin-d11c17.netlify.app", zoneFileLookupUrl)
                withContext(Dispatchers.Main) {
                    getUserAppFileUrlText.text = it
                }
            }

        }

        listFilesButton.setOnClickListener {
            listFilesText.text = "...."
            lifecycleScope.launch {
                val countResult = blockstackSession().listFiles { urlResult ->
                    if (urlResult.hasValue) {
                        if (listFilesText.text === "....") {
                            listFilesText.text = urlResult.value
                        } else {
                            listFilesText.text = listFilesText.text.toString() + "\n" + urlResult.value
                        }
                    }
                    true
                }

                Log.d(TAG, "files count " + if (countResult.hasValue) {
                    countResult.value
                } else {
                    countResult.error
                })


            }
        }

        putLocalFileButton.setOnClickListener {
            val drawable: BitmapDrawable = ContextCompat.getDrawable(this, R.drawable.blockstackteam) as BitmapDrawable

            val bitmap = drawable.bitmap
            val stream = ByteArrayOutputStream()

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val bitMapData = stream.toByteArray()

            val fpath = this.filesDir.absolutePath + "/" + imageFileName
            Log.d(TAG, "Local file path: $fpath")
            val pathUri = Uri.parse(fpath)
            val file = File(pathUri.path!!)
            file.writeBytes(bitMapData)
            Log.d(TAG, "Saved the local file")

            val options = PutFileOptions(true, dir = this.filesDir.absolutePath)
            lifecycleScope.launch {
                val readURLResult = blockstackSession().putFile(FILE_PREFIX + imageFileName, "", options)
                if (readURLResult.hasValue) {
                    val readURL = readURLResult.value!!
                    Log.d(TAG, "File stored at: $readURL")
                } else {
                    Toast.makeText(this@MainActivity, "error: ${readURLResult.error}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        getLocalFileButton.setOnClickListener {
            val fpath = this.filesDir.absolutePath + "/" + imageFileName
            val pathUri = Uri.parse(fpath)
            val file = File(pathUri.path!!)
            if (file.exists()) file.deleteRecursively()
            Log.d(TAG, "Is local file exist: ${file.exists()}")

            val options = GetFileOptions(decrypt = true, dir = this.filesDir.absolutePath)
            lifecycleScope.launch {
                val contentsResult = blockstackSession().getFile(FILE_PREFIX + imageFileName, options)
                if (contentsResult.hasValue) {
                    val contents = contentsResult.value!!
                    Log.d(TAG, "Saved local file with contents: ${contents}")

                    val imageByteArray = file.inputStream().use { it.readBytes() }
                    val bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)
                    imageView.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(this@MainActivity, "error: ${contentsResult.error}", Toast.LENGTH_SHORT).show()
                }

            }
        }

        getNameInfoButton.setOnClickListener { _ ->
            getNameInfoText.text = "Getting info ..."
            lifecycleScope.launch {
                val it = network.getNameInfo(username)

                Log.d(TAG, it.value?.json.toString())
                getNameInfoText.text = if (it.hasValue) {
                    it.value?.json.toString()
                } else {
                    it.error?.message
                }
            }
        }

        signECDSAButton.setOnClickListener {
            val appPrivateKey = ""
            val obj = signECDSA("Privacy Security UX", appPrivateKey)
            Log.d(TAG, "Public key: ${obj.publicKey}, Signature: ${obj.signature}")
            signECDSAText.text = "Public key: ${obj.publicKey}, Signature: ${obj.signature}"
        }

        if (intent?.action == Intent.ACTION_VIEW) {
            lifecycleScope.launch {
                handleAuthResponse(intent)
            }
        }
    }

    private fun onSignIn(userData: UserData) {
        userDataTextView.text = "Signed in as ${userData.decentralizedID}"
        showUserAvatar(userData.profile?.avatarImage)
        signInButton.isEnabled = false

        validateProofsButton.isEnabled = false
        getStringFileButton.isEnabled = true
        putStringFileButton.isEnabled = true
        deleteStringFileButton.isEnabled = true
        putImageFileButton.isEnabled = true
        getImageFileButton.isEnabled = true
        deleteImageFileButton.isEnabled = true
        getStringFileFromUserButton.isEnabled = true
        getAppBucketUrlButton.isEnabled = true
        listFilesButton.isEnabled = true
        putLocalFileButton.isEnabled = true
        getLocalFileButton.isEnabled = true
    }

    private fun showUserAvatar(avatarImage: String?) {
        if (avatarImage != null) {
            // use whatever suits your app architecture best to asynchronously load the avatar
            // better use a image loading library than the code below
            GlobalScope.launch(Dispatchers.Main) {
                val avatar = withContext(Dispatchers.IO) {
                    try {
                        BitmapDrawable.createFromStream(URL(avatarImage).openStream(), "src")
                    } catch (e: Exception) {
                        Log.d(TAG, e.toString())
                        null
                    }
                }
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
            val userData = blockstackSession().loadUserData()
            onSignIn(userData)

        } else if (intent?.action == Intent.ACTION_VIEW) {
            lifecycleScope.launch(Dispatchers.Main) {
                handleAuthResponse(intent)
            }
        }
    }

    private suspend fun handleAuthResponse(intent: Intent) {
        val response = intent.data?.query
        Log.d(TAG, "response $response")
        if (response != null) {
            val authResponseTokens = response.split('=')

            if (authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]
                Log.d(TAG, "authResponse: $authResponse")
                val userDataResult = blockstackSession().handlePendingSignIn(authResponse)
                if (userDataResult.hasValue) {
                    val userData = userDataResult.value!!
                    Log.d(TAG, "signed in!")
                    onSignIn(userData)
                } else {
                    Toast.makeText(this@MainActivity, "error: ${userDataResult.error}", Toast.LENGTH_SHORT).show()
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
