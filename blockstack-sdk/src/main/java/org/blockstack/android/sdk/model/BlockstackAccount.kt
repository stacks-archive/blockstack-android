package org.blockstack.android.sdk.model

import org.blockstack.android.sdk.toBtcAddress
import org.blockstack.android.sdk.toHexPublicKey64
import org.json.JSONObject
import org.kethereum.bip32.generateChildKey
import org.kethereum.bip32.model.ExtendedKey
import org.kethereum.bip44.BIP44Element
import org.kethereum.hashes.Sha256
import org.komputing.khex.extensions.toNoPrefixHexString

data class BlockstackAccount(val username: String?, val keys: ExtendedKey, val salt: String) {

    fun getAppsNode(): AppsNode {
        return AppsNode(keys.generateChildKey(BIP44Element(true, APPS_NODE_INDEX)), salt)
    }

    val ownerAddress:String
            get() = keys.keyPair.toBtcAddress()

    companion object {
        val APPS_NODE_INDEX = 0

        data class MetaData(
                var permissions: List<String> = emptyList(),
                var email: String? = null,
                var profileUrl: String? = null
        )
    }
}

data class AppsNode(val keys: ExtendedKey, val salt: String) {

    fun getAppNode(origin: String): ExtendedKey {
        val hash = Sha256.digest("$origin$salt".toByteArray()).toNoPrefixHexString()
        val appIndex = hashCode(hash)
        return keys.generateChildKey(BIP44Element(true, appIndex))
    }
}


fun BlockstackAccount.toUserData(): UserData {
    val identityAddress = this.keys.keyPair.toBtcAddress()
    return UserData(
            JSONObject().put("identityAddress", identityAddress)
                    .put("username", this.username)
                    .put("decentralizedID", "did:btc-addr:${identityAddress}")
    )
}

fun hashCode(string: String): Int {
    var hash = 0
    if (string.length == 0) return hash

    for (i in 0..string.length - 1) {
        val character = string.codePointAt(i)
        hash = (hash shl 5) - hash + character
        hash = hash and hash
    }
    return hash and 0x7fffffff
}
