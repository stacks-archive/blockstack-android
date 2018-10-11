package org.blockstack.android.sdk

import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import org.json.JSONObject
import java.io.File
import java.util.*

private val BLOCKSTACK_JS_URL_STRING = "file:///android_res/raw/blockstack.js"
private val HOSTED_BROWSER_URL_BASE = "https://browser.blockstack.org"

private val TAG = "BlockstackSession2"
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
class BlockstackSession2(context:Context, private val config: BlockstackConfig,
                        /**
                         * url of the name lookup service, defaults to core.blockstack.org/v1/names
                         */
                        val nameLookupUrl: String = "https://core.blockstack.org/v1/names/") {

    private val TAG = BlockstackSession2::class.qualifiedName

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
    private val userSession: V8Object
    private val v8: V8
    init {

        v8 = V8.createV8Runtime();
        v8.executeVoidScript(context.resources.openRawResource(R.raw.blockstack).bufferedReader().use { it.readText() });
        v8.executeVoidScript(context.resources.openRawResource(R.raw.sessionstore_android).bufferedReader().use { it.readText() });
        blockstack = v8.getObject("blockstack")
        Log.d(TAG, "initalized")
        v8.executeVoidScript("var appConfig = new blockstack.AppConfig('${config.appDomain}');var userSession = new blockstack.UserSession({appConfig:appConfig, sessionStore:androidSessionStore});")
        userSession = v8.getObject("userSession")
        loaded = true
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


    fun isUserSignedIn(): Boolean {
        Log.d(TAG, "isUserSignedIn start")
        val result = userSession.executeBooleanFunction("isUserSignedIn", V8Array(v8))
        Log.d(TAG, "isUserSignedIn end")
        return result
    }


    fun encryptContent(plainContent: Any, options: CryptoOptions): Result<CipherObject> {

        val valid = plainContent is String || plainContent is ByteArray
        if (!valid) {
            throw IllegalArgumentException("encrypt content only supports String or ByteArray")
        }

        val isBinary = plainContent is ByteArray

        val result = if (isBinary) {
            val contentString = Base64.encodeToString(plainContent as ByteArray, Base64.NO_WRAP)
            Log.d(TAG, "image " + contentString)
            val params = V8Array(v8).push(contentString).push(options.toJSON().toString()).push(true)
            userSession.executeStringFunction("encryptContent", params)
        } else {
            Log.d(TAG, "string " + plainContent)
            val params = V8Array(v8).push(plainContent as String).push(options.toJSON().toString()).push(false)
            userSession.executeStringFunction("encryptContent", params)
        }
        if (result != null && !"null".equals(result)) {
            val cipherObject = JSONObject(result)
            return Result(CipherObject(cipherObject))
        } else {
            return Result(null, "failed to encrypt")
        }

    }

}