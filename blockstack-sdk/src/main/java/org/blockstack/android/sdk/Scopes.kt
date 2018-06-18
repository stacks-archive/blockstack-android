package org.blockstack.android.sdk

/**
 * An enum of scopes supported in Blockstack Authentication.
 */
enum class Scope(val scope: String) {
    /**
     * Read and write data to the user's Gaia hub in an app-specific storage bucket.
     *
     * This is the default scope.
     */
    StoreWrite("store_write"),

    /**
     * Publish data so that other users of the app can discover and interact with the user
     */
    PublishData("publish_data"),

    /**
     * Requests the user's email if available
     */
    Email("email");

    override fun toString(): String {
        return scope
    }

    companion object {
        @JvmStatic
        fun scopesArrayToJSONString(scopes: Array<Scope>): String {
            return scopes.joinToString(prefix = "[", transform = {"\"${it.scope}\""}, postfix = "]")
        }
    }
}