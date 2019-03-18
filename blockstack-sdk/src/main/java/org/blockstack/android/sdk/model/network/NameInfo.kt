package org.blockstack.android.sdk.model.network

import org.json.JSONObject

/**
 * Object containing information about a name including
 * address that owns it, the block at which it expires, and the zone file anchored to it (if available).
 */
class NameInfo(private val jsonObject: JSONObject) {
    /**
     * The owning address.
     */
    val address: String?
        get() {
            return jsonObject.optString("address")
        }

    /**
     * The underlying blockchain.
     */
    val blockchain: String?
        get() {
            return jsonObject.optString("blockchain")
        }

    /**
     * The default decentralized identifier.
     */
    val did: String?
        get() {
            return jsonObject.optString("did")
        }

    /**
     * The id of the last transaction.
     */
    val lastTxid: String?
        get() {
            return jsonObject.optString("last_txid")
        }

    /**
     * The status of the name, one of `revoked`, `registered`, `registered_subdomain`, `available`.
     */
    val status: String?
        get() {
            return jsonObject.optString("status")
        }

    /**
     * The zone file's text content if available.
     */
    val zonefile: String?
        get() {
            return jsonObject.optString("zonefile")
        }

    /**
     * The hash of the associated zone file.
     */
    val zonefileHash: String?
        get() {
            return jsonObject.optString("zonefile_hash")
        }

    /**
     * When the name expires. Not available for subdomains.
     */
    val expireBlock: String?
        get() {
            return jsonObject.optString("expire_block")
        }

    /**
     * When the name needs to be renewed. Not available for subdomains.
     */
    val renewalDeadline: String?
        get() {
            return jsonObject.optString("renewal_deadline")
        }


    /**
     * Number of blocks before an expired name can be owned by somebody else. Not available for subdomains.
     */
    val gracePeriod: String?
        get() {
            return jsonObject.optString("grace_period")
        }

    /**
     * How the resolve subdomains of this name. Not available for subdomains.
     */
    val resolver: String?
        get() {
            return jsonObject.optString("resolver")
        }

    /**
     * The `JSONObject` that backs this object. You use this object to
     * access properties that are not yet exposed by this class.
     */
    val json: JSONObject
        get() {
            return jsonObject
        }
}