package org.blockstack.android.sdk

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.V8TypedArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import org.blockstack.android.sdk.j2v8.LogConsole
import org.blockstack.android.sdk.model.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import java.security.InvalidParameterException
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.YEAR

private val DEFAULT_BLOCKSTACK_HOST: String = "https://browser.blockstack.org/auth"
private val DEFAULT_BETA_BLOCKSTACK_HOST: String = "https://beta.browser.blockstack.org/auth"

/**
 * Main object to interact with blockstack in an activity or service
 *
 * The current implementation is a wrapper for blockstack.js using a j2v8 javascript engine.
 *
 * @param context the context used to define shared preferences for storing session data, to locate resources for this SDK.
 * Can be null if SessionStore, executor and scriptRepo is defined.
 * @param config the configuration for blockstack
 * @param sessionStore the location where session data should be stored. Defaults to the default shared preferences of the app using the SDK
 * @param executor defines where functions of this SDK should be executed. Defaults to @see AndroidExecutor with given context
 * @param scriptRepo required to locate this SDK's resources. Defaults to AndroidScriptRepo with given context.
 * Can be AndroidScriptRepo with application context as well.
 * @param betaMode flag indicating whether the beta version of the hosted blockstack browser should be used. Defaults to false.
 */
class BlockstackSession(context: Context? = null, private val config: BlockstackConfig,
                        /**
                         * url of the name lookup service, defaults to core.blockstack.org/v1/names
                         */
                        val nameLookupUrl: String = "https://core.blockstack.org/v1/names/",
                        private val sessionStore: ISessionStore = SessionStore(PreferenceManager.getDefaultSharedPreferences(context)),
                        private val executor: Executor = AndroidExecutor(context!!),
                        scriptRepo: ScriptRepo = if (context != null) AndroidScriptRepo(context) else throw InvalidParameterException("context or scriptRepo required"),
                        private val betaMode: Boolean = false,
                        callFactory: Call.Factory = OkHttpClient()
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
    private var resolveZoneFileToProfileCallback: ((Result<Profile>) -> Unit)? = null
    private val getFileCallbacks = HashMap<String, ((Result<Any>) -> Unit)>()
    private val getFileUrlCallbacks = HashMap<String, ((Result<String>) -> Unit)>()
    private val putFileCallbacks = HashMap<String, ((Result<String>) -> Unit)>()
    private val deleteFileCallbacks = HashMap<String, ((Result<Unit>) -> Unit)>()
    private var getAppBucketUrlCallback: ((Result<String>) -> Unit)? = null
    private var getUserAppFileUrlCallback: ((Result<String>) -> Unit)? = null
    private var listFilesCallback: ((Result<String>) -> Boolean)? = null
    private var listFilesCountCallback: ((Result<Int>) -> Unit)? = null

    internal val v8blockstackAndroid: V8Object
    private val v8userSessionAndroid: V8Object
    private val v8networkAndroid: V8Object
    private val v8userSession: V8Object

    /**
     * Object that is used in this blockstack user session.
     * It can be used already before the user is logged in.
     */
    val network: Network


    private val v8 = V8.createV8Runtime()

    init {
        registerConsoleMethods()

        v8.executeVoidScript(scriptRepo.globals())
        v8.executeVoidScript(scriptRepo.blockstack())
        v8.executeVoidScript(scriptRepo.base64())
        v8.executeVoidScript(scriptRepo.zoneFile())
        v8.executeVoidScript(scriptRepo.blockstackAndroid())


        v8blockstackAndroid = v8.getObject("blockstackAndroid")
        v8userSessionAndroid = v8.getObject("userSessionAndroid")
        v8networkAndroid = v8.getObject("networkAndroid")

        registerCryptoMethods()
        registerJSAndroidBridgeMethods(v8blockstackAndroid, v8userSessionAndroid, callFactory)

        val scopesString = Scope.scopesArrayToJSONString(config.scopes)
        val authenticatorURL = if (betaMode) {
            DEFAULT_BETA_BLOCKSTACK_HOST
        } else {
            DEFAULT_BLOCKSTACK_HOST
        }
        v8.executeVoidScript("var appConfig = new blockstack.AppConfig(${scopesString}, '${config.appDomain}', '${config.redirectPath}','${config.manifestPath}', null, '${authenticatorURL}');var userSession = new blockstack.UserSession({appConfig:appConfig, sessionStore:androidSessionStore});")
        v8userSession = v8.getObject("userSession")

        network = Network(v8networkAndroid, v8)

        // check verified app link verification once
        if (context != null && !doNotVerifyAppLinkConfiguration) {
            executor.onNetworkThread {
                AppLinkVerifier(context, config).verify()
                doNotVerifyAppLinkConfiguration = true
            }
        }

        loaded = true
    }

    private fun registerJSAndroidBridgeMethods(v8blockstackAndroid: V8Object, v8userSessionAndroid: V8Object, callFactory: Call.Factory) {
        val android = JSAndroidBridge(this, v8, v8blockstackAndroid, v8userSessionAndroid, callFactory)
        val v8android = V8Object(v8)
        v8.add("android", v8android)

        v8android.registerJavaMethod(android, "lookupProfileResult", "lookupProfileResult", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "lookupProfileFailure", "lookupProfileFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "validateProofsResult", "validateProofsResult", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "validateProofsFailure", "validateProofsFailure", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "resolveZoneFileToProfileResult", "resolveZoneFileToProfileResult", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "resolveZoneFileToProfileFailure", "resolveZoneFileToProfileFailure", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "signInSuccess", "signInSuccess", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "signInFailure", "signInFailure", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "getSessionData", "getSessionData", arrayOf<Class<*>>())
        v8android.registerJavaMethod(android, "setSessionData", "setSessionData", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "deleteSessionData", "deleteSessionData", arrayOf<Class<*>>())
        v8android.registerJavaMethod(android, "getFileResult", "getFileResult", arrayOf<Class<*>>(String::class.java, String::class.java, Boolean::class.java))
        v8android.registerJavaMethod(android, "getFileFailure", "getFileFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "putFileResult", "putFileResult", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "putFileFailure", "putFileFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "deleteFileResult", "deleteFileResult", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "deleteFileFailure", "deleteFileFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "getFileUrlResult", "getFileUrlResult", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "getFileUrlFailure", "getFileUrlFailure", arrayOf<Class<*>>(String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "getAppBucketUrlResult", "getAppBucketUrlResult", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "getAppBucketUrlFailure", "getAppBucketUrlFailure", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "getUserAppFileUrlResult", "getUserAppFileUrlResult", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "getUserAppFileUrlFailure", "getUserAppFileUrlFailure", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "listFilesResult", "listFilesResult", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "listFilesFailure", "listFilesFailure", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "listFilesCountResult", "listFilesCountResult", arrayOf<Class<*>>(Int::class.java))
        v8android.registerJavaMethod(android, "listFilesCountFailure", "listFilesCountFailure", arrayOf<Class<*>>(String::class.java))
        v8android.registerJavaMethod(android, "fetchAndroid", "fetchAndroid", arrayOf<Class<*>>(String::class.java, String::class.java, String::class.java))
        v8android.registerJavaMethod(android, "setLocation", "setLocation", arrayOf<Class<*>>(String::class.java))
        v8android.release()
    }

    private fun registerCryptoMethods() {
        val v8crypto = v8.getObject("crypto")
        val crypto = GlobalCrypto()
        v8crypto.registerJavaMethod(crypto, "getRandomValues", "getRandomValues", arrayOf<Class<*>>(V8TypedArray::class.java))
        v8crypto.release()
    }

    private fun registerConsoleMethods() {
        val console = LogConsole()
        val v8Console = V8Object(v8)
        v8.add("console", v8Console)
        v8Console.registerJavaMethod(console, "log", "log", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "error", "error", arrayOf<Class<*>>(Any::class.java))
        v8Console.registerJavaMethod(console, "debug", "debug", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "warn", "warn", arrayOf<Class<*>>(String::class.java))
        v8Console.release()
    }


    @Suppress("unused")
    private class GlobalCrypto {
        private val secureRandom = SecureRandom()
        fun getRandomValues(array: V8TypedArray) {
            val buffer = array.byteBuffer

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
        v8networkAndroid.release()
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
     * @param expiresAt the time at which this request is no longer valid
     * @param extraParams key, value pairs that are transferred with the auth request,
     * only Boolean and String values are supported
     */
    fun makeAuthRequest(transitPrivateKey: String, expiresAt: Number, extraParams: Map<String, Any>): String {
        val v8ExtraParams = V8Object(v8)
        for (extraParam in extraParams) {
            if (extraParam.value is Boolean) {
                v8ExtraParams.add(extraParam.key, extraParam.value as Boolean)
            } else if (extraParam.value is String) {
                v8ExtraParams.add(extraParam.key, extraParam.value as String)
            } else {
                throw InvalidParameterException("only Boolean and String values allowed")
            }
        }
        val v8params = V8Array(v8)
                .push(transitPrivateKey)
                .push("${config.appDomain}${config.redirectPath}")
                .push("${config.appDomain}${config.manifestPath}")
                .push(Scope.scopesArrayToJSONString(config.scopes))
                .push(config.appDomain.toString())
                .push(expiresAt)
                .push(v8ExtraParams)
        val result = v8userSession.executeStringFunction("makeAuthRequest", v8params)
        v8params.release()
        v8ExtraParams.release()
        return result
    }

    /**
     * Generates a ECDSA keypair to use as the ephemeral app transit private key
     * and stores the hex value of the private key in the session storage.
     *
     * @return the hex encoded private key
     */
    fun generateAndStoreTransitKey(): String {
        val v8params = V8Array(v8)
        val key = v8userSession.executeStringFunction("generateAndStoreTransitKey", v8params)
        v8params.release()
        return key
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

        if (BuildConfig.DEBUG) {
            val error = verifyAuthResponse(authResponse)
            if (error != null) {
                signInCallback(Result(null, error))
                return
            }
        }
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
            v8userSession.executeVoidFunction("redirectToSignIn", v8params)
            v8params.release()
        } catch (e: Exception) {
            errorCallback(Result(null, e.toString()))
        }
    }

    /**
     * Redirects the user to the Blockstack browser to approve the sign in request given.
     *
     * The user is redirected to the blockstackIDHost if the blockstack: protocol handler is not detected.
     * Please note that the protocol handler detection does not work on all browsers.
     *
     * @param authRequest the authentication request generated by makeAuthRequest
     * @param blockstackIDHost    the URL to redirect the user to if the blockstack protocol handler is not detected
     */
    fun redirectToSignInWithAuthRequest(authRequest: String, blockstackIDHost: String = if (betaMode) DEFAULT_BETA_BLOCKSTACK_HOST else DEFAULT_BLOCKSTACK_HOST,
                                        errorCallback: (Result<Unit>) -> Unit) {
        try {
            val v8params = V8Array(v8)
                    .push(authRequest)
                    .push(blockstackIDHost)
            v8userSession.executeVoidFunction("redirectToSignInWithAuthRequest", v8params)
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
        sessionStore.deleteSessionData()
    }

    /**
     * Lookup the profile of a user
     *
     * @param username the registered user name, like `dev_android_sdk.id`
     * @param zoneFileLookupURL the url of the zone file lookup service like `https://core.blockstack.org/v1/names`
     * @param callback is called with the profile of the user or null if not found
     */
    fun lookupProfile(username: String, zoneFileLookupURL: URL = URL(nameLookupUrl), callback: (Result<Profile>) -> Unit) {
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


    /**
     * Extracts a profile from an encoded token and optionally verifies it,
     * if publicKeyOrAddress is provided.
     * @param token the token to be decoded
     * @param publicKeyOrAddress the public key or address of the keypair that is thought to have signed the token
     * @return the profile extracted from the encoded token
     * @throws Exception if token verification fails
     */
    fun extractProfile(token: String, publicKeyOrAddress: String? = null): ProfileToken {
        val params = V8Array(v8)
                .push(token)
        if (publicKeyOrAddress != null) {
            params.push(publicKeyOrAddress)
        }
        try {
            val decodedToken = v8blockstackAndroid.executeStringFunction("extractProfile", params)
            return ProfileToken(JSONObject(decodedToken))
        } catch (e: Exception) {
            Log.d(TAG, e.toString(), e)
            throw e
        } finally {
            params.release()
        }
    }

    /**
     * Wraps a token for a profile token file
     * @param token the token to be decoded and wrapped
     * @return pair of encoded and decoded Token
     */
    fun wrapProfileToken(token: String): ProfileTokenPair {
        val params = V8Array(v8)
                .push(token)
        try {
            val decodedToken = v8blockstackAndroid.executeStringFunction("wrapProfileToken", params)
            return ProfileTokenPair(JSONObject(decodedToken))
        } catch (e: Exception) {
            Log.d(TAG, e.toString(), e)
            throw e
        } finally {
            params.release()
        }
    }

    /**
     * Signs a profile token
     * @param profile the profile to be signed
     * @param privateKey the signing private key
     * @param subject the entity that the information is about
     * @param issuer the entity that is issuing the token
     * @param signingAlgorithm the signing algorithm to use, defaults to 'ES256K'
     * @param issuedAt the time of issuance of the token, defaults to now
     * @param expiresAt the time of expiration of the token, defaults to next year
     * @return the signed profile token
     */
    fun signProfileToken(profile: Profile, privateKey: String, subject: Entity, issuer: Entity, signingAlgorithm: SigningAlgorithm = SigningAlgorithm.ES256K, issuedAt: Date = Date(), expiresAt: Date = nextYear()): ProfileTokenPair {
        val params = V8Array(v8)
                .push(profile.json.toString())
                .push(privateKey)
                .push(subject.json.toString())
                .push(issuer.json.toString())
                .push(signingAlgorithm.algorithmName)
                .push(issuedAt.toZuluTime())
                .push(expiresAt.toZuluTime())

        try {
            val signedToken = v8blockstackAndroid.executeStringFunction("signProfileToken", params)
            return wrapProfileToken(signedToken)
        } catch (e: Exception) {
            Log.d(TAG, e.toString(), e)
            throw e
        } finally {
            params.release()
        }
    }

    private fun nextYear(): Date {
        val calendar = java.util.GregorianCalendar.getInstance()
        calendar.set(YEAR, calendar.get(YEAR) + 1)
        return calendar.time
    }

    /**
     * Verifies a profile token.
     * @param token the token to be verified
     * @param publicKeyOrAddress the public key or address of the keypair that is thought to have signed the token
     * @return the verified, decoded profile token
     * @throws Exception if token verification fails
     */
    fun verifyProfileToken(token: String, publicKeyOrAddress: String): ProfileToken {
        val params = V8Array(v8)
                .push(token)
                .push(publicKeyOrAddress)
        try {
            val decodedToken = v8blockstackAndroid.executeStringFunction("verifyProfileToken", params)
            return ProfileToken(JSONObject(decodedToken))
        } catch (e: Exception) {
            Log.d(TAG, e.toString(), e)
            throw e
        } finally {
            params.release()
        }
    }

    /**
     * Parses zone file content.
     * @param zoneFileContent content of the zone file
     * @return the zone file object
     */
    fun parseZoneFile(zoneFileContent: String): ZoneFile {
        val params = V8Array(v8)
                .push(zoneFileContent)
        try {
            val zoneFile = v8blockstackAndroid.executeStringFunction("parseZoneFile", params)
            return ZoneFile(JSONObject(zoneFile))
        } catch (e: Exception) {
            Log.d(TAG, e.toString(), e)
            throw e
        } finally {
            params.release()
        }
    }

    /**
     * Parses zone file content and converts to profile
     * @param zoneFileContent content of the zone file
     * @return the zone file object
     */
    fun resolveZoneFileToProfile(zoneFileContent: String, publicKeyOrAddress: String? = null, callback: (Result<Profile>) -> Unit) {
        resolveZoneFileToProfileCallback = callback
        val params = V8Array(v8)
                .push(zoneFileContent)
        if (publicKeyOrAddress != null) {
            params.push(publicKeyOrAddress)
        }
        try {
            v8blockstackAndroid.executeVoidFunction("resolveZoneFileToProfile", params)
        } catch (e: Exception) {
            Log.d(TAG, e.toString(), e)
            throw e
        } finally {
            params.release()
        }
    }

    /* Public storage methods */

    /**
     * List the set of files in this application's Gaia storage bucket.
     *
     * @property callback invoked on each named file, should return true to continue the listing
     * operation or false to end it
     * @property countCallback called after the list operation with the number of files that
     * were listed (not necessarily the total number of files, e.g. if aborted early)
     */
    fun listFiles(callback: (Result<String>) -> Boolean, countCallback: (Result<Int>) -> Unit) {
        Log.d(TAG, "listFiles")
        listFilesCallback = callback
        listFilesCountCallback = countCallback
        v8userSessionAndroid.executeVoidFunction("listFiles", null)
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


    /**
     * Deletes the specified file from the app's data store.
     * @param path - The path to the file to delete.
     * @param options - Optional options object.
     * @param options.wasSigned - Set to true if the file was originally signed
     * in order for the corresponding signature file to also be deleted.
     * @param callback called when the file has been removed or when an error occurred.
     */
    fun deleteFile(path: String, options: DeleteFileOptions = DeleteFileOptions(), callback: (Result<Unit>) -> Unit) {
        Log.d(TAG, "delete file")
        try {
            val uniqueIdentifier = addDeleteFileCallback(callback)
            val v8params = V8Array(v8).push(path).push(options.toJSON().toString()).push(uniqueIdentifier)
            v8userSessionAndroid.executeVoidFunction("deleteFile", v8params)
            v8params.release()
        } catch (e: Exception) {
            Log.d(TAG, "delete file failure", e)
        }
    }


    /**
     * Get the URL for reading a file from an app's data store.
     * @param path  the path to the file to read
     * @param options - options object
     * @param callback
     * @returns {Promise<string>} that resolves to the URL or rejects with an error
     */
    fun getFileUrl(path: String, options: GetFileOptions, callback: (Result<String>) -> Unit) {
        val uniqueIdentifier = addGetFileUrlCallback(callback)
        val v8params = V8Array(v8).push(path).push(options.toJSON().toString()).push(uniqueIdentifier)
        v8userSessionAndroid.executeVoidFunction("getFileUrl", v8params)
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


    fun getPublicKeyFromPrivate(privateKey: String): String? {
        return v8.executeStringScript("blockstack.getPublicKeyFromPrivate('${privateKey}')")
    }

    fun makeECPrivateKey(): String? {
        return v8.executeStringScript("blockstack.makeECPrivateKey()")
    }

    fun publicKeyToAddress(publicKey: String): String? {
        return v8.executeStringScript("blockstack.publicKeyToAddress('${publicKey}')")
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

    private fun addDeleteFileCallback(callback: (Result<Unit>) -> Unit): String {
        val uniqueIdentifier = UUID.randomUUID().toString()
        deleteFileCallbacks[uniqueIdentifier] = callback
        return uniqueIdentifier
    }

    private fun addGetFileUrlCallback(callback: (Result<String>) -> Unit): String {
        val uniqueIdentifier = UUID.randomUUID().toString()
        getFileUrlCallbacks[uniqueIdentifier] = callback
        return uniqueIdentifier
    }

    @Suppress("unused")
    private class JSAndroidBridge(private val blockstackSession: BlockstackSession, private val v8: V8, private val v8blockstackAndroid: V8Object, private val v8userSessionAndroid: V8Object, private val httpClient: Call.Factory) {

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

        fun resolveZoneFileToProfileResult(profileString: String) {
            val profile = Profile(JSONObject(profileString))
            blockstackSession.resolveZoneFileToProfileCallback?.invoke(Result(profile))
        }

        fun resolveZoneFileToProfileFailure(error: String) {
            blockstackSession.resolveZoneFileToProfileCallback?.invoke(Result(null, error))
        }

        fun lookupProfileResult(username: String, userDataString: String) {
            val userData = JSONObject(userDataString)
            blockstackSession.lookupProfileCallbacks[username]?.invoke(Result(Profile(userData)))
        }

        fun lookupProfileFailure(username: String, error: String) {
            blockstackSession.lookupProfileCallbacks[username]?.invoke(Result(null, error))
        }

        fun getFileResult(content: String?, uniqueIdentifier: String, isBinary: Boolean) {
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

        fun deleteFileResult(uniqueIdentifier: String) {
            blockstackSession.deleteFileCallbacks[uniqueIdentifier]?.invoke(Result(null))
            blockstackSession.deleteFileCallbacks.remove(uniqueIdentifier)
        }

        fun deleteFileFailure(error: String, uniqueIdentifier: String) {
            blockstackSession.deleteFileCallbacks[uniqueIdentifier]?.invoke(Result(null, error))
            blockstackSession.deleteFileCallbacks.remove(uniqueIdentifier)
        }

        fun getFileUrlResult(url: String?, uniqueIdentifier: String) {
            blockstackSession.getFileUrlCallbacks[uniqueIdentifier]?.invoke(Result(url))
            blockstackSession.getFileUrlCallbacks.remove(uniqueIdentifier)
        }

        fun getFileUrlFailure(error: String, uniqueIdentifier: String) {
            blockstackSession.getFileUrlCallbacks[uniqueIdentifier]?.invoke(Result(null, error))
            blockstackSession.getFileUrlCallbacks.remove(uniqueIdentifier)
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

        fun getUserAppFileUrlFailure(error: String) {
            blockstackSession.getAppBucketUrlCallback?.invoke(Result(null, error))
        }

        fun listFilesResult(url: String) {
            val cont = blockstackSession.listFilesCallback?.invoke(Result(url))
            val params = V8Array(v8)
            params.push(cont)
            v8userSessionAndroid.executeVoidFunction("listFilesCallback", params)
            params.release()
        }

        fun listFilesFailure(error: String) {
            blockstackSession.listFilesCallback?.invoke(Result(null, error))
        }

        fun listFilesCountResult(count: Int) {
            blockstackSession.listFilesCountCallback?.invoke(Result(count))
        }

        fun listFilesCountFailure(error: String) {
            blockstackSession.listFilesCountCallback?.invoke(Result(null, error))
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

        fun fetchAndroid(url: String, optionsString: String, keyForFetchUrl: String) {

            blockstackSession.executor.onNetworkThread {
                val options = JSONObject(optionsString)

                val request = buildRequest(url, options)

                try {
                    val response = httpClient.newCall(request).execute()
                    blockstackSession.executor.onV8Thread {
                        executeFetchResolve(response, keyForFetchUrl)
                    }
                }  catch(e: Exception) {
                    Log.d(TAG, "on execute call", e)
                    blockstackSession.executor.onV8Thread {
                        executeFetchReject(e, keyForFetchUrl)
                    }
                }
            }
        }

        private fun buildRequest(url: String, options: JSONObject): Request {
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
            return builder.build()
        }

        private fun executeFetchResolve(response: Response, keyForFetchUrl: String) {
            try {
                val r = response.toJSONString()

                val v8params = V8Array(v8).push(keyForFetchUrl).push(r)
                v8blockstackAndroid.executeVoidFunction("fetchResolve", v8params)
                v8params.release()

            } catch (e: Exception) {
                Log.d(TAG, "on fetchResolve", e)
                executeFetchReject(e, keyForFetchUrl)
            }
        }

        private fun executeFetchReject(e: Exception, keyForFetchUrl: String) {
            try {
                val v8params = V8Array(v8).push(keyForFetchUrl).push(e.toString())
                v8blockstackAndroid.executeVoidFunction("executeFetchReject", v8params)
                v8params.release()
            } catch (e: Exception) {
                Log.d(TAG, "on  executeFetchReject", e)
            }
        }

        fun setLocation(location: String) {
            blockstackSession.executor.onMainThread {
                val locationUri = Uri.parse(location)
                if (shouldLaunchInCustomTabs) {
                    val builder = CustomTabsIntent.Builder()
                    val options = BitmapFactory.Options()
                    options.outWidth = 24
                    options.outHeight = 24
                    options.inScaled = true
                    val backButton = BitmapFactory.decodeResource(it.resources, R.drawable.ic_arrow_back, options)
                    builder.setCloseButtonIcon(backButton)
                    builder.setToolbarColor(ContextCompat.getColor(it, R.color.org_blockstack_purple_50_logos_types))
                    builder.setToolbarColor(ContextCompat.getColor(it, R.color.org_blockstack_purple_85_lines))
                    builder.setShowTitle(true)
                    val customTabsIntent = builder.build()
                    customTabsIntent.launchUrl(it, locationUri)
                } else {
                    it.startActivity(Intent(Intent.ACTION_VIEW, locationUri).addCategory(Intent.CATEGORY_BROWSABLE))
                }
            }
        }
    }

    companion object {
        /**
         * Flag indicating whether the authentication flow should be started in custom tabs.
         * Defaults to true.
         *
         * Set this to false only if you can't use Verified App Links.
         */
        var shouldLaunchInCustomTabs = true

        /**
         * Flag indicating that verified app links should not be checked for correct configuration
         */
        var doNotVerifyAppLinkConfiguration = false

        const val TAG = "BlockstackSession"

        fun verifyAuthResponse(authResponse: String): String? {
            try {
                val tokenParts = authResponse.split('.')
                if (tokenParts.size != 3) {
                    return "The authResponse parameter is an invalid base64 encoded token\n2 dots requires\nAuth response: $authResponse"
                }
                val decodedToken = Base64.decode(tokenParts[0], Base64.DEFAULT)
                val stringToken = decodedToken.toString(Charsets.UTF_8)
                val jsonToken = JSONObject(stringToken)
                if (jsonToken.getString("typ") != "JWT") {
                    return "The authResponse parameter is an invalid base64 encoded token\nHeader not of type JWT:${jsonToken.getString("typ")}\n Auth response: $authResponse"
                }
            } catch (e: IllegalArgumentException) {
                val error = "The authResponse parameter is an invalid base64 encoded token\n${e.message}\nAuth response: $authResponse"
                Log.w(TAG, IllegalArgumentException(error, e))
                return error
            } catch (e: JSONException) {
                val error = "The authResponse parameter is an invalid json token\n${e.message}\nAuth response: $authResponse"
                Log.w(TAG, IllegalArgumentException(error, e))
                return error
            }
            return null
        }
    }
}

private val zuluFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ", Locale.US)
private fun Date.toZuluTime(): String {
    return zuluFormatter.format(this)
}

/**
 * Executor defines where functions are executed. Three different cases are distinguished:
 * 1. main thread: to start an intent launching the login process. This has to be the UI thread.
 * 1. network thread: to make network calls. This must not be the UI thread, it is usually a thread from CommonPool.
 * 1. v8 thread: to continue after network calls. This must be on the thread that is currently used by the j2v8 engine.
 */
interface Executor {
    fun onMainThread(function: (Context) -> Unit)
    fun onNetworkThread(function: suspend () -> Unit)
    fun onV8Thread(function: () -> Unit)
}

/**
 * Standard executor for using Blockstack session in an activity.
 */
class AndroidExecutor(private val ctx: Context) : Executor {
    private val TAG = AndroidExecutor::class.simpleName
    override fun onMainThread(function: (ctx: Context) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            function(ctx)
        }
    }

    override fun onV8Thread(function: () -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            function()
        }
    }

    override fun onNetworkThread(function: suspend () -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                function()
            } catch (e: Exception) {
                Log.e(TAG, "onNetworkThread", e)
            }

        }
    }

}

/**
 * Repository to access script files used for this SDK
 * Application developers should use {@Link AndroidScriptRepo}
 */
interface ScriptRepo {
    fun globals(): String
    fun blockstack(): String
    fun base64(): String
    fun zoneFile(): String
    fun blockstackAndroid(): String

}

/**
 * Repository that provides script files for this SDK from the resources
 *
 * @param context can also be the application context
 */
class AndroidScriptRepo(private val context: Context) : ScriptRepo {
    override fun globals() = context.resources.openRawResource(R.raw.org_blockstack_globals).bufferedReader().use { it.readText() }
    override fun blockstack() = context.resources.openRawResource(R.raw.org_blockstack_blockstack).bufferedReader().use { it.readText() }
    override fun base64() = context.resources.openRawResource(R.raw.org_blockstack_base64).bufferedReader().use { it.readText() }
    override fun zoneFile() = context.resources.openRawResource(R.raw.org_blockstack_zone_file).bufferedReader().use { it.readText() }
    override fun blockstackAndroid() = context.resources.openRawResource(R.raw.org_blockstack_blockstack_android).bufferedReader().use { it.readText() }
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
