package org.blockstack.android.sdk

import android.content.Context
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.net.URL
import java.util.*

private val AUTH_URL_STRING = "file:///android_res/raw/webview.html"
private val HOSTED_BROWSER_URL_BASE = "https://browser.blockstack.org"

/**
 * Main object to interact with blockstack in an activity
 *
 * The current implementation is a wrapper for blockstack.js using a WebView.
 * This means that methods must be called on the UI thread e.g. using
 * `runOnUIThread`
 *
 * @param context the activity interacting with blockstack
 * @property config the configuration for blockstack
 * @param onLoadedCallback the callback for when this object is ready to use
 */
class BlockstackSession(context: Context,
                        private val config: BlockstackConfig,
                        onLoadedCallback: () -> Unit = {}) {

    private val TAG = BlockstackSession::class.qualifiedName

    /**
     * Flag indicating whether this object is ready to use
     */
    var loaded: Boolean = false
        private set(value) {
            field = value
        }

    private var userData: JSONObject? = null
    private var signInCallback: ((UserData) -> Unit)? = null
    private val lookupProfileCallbacks = HashMap<String, ((Profile) -> Unit)>()
    private val getFileCallbacks = HashMap<String, ((Any) -> Unit)>()
    private val putFileCallbacks = HashMap<String, ((String) -> Unit)>()



    init {
        Log.d(TAG, context.toString())
    }

    private val webView = WebView(context)

    init {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = BlockstackWebViewClient(context) {
            this.loaded = true
            onLoadedCallback()
        }
        webView.addJavascriptInterface(JavascriptInterfaceObject(this), "android")
        webView.loadUrl(AUTH_URL_STRING)
    }

    /**
     * Process a pending sign in. This method should be called by your app when it
     * receives a request to the app's custom protocol handler.
     *
     * @property authResponse authentication response token
     */
    fun handlePendingSignIn(authResponse: String, signInCallback: (UserData) -> Unit) {
        this.signInCallback = signInCallback
        Log.d(TAG, "handlePendingSignIn")

        ensureLoaded()

        val javascript = "handlePendingSignIn('${authResponse}')"
        webView.evaluateJavascript(javascript, { _: String ->

        })
    }

    /**
     * Generates an authentication request opens an activity that allows the user to
     * sign with an existing Blockstack ID already on the device or create a new one.
     *
     * @property signInCallback a function that is called with `UserData`
     * when authentication succeeds. It is not called on the UI thread so you should
     * execute any UI interactions in a `runOnUIThread` block
     */
    fun redirectUserToSignIn(signInCallback: (UserData) -> Unit) {
        this.signInCallback = signInCallback
        Log.d(TAG, "redirectUserToSignIn")

        ensureLoaded()

        val scopesString = Scope.scopesArrayToJSONString(config.scopes)
        val javascript = "redirectToSignIn('${config.appDomain}', '${config.redirectURI}', '${config.manifestURI}', ${scopesString})"
        webView.evaluateJavascript(javascript, { _: String ->
            // no op
        })
    }

    /**
     * Retrieve data of signed in user
     *
     * @property callback a function that is called with `UserData` of the signed in user
     */
    fun loadUserData(callback: (UserData?) -> Unit) {
        val javascript = "loadUserData()"
        Log.d(TAG, javascript)

        ensureLoaded()

        webView.evaluateJavascript(javascript, { result ->
            if (result != null && !"null".equals(result)) {
                val newUserData = JSONObject(result)
                userData = newUserData
                callback(UserData(newUserData))
            } else {
                callback(null)
            }
        })
    }

    /**
     * Check if a user is currently signed in
     *
     * @property callback a function that is called with a flag that is `true` if the user is signed in, `false` if not.
     */
    fun isUserSignedIn(callback: (Boolean) -> Unit) {
        val javascript = "isUserSignedIn()"
        Log.d(TAG, javascript)

        ensureLoaded()

        webView.evaluateJavascript(javascript, {
            callback(it == "true")
        })
    }

    /**
     * Sign the user out
     *
     * @property callback a function that is called after the user is signed out.
     */
    fun signUserOut(callback: () -> Unit) {
        val javascript = "signUserOut()"
        Log.d(TAG, javascript)

        ensureLoaded()

        webView.evaluateJavascript(javascript, {
            callback()
        })
    }

    /**
     * Lookup the profile of a user
     *
     * @param username the registered user name, like `dev_android_sdk.id`
     * @param zoneFileLookupURL the url of the zone file lookup service like `https://core.blockstack.org/v1/names`
     * @param callback is called with the profile of the user or null if not found
     */
    fun lookupProfile(username: String, zoneFileLookupURL: URL? = null, callback: (Profile?) -> Unit) {
        val javascript = if (zoneFileLookupURL != null) {
            "lookupProfile('$username', '$zoneFileLookupURL')"
        } else {
            "lookupProfile('$username')"
        }
        ensureLoaded()
        lookupProfileCallbacks.put(username, callback)
        webView.evaluateJavascript(javascript, {
            result ->
            if (result != null && !"null".equals(result)) {
                val newUserData = JSONObject(result)
                callback(Profile(newUserData))
            } else {
                callback(null)
            }
        })
    }

    /* Public storage methods */

    /**
     * Retrieves the specified file from the app's data store.
     *
     * @property path the path of the file from which to read data
     * @property options an instance of a `GetFileOptions` object which is used to configure
     * options such as decryption and reading files from other apps or users.
     * @property callback a function that is called with the file contents. It is not called on the
     * UI thread so you should execute any UI interactions in a `runOnUIThread` block
     */
    fun getFile(path: String, options: GetFileOptions, callback: ((Any) -> Unit)) {
        Log.d(TAG, "getFile: path: ${path} options: ${options}")

        ensureLoaded()

        val uniqueIdentifier = addGetFileCallback(callback)
        val javascript = "getFile('${path}', ${options}, '${uniqueIdentifier}')"
        webView.evaluateJavascript(javascript, { _: String ->
            // no op
        })
    }

    /**
     * Stores the data provided in the app's data store to to the file specified.
     *
     * @property path the path to store the data to
     * @property content the data to store in the file
     * @property options an instance of a `PutFileOptions` object which is used to configure
     * options such as encryption
     * @property callback a function that is called with a `String` representation of a url from
     * which you can read the file that was just put. It is not called on the UI thread so you should
     * execute any UI interactions in a `runOnUIThread` block
     */
    fun putFile(path: String, content: Any, options: PutFileOptions, callback: ((String) -> Unit)) {
        Log.d(TAG, "putFile: path: ${path} options: ${options}")

        ensureLoaded()

        val valid = content is String || content is ByteArray
        if (!valid) {
            throw IllegalArgumentException("putFile content only supports String or ByteArray")
        }

        val isBinary = content is ByteArray

        val uniqueIdentifier = addPutFileCallback(callback)
        if (isBinary) {
            val contentString = Base64.encodeToString(content as ByteArray, Base64.NO_WRAP)
            val javascript = "putFile('${path}', '${contentString}', ${options}, '${uniqueIdentifier}', true)"
            webView.evaluateJavascript(javascript, { _: String ->
                // no op
            })
        } else {
            val javascript = "putFile('${path}', '${content}', ${options}, '${uniqueIdentifier}', false)"
            webView.evaluateJavascript(javascript, { _: String ->
                // no op
            })
        }

    }

    /**
     * Encrypt content
     *
     * @plainContent can be a String or ByteArray
     * @options defines how to encrypt
     * @callback called with the cipher object or null if encryption failed
     */
    fun encryptContent(plainContent: Any, options: CryptoOptions, callback: (CipherObject?) -> Unit) {
        ensureLoaded()

        val valid = plainContent is String || plainContent is ByteArray
        if (!valid) {
            throw IllegalArgumentException("encrypt content only supports String or ByteArray")
        }

        val isBinary = plainContent is ByteArray

        val javascript = if (isBinary) {
            val contentString = Base64.encodeToString(plainContent as ByteArray, Base64.NO_WRAP)
            "encryptContent('$contentString', $options, true)"
        } else {
            "encryptContent('$plainContent', $options, false)"
        }

        webView.evaluateJavascript(javascript) { result ->
            if (result != null && !"null".equals(result)) {
                val cipherObject = JSONObject(result)
                callback(CipherObject(cipherObject))
            } else {
                callback(null)
            }
        }
    }

    /**
     * Decrypt content
     * @cipherObject can be a String or ByteArray representing the cipherObject returned by  @see encryptContent
     * @options defines how to decrypt the cipherObject
     * @callback called with the plain content as String or ByteArray depending on the given options
     */
    fun decryptContent(cipherObject: Any, options: CryptoOptions, callback: (Any) -> Unit) {
        ensureLoaded()

        val valid = cipherObject is String || cipherObject is ByteArray
        if (!valid) {
            throw IllegalArgumentException("decrypt content only supports JSONObject or ByteArray")
        }

        val isBinary = cipherObject is ByteArray

        var wasString:Boolean

        val javascript = if (isBinary) {
            val cipherTextString = Base64.encodeToString(cipherObject as ByteArray, Base64.NO_WRAP)
            wasString = JSONObject(cipherTextString).getBoolean("wasString")
            "decryptContent('$cipherTextString', $options, true)"
        } else {
            wasString = JSONObject(cipherObject as String).getBoolean("wasString")
            "decryptContent('$cipherObject', $options, false)"
        }


        webView.evaluateJavascript(javascript) {plainContent ->

            if (wasString) {
                callback(plainContent)
            } else {
                callback(Base64.decode(plainContent, Base64.DEFAULT))
            }
        }
    }

    private fun addGetFileCallback(callback: (Any) -> Unit): String {
        val uniqueIdentifier = UUID.randomUUID().toString()
        getFileCallbacks[uniqueIdentifier] = callback
        return uniqueIdentifier
    }

    private fun addPutFileCallback(callback: (String) -> Unit): String {
        val uniqueIdentifier = UUID.randomUUID().toString()
        putFileCallbacks[uniqueIdentifier] = callback
        return uniqueIdentifier
    }

    private fun ensureLoaded() {
        if (!this.loaded) {
            throw IllegalStateException("Blockstack session hasn't finished loading." +
                    " Please wait until the onLoadedCallback() has fired before performing operations.")
        }
    }

    private class JavascriptInterfaceObject(private val session: BlockstackSession) {

        @JavascriptInterface
        fun signInSuccess(userDataString: String) {
            Log.d(session.TAG, "signInSuccess")
            val userData = JSONObject(userDataString)
            session.userData = userData
            Log.d(session.TAG, session.userData.toString())
            session.signInCallback?.invoke(UserData(userData))
        }

        @JavascriptInterface
        fun lookupProfileResult(username:String, userDataString:String) {
            val userData = JSONObject(userDataString)
            session.lookupProfileCallbacks[username]?.invoke(Profile(userData))
        }

        @JavascriptInterface
        fun getFileResult(content: String, uniqueIdentifier: String, isBinary: Boolean) {
            Log.d(session.TAG, "putFileResult")

            if (isBinary) {
                val binaryContent: ByteArray = Base64.decode(content, Base64.DEFAULT)
                session.getFileCallbacks[uniqueIdentifier]?.invoke(binaryContent)
            } else {
                session.getFileCallbacks[uniqueIdentifier]?.invoke(content)
            }
            session.getFileCallbacks.remove(uniqueIdentifier)
        }

        @JavascriptInterface
        fun putFileResult(readURL: String, uniqueIdentifier: String) {
            Log.d(session.TAG, "putFileResult")

            session.putFileCallbacks[uniqueIdentifier]?.invoke(readURL)
            session.putFileCallbacks.remove(uniqueIdentifier)
        }

    }

}

private class BlockstackWebViewClient(val context: Context,
                                      val onLoadedCallback: () -> Unit) : WebViewClient() {
    private val TAG = BlockstackWebViewClient::class.qualifiedName

    override fun onPageFinished(view: WebView?, url: String?) {
        Log.d(TAG, "url loaded:" + url)
        if (AUTH_URL_STRING == url) {
            onLoadedCallback()
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        // initially overriding a function that's deprecated in API 27 in order to support API 15
        Log.d(TAG, "Navigation detected in sign in webview")
        Log.d(TAG, url)

        val authRequestToken = url.split(':')[1]
        Log.d(TAG, authRequestToken)

        val customTabsIntent = CustomTabsIntent.Builder().build()
        // on redirect load the following with
        // TODO: handle lack of custom tabs support
        customTabsIntent.launchUrl(context,
                Uri.parse("${HOSTED_BROWSER_URL_BASE}/auth?authRequest=${authRequestToken}"))
        return true
    }
}
