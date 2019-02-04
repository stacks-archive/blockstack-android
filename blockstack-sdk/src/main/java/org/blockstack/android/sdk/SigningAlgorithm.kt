package org.blockstack.android.sdk

/**
 * An enum of algorithms used to sign tokens
 *
 * @property name identifies the algorithm
 */
enum class SigningAlgorithm(val algorithmName: String) {
    /**
     * Represents ECDSA with a P-256K curve.
     */
    ES256K ("ES256K")
}