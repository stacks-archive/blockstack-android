package org.blockstack.android.sdk.model.network

import org.json.JSONObject
import java.math.BigInteger

/**
 * Object containing information about an account for a particular token type.
 * This includes its total number of expenditures and credits, lockup times, last txid, and so on.
 */
class AccountStatus(private val jsonObject: JSONObject) {
    /**
     * The account's address.
     */
    val address: String?
        get() {
            return jsonObject.optString("address")
        }

    /**
     * The block this status belongs to.
     */
    val blockId: Int?
        get() {
            return jsonObject.optInt("block_id")
        }

    /**
     * The unit of the token this status belongs to, e.g. BTC, STACKS.
     */
    val type: String?
        get() {
            return jsonObject.optString("type")
        }

    /**
     * The credit value for this account for the token.
     */
    val creditValue: BigInteger?
        get() {
            return jsonObject.optString("credit_value")?.let { BigInteger(it) }
        }

    /**
     * The debit value for this account for the token.
     */
    val debitValue: BigInteger?
        get() {
            return jsonObject.optString("debit_value")?.let { BigInteger(it) }
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