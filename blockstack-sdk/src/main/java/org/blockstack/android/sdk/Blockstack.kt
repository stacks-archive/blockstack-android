package org.blockstack.android.sdk

import android.util.Base64
import android.util.Log
import com.colendi.ecies.EncryptedResultForm
import com.colendi.ecies.Encryption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonException
import me.uport.sdk.core.decodeBase64
import me.uport.sdk.core.toBase64UrlSafe
import me.uport.sdk.jwt.*
import me.uport.sdk.jwt.model.ArbitraryMapSerializer
import me.uport.sdk.jwt.model.JwtHeader
import me.uport.sdk.signer.KPSigner
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.blockstack.android.sdk.extensions.toBtcAddress
import org.blockstack.android.sdk.extensions.toHexPublicKey64
import org.blockstack.android.sdk.extensions.toStxAddress
import org.blockstack.android.sdk.model.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.kethereum.crypto.CryptoAPI
import org.kethereum.crypto.toECKeyPair
import org.kethereum.extensions.toHexStringNoPrefix
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey
import org.kethereum.model.PublicKey
import org.komputing.khex.extensions.toNoPrefixHexString
import org.komputing.khex.model.HexString
import java.net.URI
import java.net.URL
import java.security.InvalidParameterException
import java.text.SimpleDateFormat
import java.util.*

