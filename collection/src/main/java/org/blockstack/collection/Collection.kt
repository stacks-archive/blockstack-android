package org.blockstack.collection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.model.DeleteFileOptions
import org.blockstack.android.sdk.model.GetFileOptions
import org.blockstack.android.sdk.model.PutFileOptions
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val COLLECTION_SCOPE_PREFIX = "collection."
val COLLECTION_GAIA_PREFIX = "collection"

interface Attrs {
    fun toJSON(): JSONObject {
        return JSONObject()
                .put("identifier", identifier)
                .put("createdAt", createdAt )
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
    val scope: String
        get() = "${COLLECTION_SCOPE_PREFIX}${collectionName}"

    val collectionName: String
    val fromData: (String) -> T
    val fromAttrs: (T) -> Collection<T>

    suspend fun get(id: String,
                    userSession: BlockstackSession): Collection<T> {
        val collectionConfig = userSession.getCollectionConfig(collectionName)
        val normalizedIdentifier = "$COLLECTION_GAIA_PREFIX/$id"
        val options = GetFileOptions(gaiaHubConfig = collectionConfig!!.hubConfig)

        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                userSession.getFile(normalizedIdentifier, options) {
                    if (it.hasErrors) {
                        continuation.resumeWithException(RuntimeException(it.error!!.message))
                    } else {
                        val item = fromData(it.value as String)
                        continuation.resume(fromAttrs(item))
                    }
                }
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

    suspend fun <T : Attrs> delete(identifier: String, userSession: BlockstackSession) {

        val collectionConfig = userSession.getCollectionConfig(collectionName)
        val normalizedIdentifier = "$COLLECTION_GAIA_PREFIX/$identifier"
        val gaiaHubConfig = collectionConfig!!.hubConfig

        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                userSession.deleteFile(normalizedIdentifier, DeleteFileOptions(gaiaHubConfig = gaiaHubConfig)) {
                    continuation.resume(Unit)
                }
            }
        }
    }
}

abstract class Collection<T : Attrs>(val attrs: T) {
    abstract val collectionName: String

    suspend fun delete(userSession: BlockstackSession) {

        val collectionConfig = userSession.getCollectionConfig(collectionName)
        val normalizedIdentifier = "$COLLECTION_GAIA_PREFIX/$attrs.identifier"
        val gaiaHubConfig = collectionConfig!!.hubConfig

        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                userSession.deleteFile(normalizedIdentifier, DeleteFileOptions(gaiaHubConfig = gaiaHubConfig)) {
                    continuation.resume(Unit)
                }
            }
        }
    }

    suspend fun <T : Attrs> save(userSession: BlockstackSession) {

        val collectionConfig = userSession.getCollectionConfig(collectionName)
        if (collectionConfig != null) {
            val normalizedIdentifier = "$COLLECTION_GAIA_PREFIX/$attrs.identifier"
            val gaiaHubConfig = collectionConfig.hubConfig
            val encryptionKey = collectionConfig.encryptionKey
            val content = serialize()
            return suspendCoroutine { continuation ->
                CoroutineScope(Dispatchers.IO).launch {
                    userSession.putFile(normalizedIdentifier,
                            content,
                            PutFileOptions(gaiaHubConfig = gaiaHubConfig, encryptionKey = encryptionKey)
                    ) {
                        if (it.hasErrors) {
                            continuation.resumeWithException(RuntimeException(it.error!!.message))
                        } else {
                            continuation.resume(Unit)
                        }
                    }
                }
            }
        }
    }

    fun serialize(): String {
        return attrs.serialize()
    }

}


