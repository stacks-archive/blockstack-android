package org.blockstack.android.sdk.model

import org.blockstack.android.sdk.toBtcAddress
import org.json.JSONObject
import org.kethereum.bip32.generateChildKey
import org.kethereum.bip32.model.ExtendedKey
import org.kethereum.bip44.BIP44Element
import org.kethereum.extensions.toHexStringNoPrefix
import org.kethereum.hashes.Sha256
import org.komputing.khex.extensions.toNoPrefixHexString

data class BlockstackAccount(val username: String?, val keys: ExtendedKey, val salt: String, val metaData: MetaData = MetaData()) {

    fun getAppsNode(): AppsNode {
        return AppsNode(keys.generateChildKey(BIP44Element(true, APPS_NODE_INDEX)), salt)
    }

    fun getCollectionsNode(): CollectionsNode {
        return CollectionsNode(keys.generateChildKey(BIP44Element(true, COLLECTIONS_NODE_INDEX)), salt)
    }

    val ownerAddress: String
        get() = keys.keyPair.toBtcAddress()

    companion object {
        const val APPS_NODE_INDEX = 0
        const val SIGNING_NODE_INDEX = 1
        const val ENCRYPTION_NODE_INDEX = 2
        const val COLLECTIONS_NODE_INDEX = 3

        data class MetaData(
                var permissions: List<String> = emptyList(),
                var email: String? = null,
                var profileUrl: String? = null
        )
    }
}

data class AppNode(val keys: ExtendedKey) {
    fun getPrivateKeyHex(): String {
        return keys.keyPair.privateKey.key.toHexStringNoPrefix()
    }

    fun toBtcAddress(): String {
        return keys.keyPair.toBtcAddress()
    }
}

data class AppsNode(val keys: ExtendedKey, val salt: String) {

    fun getAppNode(origin: String): AppNode {
        val hash = Sha256.digest("$origin$salt".toByteArray()).toNoPrefixHexString()
        val appIndex = hashCode(hash)
        return AppNode(keys.generateChildKey(BIP44Element(true, appIndex)))
    }
}

data class CollectionNode(val keys: ExtendedKey) {
    fun getPrivateKeyHex(): String {
        return keys.keyPair.privateKey.key.toHexStringNoPrefix()
    }
}

const val COLLECTION_IDENTIFIER_DEFAULT = "default"

data class CollectionsNode(val keys: ExtendedKey, val salt: String) {
    fun getCollectionNode(collectionTypeName: String, collectionIdentifier: String = COLLECTION_IDENTIFIER_DEFAULT): CollectionNode {
        val hash = Sha256.digest("$collectionTypeName$collectionIdentifier$salt".toByteArray()).toNoPrefixHexString()
        val collectionIndex = hashCode(hash)
        return CollectionNode(keys.generateChildKey(BIP44Element(true, collectionIndex)))
    }

    fun getCollectionEncryptionNode(collectionTypeName: String, encryptionIndex: Int, collectionIdentifier: String = COLLECTION_IDENTIFIER_DEFAULT): CollectionNode {
        val hash = Sha256.digest("$collectionTypeName$collectionIdentifier$salt".toByteArray()).toNoPrefixHexString()
        val collectionEncryptionIndex = hashCode(hash)
        return CollectionNode(keys.generateChildKey(BIP44Element(true, collectionEncryptionIndex)))
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