class Blockstack(private val callFactory: Call.Factory = OkHttpClient(),
                 val dispatcher: CoroutineDispatcher = Dispatchers.IO) {

    private var btcAddrResolver: BitAddrResolver

    init {
        btcAddrResolver = BitAddrResolver(callFactory)
    }


    suspend fun makeAuthResponseUnencrypted(account: BlockstackAccount, domainName: String,
                                            scopes: Array<Scope>): String {
        val appNode = account.getAppsNode().getAppNode(domainName)
        val privateKeyPayload = appNode.getPrivateKeyHex()

        return makeAuthResponseToken(account, privateKeyPayload, scopes)
    }

    suspend fun makeAuthResponse(account: BlockstackAccount, authRequest: String, scopes: Array<Scope>): String {
        val authRequestTriple = decodeToken(authRequest)
        return makeAuthResponse(authRequestTriple.second, account, scopes)
    }

    suspend fun makeAuthResponse(payload: JSONObject, account: BlockstackAccount, scopes: Array<Scope>): String {
        val transitPublicKey = payload.getJSONArray("public_keys").getString(0)
        val appPrivateKey = account.getAppsNode().getAppNode(payload.getString("domain_name"))

        val privateKeyPayload = encryptContent(
                appPrivateKey.getPrivateKeyHex(),
                CryptoOptions(publicKey = transitPublicKey)
        ).value?.json?.toString()?.toByteArray()?.toNoPrefixHexString()

        return makeAuthResponseToken(account, privateKeyPayload, scopes)
    }

    private suspend fun makeAuthResponseToken(account: BlockstackAccount, privateKeyPayload: String?, scopes: Array<Scope>): String = withContext(dispatcher) {
        val username = account.username
        val profile = if (username != null) {
            lookupProfile(username, null)
        } else {
            Profile(JSONObject())
        }

        val expiresAt = (Date().time + 3600 * 24 * 30) / 1000
        val payload = mapOf(
                "jti" to UUID.randomUUID().toString(),
                "iat" to Date().time / 1000,
                "exp" to expiresAt,
                "private_key" to privateKeyPayload,
                "public_keys" to arrayOf(account.keys.keyPair.toHexPublicKey64()),
                "profile" to profile.json.toMap(),
                "username" to (account.username ?: ""),
                "email" to if (scopes.contains(BaseScope.Email.scope)) {
                    (account.metaData.email ?: "")
                } else {
                    ""
                },
                "profile_url" to null,
                "hubUrl" to "https://hub.blockstack.org", //TODO: here is what?
                "blockstackAPIUrl" to "https://core.blockstack.org", //TODO: here is what?
                "associationToken" to null,
                "version" to VERSION
        )

        val signer = KPSigner(
                account.keys.keyPair.privateKey.key.toHexStringNoPrefix()
        )
        val issuerDID =
                "did:btc-addr:${account.keys.keyPair.toBtcAddress()}"

        val jwt = JWTTools()
        return@withContext jwt.createJWT(payload, issuerDID, signer, algorithm = JwtHeader.ES256K)
    }

    /**
     * Look up a user profile by blockstack ID
     *
     * @param {string} username - The Blockstack ID of the profile to look up
     * @param {string} [zoneFileLookupURL=null] - The URL
     * to use for zonefile lookup. If falsey, lookupProfile will use the
     * blockstack.js [[getNameInfo]] function.
     * @returns {Promise} that resolves to a profile object
     */
    suspend fun lookupProfile(username: String, zoneFileLookupURL: URL?): Profile = withContext(dispatcher){

        val request = buildLookupNameInfoRequest(username, zoneFileLookupURL?.toString())
        val response = callFactory.newCall(request).execute()

        if (response.isSuccessful) {
            val nameInfo = JSONObject(response.body!!.string())
            if (nameInfo.has("address") && nameInfo.has("zonefile")) {
                return@withContext resolveZoneFileToProfile(nameInfo) ?: return@withContext Profile(JSONObject())

            } else {
                throw InvalidParameterException("name info does not contain address or zonefile property")
            }
        } else {
            throw InvalidParameterException("could not fetch name info ${response.code}")
        }
    }

    private fun resolveZoneFileToProfile(nameInfo: JSONObject): Profile? {
        return resolveZoneFileToProfile(nameInfo.getString("zonefile"), nameInfo.getString("address"))
    }

    fun resolveZoneFileToProfile(zoneFileContent: String, address: String): Profile? {
        val zoneFileJson = parseZoneFile(zoneFileContent)
        val tokenFileUri = zoneFileJson.tokenFileUri
        if (tokenFileUri != null) {
            val request = Request.Builder().url(tokenFileUri)
                    .addHeader("Referrer-Policy", "no-referrer")
                    .build()
            val result = callFactory.newCall(request).execute()
            if (result.isSuccessful) {
                val tokenFile = result.body!!.string()
                val tokenRecords = JSONArray(tokenFile)
                return extractProfile(tokenRecords.getJSONObject(0).getString("token"),
                        address)
            }

        }
        return null

    }


    fun extractProfile(token: String, publicKeyOrAddress: String?): Profile? {
        val decodedToken = decodeToken(token)
        if (publicKeyOrAddress != null) {
            verifyProfileToken(token, publicKeyOrAddress)
        }
        val payload = decodedToken.second
        if (payload.has("claim")) {
            return Profile(payload.getJSONObject("claim"))
        } else {
            return null
        }
    }

    private fun buildLookupNameInfoRequest(username: String, zoneFileLookupUrl: String?): Request {
        val url = zoneFileLookupUrl ?: "$DEFAULT_CORE_API_ENDPOINT/v1/names"
        val builder = Request.Builder()
                .url("$url/$username")
        builder.addHeader("Referrer-Policy", "no-referrer")
        return builder.build()

    }

    suspend fun verifyToken(token: String): Boolean {
        val tokenTriple = decodeToken(token)
        val issuer = tokenTriple.second.getString("iss")
        val issuerAddress = DIDs.getAddressFromDID(issuer)
        btcAddrResolver.add(issuerAddress, tokenTriple.second.getJSONArray("public_keys").getString(0))
        val result = isExpirationDateValid(tokenTriple.second) &&
                isIssuanceDateValid(tokenTriple.second) &&
                doSignaturesMatchPublicKeys(token, tokenTriple.second) &&
                doPublicKeysMatchIssuer(tokenTriple.second) &&
                doPublicKeysMatchUsername(tokenTriple.second, null)
        btcAddrResolver.remove(issuerAddress)
        return result
    }

    suspend fun verifyAuthRequest(token: String): Boolean {
        val tokenTriple = decodeToken(token)
        btcAddrResolver.add(DIDs.getAddressFromDID(tokenTriple.second.getString("iss")), tokenTriple.second.getJSONArray("public_keys").getString(0))
        val result = isExpirationDateValid(tokenTriple.second) &&
                isIssuanceDateValid(tokenTriple.second) &&
                doSignaturesMatchPublicKeys(token, tokenTriple.second) &&
                doPublicKeysMatchIssuer(tokenTriple.second) &&
                isManifestUriValid(tokenTriple.second) &&
                isRedirectUriValid(tokenTriple.second)
        btcAddrResolver.remove(tokenTriple.second.getString("iss"))
        return result

    }

    private fun isRedirectUriValid(payload: JSONObject): Boolean {
        return isSameOriginAbsoluteUrl(payload.getString("domain_name"), payload.getString("redirect_uri"))
    }

    private fun isManifestUriValid(payload: JSONObject): Boolean {
        return isSameOriginAbsoluteUrl(payload.getString("domain_name"), payload.getString("manifest_uri"))
    }

    /**
     * Checks if both urls pass the same origin check & are absolute
     * @param  {[type]}  uri1 first uri to check
     * @param  {[type]}  uri2 second uri to check
     * @return {Boolean} true if they pass the same origin check
     * @private
     * @ignore
     */
    private fun isSameOriginAbsoluteUrl(uri1: String, uri2: String): Boolean {

        val parsedUri1 = URI.create(uri1)
        val parsedUri2 = URI.create(uri2)

        val port1 = if (parsedUri1.port == -1) {
            if (parsedUri1.scheme == "https:") {
                443
            } else {
                80
            }
        } else {
            parsedUri1.port
        }
        val port2 = if (parsedUri2.port == -1) {
            if (parsedUri2.scheme == "https:") {
                443
            } else {
                80
            }
        } else {
            parsedUri2.port
        }

        val match = mapOf<String, Boolean>(
                "scheme" to (parsedUri1.scheme == parsedUri2.scheme),
                "hostname" to (parsedUri1.host == parsedUri2.host),
                "port" to (port1 == port2),
                "absolute" to ((uri1.indexOf("http://") >= 0 || uri1.indexOf("https://") >= 0)
                        && (uri2.indexOf("http://") >= 0 || uri2.indexOf("https://") >= 0))
        )

        return match.getValue("scheme") && match.getValue("hostname")
                && match.getValue("port") && match.getValue("absolute")
    }


    private fun doPublicKeysMatchIssuer(payload: JSONObject): Boolean {
        val issAddress = DIDs.getAddressFromDID(payload.getString("iss"))
        val publicKey = payload.getJSONArray("public_keys").getString(0)
        val pkAddress = publicKey.toBtcAddress()
        return issAddress == pkAddress
    }

    private suspend fun doSignaturesMatchPublicKeys(token: String, payload: JSONObject): Boolean {
        try {
            JWTTools().verify(token, btcAddrResolver, false) // throws an exception if invalid
            return true
        } catch (e: InvalidJWTException) {
            return false
        }
    }

    private fun doPublicKeysMatchUsername(payload: JSONObject, nameLookupURL: String?): Boolean {
        val username = payload.optStringOrNull("username")
        if (username == null || username.isEmpty()) {
            return true
        }
        val request = buildLookupNameInfoRequest(username, nameLookupURL)
        val response = callFactory.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body!!.string()
            val nameInfo = JSONObject(body)
            val nameOwningAddress = nameInfo.optString("address")
            val addressFromIssuer = DIDs.getAddressFromDID(payload.optString("iss")) ?: ""

            //Check if the address is a stx address
            return if (nameOwningAddress.startsWith("S")) {
                if (nameOwningAddress.isNotEmpty() && nameOwningAddress == addressFromIssuer) {
                    true
                } else {
                    // Backward Compatibility (Address STX with BTC issuer)
                    // if the address is not the same, check if the profile belongs to the owner
                    nameInfo.optString("zonefile").contains(addressFromIssuer)
                }
            } else {
                // legacy
                nameOwningAddress.isNotEmpty() && nameOwningAddress == addressFromIssuer
            }
        } else {
            return false
        }
    }

    private fun isIssuanceDateValid(payload: JSONObject): Boolean {
        val issuanceDate = try {
            Date(payload.getString("iat").toLong() * 1000)
        } catch (e: NumberFormatException) {
            zuluWithMicrosFormatter.parse(payload.getString("iat"))
        }
        val now = Date()
        return now.after(issuanceDate)
    }

    private fun isExpirationDateValid(payload: JSONObject): Boolean {
        val expirationDate = try {
            Date(payload.getString("exp").toLong() * 1000)
        } catch (e: NumberFormatException) {
            zuluWithMicrosFormatter.parse(payload.getString("exp"))
        }
        val now = Date()
        return now.before(expirationDate)
    }

    fun decodeToken(token: String): Triple<JwtHeader, JSONObject, ByteArray> {

        //Split token by . from jwtUtils
        val (encodedHeader, encodedPayload, encodedSignature) = JWTUtils.splitToken(token)
        if (encodedHeader.isEmpty())
            throw InvalidJWTException("Header cannot be empty")
        else if (encodedPayload.isEmpty())
            throw InvalidJWTException("Payload cannot be empty")
        //Decode the pieces
        val headerString = String(encodedHeader.decodeBase64())
        val payloadString = String(encodedPayload.decodeBase64())
        val signatureBytes = encodedSignature.decodeBase64()

        try {
            //Parse Json
            val header = JwtHeader.fromJson(headerString)
            val payload = JSONObject(payloadString)
            return Triple(header, payload, signatureBytes)
        } catch (ex: JsonException) {
            throw JWTEncodingException("cannot parse the JWT($token)", ex)
        }
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

        val result = Encryption().encryptWithPublicKey(contentString.toByteArray(), options.publicKey)

        if (result.iv.isNotEmpty()) {
            return Result(CipherObject(result.iv, result.ephemPublicKey, result.ciphertext, result.mac, !isBinary))
        } else {
            return Result(null, ResultError(ErrorCode.UnknownError, "failed to encrypt"))
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

        if (plainContent != null) {
            if (!binary) {
                return Result(String(plainContent))
            } else {
                return Result(Base64.decode(plainContent, Base64.DEFAULT))
            }
        } else {
            return Result(null, ResultError(ErrorCode.FailedDecryptionError, "failed to decrypt"))
        }
    }

    fun getPublicKeyFromPrivate(privateKey: String): String? {
        return PrivateKey(HexString(privateKey)).toECKeyPair().toHexPublicKey64()
    }

    fun makeECPrivateKey(): String? {
        val keyPair = CryptoAPI.keyPairGenerator.generate()
        return keyPair.privateKey.key.toHexStringNoPrefix()
    }

    fun publicKeyToAddress(publicKey: String): String? {
        return publicKey.toBtcAddress()
    }

    /**
     * Get the app storage bucket URL
     *
     * @param gaiaHubUrl (String) the gaia hub URL
     * @param appPrivateKey (String) the app private key used to generate the app address
     * @result the URL of the app index file or null if it fails
     */
    suspend fun getAppBucketUrl(gaiaHubUrl: String, privateKey: String): String? = withContext(dispatcher){
        val challengeSigner = PrivateKey(HexString(privateKey)).toECKeyPair()
        val response = fetchPrivate("${gaiaHubUrl}/hub_info")
        val responseJSON = response.json()
        if (responseJSON != null) {
            val readURL = responseJSON.getString("read_url_prefix")
            val address = challengeSigner.toBtcAddress()
            return@withContext "${readURL}${address}/"
        } else {
            return@withContext null
        }
    }

    /**
     * Fetch the public read URL of a user file for the specified app.
     *
     *@param path the path to the file to read
     *@param username The Blockstack ID of the user to look up
     *@param appOrigin The app origin
     *@param zoneFileLookupURL The URL to use for zonefile lookup. If false, this will use the blockstack.js's getNameInfo function instead.
     *@result the public read URL of the file or null on error
     */
    suspend fun getUserAppFileUrl(path: String, username: String, appOrigin: String, zoneFileLookupURL: String?): String = withContext(dispatcher){
        val profile = lookupProfile(username, zoneFileLookupURL?.let { URL(it) })
        var bucketUrl = NO_URL
        if (profile.json.has("apps")) {
            val apps = profile.json.getJSONObject("apps")
            if (apps.has(appOrigin)) {
                val url = apps.getString(appOrigin)
                val bucket = url.replace(Regex("/+(\\?|#|$)"), "/$1")
                bucketUrl = "${bucket}${path}"
            }
        }
        return@withContext bucketUrl
    }

    private suspend fun fetchPrivate(url: String): Response = withContext(dispatcher) {
        val request = Request.Builder().url(url)
                .addHeader("Referrer-Policy", "no-referrer")
                .build()
        return@withContext callFactory.newCall(request).execute()
    }

    fun wrapProfileToken(token: String): ProfileTokenPair {
        val decodedToken = decodeToken(token)
        val jsonToken = JSONObject()
                .put("token", token)
                .put("decodedToken", tokenTripleToJSON(decodedToken))

        return ProfileTokenPair(jsonToken)
    }

    /**
     * Verifies a profile token
     * @param {String} token - the token to be verified
     * @param {String} publicKeyOrAddress - the public key or address of the
     *   keypair that is thought to have signed the token
     * @returns {Object} - the verified, decoded profile token
     * @throws {Error} - throws an error if token verification fails
     */
    fun verifyProfileToken(token: String, publicKeyOrAddress: String): ProfileToken {
        val decodedToken = decodeToken(token)
        val payload = decodedToken.second

        // Inspect and verify the subject
        if (payload.has("subject")) {
            if (!payload.getJSONObject("subject").has("publicKey")) {
                throw Error("Token doesn\'t have a subject public key")
            }
        } else {
            throw  Error("Token doesn\'t have a subject")
        }

        // Inspect and verify the issuer
        if (payload.has("issuer")) {
            if (!payload.getJSONObject("issuer").has("publicKey")) {
                throw  Error("Token doesn\'t have an issuer public key")
            }
        } else {
            throw  Error("Token doesn\'t have an issuer")
        }

        // Inspect and verify the claim
        if (!payload.has("claim")) {
            throw Error("Token doesn\'t have a claim")
        }

        val issuerPublicKey = payload.getJSONObject("issuer").getString("publicKey")
        val uncompressedBtcAddress = issuerPublicKey.toBtcAddress()
        val uncompressedStxAddress = issuerPublicKey.toStxAddress(true)

            if (publicKeyOrAddress == issuerPublicKey) {
                // pass
            } else {
                when (publicKeyOrAddress) {
                    uncompressedBtcAddress -> { /* Pass */}
                    uncompressedStxAddress -> { /* Pass */}
                    else -> {
                        throw Error("Token issuer public key does not match the verifying value")
                    }
                }
            }


        return ProfileToken(tokenTripleToJSON(decodedToken))

    }

    private fun tokenTripleToJSON(decodedToken: Triple<JwtHeader, JSONObject, ByteArray>): JSONObject {
        return JSONObject()
                .put("header", JSONObject().put("typ", decodedToken.first.typ)
                        .put("alg", decodedToken.first.alg))
                .put("payload", decodedToken.second)
                .put("signature", decodedToken.third.toBase64UrlSafe())
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
    suspend fun signProfileToken(profile: Profile, privateKey: String, subject: Entity, issuer: Entity, issuedAt: Date = Date(), expiresAt: Date = nextYear()): ProfileTokenPair = withContext(dispatcher) {


        val payload = mapOf(
                "jti" to UUID.randomUUID().toString(),
                "iat" to issuedAt.toZuluTime(),
                "exp" to expiresAt.toZuluTime(),
                "subject" to subject.json.toMap(),
                "issuer" to issuer.json.toMap(),
                "claim" to profile.json.toMap()
        )

        val header = JwtHeader(alg = JwtHeader.ES256K)
        val serializedPayload = Json(JsonConfiguration.Stable)
                .stringify(ArbitraryMapSerializer, payload)
        Log.d(BlockstackSession.TAG, header.toJson() + ", " + serializedPayload)
        val signingInput = listOf(header.toJson(), serializedPayload)
                .map { it.toBase64UrlSafe() }
                .joinToString(".")

        val jwtSigner = JWTSignerAlgorithm(header)
        val signature: String = jwtSigner.sign(signingInput, KPSigner(privateKey))
        val token = listOf(signingInput, signature).joinToString(".")
        Log.d(TAG, token)
        return@withContext wrapProfileToken(token)
    }

    companion object {
        const val NO_URL = "NO_URL"
        val TAG = Blockstack::class.java.simpleName
        fun verifyAuthResponse(authResponse: String): ResultError? {
            try {
                val tokenParts = authResponse.split('.')
                if (tokenParts.size != 3) {
                    return ResultError(ErrorCode.UnknownError, "The authResponse parameter is an invalid base64 encoded token\n2 dots requires\nAuth response: $authResponse")
                }
                val decodedToken = Base64.decode(tokenParts[0], Base64.DEFAULT)
                val stringToken = decodedToken.toString(Charsets.UTF_8)
                val jsonToken = JSONObject(stringToken)
                if (jsonToken.getString("typ") != "JWT") {
                    return ResultError(ErrorCode.UnknownError, "The authResponse parameter is an invalid base64 encoded token\nHeader not of type JWT:${jsonToken.getString("typ")}\n Auth response: $authResponse")
                }
            } catch (e: IllegalArgumentException) {
                val error = ResultError(ErrorCode.UnknownError, "The authResponse parameter is an invalid base64 encoded token\n${e.message}\nAuth response: $authResponse")
                Log.w(TAG, error.toString(), e)
                return error
            } catch (e: JSONException) {
                val error = ResultError(ErrorCode.UnknownError, "The authResponse parameter is an invalid json token\n${e.message}\nAuth response: $authResponse")
                Log.w(TAG, error.toString(), e)
                return error
            }
            return null
        }
    }
}


private val zuluFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ", Locale.US)
private val zuluWithMicrosFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
private fun Date.toZuluTime(): String {
    return zuluFormatter.format(this)
}

private fun Response.json(): JSONObject? {
    if (this.isSuccessful) {
        return this.body?.string().let {
            if (it != null) {
                JSONObject(it)
            } else {
                null
            }
        }
    } else {
        return null
    }
}

private fun JSONObject.toMap(): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    this.keys().forEach {
        val value = this.get(it)
        result.put(it, when (value) {
            is Number -> value
            is String -> value
            is Boolean -> value
            is JSONObject -> value.toMap()
            is JSONArray -> value.toMap()
            else -> {
                throw InvalidParameterException("$it has unsupported type $value")
            }
        })
    }
    return result
}

