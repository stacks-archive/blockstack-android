package org.blockstack.android.sdk

/**
 * Object representing the result of a blockstack method call
 *
 * @property value the result of the method call
 * @property error the error with code and message
 */
class Result<T>(val value: T?, val error: ResultError? = null) {

    /**
     * returns true if the method call returned a value successfully
     */
    inline val hasValue: Boolean
        get() = value != null

    /**
     * returns true if the method did not return a value, but an error
     */
    inline val hasErrors: Boolean
        get() = value == null && error != null

}