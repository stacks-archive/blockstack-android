package org.blockstack.android.sdk

import android.util.Log
import com.colendi.ecies.EncryptedResultForm
import com.colendi.ecies.Encryption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import me.uport.sdk.core.toBase64UrlSafe
import me.uport.sdk.jwt.JWTSignerAlgorithm
import me.uport.sdk.jwt.model.ArbitraryMapSerializer
import me.uport.sdk.jwt.model.JwtHeader
import me.uport.sdk.signer.KPSigner
import okhttp3.*
import okio.ByteString
import org.blockstack.android.sdk.model.*
import org.json.JSONArray
import org.json.JSONObject
import org.kethereum.crypto.CryptoAPI
import org.kethereum.crypto.SecureRandomUtils
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.signMessageHash
import org.kethereum.crypto.toECKeyPair
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.hashes.sha256
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey
import org.kethereum.model.PublicKey
import org.kethereum.model.SignatureData
import org.komputing.khex.extensions.hexToByteArray
import org.komputing.khex.extensions.toNoPrefixHexString
import java.lang.Integer.parseInt
import java.math.BigInteger
import java.security.InvalidParameterException
import java.security.SignatureException
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.ln
import kotlin.math.log10

class BlockstackSession(private val sessionStore: SessionStore, private val appConfig: BlockstackConfig? = null,
                        private val callFactory: Call.Factory = OkHttpClient(), val blockstack: Blockstack) {

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
     * @param signInCallback called with the user data after sign-in or with an error
     *
     */
    suspend fun handlePendingSignIn(authResponse: String, signInCallback: (Result<UserData>) -> Unit) {
        val transitKey = sessionStore.getTransitPrivateKey()
        val nameLookupUrl = sessionStore.sessionData.json.optString("core-node", "https://core.blockstack.org")

        val tokenTriple = try {
            blockstack.decodeToken(authResponse)
        } catch (e: IllegalArgumentException) {
            signInCallback(Result(null, ResultError(ErrorCode.LoginFailedError, "The authResponse parameter is an invalid base64 encoded token\n" +
                    "2 dots requires\n" +
                    "Auth response: $authResponse")))
            return
        }
        val tokenPayload = tokenTriple.second
        val isValidToken = blockstack.verifyToken(authResponse)

        if (!isValidToken) {
            signInCallback(Result(null, ResultError(ErrorCode.LoginFailedError, "invalid auth response")))
            return
        }
        val appPrivateKey = decrypt(tokenPayload.getString("private_key"), transitKey)
        val coreSessionToken = decrypt(tokenPayload.optString("core_token"), transitKey)

        val userData = authResponseToUserData(tokenPayload, nameLookupUrl, appPrivateKey, coreSessionToken, authResponse)

        this.appPrivateKey = appPrivateKey
        sessionStore.sessionData.json.put("userData", userData.json)

        signInCallback(Result(userData))
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
            val fetchProfileJson = CoroutineScope(Dispatchers.IO).async {
                val request = Request.Builder().url(profileUrl)
                        .build()
                val response = callFactory.newCall(request).execute()
                if (response.isSuccessful) {
                    val profiles = JSONArray(response.body()!!.string())
                    if (profiles.length() > 0) {
                        profiles.getJSONObject(0)
                    } else {
                        JSONObject()
                    }
                } else {
                    Log.d(TAG, "invalid profile url $profileUrl: ${response.code()}")
                    JSONObject()
                }
            }
            return fetchProfileJson.await()
        }
        return tokenPayload.getJSONObject("profile")
    }

    private fun decrypt(cipherObjectHex: String, privateKey: String): String? {
        if (cipherObjectHex.isEmpty() || cipherObjectHex === "null") {
            return null
        }
        return blockstack.decryptContent(cipherObjectHex.hexToByteArray().toString(Charsets.UTF_8), false,
                CryptoOptions(privateKey = privateKey)
        ).value as String
    }

    suspend fun connectToGaia(gaiaHubUrl: String,
                              challengeSignerHex: String,
                              associationToken: String?): GaiaHubConfig {

        val builder = Request.Builder()
                .url("${gaiaHubUrl}/hub_info")
        builder.addHeader("Referrer-Policy", "no-referrer")

        val response = callFactory.newCall(builder.build()).execute()

        val hubInfo = JSONObject(response.body()!!.string())

        val readURL = hubInfo.getString("read_url_prefix")
        val token = makeV1GaiaAuthToken(hubInfo, challengeSignerHex, gaiaHubUrl, associationToken)
        val address = PrivateKey(challengeSignerHex).toECKeyPair().toBtcAddress()

        return GaiaHubConfig(readURL, address, token, gaiaHubUrl)

    }


    /**
     *  @ignore
     */
    suspend fun getOrSetLocalGaiaHubConnection(): GaiaHubConfig {
        val sessionData = sessionStore.sessionData
        val userData = sessionData.json.optJSONObject("userData")
                ?: throw IllegalStateException("Missing userData")
        val hubConfig = userData.optJSONObject("gaiaHubConfig")
        if (hubConfig != null) {
            val config = GaiaHubConfig(hubConfig.getString("url_prefix"), hubConfig.getString("address"),
                    hubConfig.getString("token"),
                    hubConfig.getString("server"))
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

        if (userData == null) {
            throw IllegalStateException("Missing userData")
        }

        if (userData.hubUrl == null) {
            userData.json.put("hubUrl", BLOCKSTACK_DEFAULT_GAIA_HUB_URL)
        }

        val gaiaConfig = connectToGaia(
                userData.hubUrl,
                userData.appPrivateKey,
                userData.json.optStringOrNull("gaiaAssociationToken"))

        userData.json.put("gaiaHubConfig", gaiaConfig)
        val sessionData = sessionStore.sessionData.json
        sessionData.put("userData", userData.json)
        sessionStore.sessionData = SessionData(sessionData)
        gaiaHubConfig = gaiaConfig
        return gaiaConfig
    }

    /**
     *
     * @param hubInfo
     * @param signerKeyHex
     * @param hubUrl
     * @param associationToken
     *
     * @ignore
     */
    suspend fun makeV1GaiaAuthToken(hubInfo: JSONObject,
                                    signerKeyHex: String,
                                    hubUrl: String,
                                    associationToken: String?): String {

        val challengeText = hubInfo.getString("challenge_text")
        val handlesV1Auth = hubInfo.optString("latest_auth_version")?.substring(1)?.let {
            parseInt(it, 10) >= 1
        } ?: false

        val iss = PrivateKey(signerKeyHex).toECKeyPair().toHexPublicKey64()

        if (!handlesV1Auth) {
            throw NotImplementedError("only v1 auth supported, please upgrade your gaia server")
        }

        val saltArray = ByteArray(16) { 0 }
        SecureRandomUtils.secureRandom().nextBytes(saltArray)
        val salt = saltArray.toNoPrefixHexString()
        // {"gaiaChallenge":"[\"gaiahub\",\"0\",\"storage2.blockstack.org\",\"blockstack_storage_please_sign\"]","hubUrl":"https://hub.blockstack.org","iss":"024634ee1d4ff57f2e0ec7a847e1705ec562949f84a83d1f5fdb5956220a9775e0","salt":"c3b9b4aefad343f204bd95c2fa1ae0a4"}
        val payload = mapOf("gaiaChallenge" to challengeText,
                "hubUrl" to hubUrl,
                "iss" to iss,
                "salt" to salt)

        val header = JwtHeader(alg = JwtHeader.ES256K)
        val serializedPayload = Json(JsonConfiguration.Stable)
                .stringify(ArbitraryMapSerializer, payload)
        Log.d(TAG, header.toJson() + ", " + serializedPayload)
        val signingInput = listOf(header.toJson(), serializedPayload)
                .map { it.toBase64UrlSafe() }
                .joinToString(".")

        val jwtSigner = JWTSignerAlgorithm(header)
        val signature: String = jwtSigner.sign(signingInput, KPSigner(signerKeyHex))
        val token = listOf(signingInput, signature).joinToString(".")
        Log.d(TAG, token)
        return "v1:${token}"
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
    suspend fun getFile(path: String, options: GetFileOptions, callback: (Result<Any>) -> Unit) {
        Log.d(TAG, "getFile: path: ${path} options: ${options}")

        val getRequest = buildGetRequest(path, gaiaHubConfig!!)

        withContext(Dispatchers.IO) {
            try {
                val response = callFactory.newCall(getRequest).execute()
                Log.d(TAG, "get2" + response.toString())

                if (!response.isSuccessful) {
                    callback(Result(null, ResultError(ErrorCode.UnknownError, "Error when loading from Gaia hub, status:" + response.code())))
                    return@withContext
                }
                val contentType = response.header("Content-Type")

                var result: Any? = null
                if (options.decrypt) {
                    val responseJSON = JSONObject(response.body()!!.string())

                    val cipherObject = if (options.verify) {
                        handleSignedEncryptedContent(responseJSON)
                    } else {
                        CipherObject(responseJSON)
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
                        response.body()?.string()
                    } else {
                        response.body()?.bytes()
                    }

                    if (options.verify) {
                        val signatureRequest = buildGetRequest("$path.sig", gaiaHubConfig!!)
                        val response = callFactory.newCall(signatureRequest).execute()
                        if (response.isSuccessful) {
                            val signature = JSONObject(response.body()!!.string())
                            // TODO verify signature
                            Log.d(TAG, signature.toString())
                        } else {
                            callback(Result(null, ResultError(ErrorCode.SignatureVerificationError, "Failed to verify signature: Failed to obtain signature for file: $path")))
                            return@withContext
                        }
                    }
                }

                if (result !== null) {
                    callback(Result(result))
                } else {
                    callback(Result(null, ResultError(ErrorCode.UnknownError, "invalid response from getFile")))
                }
            } catch (e: Exception) {
                Log.d(TAG, e.message, e)
                callback(Result(null, ResultError(ErrorCode.UnknownError, e.message
                        ?: e.toString())))
            }

        }
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
    suspend fun putFile(path: String, content: Any, options: PutFileOptions, callback: (Result<String>) -> Unit) {
        Log.d(TAG, "putFile: path: ${path} options: ${options}")
        val valid = content is String || content is ByteArray
        if (!valid) {
            throw IllegalArgumentException("putFile content only supports String or ByteArray")
        }

        val contentType: String
        val requestContent = if (options.encrypt) {

            contentType = "application/json"

            val enc = Encryption()
            val appPrivateKeyPair = PrivateKey(appPrivateKey!!).toECKeyPair()
            val publicKey = appPrivateKeyPair.toHexPublicKey64()
            val contentByteArray = if (content is String) {
                content.toByteArray()
            } else {
                content as ByteArray
            }
            val result =
                    enc.encryptWithPublicKey(contentByteArray, publicKey)
            val jsonString = CipherObject(result.iv, result.ephemPublicKey, result.ciphertext, result.mac, content is String)
                    .json.toString()

            if (!options.shouldSign()) {
                ByteString.encodeUtf8(jsonString)
            } else {
                val signatureObject = signContent(jsonString, getSignKey(options))
                val signedCipherObject = JSONObject()
                        .put("signature", signatureObject.signature)
                        .put("publicKey", signatureObject.publicKey)
                        .put("cipherText", jsonString)

                ByteString.encodeUtf8(signedCipherObject.toString())
            }
        } else {
            contentType = options.contentType ?: if (content is String) {
                "text/plain"
            } else {
                "application/octet-stream"
            }
            if (content is String) {
                ByteString.encodeUtf8(content)
            } else {
                ByteString.of(content as ByteArray, 0, content.size)
            }
        }

        val putRequest = buildPutRequest(path, requestContent, contentType, gaiaHubConfig!!)
        withContext(Dispatchers.IO) {
            try {
                val response = callFactory.newCall(putRequest).execute()
                Log.d(TAG, "put2 $response")

                if (!response.isSuccessful) {
                    throw Error("Error when uploading to Gaia hub")
                }
                val responseText = response.body()?.string()
                if (responseText !== null) {
                    val responseJSON = JSONObject(responseText)

                    if (!options.encrypt && options.shouldSign()) {
                        val signedContent = signContent(requestContent.toByteArray(), getSignKey(options))
                        val putSignatureRequest = buildPutRequest("$path.sig", signedContent.toJSONString(), "application/json", gaiaHubConfig!!)
                        val responseSignature = callFactory.newCall(putSignatureRequest).execute()
                        Log.d(TAG, "put2signature" + responseSignature.toString())
                    }
                    callback(Result(responseJSON.getString("publicURL")))
                } else {
                    callback(Result(null, ResultError(ErrorCode.UnknownError, "invalid response from putFile $responseText")))
                }
            } catch (e: Exception) {
                Log.d(TAG, e.message, e)
                callback(Result(null, ResultError(ErrorCode.UnknownError, e.message
                        ?: e.toString())))
            }

        }
    }

    private fun getSignKey(options: PutFileOptions): String {
        return if (options.sign is Boolean) {
            appPrivateKey!!
        } else {
            options.sign as String
        }
    }

    private fun signContent(content: Any, privateKey: String): SignatureObject {
        val contentBuffer = if (content is ByteArray) {
            content
        } else {
            (content as String).toByteArray()
        }
        val keyPair = PrivateKey(privateKey.hexToBigInteger()).toECKeyPair()
        val sigData = signMessageHash(contentBuffer.sha256(), keyPair, false)

        val signatureString = sigData.toDER()

        return SignatureObject(signatureString,
                keyPair.toHexPublicKey64()
        )
    }


    private fun handleSignedEncryptedContent(responseJSON: JSONObject): CipherObject {
        val signature = responseJSON.getString("signature")
        val signerPublicKey = responseJSON.getString("publicKey")
        val cipherText = responseJSON.getString("cipherText")

        val signerAddress = signerPublicKey.toBtcAddress()

        val contentHash = cipherText.toByteArray().sha256()

        val keyPair = ECKeyPair(PrivateKey(BigInteger.ZERO), PublicKey(signerPublicKey))
        keyPair.verify(contentHash, signature)

        return CipherObject(JSONObject(cipherText))
    }

    private fun buildPutRequest(path: String, content: ByteString, contentType: String, hubConfig: GaiaHubConfig): Request {
        val url = "${hubConfig.server}/store/${hubConfig.address}/${path}"

        val builder = Request.Builder()
                .url(url)
        builder.method("POST", RequestBody.create(MediaType.get(contentType), content))
        builder.addHeader("Content-Type", contentType)
        builder.addHeader("Authorization", "bearer ${hubConfig.token}")
        builder.addHeader("Referrer-Policy", "no-referrer")
        return builder.build()
    }

    private fun buildGetRequest(path: String, hubConfig: GaiaHubConfig): Request {
        val url = "${hubConfig.urlPrefix}${hubConfig.address}/${path}"
        val builder = Request.Builder()
                .url(url)
        builder.addHeader("Referrer-Policy", "no-referrer")
        return builder.build()
    }


    suspend fun deleteFile(path: String, options: DeleteFileOptions = DeleteFileOptions(), callback: (Result<Unit>) -> Unit) {
        val deleteRequest = buildDeleteRequest(path, gaiaHubConfig!!)

        withContext(Dispatchers.IO) {
            try {
                val response = callFactory.newCall(deleteRequest).execute()
                if (response.isSuccessful) {
                    callback(Result(null))
                } else {
                    callback(Result(null, ResultError(ErrorCode.UnknownError, "Failed to delete file: ${response.code()}")))
                }
            } catch (e: Exception) {
                Log.d(TAG, e.message, e)
                callback(Result(null, ResultError(ErrorCode.UnknownError, e.message
                        ?: e.toString())))
            }
        }
    }

    private fun buildDeleteRequest(filename: String, hubConfig: GaiaHubConfig): Request {
        val url = "${hubConfig.server}/delete/${hubConfig.address}/${filename}"
        val builder = Request.Builder()
                .url(url)
        builder.method("DELETE", null)
        builder.header("Authorization", "bearer ${hubConfig.token}")
        return builder.build()
    }


    /**
     * List the set of files in this application's Gaia storage bucket.
     * @param {function} callback - a callback to invoke on each named file that
     * returns `true` to continue the listing operation or `false` to end it
     * @return {Promise} that resolves to the number of files listed
     */
    suspend fun listFiles(callback: (String) -> Boolean): Result<Int> {
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
     * @param callback
     * @returns {Promise<string>} that resolves to the URL or rejects with an error
     */
    suspend fun getFileUrl(path: String, options: GetFileOptions): Result<String> {

        val readUrl: String?
        if (options.username != null) {
            readUrl = blockstack.getUserAppFileUrl(path, options.username,
                    options.app ?: appConfig?.appDomain.toString(),
                    options.zoneFileLookupURL?.toString())
        } else {
            val gaiaHubConfig = getOrSetLocalGaiaHubConnection()
            readUrl = getFullReadUrl(path, gaiaHubConfig)
        }

        if (readUrl == null) {
            return Result(null, ResultError(ErrorCode.UnknownError, "Missing readURL"))
        } else {
            return Result(readUrl)
        }
    }

    private fun getFullReadUrl(filename: String, hubConfig: GaiaHubConfig): String? {
        return "${hubConfig.urlPrefix}${hubConfig.address}/${filename}"
    }

    private suspend fun listFilesLoop(callback: (String) -> Boolean, page: String?, callCount: Int, fileCount: Int): Int {
        if (callCount > 65536) {
            throw RuntimeException("Too many entries to list")
        }

        val request = buildListFilesRequest(page, getHubConfig())
        val response = withContext(Dispatchers.IO) {
            callFactory.newCall(request).execute()
        }
        if (!response.isSuccessful) {
            if (callCount == 0) {
                // TODO reconnect
                throw NotImplementedError("reconnect to gaia ${response.code()}")
            } else {
                throw IOException("call to list-files failed ${response.code()}")
            }
        } else {
            val responseJson = JSONObject(response.body()!!.string())
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
                val shouldContinue = callback(fileEntries.getString(index))
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

    private fun getHubConfig(): GaiaHubConfig {
        return gaiaHubConfig ?: throw RuntimeException("not connected to gaia")
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

                .method("POST", RequestBody.create(MediaType.get(CONTENT_TYPE_JSON), pageRequest))
                .build()
    }

    fun isUserSignedIn(): Boolean {
        return appPrivateKey != null
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
        sessionStore.sessionData.json.remove("userData")
        appPrivateKey = null
    }

    companion object {
        val TAG = BlockstackSession::class.java.simpleName
        val CONTENT_TYPE_JSON = "application/json"
    }
}

private fun ECKeyPair.verify(contentHash: ByteArray, signature: String) {
    val sig: SignatureData = signature.fromDER()
    val recId = sig.v.toByteArray().last() - 27

}

data class Position(var place:Int)

fun String.fromDER(): SignatureData {
    val data = this.hexToByteArray()
    val p = Position(0)
    if (data[p.place++] != 0x30.toByte()) {
        throw InvalidParameterException()
    }
    val len = getLength(data, p);
    if ((len + p.place) != data.size) {
        throw InvalidParameterException()
    }
    if (data[p.place++] != 0x02.toByte()) {
        throw InvalidParameterException()
    }
    val rlen = getLength(data, p);
    var r = data.sliceArray(p.place until rlen + p.place);
    p.place += rlen;
    if (data[p.place++] != 0x02.toByte()) {
        throw InvalidParameterException()
    }
    val slen = getLength(data, p);
    if (data.size != slen + p.place) {
        throw InvalidParameterException()
    }
    var s = data.sliceArray(p.place until slen + p.place);
    if (r[0] == ZERO && (r[1] and LENGTH) != ZERO) {
        r = r.sliceArray(1 until r.size)
    }
    if (s[0] == ZERO && (s[1] and LENGTH) != ZERO) {
        s = s.sliceArray(1 until s.size)
    }

    return SignatureData(BigInteger(r), BigInteger(s), BigInteger.ZERO)
}

const val ZERO = 0.toByte()
const val LENGTH = 0x80.toByte() // 128


fun addSize(arr: MutableList<Byte>, len: Int) {
    if (len < 128) {
        arr.add(len.toByte())
        return
    }
    val l = (log10(len.toDouble()) / ln(2.toDouble())).toInt() ushr 3
    var octets = 1 + l
    arr.add(octets.toByte() or LENGTH)
    octets -= 1
    while (octets.toByte() != ZERO) {
        arr.add(((len ushr (octets shl 3)) and 0xff).toByte())
    }
    arr.add(len.toByte())
}


fun getLength(buf:ByteArray, p:Position): Byte {
    val initial = buf[p.place++]
    if (initial and LENGTH == ZERO) {
        return initial;
    }
    val octetLen = initial and 0xf.toByte()
    var value = 0
    var off = p.place;
    for (i in 0 until octetLen) {
        value = value shl 8
        value = value or buf[off].toInt()
        off +=1
    }
    p.place = off
    return value.toByte()
}

fun rmPadding(buf: ByteArray): ByteArray {
    var i = 0
    val len = buf.size - 1
    while (buf[i] == ZERO && (buf[i + 1] and LENGTH) == ZERO && i < len) {
        i++
    }
    if (i == 0) {
        return buf
    }
    return buf.sliceArray(i until buf.size)
}

fun SignatureData.toDER(): String {
    var r = this.r.toByteArray()
    var s = this.s.toByteArray()

    // Pad values
    if (r[0] and LENGTH != ZERO) {
        r = byteArrayOf(0) + r
    }
    // Pad values
    if (s[0] and LENGTH != ZERO) {
        s = byteArrayOf(0) + s
    }

    r = rmPadding(r)
    s = rmPadding(s)

    while (s[0] == ZERO && (s[1] and LENGTH) == ZERO) {
        s = s.sliceArray(1 until s.size)
    }
    val arr = mutableListOf<Byte>(0x02)
    addSize(arr, r.size)
    arr.addAll(r.toTypedArray())

    arr.add(0x02.toByte())
    addSize(arr, s.size)
    arr.addAll(s.toTypedArray())

    var res = mutableListOf<Byte>(0x30)
    addSize(res, arr.size)
    res.addAll(arr)

    return res.toNoPrefixHexString()
}

fun JSONObject.optStringOrNull(name: String): String? {
    if (isNull(name)) {
        return null
    } else {
        return optString(name)
    }
}
