package org.blockstack.android.sdk

/**
 * An enum of algorithms used to sign tokens
 *
 * @property name identifies the algorithm
 */
enum class SigningAlgorithm(name: String) {
    /**
     * Represents ECDSA with a P-256K curve.
     */
    ESK256 ("ES256K")
}