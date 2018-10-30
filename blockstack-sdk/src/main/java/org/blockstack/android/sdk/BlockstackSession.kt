package org.blockstack.android.sdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.util.Base64
import android.util.Log
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.V8TypedArray
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.blockstack.android.sdk.j2v8.LogConsole
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.security.InvalidParameterException
import java.security.SecureRandom
import java.util.*

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
class BlockstackSession(context: Context? = null, private val config: BlockstackConfig,
                        /**
                         * url of the name lookup service, defaults to core.blockstack.org/v1/names
                         */
                        val nameLookupUrl: String = "https://core.blockstack.org/v1/names/",
                        private val sessionStore: ISessionStore = SessionStore(PreferenceManager.getDefaultSharedPreferences(context)),
                        private val executor: Executor = AndroidExecutor(context!!),
                        scriptRepo: ScriptRepo = if (context != null) AndroidScriptRepo(context) else throw InvalidParameterException("context or scriptRepo required")
) {

    private val TAG = BlockstackSession::class.simpleName

    /**
     * Flag indicating whether this object is ready to use
     */
    var loaded: Boolean = false
        private set(value) {
            field = value
        }

    private var signInCallback: ((Result<UserData>) -> Unit)? = null
    private val lookupProfileCallbacks = HashMap<String, ((Result<Profile>) -> Unit)>()
    private var validateProofsCallback: ((Result<ArrayList<Proof>>) -> Unit)? = null
    private val getFileCallbacks = HashMap<String, ((Result<Any>) -> Unit)>()
    private val putFileCallbacks = HashMap<String, ((Result<String>) -> Unit)>()
    private var getAppBucketUrlCallback: ((Result<String>) -> Unit)? = null
    private var getUserAppFileUrlCallback: ((Result<String>) -> Unit)? = null


    private val v8blockstackAndroid: V8Object
    private val v8userSessionAndroid: V8Object
    private val v8userSession: V8Object
    private val v8 = V8.createV8Runtime()

    init {
        registerConsoleMethods()

        v8.executeVoidScript(scriptRepo.globals())
        v8.executeVoidScript(scriptRepo.blockstack());
        v8.executeVoidScript(scriptRepo.base64());
        v8.executeVoidScript(scriptRepo.blockstackAndroid());

        v8blockstackAndroid = v8.getObject("blockstackAndroid")
        v8userSessionAndroid = v8.getObject("userSessionAndroid")

        registerCryptoMethods()
        registerJSAndroidBridgeMethods(v8blockstackAndroid)

        val scopesString = Scope.scopesArrayToJSONString(config.scopes)
        v8.executeVoidScript("var appConfig = new blockstack.AppConfig(${scopesString}, '${config.appDomain}', '${config.redirectPath}','${config.manifestPath}');var userSession = new blockstack.UserSession({appConfig:appConfig, sessionStore:androidSessionStore});")
        v8userSession = v8.getObject("userSession")

        loaded = true
    }

    private fun registerJSAndroidBridgeMethods(v8blockstack: V8Object) {
        val android = JSAndroidBridge(this, v8, v8blockstack)
        val v8android = V8Object(v8)
        v8.add("android", v8android)

        v8android.registerJavaMethod(android, "lookupProfileResult", "lookupProfileResult", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "lookupProfileFailure", "lookupProfileFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "validateProofsResult", "validateProofsResult", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "validateProofsFailure", "validateProofsFailure", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "signInSuccess", "signInSuccess", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "signInFailure", "signInFailure", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "getSessionData", "getSessionData", arrayOf<Class<*>>())
        v8android.registerJavaMethod(android, "setSessionData", "setSessionData", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "deleteSessionData", "deleteSessionData", arrayOf<Class<*>>())
        v8android.registerJavaMethod(android, "getFileResult", "getFileResult", arrayOf<Class<*>>(String::class.java, String::class.java, Boolean::class.java))
        v8android.registerJavaMethod(android, "getFileFailure", "getFileFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "putFileResult", "putFileResult", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "putFileFailure", "putFileFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "getAppBucketUrlResult", "getAppBucketUrlResult", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "getAppBucketUrlFailure", "getAppBucketUrlFailure", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "getUserAppFileUrlResult", "getUserAppFileUrlResult", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "getUserAppFileUrlResult", "getUserAppFileUrlResult", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "fetchAndroid", "fetchAndroid", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "setLocation", "setLocation", arrayOf<Class<*>>(String::class.java))
        v8android.release()
    }

    private fun registerCryptoMethods() {
        val v8global = v8.getObject("global")
        val v8crypto = v8global.getObject("crypto")
        v8global.release()

        val crypto = GlobalCrypto()
        v8crypto.registerJavaMethod(crypto, "getRandomValues", "getRandomValues", arrayOf<Class<*>>(V8TypedArray::class.java))
        v8crypto.release()
    }

    private fun registerConsoleMethods() {
        val console = LogConsole()
        val v8Console = V8Object(v8)
        v8.add("console", v8Console)
        v8Console.registerJavaMethod(console, "log", "log", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "error", "error", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "debug", "debug", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "warn", "warn", arrayOf<Class<*>>(String::class.java))
        v8Console.release()
    }


    @Suppress("unused")
    private class GlobalCrypto {
        private val secureRandom = SecureRandom()
        fun getRandomValues(array: V8TypedArray) {
            val buffer = array.getByteBuffer()

            val bytes = ByteArray(array.length())
            secureRandom.nextBytes(bytes)
            for (i in 0..buffer.limit() - 1) {
                buffer.put(i, bytes[i])
            }
        }
    }

    /**
     * Releases resources of the users blockstack session
     */
    fun release() {
        v8userSession.release()
        v8userSessionAndroid.release()
        v8blockstackAndroid.release()
        v8.release()
    }

    fun releaseThreadLock() {
        v8.locker.release()
    }

    fun aquireThreadLock() {
        v8.locker.acquire()
    }

    /**
     * Generates an authentication request that can be sent to the Blockstack browser
     * for the user to approve sign in. This authentication request can then be used for
     * sign in by passing it to the redirectToSignInWithAuthRequest method.
     *
     * Note: This method should only be used if you want to roll your own authentication flow.
     * Typically you'd use redirectToSignIn which takes care of this under the hood.
     *
     * @param transitPrivateKey hex encoded transit private key
     * @param redirectPath location to redirect user to after sign in approval
     * @param manifestPath location of this app's manifest file
     * @param scopes the permissions this app is requesting
     * @param appDomain the origin of this app
     * @param expiresAt the time at which this request is no longer valid
     */
    fun makeAuthRequest(transitPrivateKey: String, redirectURI: String, manifestURI: String, scopes: Array<String>, appDomain: String, expiresAt: Number): String {
        val v8params = V8Array(v8)
                .push(transitPrivateKey)
                .push(redirectURI)
                .push(manifestURI)
                .push(scopes)
                .push(appDomain)
                .push(expiresAt)
        val result = v8blockstackAndroid.executeStringFunction("makeAuthRequest", v8params)
        v8params.release()
        return result
    }

    /**
     * Process a pending sign in. This method should be called by your app when it
     * receives a request to the app's custom protocol handler.
     *
     * @param authResponse authentication response token
     * @param signInCallback called with the user data after sign-in or with an error
     *
     */
    fun handlePendingSignIn(authResponse: String, signInCallback: (Result<UserData>) -> Unit) {
        this.signInCallback = signInCallback

        val v8params = V8Array(v8)
                .push(authResponse)
        v8userSessionAndroid.executeVoidFunction("handlePendingSignIn", v8params)
        v8params.release()
    }

    /**
     * Generates an authentication request opens an activity that allows the user to
     * sign with an existing Blockstack ID already on the device or create a new one.
     *
     * @param errorCallback a function that is called when the redirection failed.
     */
    fun redirectUserToSignIn(errorCallback: (Result<Unit>) -> Unit) {
        try {
            val v8params = V8Array(v8)
                    .push(nameLookupUrl)
            v8userSession.executeVoidFunction("redirectToSignIn", v8params)
            v8params.release()
        } catch (e: Exception) {
            errorCallback(Result(null, e.toString()))
        }
    }

    /**
     * Retrieve data of signed in user
     *
     * @return `UserData` of the signed in user
     */
    fun loadUserData(): UserData? {
        try {
            val result = v8userSessionAndroid.executeStringFunction("loadUserData", null)
            return UserData(JSONObject(result))
        } catch (e: Exception) {
            Log.d(TAG, "error in loadUserData " + e.toString(), e)
            return null
        }
    }

    /**
     * Check if a user is currently signed in
     *
     * @param callback a function that is called with a flag that is `true` if the user is signed in, `false` if not.
     */
    fun isUserSignedIn(): Boolean {
        val result = v8userSession.executeBooleanFunction("isUserSignedIn", null)
        return result
    }

    /**
     * Sign the user out
     */
    fun signUserOut() {
        v8userSession.executeVoidFunction("signUserOut", null)
        v8userSession.release()
        sessionStore.deleteSessionData()
    }

    /**
     * Lookup the profile of a user
     *
     * @param username the registered user name, like `dev_android_sdk.id`
     * @param zoneFileLookupURL the url of the zone file lookup service like `https://core.blockstack.org/v1/names`
     * @param callback is called with the profile of the user or null if not found
     */
    fun lookupProfile(username: String, zoneFileLookupURL: URL, callback: (Result<Profile>) -> Unit) {
        lookupProfileCallbacks.put(username, callback)
        val v8params = V8Array(v8)
                .push(username)
                .push(zoneFileLookupURL.toString())
        v8blockstackAndroid.executeVoidFunction("lookupProfile", v8params)
        v8params.release()
    }

    /**
     * Validates the social proofs in a user's profile.
     * Currently supports validation of Facebook, Twitter, GitHub, Instagram, LinkedIn and HackerNews accounts.
     *
     * @param profile  The profile to be validated
     * @param ownerAddress  The owner bitcoin address to be validated
     * @param name (default = null) The Blockstack name to be validated
     * @param callback called with a list of validated proof objects or an error
     */
    fun validateProofs(profile: Profile, ownerAdress: String, name: String? = null, callback: (Result<ArrayList<Proof>>) -> Unit) {
        validateProofsCallback = callback

        val params = V8Array(v8)
                .push(profile.json.toString())
                .push(ownerAdress)
        if (name != null) {
            params.push(name)
        }
        v8blockstackAndroid.executeVoidFunction("validateProofs", params)
        params.release()
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
    fun getFile(path: String, options: GetFileOptions, callback: (Result<Any>) -> Unit) {
        Log.d(TAG, "getFile: path: ${path} options: ${options}")

        val uniqueIdentifier = addGetFileCallback(callback)
        val v8params = V8Array(v8).push(path).push(options.toJSON().toString()).push(uniqueIdentifier)
        v8userSessionAndroid.executeVoidFunction("getFile", v8params)
        v8params.release()
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
    fun putFile(path: String, content: Any, options: PutFileOptions, callback: (Result<String>) -> Unit) {
        Log.d(TAG, "putFile: path: ${path} options: ${options}")


        val valid = content is String || content is ByteArray
        if (!valid) {
            throw IllegalArgumentException("putFile content only supports String or ByteArray")
        }

        val uniqueIdentifier = addPutFileCallback(callback)

        val v8params: V8Array
        val isBinary = content is ByteArray
        if (isBinary) {
            val contentString = Base64.encodeToString(content as ByteArray, Base64.NO_WRAP)
            v8params = V8Array(v8).push(path).push(contentString).push(options.toJSON().toString()).push(uniqueIdentifier).push(true)
        } else {
            v8params = V8Array(v8).push(path).push(content).push(options.toJSON().toString()).push(uniqueIdentifier).push(false)
        }

        v8userSessionAndroid.executeVoidFunction("putFile", v8params)
        v8params.release()

    }

    /* Crypto methods */

    /**
     * Encrypt content
     *
     * @plainContent can be a String or ByteArray
     * @options defines how to encrypt
     * @return result object with `CipherObject` or error if encryption failed
     */
    fun encryptContent(plainContent: Any, options: CryptoOptions): Result<CipherObject> {

        val valid = plainContent is String || plainContent is ByteArray
        if (!valid) {
            throw IllegalArgumentException("encrypt content only supports String or ByteArray")
        }

        val v8params: V8Array
        val isBinary = plainContent is ByteArray
        if (isBinary) {
            val contentString = Base64.encodeToString(plainContent as ByteArray, Base64.NO_WRAP)
            v8params = V8Array(v8).push(contentString).push(options.toJSON().toString())
        } else {
            v8params = V8Array(v8).push(plainContent as String).push(options.toJSON().toString())
        }

        val result = v8userSessionAndroid.executeStringFunction("encryptContent", v8params)
        v8params.release()

        if (result != null && !"null".equals(result)) {
            val cipherObject = JSONObject(result)
            return Result(CipherObject(cipherObject))
        } else {
            return Result(null, "failed to encrypt")
        }
    }

    /**
     * Decrypt content
     * @cipherObject can be a String or ByteArray representing the cipherObject returned by  @see encryptContent
     * @binary flag indicating whether a ByteArray or String was encrypted
     * @options defines how to decrypt the cipherObject
     * @return result object with plain content as String or ByteArray depending on the given binary flag or error
     */
    fun decryptContent(cipherObject: Any, binary: Boolean, options: CryptoOptions): Result<Any> {

        val valid = cipherObject is String || cipherObject is ByteArray
        if (!valid) {
            throw IllegalArgumentException("decrypt content only supports JSONObject or ByteArray not " + cipherObject::class.java)
        }

        val v8params: V8Array
        val isBinary = cipherObject is ByteArray
        if (isBinary) {
            val cipherTextString = Base64.encodeToString(cipherObject as ByteArray, Base64.NO_WRAP)
            v8params = V8Array(v8).push(cipherTextString).push(options.toJSON().toString()).push(true)
        } else {
            v8params = V8Array(v8).push(cipherObject).push(options.toJSON().toString()).push(true)
        }
        val plainContent = v8userSessionAndroid.executeStringFunction("decryptContent", v8params)
        v8params.release()

        if (plainContent != null && !"null".equals(plainContent)) {
            if (!binary) {
                return Result(plainContent.removeSurrounding("\""))
            } else {
                return Result(Base64.decode(plainContent, Base64.DEFAULT))
            }
        } else {
            return Result(null, "failed to decrypt")
        }
    }

    /**
     * Get the app storage bucket URL
     *
     * @param gaiaHubUrl (String) the gaia hub URL
     * @param appPrivateKey (String) the app private key used to generate the app address
     * @param callback called with the URL of the app index file or error if it fails
     */
    fun getAppBucketUrl(gaiaHubUrl: String, appPrivateKey: String, callback: (Result<String>) -> Unit) {
        getAppBucketUrlCallback = callback
        val v8params = V8Array(v8)
                .push(gaiaHubUrl)
                .push(appPrivateKey)
        v8blockstackAndroid.executeVoidFunction("getAppBucketUrl", v8params)
        v8params.release()
    }

    /**
     * Fetch the public read URL of a user file for the specified app.
     *
     *@param path the path to the file to read
     *@param username The Blockstack ID of the user to look up
     *@param appOrigin The app origin
     *@param zoneFileLookupURL The URL to use for zonefile lookup. If false, this will use the blockstack.js's getNameInfo function instead.
     *@param callback called with the public read URL of the file or an error
     */
    fun getUserAppFileUrl(path: String, username: String, appOrigin: String, zoneFileLookupURL: String?, callback: (Result<String>) -> Unit) {
        getUserAppFileUrlCallback = callback
        val v8params = V8Array(v8)
                .push(path)
                .push(username)
                .push(appOrigin)

        if (zoneFileLookupURL != null) {
            v8params.push(zoneFileLookupURL)
        }
        v8blockstackAndroid.executeVoidFunction("getUserAppFileUrl", v8params)
        v8params.release()
    }

    private fun addGetFileCallback(callback: (Result<Any>) -> Unit): String {
        val uniqueIdentifier = UUID.randomUUID().toString()
        getFileCallbacks[uniqueIdentifier] = callback
        return uniqueIdentifier
    }

    private fun addPutFileCallback(callback: (Result<String>) -> Unit): String {
        val uniqueIdentifier = UUID.randomUUID().toString()
        putFileCallbacks[uniqueIdentifier] = callback
        return uniqueIdentifier
    }


    @Suppress("unused")
    private class JSAndroidBridge(private val blockstackSession: BlockstackSession, private val v8: V8, private val v8blockstackAndroid: V8Object) {

        private val httpClient = OkHttpClient()

        fun signInSuccess(userDataString: String) {
            val userData = JSONObject(userDataString)
            if (blockstackSession.signInCallback != null) {
                blockstackSession.signInCallback!!.invoke(Result(UserData(userData)))
                blockstackSession.signInCallback = null
            }
        }

        fun signInFailure(error: String) {
            if (blockstackSession.signInCallback != null) {
                blockstackSession.signInCallback!!.invoke(Result(null, error))
                blockstackSession.signInCallback = null
            }
        }

        fun validateProofsResult(proofs: String) {
            val proofArray = JSONArray(proofs)
            val proofList = arrayListOf<Proof>()
            for (i in 0..proofArray.length() - 1) {
                proofList.add(Proof(proofArray.getJSONObject(i)))
            }
            blockstackSession.validateProofsCallback?.invoke(Result(proofList))
        }

        fun validateProofsFailure(error: String) {
            blockstackSession.validateProofsCallback?.invoke(Result(null, error))
        }

        fun lookupProfileResult(username: String, userDataString: String) {
            val userData = JSONObject(userDataString)
            blockstackSession.lookupProfileCallbacks[username]?.invoke(Result(Profile(userData)))
        }

        fun lookupProfileFailure(username: String, error: String) {
            blockstackSession.lookupProfileCallbacks[username]?.invoke(Result(null, error))
        }

        fun getFileResult(content: String, uniqueIdentifier: String, isBinary: Boolean) {
            if (isBinary) {
                val binaryContent: ByteArray = Base64.decode(content, Base64.NO_WRAP)
                blockstackSession.getFileCallbacks[uniqueIdentifier]?.invoke(Result(binaryContent))
            } else {
                blockstackSession.getFileCallbacks[uniqueIdentifier]?.invoke(Result(content))
            }
            blockstackSession.getFileCallbacks.remove(uniqueIdentifier)
        }

        fun getFileFailure(error: String, uniqueIdentifier: String) {
            blockstackSession.getFileCallbacks[uniqueIdentifier]?.invoke(Result(null, error))
            blockstackSession.getFileCallbacks.remove(uniqueIdentifier)
        }

        fun putFileResult(readURL: String, uniqueIdentifier: String) {
            blockstackSession.putFileCallbacks[uniqueIdentifier]?.invoke(Result(readURL))
            blockstackSession.putFileCallbacks.remove(uniqueIdentifier)
        }

        fun putFileFailure(error: String, uniqueIdentifier: String) {
            blockstackSession.putFileCallbacks[uniqueIdentifier]?.invoke(Result(null, error))
            blockstackSession.putFileCallbacks.remove(uniqueIdentifier)
        }

        fun getAppBucketUrlResult(url: String) {
            blockstackSession.getAppBucketUrlCallback?.invoke(Result(url))
        }

        fun getAppBucketUrlFailure(error: String) {
            blockstackSession.getAppBucketUrlCallback?.invoke(Result(null, error))
        }

        fun getUserAppFileUrlResult(url: String) {
            blockstackSession.getUserAppFileUrlCallback?.invoke(Result(url))
        }

        fun getUserAppFileUrlFailre(error: String) {
            blockstackSession.getAppBucketUrlCallback?.invoke(Result(null, error))
        }

        fun getSessionData(): String {
            return blockstackSession.sessionStore.sessionData.json.toString()
        }

        fun setSessionData(sessionData: String) {
            blockstackSession.sessionStore.sessionData = SessionData(JSONObject(sessionData))
        }

        fun deleteSessionData() {
            return blockstackSession.sessionStore.deleteSessionData()
        }

        fun fetchAndroid(url: String, optionsString: String) {
            val options = JSONObject(optionsString)

            val builder = Request.Builder()
                    .url(url)

            if (options.has("method")) {
                var body: RequestBody? = null
                if (options.has("body")) {
                    val bodyString = options.getString("body")
                    if (options.has("bodyEncoded")) {
                        body = RequestBody.create(null, Base64.decode(bodyString, Base64.NO_WRAP))
                    } else {
                        body = RequestBody.create(null, bodyString)
                    }
                }
                builder.method(options.getString("method"), body)
            }

            if (options.has("headers")) {
                val headers = options.getJSONObject("headers")
                for (key in headers.keys()) {
                    builder.header(key, headers.getString(key))
                }
            }
            blockstackSession.executor.onNetworkThread {
                val response = httpClient.newCall(builder.build()).execute()
                blockstackSession.executor.onV8Thread {
                    try {
                        val r = response.toJSONString()
                        val v8params = V8Array(v8).push(url).push(r)
                        v8blockstackAndroid.executeVoidFunction("fetchResolve", v8params)
                        v8params.release()
                    } catch (e: Exception) {
                        Log.d("BlockstackSession", "onfetch", e)
                    }
                }
            }
        }

        fun setLocation(location: String) {
            blockstackSession.executor.onMainThread {
                ContextCompat.startActivity(it, Intent(Intent.ACTION_VIEW, Uri.parse(location)), null)
            }
        }
    }
}

interface Executor {
    fun onMainThread(function: (Context) -> Unit)
    fun onV8Thread(function: () -> Unit)
    fun onNetworkThread(function: suspend () -> Unit)
}

class AndroidExecutor(private val ctx: Context) : Executor {
    private val TAG = AndroidExecutor::class.simpleName
    override fun onMainThread(function: (ctx: Context) -> Unit) {
        launch(UI) { function.invoke(ctx) }
    }

    override fun onV8Thread(function: () -> Unit) {
        launch(UI) {
            function.invoke()
        }
    }

    override fun onNetworkThread(function: suspend () -> Unit) {
        async(CommonPool) {
            try {
                function.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "onNetworkThread", e)
            }

        }
    }

}

interface ScriptRepo {
    fun globals(): String
    fun blockstack(): String
    fun base64(): String
    fun blockstackAndroid(): String

}

class AndroidScriptRepo(private val context: Context) : ScriptRepo {
    override fun globals() = context.resources.openRawResource(R.raw.globals).bufferedReader().use { it.readText() }
    override fun blockstack() = context.resources.openRawResource(R.raw.blockstack).bufferedReader().use { it.readText() }
    override fun base64() = context.resources.openRawResource(R.raw.base64).bufferedReader().use { it.readText() }
    override fun blockstackAndroid() = context.resources.openRawResource(R.raw.blockstack_android).bufferedReader().use { it.readText() }
}

private fun Response.toJSONString(): String {
    val headersJson = JSONObject()
    headers().names().forEach { headersJson.put(it.toLowerCase(), header(it)) }
    val bodyEncoded: Boolean
    val bodyJson: String
    if (headersJson.optString("content-type")?.contentEquals("application/octet-stream") == true) {
        bodyEncoded = true
        val bytes = body()?.bytes()
        if (bytes != null) {
            bodyJson = Base64.encodeToString(bytes, Base64.NO_WRAP)
        } else {
            bodyJson = ""
        }
    } else {
        bodyEncoded = false
        bodyJson = body()?.string() ?: ""
    }


    return JSONObject()
            .put("status", code())
            .put("body", bodyJson)
            .put("bodyEncoded", bodyEncoded)
            .put("headers", headersJson)
            .toString()
}