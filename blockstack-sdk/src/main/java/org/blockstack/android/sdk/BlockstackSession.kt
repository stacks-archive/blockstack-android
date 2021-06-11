package org.blockstack.android.sdk

import android.util.Log
import com.colendi.ecies.EncryptedResultForm
import com.colendi.ecies.Encryption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.uport.sdk.core.hexToByteArray
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import org.blockstack.android.sdk.ecies.signContent
import org.blockstack.android.sdk.ecies.signEncryptedContent
import org.blockstack.android.sdk.ecies.verify
import org.blockstack.android.sdk.model.*
import org.json.JSONArray
import org.json.JSONObject
import org.kethereum.crypto.toECKeyPair
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey
import org.kethereum.model.PublicKey
import org.komputing.khash.sha256.extensions.sha256
import org.komputing.khex.model.HexString
import java.io.IOException
import java.math.BigInteger
import java.security.InvalidParameterException
import java.util.*

const val SIGNATURE_FILE_EXTENSION = ".sig"

class BlockstackSession(private val sessionStore: ISessionStore, private val appConfig: BlockstackConfig? = null,
                        private val callFactory: Call.Factory = OkHttpClient(),
                        val blockstack: Blockstack = Blockstack(),
                        val hub: Hub = Hub(callFactory),
                        val dispatcher: CoroutineDispatcher = Dispatchers.IO) {

    private var appPrivateKey: String?
    var gaiaHubConfig: GaiaHubConfig? = null

    init {
        val appPrivateKey = sessionStore.sessionData.json.optJSONObject("userData")?.optString("appPrivateKey")
        this.appPrivateKey = appPrivateKey
    }


    /**
     * Process a pending sign in. This method should be called by your app when it
     * receives a request to the app's custom protocol handler.
     *
     * @param authResponse authentication response token
     * @return result object with the user data after sign-in or with an error
     *
     */
    suspend fun handlePendingSignIn(authResponse: String): Result<out UserData> = withContext(dispatcher) {
        val transitKey = sessionStore.getTransitPrivateKey()
        val nameLookupUrl = sessionStore.sessionData.json.optString("core-node", "https://core.blockstack.org")

        val tokenTriple = try {
            blockstack.decodeToken(authResponse)
        } catch (e: IllegalArgumentException) {
            return@withContext Result(null, ResultError(ErrorCode.LoginFailedError, "The authResponse parameter is an invalid base64 encoded token\n" +
                    "2 dots requires\n" +
                    "Auth response: $authResponse"))
        }
        val tokenPayload = tokenTriple.second
        val isValidToken = blockstack.verifyToken(authResponse)

        if (!isValidToken) {
            return@withContext Result(null, ResultError(ErrorCode.LoginFailedError, "invalid auth response"))
        }
        val appPrivateKey = decrypt(tokenPayload.getString("private_key"), transitKey)

        if (appPrivateKey == null) {
            return@withContext Result(null, ResultError(ErrorCode.LoginFailedError, "auth response used different transient key"))
        }

        val coreSessionToken = decrypt(tokenPayload.optString("core_token"), transitKey)

        val userData = authResponseToUserData(tokenPayload, nameLookupUrl, appPrivateKey, coreSessionToken, authResponse)

        this@BlockstackSession.appPrivateKey = appPrivateKey
        sessionStore.updateUserData(userData)

        return@withContext Result(userData)
    }

    suspend fun handleUnencryptedSignIn(authResponse: String): Result<UserData> {
        val nameLookupUrl = sessionStore.sessionData.json.optString("core-node", "https://core.blockstack.org")

        val tokenTriple = blockstack.decodeToken(authResponse)
        val tokenPayload = tokenTriple.second
        try {
            val isValidToken = blockstack.verifyToken(authResponse)
            if (!isValidToken) {
                return Result(null, ResultError(ErrorCode.LoginFailedError, "invalid auth response"))
            }
        } catch (e: Exception) {
            return Result(null, ResultError(ErrorCode.LoginFailedError, "invalid auth response " + e.message))
        }

        val appPrivateKey = tokenPayload.getString("private_key")
        val coreSessionToken = tokenPayload.optString("core_token")
        val userData = authResponseToUserData(tokenPayload, nameLookupUrl, appPrivateKey, coreSessionToken, authResponse)
        sessionStore.updateUserData(userData)
        return Result(userData)
    }


    suspend fun authResponseToUserData(tokenPayload: JSONObject, nameLookupUrl: String, appPrivateKey: String?, coreSessionToken: String?, authResponse: String): UserData {
        val iss = tokenPayload.getString("iss")

        val identityAddress = DIDs.getAddressFromDID(iss)
        val userData = UserData(JSONObject()
                .put("username", tokenPayload.getString("username"))
                .put("profile", extractProfile(tokenPayload, nameLookupUrl))
                .put("email", tokenPayload.optString("email"))
                .put("decentralizedID", iss)
                .put("identityAddress", identityAddress)
                .put("appPrivateKey", appPrivateKey)
                .put("coreSessionToken", coreSessionToken)
                .put("authResponseToken", authResponse)
                .put("hubUrl", tokenPayload.optString("hubUrl", BLOCKSTACK_DEFAULT_GAIA_HUB_URL))
                .put("gaiaAssociationToken", tokenPayload.optString("associationToken")))
        return userData
    }


    private suspend fun extractProfile(tokenPayload: JSONObject, nameLookupUrl: String): JSONObject {
        val profileUrl = tokenPayload.optStringOrNull("profile_url")
        if (profileUrl != null && profileUrl.isNotBlank()) {
            return withContext(dispatcher) {
                val request = Request.Builder().url(profileUrl)
                        .build()
                val response = callFactory.newCall(request).execute()
                if (response.isSuccessful) {
                    val profiles = JSONArray(response.body!!.string())
                    if (profiles.length() > 0) {
                        profiles.getJSONObject(0)
                    } else {
                        JSONObject()
                    }
                } else {
                    Log.d(TAG, "invalid profile url $profileUrl: ${response.code}")
                    JSONObject()
                }
            }
        }
        return tokenPayload.getJSONObject("profile")
    }

    private fun decrypt(cipherObjectHex: String, privateKey: String): String? {
        if (cipherObjectHex.isEmpty() || cipherObjectHex === "null") {
            return null
        }
        return blockstack.decryptContent(cipherObjectHex.hexToByteArray().toString(Charsets.UTF_8), false,
                CryptoOptions(privateKey = privateKey)
        ).value as String?
    }

    /**
     *  @ignore
     */
    suspend fun getOrSetLocalGaiaHubConnection(): GaiaHubConfig {
        if (gaiaHubConfig != null) return gaiaHubConfig!!

        val userData = this.loadUserData()
        val hubConfig = userData.json.optJSONObject("gaiaHubConfig")
        if (hubConfig != null) {
            val config = GaiaHubConfig(
                hubConfig.getString("url_prefix"),
                hubConfig.getString("address"),
                hubConfig.getString("token"),
                hubConfig.getString("server")
            )
            gaiaHubConfig = config
            return config
        }

        return this.setLocalGaiaHubConnection()
    }

    /**
     * These two functions are app-specific connections to gaia hub,
     *   they read the user data object for information on setting up
     *   a hub connection, and store the hub config to localstorage
     * @private
     * @returns {Promise} that resolves to the new gaia hub connection
     */
    suspend fun setLocalGaiaHubConnection(): GaiaHubConfig {
        val userData = this.loadUserData()
        if (userData.json.optStringOrNull("hubUrl") == null) {
            userData.json.put("hubUrl", BLOCKSTACK_DEFAULT_GAIA_HUB_URL)
        }

        val gaiaConfig = hub.connectToGaia(
            userData.hubUrl,
            userData.appPrivateKey,
            userData.json.optStringOrNull("gaiaAssociationToken")
        )

        userData.json.put("gaiaHubConfig", gaiaConfig.toJSON())

        val sessionData = sessionStore.sessionData.json
        sessionData.put("userData", userData.json)
        sessionStore.sessionData = SessionData(sessionData)

        gaiaHubConfig = gaiaConfig
        return gaiaConfig
    }

    /**
     * Retrieves the specified file from the app's data store.
     * The method is called with `Dispatchers.IO`
     *
     * @property path the path of the file from which to read data
     * @property options an instance of a `GetFileOptions` object which is used to configure
     * options such as decryption and reading files from other apps or users.
     * @return a result object with the file contents or error
     */
    suspend fun getFile(path: String, options: GetFileOptions): Result<out Any> {
        Log.d(TAG, "getFile: path: $path options: $options")
        return withContext(dispatcher) {
            val urlResult = getFileUrl(path, options)
            if (urlResult.hasErrors) {
                return@withContext urlResult
            }
            val getRequest = hub.buildGetRequest(urlResult.value!!)

            val exception = kotlin.runCatching {
                val response = callFactory.newCall(getRequest).execute()

                if (!response.isSuccessful) {
                    return@withContext Result(null, ResultError(ErrorCode.NetworkError, "Error when loading from Gaia hub, status:" + response.code, response.code.toString()))
                }
                val contentType = response.header("Content-Type")

                val result: Any?
                if (options.decrypt) {
                    val responseContent = response.body!!.string()

                    val cipherObject = if (options.verify) {
                        val expectedAddress = if (options.username != null) {
                            getGaiaAddress(options.app
                                    ?: appConfig!!.appDomain.getOrigin(), options.username)
                        } else {
                            gaiaHubConfig!!.address
                        }
                        handleSignedEncryptedContent(responseContent, expectedAddress)
                    } else {
                        CipherObject(JSONObject(responseContent))
                    }

                    val decryptedContent = Encryption().decryptWithPrivateKey(EncryptedResultForm(cipherObject.ephemeralPK,
                            cipherObject.iv, cipherObject.mac, cipherObject.cipherText, appPrivateKey))
                    result = if (cipherObject.wasString) {
                        String(decryptedContent)
                    } else {
                        decryptedContent
                    }
                } else {
                    result = if (contentType === null
                            || contentType.startsWith("text")
                            || contentType == "application/json") {
                        response.body?.string()
                    } else {
                        response.body?.bytes()
                    }

                    if (options.verify) {
                        val signatureRequest = hub.buildGetRequest(hub.getFullReadUrl("$path$SIGNATURE_FILE_EXTENSION", gaiaHubConfig!!))
                        val signatureResponse = callFactory.newCall(signatureRequest).execute()
                        if (signatureResponse.isSuccessful) {
                            val signatureObject = SignatureObject.fromJSONString(signatureResponse.body!!.string())
                            val resultHash = if (result is String) {
                                result.toByteArray()
                            } else {
                                result as ByteArray
                            }.sha256()
                            val keyPair = ECKeyPair(PrivateKey(0.toBigInteger()), PublicKey(HexString(signatureObject.publicKey)))
                            if (!keyPair.verify(resultHash, signatureObject.signature)) {
                                return@withContext Result(null, ResultError(ErrorCode.SignatureVerificationError, "Failed to verify signature: Invalid signature for file: $path"))
                            } else {
                                return@withContext Result(result)
                            }

                        } else {
                            return@withContext Result(null, ResultError(ErrorCode.SignatureVerificationError, "Failed to verify signature: Failed to obtain signature for file: $path"))

                        }
                    }
                }

                if (result !== null) {
                    return@withContext Result(result)
                } else {
                    return@withContext Result(null, ResultError(ErrorCode.UnknownError, "invalid response from getFile"))
                }
            }

            val e = exception.exceptionOrNull()
            if (e != null) {
                Log.d(TAG, e.message, e)
                return@withContext Result(null, ResultError(ErrorCode.UnknownError, e.message
                        ?: e.toString()))
            } else {
                return@withContext exception.getOrNull()!!
            }

        }
    }

    suspend fun getGaiaAddress(appDomain: String, username: String): String {
        return withContext(dispatcher) {
            val fileUrl = blockstack.getUserAppFileUrl("/", username, appDomain, null)
            val address = Regex("([13][a-km-zA-HJ-NP-Z0-9]{26,35})").find(fileUrl)?.value
            return@withContext address ?: ""
        }
    }


    /**
     * Stores the data provided in the app's data store to to the file specified.
     *
     * @property path the path to store the data to
     * @property content the data to store in the file
     * @property options an instance of a `PutFileOptions` object which is used to configure
     * options such as encryption
     * @property a result object wiht a `String` representation of a url from
     * which you can read the file that was just put.
     */
    suspend fun putFile(path: String, content: Any, options: PutFileOptions): Result<out String> = withContext(dispatcher) {
        Log.d(TAG, "putFile: path: ${path} options: ${options}")
        val valid = content is String || content is ByteArray
        if (!valid) {
            throw IllegalArgumentException("putFile content only supports String or ByteArray")
        }

        val contentType: String
        val requestContent = if (options.encrypt || options.encryptionKey != null) {

            contentType = "application/json"

            val enc = Encryption()
            val publicKey = options.encryptionKey
                    ?: PrivateKey(HexString(appPrivateKey!!)).toECKeyPair().toHexPublicKey64()
            val contentByteArray = if (content is String) {
                content.toByteArray()
            } else {
                content as ByteArray
            }
            val result =
                    enc.encryptWithPublicKey(contentByteArray, publicKey)
            val jsonString = CipherObject(result.iv, result.ephemPublicKey, result.ciphertext, result.mac, content is String)
                    .json.toString()

            if (options.shouldSign()) {
                val signedCipherObject = signEncryptedContent(jsonString, getSignKey(options))
                signedCipherObject.toJSONByteString()
            } else {
                jsonString.encodeUtf8()
            }
        } else {
            contentType = options.contentType ?: if (content is String) {
                "text/plain"
            } else {
                "application/octet-stream"
            }
            if (content is String) {
                content.encodeUtf8()
            } else {
                (content as ByteArray).toByteString(0, content.size)
            }
        }


        try {
            val response = hub.uploadToGaiaHub(path, requestContent, getOrSetLocalGaiaHubConnection(), contentType)
            if (!response.isSuccessful) {
                return@withContext Result(null, ResultError(ErrorCode.NetworkError, "upload to gaia failed: ${response.code}", response.code.toString()))
            }
            val responseText = response.body?.string()
            if (responseText !== null) {
                if (!options.shouldEncrypt() && options.shouldSign()) {
                    val signedContent = signContent(requestContent.toByteArray(), getSignKey(options))
                    try {
                        val sigResponse = hub.uploadToGaiaHub("$path$SIGNATURE_FILE_EXTENSION", signedContent.toJSONByteString(), gaiaHubConfig!!, "application/json")
                        if (!sigResponse.isSuccessful) {
                            return@withContext Result(null, ResultError(ErrorCode.NetworkError, "failed to upload putFile signature $responseText", sigResponse.code.toString()))
                        }
                    } catch (e: Exception) {
                        return@withContext Result(null, ResultError(ErrorCode.UnknownError, "invalid response from putFile signature $responseText"))
                    }
                }

                val responseJSON = JSONObject(responseText)
                return@withContext Result(responseJSON.getString("publicURL"))
            } else {
                return@withContext Result(null, ResultError(ErrorCode.UnknownError, "invalid response from putFile $responseText"))
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
            return@withContext Result(null, ResultError(ErrorCode.UnknownError, e.message
                    ?: e.toString()))
        }


    }

    private fun getSignKey(options: PutFileOptions): String {
        return if (options.sign is Boolean) {
            appPrivateKey!!
        } else {
            options.sign as String
        }
    }


    private fun handleSignedEncryptedContent(responseContent: String, expectedAddress: String): CipherObject {
        val signedCipherObject = SignedCipherObject.fromJSONString(responseContent)

        val signerAddress = signedCipherObject.publicKey.toBtcAddress()
        if (signerAddress != expectedAddress) {
            throw InvalidParameterException("Unexpected signer address $signerAddress != $expectedAddress")
        }

        val contentHash = signedCipherObject.signature.toByteArray().sha256()

        val keyPair = ECKeyPair(PrivateKey(BigInteger.ZERO), PublicKey(HexString(signedCipherObject.publicKey)))
        if (keyPair.verify(contentHash, signedCipherObject.signature)) {
            return CipherObject(JSONObject(signedCipherObject.signature))
        } else {
            throw InvalidParameterException("Invalid signature")
        }
    }


    suspend fun deleteFile(path: String, options: DeleteFileOptions = DeleteFileOptions()): Result<out Unit> = withContext(dispatcher) {
        try {
            val response = hub.deleteFromGaiaHub(path, options.gaiaHubConfig ?: getOrSetLocalGaiaHubConnection())
            if (response != null) {
                if (response.isSuccessful) {
                    return@withContext Result(Unit)
                } else {
                    return@withContext Result(null, ResultError(ErrorCode.NetworkError,
                            "failed to delete $path", response.code.toString()))
                }
            } else {
                return@withContext Result(null, ResultError(ErrorCode.UnknownError, "failed to delete $path"))
            }
        } catch (e: Exception) {
            return@withContext Result(null, ResultError(ErrorCode.UnknownError,
                    "failed to delete $path: ${e.message ?: e.toString()}"))
        }
    }


    /**
     * List the set of files in this application's Gaia storage bucket.
     * @param {function} callback - a callback to invoke on each named file that
     * returns `true` to continue the listing operation or `false` to end it
     * @return {Promise} that resolves to the number of files listed
     */
    suspend fun listFiles(callback: (Result<String>) -> Boolean): Result<Int> {
        try {
            val fileCount = listFilesLoop(callback, null, 0, 0)
            return Result(fileCount)
        } catch (e: Exception) {
            return Result(null, ResultError(ErrorCode.UnknownError, e.message ?: e.toString()))
        }
    }

    /**
     * Get the URL for reading a file from an app's data store.
     * @param path  the path to the file to read
     * @param options - options object
     * @returns {Promise<string>} that resolves to the URL or rejects with an error
     */
    suspend fun getFileUrl(path: String, options: GetFileOptions): Result<String> {

        val readUrl: String = if (options.username != null) {
            blockstack.getUserAppFileUrl(path, options.username,
                    options.app ?: appConfig!!.appDomain.getOrigin(),
                    options.zoneFileLookupURL?.toString())
        } else {
            hub.getFullReadUrl(path, getOrSetLocalGaiaHubConnection())
        }

        return if (readUrl == Blockstack.NO_URL) {
            Result(value = null, error = ResultError(ErrorCode.MissingReadUrl, "${options.username} has not yet used the app"))
        } else {
            Result(readUrl)
        }
    }

    suspend fun listFilesLoop(callback: (Result<String>) -> Boolean, page: String?, callCount: Int, fileCount: Int): Int {
        if (callCount > 65536) {
            throw RuntimeException("Too many entries to list")
        }

        val response = withContext(dispatcher) {
            val request = buildListFilesRequest(page, getOrSetLocalGaiaHubConnection())
            callFactory.newCall(request).execute()
        }
        if (!response.isSuccessful) {
            if (callCount == 0) {
                // Try again one more time
                return listFilesLoop(callback, page, callCount + 1, fileCount)
            } else {
                Log.d(TAG, "Gaia's list-files error ${response.code}: ${response.body?.string()}")
                throw IOException("call to list-files failed ${response.code}")
            }
        } else {
            val responseJson = JSONObject(response.body!!.string())
            val fileEntries = responseJson.optJSONArray("entries")
            val nextPage = if (responseJson.isNull("page")) {
                null
            } else {
                responseJson.getString("page")
            }

            if (fileEntries === null) {
                // indicates a misbehaving Gaia hub or a misbehaving driver
                // (i.e. the data is malformed)
                throw RuntimeException("Bad listFiles response: no entries")
            }

            for (index in 0 until fileEntries.length()) {
                val shouldContinue = callback(Result(fileEntries.getString(index)))
                if (!shouldContinue) {
                    return fileCount + index
                }
            }
            if (nextPage != null && nextPage.isNotEmpty() && fileEntries.length() > 0) {
                return listFilesLoop(callback, nextPage, callCount + 1, fileCount + fileEntries.length())
            } else {
                return fileCount + fileEntries.length()
            }
        }
    }


    private fun buildListFilesRequest(page: String?, hubConfig: GaiaHubConfig): Request {
        val pageRequest = JSONObject().put("page", page).toString()
        Log.d(TAG, pageRequest)
        return Request.Builder()
                .url("${hubConfig.server}/list-files/${hubConfig.address}")
                .addHeader("Content-Type", CONTENT_TYPE_JSON)
                .addHeader("Content-Length", pageRequest.length.toString())
                .addHeader("Authorization", "bearer ${hubConfig.token}")
                .addHeader("Referrer-Policy", "no-referrer")

                .method("POST", pageRequest.toRequestBody(CONTENT_TYPE_JSON.toMediaType()))
                .build()
    }

    fun isUserSignedIn(): Boolean {
        return sessionStore.sessionData.json.optJSONObject("userData")?.optString("appPrivateKey") != null
    }

    fun loadUserData(): UserData {
        val userDataJson = sessionStore.sessionData.json.optJSONObject("userData")
        if (userDataJson != null) {
            return UserData(userDataJson)
        } else {
            throw IllegalStateException("No user data found. Did the user sign in?")
        }
    }

    fun signUserOut() {
        sessionStore.deleteSessionData()
        appPrivateKey = null
        gaiaHubConfig = null
    }

    fun validateProofs(profile: Profile, ownerAddress: String, optString: String?): Result<ArrayList<Proof>> {
        return Result(null, ResultError(ErrorCode.UnknownError, "Not implemented"))
    }

    fun encryptContent(content: Any, options: CryptoOptions): Result<CipherObject> {
        val updatedOptions =
                if (options.publicKey == null) {
                    val appPrivateKeyPair = PrivateKey(HexString(appPrivateKey!!)).toECKeyPair()
                    val publicKey = appPrivateKeyPair.toHexPublicKey64()
                    CryptoOptions(publicKey = publicKey)
                } else {
                    options
                }
        return blockstack.encryptContent(content, updatedOptions)
    }

    fun decryptContent(cipherObject: String, binary: Boolean, options: CryptoOptions): Result<Any> {
        val updatedOptions =
                if (options.privateKey == null) {
                    CryptoOptions(privateKey = appPrivateKey)
                } else {
                    options
                }
        return blockstack.decryptContent(cipherObject, binary, updatedOptions)
    }

    companion object {
        val TAG = BlockstackSession::class.java.simpleName
        val CONTENT_TYPE_JSON = "application/json"
    }
}

fun JSONObject.optStringOrNull(name: String): String? {
    if (isNull(name)) {
        return null
    } else {
        return optString(name)
    }
}
