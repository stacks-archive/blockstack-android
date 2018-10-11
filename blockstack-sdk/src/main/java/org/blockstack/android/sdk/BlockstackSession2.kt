package org.blockstack.android.sdk

import android.content.Context
import android.preference.PreferenceManager
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg
import org.json.JSONObject
import java.util.*


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
    private val sessionStore = SessionStore(PreferenceManager.getDefaultSharedPreferences(context))


    private val blockstack:V8Object
    private val userSession: V8Object
    private val v8: V8
    init {

        v8 = V8.createV8Runtime()
        v8.executeVoidScript(context.resources.openRawResource(R.raw.blockstack).bufferedReader().use { it.readText() });
        v8.executeVoidScript(context.resources.openRawResource(R.raw.sessionstore_android).bufferedReader().use { it.readText() });
        v8.executeVoidScript(context.resources.openRawResource(R.raw.blockstack_android2).bufferedReader().use { it.readText() });
        blockstack = v8.getObject("blockstack")

        val console = LogConsole()
        val v8Console = V8Object(v8)
        v8.add("console", v8Console)
        v8Console.registerJavaMethod(console, "log", "log", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "error", "error", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "debug", "debug", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "warn", "warn", arrayOf<Class<*>>(String::class.java))
        v8Console.release()

        val android = JavascriptInterface2Object(this, v8, blockstack)
        val v8android = V8Object(v8)
        v8.add("android", v8android)
        v8android.registerJavaMethod(android, "getSessionData", "getSessionData", arrayOf<Class<*>>())
        v8android.registerJavaMethod(android, "setSessionData", "setSessionData", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "deleteSessionData", "deleteSessionData", arrayOf<Class<*>>())
        v8android.registerJavaMethod(android, "getFileResult", "getFileResult", arrayOf<Class<*>>(String::class.java, String::class.java, Boolean::class.java))
        v8android.registerJavaMethod(android, "getFileFailure", "getFileFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "putFileResult", "putFileResult", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "putFileFailure", "putFileFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "fetch2", "fetch2", arrayOf<Class<*>>(String::class.java, String::class.java))

        v8.executeVoidScript("var appConfig = new blockstack.AppConfig('${config.appDomain}');var userSession = new blockstack.UserSession({appConfig:appConfig, sessionStore:androidSessionStore});")
        userSession = v8.getObject("userSession")
        loaded = true
    }

    internal interface Console {
        fun error(msg: String)
        fun warn(msg: String)
        fun debug(msg: String)
        fun log(msg: String)
    }

    class LogConsole: Console {
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

    fun isUserSignedIn(): Boolean {
        Log.d(TAG, "isUserSignedIn start")
        val result = userSession.executeBooleanFunction("isUserSignedIn", null)
        Log.d(TAG, "isUserSignedIn end")
        return result
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
    fun decryptContent(cipherObject: Any, binary:Boolean, options: CryptoOptions, callback: (Result<Any>) -> Unit) {

        val valid = cipherObject is String || cipherObject is ByteArray
        if (!valid) {
            throw IllegalArgumentException("decrypt content only supports JSONObject or ByteArray")
        }

        val isBinary = cipherObject is ByteArray

        var wasString: Boolean

        val plainContent = if (isBinary) {
            val cipherTextString = Base64.encodeToString(cipherObject as ByteArray, Base64.NO_WRAP)
            wasString = JSONObject(cipherTextString).getBoolean("wasString")
            val params = V8Array(v8).push(cipherTextString).push(options.toJSON().toString()).push(true)
            blockstack.executeStringFunction("decryptContent", params)
        } else {
            wasString = JSONObject(cipherObject as String).getBoolean("wasString")
            val params = V8Array(v8).push(cipherObject).push(options.toJSON().toString()).push(true)
            blockstack.executeStringFunction("decryptContent", params)
        }


        if (plainContent != null && !"null".equals(plainContent)) {

            if (!binary) {
                callback(Result(plainContent.removeSurrounding("\"")))
            } else {
                callback(Result(Base64.decode(plainContent, Base64.DEFAULT)))
            }
        } else {
            callback(Result(null, "failed to decrypt"))
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

    private interface JavaScriptInterface2 {
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
    }

    private class JavascriptInterface2Object(private val session: BlockstackSession2, val v8:V8, val blockstack:V8Object) : JavaScriptInterface2 {

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

        @JavascriptInterface
        override fun getSessionData(): String {
            return session.sessionStore.sessionData.json.toString()
        }

        @JavascriptInterface
        override fun setSessionData(sessionData: String) {
            session.sessionStore.sessionData = SessionData(JSONObject(sessionData))
        }

        @JavascriptInterface
        override fun deleteSessionData() {
            return session.sessionStore.deleteSessionData()
        }

        private val httpClient = OkHttpClient()

        @JavascriptInterface
        override fun fetch2(url: String, optionsString: String) {
            val options = JSONObject(optionsString)

            val builder = Request.Builder()
                    .url(url)

            if (options.has("method")) {
                var body: RequestBody? = null
                if (options.has("body")) {
                    body = RequestBody.create(null, options.getString("body"))
                }
                builder.method(options.getString("method"), body)
            }

            if (options.has("headers")) {
                val headers = options.getJSONObject("headers")
                for (key in headers.keys()) {
                    builder.header(key, headers.getString(key))
                }
            }

            async(UI) {
                val response = bg {
                    httpClient.newCall(builder.build()).execute()
                }.await()
                val r = response.toJSONString()
                var params = V8Array(v8).push(url).push(r)
                blockstack.executeVoidFunction("fetchResolve", params)
            }

        }

    }
}

fun Response.toJSONString(): String {
    return JSONObject()
            .put("status", code())
            .put("body", body()?.string()?:"")
            .toString()
}