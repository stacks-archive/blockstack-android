package org.blockstack.android.sdk

import android.util.Log
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import org.blockstack.android.sdk.model.network.AccountStatus
import org.blockstack.android.sdk.model.network.Denomination
import org.blockstack.android.sdk.model.network.NameInfo
import org.blockstack.android.sdk.model.network.NamespaceInfo
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger

/**
 * Object giving access to information about the blockstack network.
 */
class Network internal constructor(private val v8networkAndroid: V8Object, val v8: V8) {

    init {
        registerJSNetworkBridgeMethods()
    }

    private fun registerJSNetworkBridgeMethods() {
        val network = JSNetworkBridge(this)
        v8networkAndroid.registerJavaMethod(network, "getNamePriceResult", "getNamePriceResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNamePriceFailure", "getNamePriceFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNamespacePriceResult", "getNamespacePriceResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNamespacePriceFailure", "getNamespacePriceFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getGracePeriodResult", "getGracePeriodResult", arrayOf<Class<*>>(Int::class.java))
        v8networkAndroid.registerJavaMethod(network, "getGracePeriodFailure", "getGracePeriodFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNamesOwnedResult", "getNamesOwnedResult", arrayOf<Class<*>>(V8Array::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNamesOwnedFailure", "getNamesOwnedFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNamespaceBurnAddressResult", "getNamespaceBurnAddressResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNamespaceBurnAddressFailure", "getNamespaceBurnAddressFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNamespaceInfoResult", "getNamespaceInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNamespaceInfoFailure", "getNamespaceInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getZonefileResult", "getZonefileResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getZonefileFailure", "getZonefileFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getAccountStatusResult", "getAccountStatusResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getAccountStatusFailure", "getAccountStatusFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getAccountHistoryPageResult", "getAccountHistoryPageResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getAccountHistoryPageFailure", "getAccountHistoryPageFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getAccountAtResult", "getAccountAtResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getAccountAtFailure", "getAccountAtFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getAccountTokensResult", "getAccountTokensResult", arrayOf<Class<*>>(V8Array::class.java))
        v8networkAndroid.registerJavaMethod(network, "getAccountTokensFailure", "getAccountTokensFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getAccountBalanceResult", "getAccountBalanceResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getAccountBalanceFailure", "getAccountBalanceFailure", arrayOf<Class<*>>(String::class.java))

    }

    private var getNamePriceCallback: ((Result<Denomination>) -> Unit)? = null

    /**
     * Get the price to pay for registering a name.
     * @param fullyQualifiedName can be a name or subdomain name.
     * @param callback  called with a result object that contains the denomination (units, amount) of the requested price
     * or error if the request failed.
     */
    fun getNamePrice(fullyQualifiedName: String, callback: (Result<Denomination>) -> Unit) {
        getNamePriceCallback = callback
        val v8Params = V8Array(v8)
                .push(fullyQualifiedName)
        v8networkAndroid.executeVoidFunction("getNamePrice", v8Params)
        v8Params.release()
    }

    private var getNamespacePriceCallback: ((Result<Denomination>) -> Unit)? = null

    /**
     * Get the price to pay for registering a namesapce.
     * @param fullyQualifiedName can be a name or subdomain name.
     * @param callback  called with a result object that contains the denomination (units, amount) of the requested price
     * or error if the request failed.
     */
    fun getNamespacePrice(namespaceID: String, callback: (Result<Denomination>) -> Unit) {
        getNamespacePriceCallback = callback
        val v8Params = V8Array(v8)
                .push(namespaceID)
        v8networkAndroid.executeVoidFunction("getNamespacePrice", v8Params)
        v8Params.release()
    }

    private var getGracePeriodCallback: ((Result<Int>) -> Unit)? = null

    /**
     * Get the number of blocks that can pass between a name expiring and the name being able to be re-registered by a different owner.
     * @param callback called with a result object that contains the number of blocks or error if the request failed.
     */
    fun getGracePeriod(callback: (Result<Int>) -> Unit) {
        getGracePeriodCallback = callback
        v8networkAndroid.executeVoidFunction("getGracePeriod", null)
    }

    private var getNamesOwnedCallback: ((Result<List<String>>) -> Unit)? = null

    /**
     * Get the names -- both on-chain and off-chain -- owned by an address.
     * @param address the blockchain address (the hash of the owner public key)
     * @param callback called with a result object that contains the list of names or error if the request failed.
     */
    fun getNamesOwned(address: String, callback: (Result<List<String>>) -> Unit) {
        getNamesOwnedCallback = callback
        val v8Params = V8Array(v8)
                .push(address)
        v8networkAndroid.executeVoidFunction("getNamesOwned", v8Params)
        v8Params.release()
    }

    private var getNamespaceBurnAddressCallback: ((Result<String>) -> Unit)? = null

    /**
     * Get the blockchain address to which a name's registration fee must be sent (the address will depend on the namespace in which it is registered.).
     * @param namespaceId the namespace ID
     * @param callback called with a result object that contains the address as string or error if the request failed.
     */
    fun getNamespaceBurnAddress(namespaceId: String, callback: (Result<String>) -> Unit) {
        getNamespaceBurnAddressCallback = callback
        val v8Params = V8Array(v8)
                .push(namespaceId)
        v8networkAndroid.executeVoidFunction("getNamespaceBurnAddress", v8Params)
        v8Params.release()
    }

    private var getNameInfoCallback: ((Result<NameInfo>) -> Unit)? = null

    /**
     * Get WHOIS-like information for a name, including the address that owns it, the block at which it expires, and the zone file anchored to it (if available).
     * @param fullyQualifiedName the name to query. Can be on-chain of off-chain.
     * @param callback called with a result object that contains the WHOIS-like name information or error if the request failed.
     */
    fun getNameInfo(fullyQualifiedName: String, callback: (Result<NameInfo>) -> Unit) {
        getNameInfoCallback = callback
        val v8params = V8Array(v8)
                .push(fullyQualifiedName)
        v8networkAndroid.executeVoidFunction("getNameInfo", v8params)
        v8params.release()
    }

    private var getNamespaceInfoCallback: ((Result<NamespaceInfo>) -> Unit)? = null

    /**
     *Get the pricing parameters and creation history of a namespace.
     * @param namespaceId the namespace to query
     * @param callback called with a result object that contains the namespace information or error if the request failed.
     */
    fun getNamespaceInfo(namespaceId: String, callback: (Result<NamespaceInfo>) -> Unit) {
        getNamespaceInfoCallback = callback
        val v8Params = V8Array(v8)
                .push(namespaceId)
        v8networkAndroid.executeVoidFunction("getNamespaceInfo", v8Params)
        v8Params.release()
    }

    private var getZonefileCallback: ((Result<String>) -> Unit)? = null

    /**
     * Get a zone file, given its hash.
     * @param zonefileHash the ripemd160(sha256) hash of the zone file.
     * @param callback called with a result object that contains the zone file's text
     * or error if the request failed or the zone file obtained does not match the hash.
     */
    fun getZonefile(zonefileHash: String, callback: (Result<String>) -> Unit) {
        getZonefileCallback = callback
        val v8Params = V8Array(v8)
                .push(zonefileHash)
        v8networkAndroid.executeVoidFunction("getZonefile", v8Params)
        v8Params.release()
    }

    private var getAccountStatusCallback: ((Result<AccountStatus>) -> Unit)? = null

    /**
     * Get the status of an account for a particular token holding. This includes its total number of expenditures and credits, lockup times, last txid, and so on.
     * @param address  the account's address
     * @param tokenType the token type to query
     * @param callback called with a result object that contains the state of the account for this token
     * or error if the request failed
     */
    fun getAccountStatus(address: String, tokenType: String, callback: (Result<AccountStatus>) -> Unit) {
        getAccountStatusCallback = callback
        val v8Params = V8Array(v8)
                .push(address)
                .push(tokenType)
        v8networkAndroid.executeVoidFunction("getAccountStatus", v8Params)
        v8Params.release()
    }

    private var getAccountHistoryPageCallback: ((Result<List<AccountStatus>>) -> Unit)? = null

    /**
     * Get a page of an account's transaction history.
     * @param address the account's address
     * @param page the page number. Page 0 contains the most recent transactions
     * @param callback called with a result object that contains a list of account statuses at various block heights (e.g. prior balances, txids, etc)
     */
    fun getAccountHistoryPage(address: String, page: Int, callback: (Result<List<AccountStatus>>) -> Unit) {
        getAccountHistoryPageCallback = callback
        val v8Params = V8Array(v8)
                .push(address)
                .push(page)
        v8networkAndroid.executeVoidFunction("getAccountHistoryPage", v8Params)
        v8Params.release()
    }

    private var getAccountAtCallback: ((Result<List<AccountStatus>>) -> Unit)? = null

    /**
     * Get the state(s) of an account at a particular block height. This includes the state of the account
     * beginning with this block's transactions,
     * as well as all of the states the account passed through when this block was processed (if any).
     * @param address the accounts's address.
     * @param blockHeight the block to query.
     * @param callback called with result object that contains the account states of the account at this block
     * or error if the request failed
     */
    fun getAccountAt(address: String, blockHeight: Int, callback: (Result<List<AccountStatus>>) -> Unit) {
        getAccountAtCallback = callback
        val v8Params = V8Array(v8)
                .push(address)
                .push(blockHeight)
        v8networkAndroid.executeVoidFunction("getAccountAt", v8Params)
        v8Params.release()
    }

    private var getAccountTokensCallback: ((Result<List<String>>) -> Unit)? = null

    /**
     * Get the set of token types that this account owns
     * @param address the accounts's address
     * @param callback called with a result object that contains the list of types of token this account holds (excluding the underlying blockchain's tokens)
     * or error if the request failed
     */
    fun getAccountTokens(address: String, callback: (Result<List<String>>) -> Unit) {
        getAccountTokensCallback = callback
        val v8Params = V8Array(v8)
                .push(address)
        v8networkAndroid.executeVoidFunction("getAccountTokens", v8Params)
        v8Params.release()
    }

    private var getAccountBalanceCallback: ((Result<BigInteger>) -> Unit)? = null

    /**
     * Get the number of tokens owned by an account. If the account does not exist or has no tokens of this type, then 0 will be returned.
     * @param address the account's address.
     * @param tokenType the type of token to query.
     * @param callback called with a result object that contains the number of tokens held by this account in
     * the smallest denomination.
     */
    fun getAccountBalance(address: String, tokenType: String, callback: (Result<BigInteger>) -> Unit) {
        getAccountBalanceCallback = callback
        val v8Params = V8Array(v8)
                .push(address)
                .push(tokenType)
        v8networkAndroid.executeVoidFunction("getAccountBalance", v8Params)
        v8Params.release()
    }

    private class JSNetworkBridge(private val network: Network) {
        fun getNamePriceResult(denomination: String) {
            network.getNamePriceCallback?.invoke(Result(Denomination(JSONObject(denomination))))
        }

        fun getNamePriceFailure(error: String) {
            network.getNamePriceCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }

        fun getNamespacePriceResult(namespacePrice: String) {
            network.getNamespacePriceCallback?.invoke(Result(Denomination(JSONObject(namespacePrice))))
        }

        fun getNamespacePriceFailure(error: String) {
            network.getNamespacePriceCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }

        fun getGracePeriodResult(gracePeriod: Int) {
            network.getGracePeriodCallback?.invoke(Result(gracePeriod))
        }

        fun getGracePeriodFailure(error: String) {
            network.getGracePeriodCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }

        fun getNamesOwnedResult(names: V8Array) {
            val nameList = ArrayList<String>()
            for (index in 0 until names.length()) {
                nameList.add(names.getString(index))
            }
            names.release()
            network.getNamesOwnedCallback?.invoke(Result(nameList))
        }

        fun getNamesOwnedFailure(error: String) {
            network.getNamesOwnedCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }

        fun getNamespaceBurnAddressResult(burnAddress: String) {
            network.getNamespaceBurnAddressCallback?.invoke(Result(burnAddress))
        }

        fun getNamespaceBurnAddressFailure(error: String) {
            network.getNamespaceBurnAddressCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }

        fun getNameInfoResult(nameInfo: String) {
            network.getNameInfoCallback?.invoke(Result(NameInfo(JSONObject(nameInfo))))
        }

        fun getNameInfoFailure(error: String) {
            network.getNameInfoCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }

        fun getNamespaceInfoResult(namespaceInfo: String) {
            network.getNamespaceInfoCallback?.invoke(Result(NamespaceInfo(JSONObject(namespaceInfo))))
        }

        fun getNamespaceInfoFailure(error: String) {
            network.getNamespaceInfoCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }

        fun getZonefileResult(zoneFileContent: String) {
            network.getZonefileCallback?.invoke(Result(zoneFileContent))
        }

        fun getZonefileFailure(error: String) {
            network.getZonefileCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }

        fun getAccountStatusResult(accountStatus: String) {
            network.getAccountStatusCallback?.invoke(Result(AccountStatus(JSONObject(accountStatus))))
        }

        fun getAccountStatusFailure(error: String) {
            network.getAccountStatusCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }

        fun getAccountHistoryPageResult(accountStatuses: String) {
            val list = JSONArray(accountStatuses)
            val accountHistoryPage = ArrayList<AccountStatus>(list.length())
            for (index in 0..list.length() - 1) {
                accountHistoryPage.add(AccountStatus(list.getJSONObject(index)))
            }
            network.getAccountHistoryPageCallback?.invoke(Result(accountHistoryPage))
        }

        fun getAccountHistoryPageFailure(error: String) {
            network.getAccountHistoryPageCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }

        fun getAccountAtResult(accountStatuses: String) {
            val list = JSONArray(accountStatuses)
            val account = ArrayList<AccountStatus>(list.length())
            for (index in 0..list.length() - 1) {
                account.add(AccountStatus(list.getJSONObject(index)))
            }
            network.getAccountAtCallback?.invoke(Result(account))
        }

        fun getAccountAtFailure(error: String) {
            network.getAccountAtCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }

        fun getAccountTokensResult(tokens: V8Array) {
            Log.d("network", "received " + tokens.length())
            val tokenList = ArrayList<String>()
            for (index in 0 until tokens.length()) {
                tokenList.add(tokens.getString(index))
            }
            tokens.release()
            network.getAccountTokensCallback?.invoke(Result(tokenList))
        }

        fun getAccountTokensFailure(error: String) {
            network.getAccountTokensCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }

        fun getAccountBalanceResult(balance: String) {
            val value = BigInteger(balance)
            network.getAccountBalanceCallback?.invoke(Result(value))
        }

        fun getAccountBalanceFailure(error: String) {
            network.getAccountBalanceCallback?.invoke(Result(null, ResultError.fromJS(error)))
        }
    }

}