package org.blockstack.android.sdk

import android.util.Base64
import android.util.Log
import com.colendi.ecies.EncryptedResultForm
import com.colendi.ecies.Encryption
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import me.uport.sdk.core.toBase64UrlSafe
import me.uport.sdk.jwt.JWTSignerAlgorithm
import me.uport.sdk.jwt.JWTTools
import me.uport.sdk.jwt.model.ArbitraryMapSerializer
import me.uport.sdk.jwt.model.JwtHeader
import me.uport.sdk.jwt.model.JwtHeader.Companion.ES256K
import me.uport.sdk.signer.KPSigner
import okhttp3.*
import org.blockstack.android.sdk.model.*
import org.json.JSONObject
import org.kethereum.crypto.SecureRandomUtils
import org.kethereum.crypto.getCompressedPublicKey
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toECKeyPair
import org.kethereum.encodings.encodeToBase58String
import org.kethereum.extensions.toHexStringNoPrefix
import org.kethereum.hashes.ripemd160
import org.kethereum.hashes.sha256
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey
import org.komputing.khex.extensions.hexToByteArray
import org.komputing.khex.extensions.toNoPrefixHexString
import java.lang.Integer.parseInt
import java.security.InvalidParameterException
import java.security.SecureRandom
import java.util.*
import kotlin.math.exp