private fun JSONArray.toMap(): Array<Any?> {
    val array = arrayOfNulls<Any>(length())
    for (index in 0 until length()) {
        array[index] = getJSONObject(index).toMap()
    }
    return array
}

@Deprecated(
    "Import the extention from extensions.Addresses",
    ReplaceWith(
    "org.blockstack.android.sdk.toBtcAddress()",
    "org.blockstack.android.sdk.extensions.toBtcAddress()"
    )
)
fun String.toBtcAddress(): String {
    return toBtcAddress()
}

@Deprecated(
    "Import the extention from extensions.Addresses",
    ReplaceWith(
        "org.blockstack.android.sdk.toHexPublicKey64()",
        "org.blockstack.android.sdk.extensions.toHexPublicKey64()"
    )
)
fun ECKeyPair.toHexPublicKey64(): String {
    return toHexPublicKey64()
}

@Deprecated(
    "Import the extention from extensions.Addresses",
    ReplaceWith(
        "org.blockstack.android.sdk.toBtcAddress()",
        "org.blockstack.android.sdk.extensions.toBtcAddress()"
    )
)
fun ECKeyPair.toBtcAddress(): String {
    return toBtcAddress()
}


@Deprecated(
    "Import the extention from extensions.Addresses",
    ReplaceWith(
        "org.blockstack.android.sdk.toBtcAddress()",
        "org.blockstack.android.sdk.extensions.toBtcAddress()"
    )
)
fun PublicKey.toBtcAddress(): String {
    return toBtcAddress()
}


private fun nextYear(): Date {
    val calendar = GregorianCalendar.getInstance()
    calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1)
    return calendar.time
}


fun URI.getOrigin(): String {
    return if (this.port != -1) {
        "${this.scheme}://${this.host}:${this.port}"
    } else {
        "${this.scheme}://${this.host}"
    }
}

