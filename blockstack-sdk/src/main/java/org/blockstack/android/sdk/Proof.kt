package org.blockstack.android.sdk

import org.json.JSONObject

/**
 * Object containing a social proof usually created by `BlockstackSession.validateProofs`.
 * The proof is not valid, if the claim couldn't be verified for whatever reasons.
 *
 * This object is backed by the original JSON representation.
 */
class Proof(private val jsonObject: JSONObject) {

    /**
     * The name of the social service.
     */
    val service: String
        get() = jsonObject.optString("service")

    /**
     * The url used to proof the claim.
     */
    val proofUrl: String
        get() = jsonObject.optString("proof_url")

    /**
     * The identifier of the social service that is claimed.
     */
    val identifier: String
        get() = jsonObject.optString("identifier")

    /**
     * The validity of the proof.
     */
    val valid: Boolean
        get() = jsonObject.optBoolean("valid")

    /**
     * The `JSONObject` that backs this object. You use this object to
     * access properties that are not yet exposed by this class.
     */
    val json: JSONObject
        get() = jsonObject
}