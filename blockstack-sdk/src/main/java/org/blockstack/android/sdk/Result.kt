package org.blockstack.android.sdk

class Result<T>(val value: T?, val error: String? = null) {

    val hasValue: Boolean
        get() = value != null

    val hasErrors: Boolean
        get() = value == null && error != null && !error.isEmpty()

}