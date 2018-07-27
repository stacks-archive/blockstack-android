package org.blockstack.android.sdk

/**
 * Object representing the result of a blockstack method call
 *
 * @property value the result of the method call
 * @property error the error as string
 */
class Result<T>(val value: T?, val error: String? = null) {

    /**
     * returns true if the method call returned a value successfully
     */
    val hasValue: Boolean
        get() = value != null

    /**
     * returns true if the method did not return a value, but an error
     */
    val hasErrors: Boolean
        get() = value == null && error != null && !error.isEmpty()

}