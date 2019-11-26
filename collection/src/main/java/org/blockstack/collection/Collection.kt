package org.blockstack.collection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.blockstack.android.sdk.*
import org.blockstack.android.sdk.model.DeleteFileOptions
import org.blockstack.android.sdk.model.GetFileOptions
import org.blockstack.android.sdk.model.PutFileOptions
import org.json.JSONObject
import org.kethereum.crypto.toECKeyPair
import org.kethereum.model.PrivateKey

val COLLECTION_SCOPE_PREFIX = "collection."
val COLLECTION_GAIA_PREFIX = "collection"

interface Attrs {
    fun toJSON(): JSONObject {
        return JSONObject()
                .put("identifier", identifier)
                .put("createdAt", createdAt)
                .put("updatedAt", updatedAt)
                .put("signingKeyId", signingKeyId)
                .put("_id", _id)
    }

    fun serialize(): String {
        return toJSON().toString()
    }

    val identifier: String
    val createdAt: Number?
    val updatedAt: Number?
    val signingKeyId: String?
    val _id: String?

}

interface CollectionAPI<T : Attrs> {
    val scope: Scope
        get() = Scope("${COLLECTION_SCOPE_PREFIX}${collectionName}")

    val collectionName: String
    val fromData: (String) -> T
    val fromAttrs: (T) -> Collection<T>

    suspend fun get(id: String,
                    userSession: BlockstackSession): Collection<T> {
        val collectionConfig = userSession.getCollectionConfig(collectionName)
        val normalizedIdentifier = "$COLLECTION_GAIA_PREFIX/$id"
        val options = GetFileOptions(gaiaHubConfig = collectionConfig!!.hubConfig)

        return withContext(Dispatchers.IO) {
            val it = userSession.getFile(normalizedIdentifier, options)
            if (it.hasErrors) {
                throw RuntimeException(it.error!!.message)
            } else {
                val item = fromData(it.value as String)
                fromAttrs(item)
            }
        }
    }

    suspend fun list(itemCallback: (String) -> Boolean, userSession: BlockstackSession): Int {
        val collectionConfig = userSession.getCollectionConfig(collectionName)
        val gaiaHubConfig = collectionConfig!!.hubConfig
        val collectionGaiaPathPrefix = "$COLLECTION_GAIA_PREFIX/"

        return userSession.listFilesLoop(page = null, callCount = 0, fileCount = 0,
                gaiaHubConfig = gaiaHubConfig,
                callback = {
                    val path = it.value
                    if (path != null && path.startsWith(collectionGaiaPathPrefix)) {
                        // Remove collection/ prefix from file names
                        val identifier = path.substring(collectionGaiaPathPrefix.length)
                        itemCallback(identifier)
                    } else {
                        // Skip non-collection prefix files
                        true
                    }
                })
    }

    suspend fun <T : Attrs> delete(identifier: String, userSession: BlockstackSession): Result<out Unit> {

        val collectionConfig = userSession.getCollectionConfig(collectionName)
        val normalizedIdentifier = "$COLLECTION_GAIA_PREFIX/$identifier"
        val gaiaHubConfig = collectionConfig!!.hubConfig

        return withContext(Dispatchers.IO) {
            userSession.deleteFile(normalizedIdentifier, DeleteFileOptions(gaiaHubConfig = gaiaHubConfig))
        }
    }
}

abstract class Collection<T : Attrs>(val attrs: T) {
    abstract val collectionName: String

    suspend fun delete(userSession: BlockstackSession): Result<out Unit> {

        val collectionConfig = userSession.getCollectionConfig(collectionName)
        val normalizedIdentifier = "$COLLECTION_GAIA_PREFIX/$attrs.identifier"
        val gaiaHubConfig = collectionConfig!!.hubConfig

        return withContext(Dispatchers.IO) {
            userSession.deleteFile(normalizedIdentifier, DeleteFileOptions(gaiaHubConfig = gaiaHubConfig))
        }
    }

    suspend fun save(userSession: BlockstackSession): Result<out Unit> {

        val collectionConfig = userSession.getCollectionConfig(collectionName)
        if (collectionConfig != null) {
            val normalizedIdentifier = "$COLLECTION_GAIA_PREFIX/${attrs.identifier}"
            val gaiaHubConfig = collectionConfig.hubConfig
            val encryptionKey = collectionConfig.encryptionKey
            val content = serialize()
            return withContext(Dispatchers.IO) {
                val it = userSession.putFile(normalizedIdentifier,
                        content,
                        PutFileOptions(gaiaHubConfig = gaiaHubConfig, encryptionKey = PrivateKey(encryptionKey).toECKeyPair().toHexPublicKey64())
                )
                if (it.hasErrors) {
                    Result(null, ResultError(ErrorCode.UnknownError, it.error!!.message))
                } else {
                    Result(Unit)
                }


            }
        } else {
            return Result(null, ResultError(ErrorCode.UnknownError, "No collection config found"))
        }
    }

    fun serialize(): String {
        return attrs.serialize()
    }

}


