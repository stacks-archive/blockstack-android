package org.blockstack.android.sdk

/**
 * An enum of scopes supported in Blockstack Authentication.
 *
 * @property scope identifies the permission, same as in blockstack.js
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

    /**
     * returns the scope as string
     */
    override fun toString(): String {
        return scope
    }

    companion object {
        /**
         * converts an array of scopes into a string usable by blockstack.js
         */
        @JvmStatic
        fun scopesArrayToJSONString(scopes: Array<Scope>): String {
            return scopes.joinToString(prefix = "[", transform = { "\"${it.scope}\"" }, postfix = "]")
        }

        /**
         * Creates `Scope` from its @property scope, i.e. the javascript name.
         * Throws IllegalArgumentException if scope name is not defined.
         *
         * @param scopeJSName name of scope as defined in blockstack.js
         *
         */
        @JvmStatic
        fun fromJSName(scopeJSName: String): Scope {
            for (scope in Scope.values()) {
                if (scopeJSName === scope.scope) {
                    return scope
                }
            }
            throw IllegalArgumentException("scope '$scopeJSName' not defined, available scopes: ${Scope.values().joinToString()}")
        }
    }
}