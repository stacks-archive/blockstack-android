package org.blockstack.android.sdk

import org.json.JSONException
import org.json.JSONObject

/**
 * Object containing details about errors from blockstack calls
 */
open class ResultError(
        /**
         * error code identifying the type of error
         */
        val code: ErrorCode,
        /**
         * details about the error
         */
        val message: String,
        /**
         * parameter
         */
        val parameter: String? = null,

        /**
         * json object representing the error, may contain more information
         */
        val json: JSONObject? = null) {

    /**
     * converts object to code:message string
     */
    override fun toString(): String {
        return "$code:$message" + (":${parameter?:""}")
    }

    companion object {
        /**
         * converts a json error object into the corresponding object
         * @param error json string representation of the error
         */
        fun fromJS(error: String): ResultError {
            try {
                val jsonError = JSONObject(error)
                return ResultError(ErrorCode.fromJS(jsonError.optString("code", ErrorCode.UnknownError.code)),
                        jsonError.optString("message"),
                        jsonError.optString("parameter"),
                        jsonError)
            } catch (e:JSONException) {
                return ResultError(ErrorCode.UnknownError, error)
            }
        }
    }
}

/*
class MissingParameterError(message: String, parameter: String?) : ResultError(ErrorCode.MissingParameterError, message, parameter)
class RemoteServiceError(val response: Any, message: String, parameter: String?) : ResultError(ErrorCode.RemoteServiceError, message, parameter)
class FailedDecryptionError(message: String, parameter: String?) : ResultError(ErrorCode.FailedDecryptionError, message, parameter)
class InvalidDidError(message: String, parameter: String?) : ResultError(ErrorCode.InvalidDidError, message, parameter)
class NotEnoughFundsError(val leftToFund: Number, message: String, parameter: String?) : ResultError(ErrorCode.MissingParameterError, message, parameter)
class InvalidAmountError(val fees: Number, val specifiedAmount: Number, message: String, parameter: String?) : ResultError(ErrorCode.InvalidAmountError, message, parameter)
class LoginFailedError(message: String, parameter: String?) : ResultError(ErrorCode.LoginFailedError, message, parameter)
class SignatureVerificationError(message: String, parameter: String?) : ResultError(ErrorCode.SignatureVerificationError, message, parameter)
class UnknownError(message: String, parameter: String?) : ResultError(ErrorCode.UnknownError, message, parameter)
*/

/**
 * An enum of error codes thrown by Blockstack calls
 * @property code identifies the error
 */
enum class ErrorCode(val code: String) {
    /**
     * Missing parameter
     */
    MissingParameterError("missing_parameter"),
    /**
     * Error thrown by remote service
     */
    RemoteServiceError("remote_service_error"),
    /**
     * Error during decyption
     */
    FailedDecryptionError("failed_decryption_error"),
    /**
     * Invalid decentralized ID
     */
    InvalidDidError("invalid_did_error"),
    /**
     * User has not enough funds
     */
    NotEnoughFundsError("not_enough_error"),
    /**
     * Amount is not valid
     */
    InvalidAmountError("invalid_amount_error"),
    /**
     * User could not be signed-in
     */
    LoginFailedError("login_failed_error"),
    /**
     * Signature could not be verified
     */
    SignatureVerificationError("signature_verification_error"),
    /**
     * User could not be redirected to sign in
     */

    RedirectFailed("redirect_failed_error"),
    /**
     * Network error
     */
    NetworkError("network_error"),

    /**
     * User does not provide a gaia bucket read url for shared data.
     * Usually, this means that the user has not yet used the corresponding app.
     */
    MissingReadUrl("missing_read_url"),

    /**
     * Other error than the one above
     */
    UnknownError("unknown");

    /**
     * the code as string
     */
    override fun toString(): String {
        return code
    }

    companion object {
        /**
         * converts a json error code into the corresponding enum
         */
        fun fromJS(code: String) =
                values().find { it.code == code }
                        ?: throw IllegalArgumentException("$code not valid")
    }
}
