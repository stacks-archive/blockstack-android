package org.blockstack.android.sdk

import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.squareup.duktape.Duktape
import org.json.JSONObject
import java.io.File
import java.util.*

private val BLOCKSTACK_JS_URL_STRING = "file:///android_res/raw/blockstack.js"
private val HOSTED_BROWSER_URL_BASE = "https://browser.blockstack.org"

/**
 * Main object to interact with blockstack in an activity
 *
 * The current implementation is a wrapper for blockstack.js using a WebView.
 * This means that methods must be called on the UI thread e.g. using
 * `runOnUIThread`
 *
 * @param config the configuration for blockstack
 * @param onLoadedCallback the callback for when this object is ready to use
 */
class BlockstackSession(context:Context, private val config: BlockstackConfig,
                        /**
                         * url of the name lookup service, defaults to core.blockstack.org/v1/names
                         */
                        val nameLookupUrl: String = "https://core.blockstack.org/v1/names/") {

    private val TAG = BlockstackSession::class.qualifiedName

    /**
     * Flag indicating whether this object is ready to use
     */
    var loaded: Boolean = false
        private set(value) {
            field = value
        }

    private var userData: JSONObject? = null
    private var signInCallback: ((Result<UserData>) -> Unit)? = null
    private val lookupProfileCallbacks = HashMap<String, ((Result<Profile>) -> Unit)>()
    private val getFileCallbacks = HashMap<String, ((Result<Any>) -> Unit)>()
    private val putFileCallbacks = HashMap<String, ((Result<String>) -> Unit)>()


    private val blockstack:V8Object
    private val v8: V8
    init {

        v8 = V8.createV8Runtime();
        v8.executeVoidScript(context.resources.openRawResource(R.raw.blockstack).bufferedReader().use { it.readText() });
        v8.release();
        blockstack = v8.getObject("blockstack")
    }

    internal interface Blockstack {
        fun makeAuthRequest(transitPrivateKey:String, redirectURI:String, manifestURI:String, scopes:Array<String>, appDomain:String):String
    }

    /**
     * Creates an auth response using the given private key. Usually not needed.
     *
     * This method creates an auth response token from the given private key. It
     * is currently used for integration tests.
     *
     * @param privateKey the private key of the user that wants to sign in
     * @param callback called with the auth response as string in json format
     */
    fun makeAuthResponse(privateKey: String): String {
        val params = V8Array(v8).push(privateKey).push("").push( "").pushNull().push("")
        val authResponse = blockstack.executeStringFunction("makeAuthResponse", params);
        return authResponse
    }

    private interface JavaScriptInterface {
        fun signInSuccess(userDataString: String)
        fun signInFailure(error: String)
        fun lookupProfileResult(username: String, userDataString: String)
        fun lookupProfileFailure(username: String, error: String)
        fun getFileResult(content: String, uniqueIdentifier: String, isBinary: Boolean)
        fun getFileFailure(error: String, uniqueIdentifier: String)
        fun putFileResult(readURL: String, uniqueIdentifier: String)
        fun putFileFailure(error: String, uniqueIdentifier: String)
    }

    private class JavascriptInterfaceObject(private val session: BlockstackSession) : JavaScriptInterface {

        @JavascriptInterface
        override fun signInSuccess(userDataString: String) {
            Log.d(session.TAG, "signInSuccess")
            val userData = JSONObject(userDataString)
            session.userData = userData
            Log.d(session.TAG, session.userData.toString())
            session.signInCallback?.invoke(Result(UserData(userData)))
        }

        @JavascriptInterface
        override fun signInFailure(error: String) {
            session.signInCallback?.invoke(Result(null, error))
        }

        @JavascriptInterface
        override fun lookupProfileResult(username: String, userDataString: String) {
            val userData = JSONObject(userDataString)
            session.lookupProfileCallbacks[username]?.invoke(Result(Profile(userData)))
        }

        @JavascriptInterface
        override fun lookupProfileFailure(username: String, error: String) {
            session.lookupProfileCallbacks[username]?.invoke(Result(null, error))
        }

        @JavascriptInterface
        override fun getFileResult(content: String, uniqueIdentifier: String, isBinary: Boolean) {
            Log.d(session.TAG, "putFileResult")

            if (isBinary) {
                val binaryContent: ByteArray = Base64.decode(content, Base64.DEFAULT)
                session.getFileCallbacks[uniqueIdentifier]?.invoke(Result(binaryContent))
            } else {
                session.getFileCallbacks[uniqueIdentifier]?.invoke(Result(content))
            }
            session.getFileCallbacks.remove(uniqueIdentifier)
        }

        @JavascriptInterface
        override fun getFileFailure(error: String, uniqueIdentifier: String) {
            session.getFileCallbacks[uniqueIdentifier]?.invoke(Result(null, error))
            session.getFileCallbacks.remove(uniqueIdentifier)
        }

        @JavascriptInterface
        override fun putFileResult(readURL: String, uniqueIdentifier: String) {
            Log.d(session.TAG, "putFileResult")

            session.putFileCallbacks[uniqueIdentifier]?.invoke(Result(readURL))
            session.putFileCallbacks.remove(uniqueIdentifier)
        }

        @JavascriptInterface
        override fun putFileFailure(error: String, uniqueIdentifier: String) {
            session.putFileCallbacks[uniqueIdentifier]?.invoke(Result(null, error))
            session.putFileCallbacks.remove(uniqueIdentifier)
        }

    }

}