class BlockstackSession2(sessionStore: SessionStore, private val executor: Executor, private val appConfig: BlockstackConfig?,
                         private val callFactory: Call.Factory = OkHttpClient()) {

    private var appPrivateKey: String?
    var gaiaHubConfig: GaiaHubConfig? = null
    private val secureRandom = SecureRandom()
    private val jwtTools = JWTTools()

    init {
        val appPrivateKey = sessionStore.sessionData.json.getJSONObject("userData").getString("appPrivateKey")
        this.appPrivateKey = appPrivateKey
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
    suspend fun makeAuthRequest(transitPrivateKey: String, expiresAt: Long, extraParams: Map<String, Any>): String {

        val domainName = appConfig!!.appDomain.toString()
        val manifestUrl = "${domainName}${appConfig.manifestPath}"
        val redirectUrl = "${domainName}${appConfig.redirectPath}"
        val transitKeyPair = PrivateKey(transitPrivateKey).toECKeyPair()
        val btcAddress = transitKeyPair.toBtcAddress()
        val issuerDID = "did:btc-addr:${btcAddress}"
        val payload = mapOf(
                "jti" to UUID.randomUUID(),
                "iat" to Date().time/1000,
                "exp" to expiresAt / 1000,
                "iss" to issuerDID,
                "public_keys" to arrayOf(transitKeyPair.toHexPublicKey64()),
                "domain_name" to domainName ,
                "manifest_uri" to manifestUrl,
                "redirect_uri" to redirectUrl,
                "version" to "1.3.1",
                "do_not_include_profile" to true,
                "supports_hub_url" to true,
                "scopes" to appConfig.scopes.map{it.name}


        )
        return JWTTools().createJWT(payload, issuerDID, KPSigner(transitPrivateKey), algorithm = ES256K)
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

        val iss = "02" + PrivateKey(signerKeyHex).toECKeyPair().publicKey.key.toHexStringNoPrefix().slice(0..63)

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
    fun getFile(path: String, options: GetFileOptions, callback: (Result<Any>) -> Unit) {
        Log.d(TAG, "getFile: path: ${path} options: ${options}")

        val getRequest = buildGetRequest(path, options, gaiaHubConfig!!)
        executor.onNetworkThread {
            try {
                val response = callFactory.newCall(getRequest).execute()
                Log.d(TAG, "get2" + response.toString())

                if (!response.isSuccessful) {
                    throw Error("Error when loading from Gaia hub, status:" + response.code())
                }
                val contentType = response.header("Content-Type")

                var result: Any? = null
                if (options.decrypt) {
                    val responseJSON = JSONObject(response.body()!!.string())
                    val cipherObject = CipherObject(responseJSON)

                    result = Encryption().decryptWithPrivateKey(EncryptedResultForm(cipherObject.ephemeralPK,
                            cipherObject.iv, cipherObject.mac, cipherObject.cipherText, appPrivateKey))
                } else {
                    result = if (contentType === null
                            || contentType.startsWith("text")
                            || contentType === "application/json") {
                        response.body()?.string()
                    } else {
                        response.body()?.bytes()
                    }
                }

                if (result !== null) {
                    callback(Result(result))
                } else {
                    callback(Result(null, "invalid response from getFile"))
                }
            } catch (e: Exception) {
                Log.d(TAG, e.message, e)
                callback(Result(null, e.message))
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
    fun putFile(path: String, content: Any, options: PutFileOptions, callback: (Result<String>) -> Unit) {
        Log.d(TAG, "putFile: path: ${path} options: ${options}")
        val valid = content is String || content is ByteArray
        if (!valid) {
            throw IllegalArgumentException("putFile content only supports String or ByteArray")
        }

        val requestContent = if (options.encrypt) {
            val enc = Encryption()
            val appPrivateKeyPair = PrivateKey(appPrivateKey!!).toECKeyPair()
            val publicKey = appPrivateKeyPair.toHexPublicKey64()
            val result = enc.encryptWithPublicKey(content as String, publicKey)
            CipherObject(result.iv, result.ephemPublicKey, result.ciphertext, result.mac, content is String)
                    .json.toString()
        } else {
            content as String
        }

        val putRequest = buildPutRequest(path, requestContent, options, gaiaHubConfig!!)
        executor.onNetworkThread {
            try {
                val response = callFactory.newCall(putRequest).execute()
                Log.d(TAG, "put2" + response.toString())

                if (!response.isSuccessful) {
                    throw Error("Error when uploading to Gaia hub")
                }
                val responseText = response.body()?.string()
                if (responseText !== null) {
                    val responseJSON = JSONObject(responseText)

                    callback(Result(responseJSON.getString("publicURL")))
                } else {
                    callback(Result(null, "invalid response from putFile $responseText"))
                }
            } catch (e: Exception) {
                Log.d(TAG, e.message, e)
                callback(Result(null, e.message))
            }

        }
    }

    private fun buildPutRequest(path: String, content: String, options: PutFileOptions, hubConfig: GaiaHubConfig): Request {
        val url = "${hubConfig.server}/store/${hubConfig.address}/${path}"

        val contentType = options.contentType ?: "application/octet-stream"
        val builder = Request.Builder()
                .url(url)
        builder.method("POST", RequestBody.create(MediaType.get(contentType), content))
        builder.addHeader("Content-Type", contentType)
        builder.addHeader("Authorization", "bearer ${hubConfig.token}")
        builder.addHeader("Referrer-Policy", "no-referrer")
        return builder.build()
    }

    private fun buildGetRequest(path: String, options: GetFileOptions, hubConfig: GaiaHubConfig): Request {
        val url = "${hubConfig.urlPrefix}${hubConfig.address}/${path}"
        val builder = Request.Builder()
                .url(url)
        builder.addHeader("Referrer-Policy", "no-referrer")
        return builder.build()
    }



    fun deleteFile(path: String, options: DeleteFileOptions = DeleteFileOptions(), callback: (Result<Unit>) -> Unit) {
        val deleteRequest = buildDeleteRequest(path, gaiaHubConfig!!)

        executor.onNetworkThread {
            try {
                val result = callFactory.newCall(deleteRequest).execute()
                callback(Result(null))
            } catch (e: Exception) {
                Log.d(TAG, e.message, e)
                callback(Result(null, e.message))
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

        val isBinary = plainContent is ByteArray

        val contentString = if (isBinary) {
            Base64.encodeToString(plainContent as ByteArray, Base64.NO_WRAP)
        } else {
            plainContent as String
        }

        val result = Encryption().encryptWithPublicKey(contentString, options.publicKey)

        if (result.iv.isNotEmpty()) {
            return Result(CipherObject(result.iv, result.ephemPublicKey, result.ciphertext, result.mac, !isBinary))
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
            throw IllegalArgumentException("decrypt content only supports (json) String or ByteArray not " + cipherObject::class.java)
        }

        val isByteArray = cipherObject is ByteArray
        val cipherObjectString = if (isByteArray) {
            Base64.encodeToString(cipherObject as ByteArray, Base64.NO_WRAP)
        } else {
            cipherObject as String
        }
        val cipher = CipherObject(JSONObject(cipherObjectString))
        val plainContent = Encryption().decryptWithPrivateKey(EncryptedResultForm(cipher.ephemeralPK, cipher.iv, cipher.mac, cipher.cipherText, options.privateKey))

        if (plainContent != null && !"null".equals(plainContent)) {
            if (!binary) {
                return Result(plainContent)
            } else {
                return Result(Base64.decode(plainContent, Base64.DEFAULT))
            }
        } else {
            return Result(null, "failed to decrypt")
        }
    }

    companion object {
        val TAG = BlockstackSession2::class.java.simpleName
    }
}

private fun checksum(extended: String): String {
    val checksum = extended.hexToByteArray().sha256().sha256()
    val shortPrefix = checksum.slice(0..3)
    return shortPrefix.toNoPrefixHexString()
}


fun ECKeyPair.toHexPublicKey64(): String {
    return "02" + this.publicKey.key.toHexStringNoPrefix().substring(0, 64)
}

fun ECKeyPair.toBtcAddress(): String {
    val publicKey = toHexPublicKey64()
    val sha256 = publicKey.hexToByteArray().sha256()
    val hash160 = sha256.ripemd160()
    val extended = "00${hash160.toNoPrefixHexString()}"
    val checksum = checksum(extended)
    val address = (extended + checksum).hexToByteArray().encodeToBase58String()
    return address
}
