package org.blockstack.android.sdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat.startActivity
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
import org.json.JSONObject
import java.net.URL
import java.security.InvalidParameterException
import java.security.SecureRandom
import java.util.*


private val HOSTED_BROWSER_URL_BASE = "https://browser.blockstack.org"

private val TAG = "BlockstackSession"

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


    private val blockstack: V8Object
    private val userSession: V8Object
    private val v8: V8

    init {
        v8 = V8.createV8Runtime()

        val console = LogConsole()
        val v8Console = V8Object(v8)
        v8.add("console", v8Console)
        v8Console.registerJavaMethod(console, "log", "log", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "error", "error", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "debug", "debug", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "warn", "warn", arrayOf<Class<*>>(String::class.java))
        v8Console.release()


        v8.executeVoidScript(scriptRepo.blockstack());
        v8.executeVoidScript(scriptRepo.base64());
        v8.executeVoidScript(scriptRepo.sessionStoreAndroid());
        v8.executeVoidScript(scriptRepo.blockstackAndroid2());
        blockstack = v8.getObject("blockstack")

        val v8crypto = v8.getObject("global").getObject("crypto")
        val crypto = GlobalCrypto(v8)
        v8crypto.registerJavaMethod(crypto, "getRandomValues", "getRandomValues", arrayOf<Class<*>>(V8TypedArray::class.java))

        val android = JavascriptInterface2Object(this, v8, blockstack)
        val v8android = V8Object(v8)
        v8.add("android", v8android)

        v8android.registerJavaMethod(android, "lookupProfileResult", "lookupProfileResult", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "lookupProfileFailure", "lookupProfileFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "signInSuccess", "signInSuccess", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "signInFailure", "signInFailure", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "getSessionData", "getSessionData", arrayOf<Class<*>>())
        v8android.registerJavaMethod(android, "setSessionData", "setSessionData", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "deleteSessionData", "deleteSessionData", arrayOf<Class<*>>())
        v8android.registerJavaMethod(android, "getFileResult", "getFileResult", arrayOf<Class<*>>(String::class.java, String::class.java, Boolean::class.java))
        v8android.registerJavaMethod(android, "getFileFailure", "getFileFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "putFileResult", "putFileResult", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "putFileFailure", "putFileFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "fetch2", "fetch2", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "setLocation", "setLocation", arrayOf<Class<*>>(String::class.java))
        val scopesString = Scope.scopesArrayToJSONString(config.scopes)
        v8.executeVoidScript("var appConfig = new blockstack.AppConfig(${scopesString}, '${config.appDomain}', '${config.redirectPath}','${config.manifestPath}');var userSession = new blockstack.UserSession({appConfig:appConfig, sessionStore:androidSessionStore});")
        userSession = v8.getObject("userSession")

        loaded = true
        Log.d(TAG, "session loaded")
    }

    internal interface Console {
        fun error(msg: String)
        fun warn(msg: String)
        fun debug(msg: String)
        fun log(msg: String)
    }

    class LogConsole : Console {
        override fun error(msg: String) {
            Log.e(TAG, msg)
        }

        override fun warn(msg: String) {
            Log.w(TAG, msg)
        }

        override fun log(msg: String) {
            Log.i(TAG, msg)
        }

        override fun debug(msg: String) {
            Log.d(TAG, msg)
        }
    }

    class GlobalCrypto(val v8: V8) {
        val secureRandom = SecureRandom()
        fun getRandomValues(array: V8TypedArray) {
            val buffer = array.getByteBuffer()

            val bytes = ByteArray(array.length())
            secureRandom.nextBytes(bytes)
            for (i in 0..buffer.limit() - 1) {
                buffer.put(i, bytes[i])
            }
        }
    }

    fun isUserSignedIn(): Boolean {
        Log.d(TAG, "isUserSignedIn start")
        val result = userSession.executeBooleanFunction("isUserSignedIn", null)
        Log.d(TAG, "isUserSignedIn end result:" + result.toString())
        return result
    }

    fun redirectUserToSignIn(callback: (Result<UserData>) -> Unit) {

        Log.d(TAG, "redirectUserToSignIn")

        try {
            val params = V8Array(v8)
                    .push(nameLookupUrl)
            userSession.executeFunction("redirectToSignIn", params)
        } catch (e: Exception) {
            Log.e(TAG, "redirectUserToSignIn", e)
        }
    }

    fun handlePendingSignIn(authResponse: String, callback: (Result<UserData>) -> Unit) {
        this.signInCallback = callback
        val params = V8Array(v8)
                .push(authResponse)
        Log.d(TAG, "handling " + authResponse)
        blockstack.executeFunction("handlePendingSignIn", params)
    }

    fun loadUserData(): UserData? {
        try {
            val result = blockstack.executeStringFunction("loadUserData2", null)
            Log.d(TAG, "userData " + result)
            return UserData(JSONObject(result))
        } catch (e: Exception) {
            Log.d(TAG, "error in loadUserData " + e.toString(), e)
            return null
        }
    }

    fun lookupProfile(username: String, zoneFileLookupURL: URL, callback: (Result<Profile>) -> Unit) {
        lookupProfileCallbacks.put(username, callback)
        val params = V8Array(v8)
                .push(username)
                .push(zoneFileLookupURL.toString())
        blockstack.executeVoidFunction("lookupProfile2", params)
    }


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
        val params = V8Array(v8).push(path).push(options.toJSON().toString()).push(uniqueIdentifier)
        blockstack.executeVoidFunction("getFile", params)
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

        val isBinary = content is ByteArray
        val uniqueIdentifier = addPutFileCallback(callback)

        return if (isBinary) {
            val contentString = Base64.encodeToString(content as ByteArray, Base64.NO_WRAP)
            val params = V8Array(v8).push(path).push(contentString).push(options.toJSON().toString()).push(uniqueIdentifier).push(true)
            blockstack.executeVoidFunction("putFile", params)
        } else {
            val params = V8Array(v8).push(path).push(content).push(options.toJSON().toString()).push(uniqueIdentifier).push(false)
            blockstack.executeVoidFunction("putFile", params)
        }

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
            val params = V8Array(v8).push(contentString).push(options.toJSON().toString())
            blockstack.executeStringFunction("encryptContent", params)
        } else {
            Log.d(TAG, "string " + plainContent)
            val params = V8Array(v8).push(plainContent as String).push(options.toJSON().toString())
            blockstack.executeStringFunction("encryptContent", params)
        }
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
     * @options defines how to decrypt the cipherObject
     * @callback called with the plain content as String or ByteArray depending on the given options
     */
    fun decryptContent(cipherObject: Any, binary: Boolean, options: CryptoOptions): Result<Any> {

        val valid = cipherObject is String || cipherObject is ByteArray
        if (!valid) {
            throw IllegalArgumentException("decrypt content only supports JSONObject or ByteArray not " + cipherObject::class.java)
        }

        val isBinary = cipherObject is ByteArray

        val plainContent = if (isBinary) {
            val cipherTextString = Base64.encodeToString(cipherObject as ByteArray, Base64.NO_WRAP)
            val params = V8Array(v8).push(cipherTextString).push(options.toJSON().toString()).push(true)
            blockstack.executeStringFunction("decryptContent", params)
        } else {
            val params = V8Array(v8).push(cipherObject).push(options.toJSON().toString()).push(true)
            blockstack.executeStringFunction("decryptContent", params)
        }


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

    fun signUserOut() {
        userSession.executeFunction("signUserOut", null)
    }

    private interface JavaScriptInterface2 {
        fun signInSuccess(userDataString: String)
        fun signInFailure(error: String)
        fun lookupProfileResult(username: String, userDataString: String)
        fun lookupProfileFailure(username: String, error: String)
        fun getFileResult(content: String, uniqueIdentifier: String, isBinary: Boolean)
        fun getFileFailure(error: String, uniqueIdentifier: String)
        fun putFileResult(readURL: String, uniqueIdentifier: String)
        fun putFileFailure(error: String, uniqueIdentifier: String)
        fun getSessionData(): String
        fun setSessionData(sessionData: String)
        fun deleteSessionData()
        fun fetch2(url: String, options: String)
        fun setLocation(location: String)
    }

    private class JavascriptInterface2Object(private val session: BlockstackSession, val v8: V8, val blockstack: V8Object) : JavaScriptInterface2 {

        override fun signInSuccess(userDataString: String) {
            Log.d(TAG, "sign in success " + userDataString)
            val userData = JSONObject(userDataString)
            if (session.signInCallback != null) {
                session.signInCallback!!.invoke(Result(UserData(userData)))
            }
        }

        override fun signInFailure(error: String) {
            Log.d(TAG, "sign in error " + error)
            if (session.signInCallback != null) {
                session.signInCallback!!.invoke(Result(null, error))
            }
        }

        override fun lookupProfileResult(username: String, userDataString: String) {
            val userData = JSONObject(userDataString)
            session.lookupProfileCallbacks[username]?.invoke(Result(Profile(userData)))
        }

        override fun lookupProfileFailure(username: String, error: String) {
            session.lookupProfileCallbacks[username]?.invoke(Result(null, error))
        }

        override fun getFileResult(content: String, uniqueIdentifier: String, isBinary: Boolean) {
            Log.d(session.TAG, "getFileResult isBinary? " + isBinary)

            if (isBinary) {
                val binaryContent: ByteArray = Base64.decode(content, Base64.NO_WRAP)
                session.getFileCallbacks[uniqueIdentifier]?.invoke(Result(binaryContent))
            } else {
                session.getFileCallbacks[uniqueIdentifier]?.invoke(Result(content))
            }
            session.getFileCallbacks.remove(uniqueIdentifier)
        }

        override fun getFileFailure(error: String, uniqueIdentifier: String) {
            session.getFileCallbacks[uniqueIdentifier]?.invoke(Result(null, error))
            session.getFileCallbacks.remove(uniqueIdentifier)
        }

        override fun putFileResult(readURL: String, uniqueIdentifier: String) {
            Log.d(session.TAG, "putFileResult")

            session.putFileCallbacks[uniqueIdentifier]?.invoke(Result(readURL))
            session.putFileCallbacks.remove(uniqueIdentifier)
        }

        override fun putFileFailure(error: String, uniqueIdentifier: String) {
            session.putFileCallbacks[uniqueIdentifier]?.invoke(Result(null, error))
            session.putFileCallbacks.remove(uniqueIdentifier)
        }

        override fun getSessionData(): String {
            return session.sessionStore.sessionData.json.toString()
        }

        override fun setSessionData(sessionData: String) {
            session.sessionStore.sessionData = SessionData(JSONObject(sessionData))
        }

        override fun deleteSessionData() {
            return session.sessionStore.deleteSessionData()
        }

        private val httpClient = OkHttpClient()

        override fun fetch2(url: String, optionsString: String) {
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
            session.executor.onWorkerThread {
                val response = httpClient.newCall(builder.build()).execute()

                session.executor.onMainThread {
                    val r = response.toJSONString()
                    var params = V8Array(v8).push(url).push(r)
                    blockstack.executeVoidFunction("fetchResolve", params)
                }
            }
        }

        override fun setLocation(location: String) {
            session.executor.onMainThread {
                startActivity(it, Intent(Intent.ACTION_VIEW, Uri.parse(location)), null)
            }
        }
    }
}

