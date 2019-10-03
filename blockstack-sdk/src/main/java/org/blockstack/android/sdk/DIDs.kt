package org.blockstack.android.sdk

import me.uport.sdk.universaldid.*
import okhttp3.Call
import okhttp3.Request
import org.json.JSONObject
import java.util.*

class DIDs {


    companion object {
        fun getAddressFromDID(did: String?): String? {
            if (did == null) {
                return null
            }

            val didType = getDIDType(did)

            if (didType == "btc-addr") {
                return did.split(':')[2]
            } else {
                return null
            }
        }


        private fun getDIDType(decentralizedID: String): String {
            val didParts = decentralizedID.split(':')

            if (didParts.size != 3) {
                throw InvalidDIDError("Decentralized IDs must have 3 parts")
            }

            if (didParts[0].toLowerCase(Locale.US) != "did") {
                throw InvalidDIDError("Decentralized IDs must start with 'did'")
            }

            return didParts[1].toLowerCase(Locale.US)
        }
    }
}

class InvalidDIDError(msg: String) : Throwable(msg)

class BitAddrResolver(private val okHttpClient: Call.Factory) : DIDResolver {
    override fun canResolve(potentialDID: String): Boolean {
        return DIDs.getAddressFromDID(potentialDID) != null
    }

    override suspend fun resolve(did: String): DIDDocument {

        val address = DIDs.getAddressFromDID(did)
        val publicKeyHex = getPublicKeyHex(address)
                ?: throw InvalidDIDError("no DID document available")

        return object : DIDDocument {
            override val authentication: List<AuthenticationEntry>
                get() = listOf(AuthenticationEntry(PublicKeyType.Secp256k1SignatureAuthentication2018, publicKeyHex))
            override val context: String?
                get() = null
            override val id: String?
                get() = did
            override val publicKey: List<PublicKeyEntry>
                get() = listOf(PublicKeyEntry(did, PublicKeyType.Secp256k1VerificationKey2018, did, publicKeyHex = publicKeyHex))
            override val service: List<ServiceEntry>
                get() = emptyList()

        }
    }

    private fun getPublicKeyHex(address: String?): String? {
        if (publicKeyMap.contains(address)) {
            return publicKeyMap[address]
        } else {
            return null
        }
    }

    fun add(address: String?, publicKey: String?) {
        if (address != null) {
            publicKeyMap[address] = publicKey
        }
    }

    fun remove(address: String?) {
        if (address != null) {
            publicKeyMap.remove(address)
        }
    }

    private val publicKeyMap: MutableMap<String, String?> = mutableMapOf()
    override val method: String
        get() = "btc-addr"

}

