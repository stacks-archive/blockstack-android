package org.blockstack.android.sdk

data class Scope(val name: String) {
    companion object {
        /**
         * converts an array of scopes into a string usable by blockstack.js
         */
        @JvmStatic
        fun scopesArrayToJSONString(scopes: Array<String>): String {
            return scopes.joinToString(prefix = "[", transform = { "\"${it}\"" }, postfix = "]")
        }

        /**
         * converts an array of scopes into a string usable by blockstack.js
         */
        @JvmStatic
        fun scopesArrayToJSONString(scopes: Array<Scope>): String {
            return scopes.joinToString(prefix = "[", transform = { "\"${it.name}\"" }, postfix = "]")
        }

        /**
         * Creates `Scope` from its @property scope, i.e. the javascript name.
         * Throws IllegalArgumentException if scope name is not defined.
         *
         * @param scopeJSName name of scope as defined in blockstack.js
         *
         */
        @JvmStatic
        fun fromJSName(scopeJSName: String): BaseScope {
            for (scope in BaseScope.values()) {
                if (scopeJSName === scope.name) {
                    return scope
                }
            }
            throw IllegalArgumentException("scope '$scopeJSName' not defined, available scopes: ${BaseScope.values().joinToString()}")
        }
    }
}

/**
 * An enum of scopes supported in Blockstack Authentication.
 *
 * @property scope identifies the permission, same as in blockstack.js
 */
enum class BaseScope(val scope: Scope) {
    /**
     * Read and write data to the user's Gaia hub in an app-specific storage bucket.
     *
     * This is the default scope.
     */
    StoreWrite(Scope("store_write")),

    /**
     * Publish data so that other users of the app can discover and interact with the user
     */
    PublishData(Scope("publish_data")),

    /**
     * Requests the user's email if available
     */
    Email(Scope("email"));

    /**
     * returns the scope as string
     */
    override fun toString(): String {
        return scope.name
    }
}