fun Response.toJSONString(): String {
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

interface Executor {
    fun onMainThread(function: (Context) -> Unit)
    fun onWorkerThread(function: suspend () -> Unit)
}

class AndroidExecutor(private val ctx: Context) : Executor {
    override fun onMainThread(function: (ctx: Context) -> Unit) {
        launch(UI) { function.invoke(ctx) }
    }

    override fun onWorkerThread(function: suspend () -> Unit) {
        async(CommonPool) {
            try {
                Log.d(TAG, "onWorkerThread" + function::class.java.toString())
                function.invoke()
                Log.d(TAG, "onWorkerThread" + function::class.java.toString())
            } catch (e: Exception) {
                Log.e(TAG, "onWorkerThread", e)
            }

        }
    }

}

interface ScriptRepo {
    fun blockstack(): String
    fun base64(): String
    fun sessionStoreAndroid(): String
    fun blockstackAndroid2(): String

}

class AndroidScriptRepo(private val context: Context) : ScriptRepo {
    override fun blockstack() = context.resources.openRawResource(R.raw.blockstack).bufferedReader().use { it.readText() }
    override fun base64() = context.resources.openRawResource(R.raw.base64).bufferedReader().use { it.readText() }
    override fun sessionStoreAndroid() = context.resources.openRawResource(R.raw.sessionstore_android).bufferedReader().use { it.readText() }
    override fun blockstackAndroid2() = context.resources.openRawResource(R.raw.blockstack_android2).bufferedReader().use { it.readText() }
}