package org.blockstack.android.sdk

import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import org.blockstack.android.sdk.model.network.AccountStatus
import org.blockstack.android.sdk.model.network.Denomination
import org.blockstack.android.sdk.model.network.NameInfo
import org.blockstack.android.sdk.model.network.NamespaceInfo
import org.json.JSONObject
import java.math.BigInteger

/**
 * Object giving access to information about the blockstack network.
 */
class Network internal constructor(private val v8networkAndroid: V8Object, val v8: V8) {

    private var getNameInfoCallback: ((Result<NameInfo>) -> Unit)? = null
    private var getNamePriceCallback: ((Result<Denomination>) -> Unit)? = null

    init {
        registerJSNetworkBridgeMethods()
    }

    private fun registerJSNetworkBridgeMethods() {
        val network = JSNetworkBridge(this)
        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))

        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))
    }

    fun getNamePrice(fullyQualifiedName: String, callback: (Result<Denomination>) -> Unit) {
        getNamePriceCallback = callback
        val v8Params = V8Array(v8)
                .push(fullyQualifiedName)
        v8networkAndroid.executeVoidFunction("getNamePrice", v8Params)
        v8Params.release()
    }

    private var getNamespacePriceCallback: ((Result<Denomination>) -> Unit)? = null

    fun getNamespacePrice(namespaceID: String, callback: (Result<Denomination>) -> Unit) {
        getNamespacePriceCallback = callback
        val v8Params = V8Array(v8)
                .push(namespaceID)
        v8networkAndroid.executeVoidFunction("getNamespacePrice", v8Params)
        v8Params.release()
    }

    private var getGracePeriodCallback: ((Result<Int>) -> Unit)? = null

    fun getGracePeriod(callback: (Result<Int>) -> Unit) {
        getGracePeriodCallback = callback
        v8networkAndroid.executeVoidFunction("getGracePeriod", null)
    }

    private var getNamesOwnedCallback: ((Result<List<String>>) -> Unit)? = null

    fun getNamesOwned(address: String, callback: (Result<List<String>>) -> Unit) {
        getNamesOwnedCallback = callback
        val v8Params = V8Array(v8)
                .push(address)
        v8networkAndroid.executeVoidFunction("getNamesOwned", v8Params)
        v8Params.release()
    }

    private var getNamespaceBurnAddressCallback: ((Result<String>) -> Unit)? = null

    fun getNamespaceBurnAddress(namespaceId: String, callback: (Result<String>) -> Unit) {
        getNamespaceBurnAddressCallback = callback
        val v8Params = V8Array(v8)
                .push(namespaceId)
        v8networkAndroid.executeVoidFunction("getNamespaceBurnAddress", v8Params)
        v8Params.release()
    }

    fun getNameInfo(fullyQualifiedName: String, callback: (Result<NameInfo>) -> Unit) {
        getNameInfoCallback = callback
        val v8params = V8Array(v8)
                .push(fullyQualifiedName)
        v8networkAndroid.executeVoidFunction("getNameInfo", v8params)
        v8params.release()
    }

    private var getNamespaceInfoCallback: ((Result<NamespaceInfo>) -> Unit)? = null

    fun getNamespaceInfo(namespaceId: String, callback: (Result<NamespaceInfo>) -> Unit) {
        getNamespaceInfoCallback = callback
        val v8Params = V8Array(v8)
                .push(namespaceId)
        v8networkAndroid.executeVoidFunction("getNamespaceInfo", v8Params)
        v8Params.release()
    }

    private var getZonefileCallback: ((Result<String>) -> Unit)? = null

    fun getZonefile(zonefileHash: String, callback: (Result<String>) -> Unit) {
        getZonefileCallback = callback
        val v8Params = V8Array(v8)
                .push(zonefileHash)
        v8networkAndroid.executeVoidFunction("getZonefile", v8Params)
        v8Params.release()
    }

    private var getAccountStatusCallback: ((Result<AccountStatus>) -> Unit)? = null

    fun getAccountStatus(address: String, tokenType: String, callback: (Result<AccountStatus>) -> Unit) {
        getAccountStatusCallback = callback
        val v8Params = V8Array(v8)
                .push(address)
                .push(tokenType)
        v8networkAndroid.executeVoidFunction("getAccountStatus", v8Params)
        v8Params.release()
    }

    private var getAccountHistoryPageCallback: ((Result<List<AccountStatus>>) -> Unit)? = null

    fun getAccountHistoryPage(address: String, page: Int, callback: (Result<List<AccountStatus>>) -> Unit) {
        getAccountHistoryPageCallback = callback
        val v8Params = V8Array(v8)
                .push(address)
                .push(page)
        v8networkAndroid.executeVoidFunction("getAccountHistoryPage", v8Params)
        v8Params.release()
    }

    private var getAccountAtCallback: ((Result<List<AccountStatus>>) -> Unit)? = null

    fun getAccountAt(address: String, blockHeight: Int, callback: (Result<List<AccountStatus>>) -> Unit) {
        getAccountAtCallback = callback
        val v8Params = V8Array(v8)
                .push(address)
                .push(blockHeight)
        v8networkAndroid.executeVoidFunction("getAccountAt", v8Params)
        v8Params.release()
    }

    private var getAccountTokensCallback: ((Result<List<String>>) -> Unit)? = null

    fun getAccountTokens(address: String, callback: (Result<List<String>>) -> Unit) {
        getAccountTokensCallback = callback
        val v8Params = V8Array(v8)
                .push(address)
        v8networkAndroid.executeVoidFunction("getAccountTokens", v8Params)
        v8Params.release()
    }

    private var getAccountBalanceCallback: ((Result<BigInteger>) -> Unit)? = null

    fun getAccountBalance(address: String, tokenType: String, callback: (Result<BigInteger>) -> Unit) {
        getAccountBalanceCallback = callback
        val v8Params = V8Array(v8)
                .push(address)
                .push(tokenType)
        v8networkAndroid.executeVoidFunction("getAccountBalance", v8Params)
        v8Params.release()
    }

    private var estimateTokenTransferCallback: ((Result<BigInteger>) -> Unit)? = null

    fun estimateTokenTransfer(recipientAddress: String, tokenType: String, tokenAmount: BigInteger, scratchArea: String, senderUtxos: Number = 1, additionalOutputs: Number = 1, callback: (Result<BigInteger>) -> Unit) {
        estimateTokenTransferCallback = callback
        val v8Params = V8Array(v8)
                .push(recipientAddress)
                .push(tokenType)
                .push(tokenAmount)
                .push(scratchArea)
                .push(senderUtxos)
                .push(additionalOutputs)
        v8networkAndroid.executeVoidFunction("estimateTokenTransfer", v8Params)
        v8Params.release()
    }

    private class JSNetworkBridge(private val network: Network) {
        fun getNamePriceResult(denomination: String) {
            network.getNamePriceCallback?.invoke(Result(Denomination(JSONObject(denomination))))
        }

        fun getNamePriceFailure(error: String) {
            network.getNamePriceCallback?.invoke(Result(null, error))
        }

        fun getNamespacePriceResult(namespacePrice: String) {
            network.getNamespacePriceCallback?.invoke(Result(Denomination(JSONObject(namespacePrice))))
        }
        fun getNamespacePriceFailure(error: String) {
            network.getNamespacePriceCallback?.invoke(Result(null, error))
        }

        fun getGracePeriodResult(gracePeriod: Int) {
            network.getGracePeriodCallback?.invoke(Result(gracePeriod))
        }
        fun getGracePeriodFailure(error: String) {
            network.getGracePeriodCallback?.invoke(Result(null, error))
        }

        fun getNamesOwnedResult(names: List<String>) {
            network.getNamesOwnedCallback?.invoke(Result(names))
        }
        fun getNamesOwnedFailure(error: String) {
            network.getNamesOwnedCallback?.invoke(Result(null, error))
        }

        fun getNamespaceBurnAddressResult(burnAddress: String) {
            network.getNamespaceBurnAddressCallback?.invoke(Result(burnAddress))
        }
        fun getNamespaceBurnAddressFailure(error: String) {
            network.getNamespaceBurnAddressCallback?.invoke(Result(null, error))
        }

        fun getNameInfoResult(nameInfo: String) {
            network.getNameInfoCallback?.invoke(Result(NameInfo(JSONObject(nameInfo))))
        }
        fun getNameInfoFailure(error: String) {
            network.getNameInfoCallback?.invoke(Result(null, error))
        }

        fun getNamespaceInfo(namespaceInfo: String) {
            network.getNamespaceInfoCallback?.invoke(Result(NamespaceInfo(JSONObject(namespaceInfo))))
        }
        fun getNamespaceInfoFailure(error: String) {
            network.getNamespaceInfoCallback?.invoke(Result(null, error))
        }

        fun getNameInfoResult(nameInfo: String) {
            network.getNameInfoCallback?.invoke(Result(NameInfo(JSONObject(nameInfo))))
        }

        fun getNameInfoFailure(error: String) {
            network.getNameInfoCallback?.invoke(Result(null, error))
        }
        fun getNameInfoResult(nameInfo: String) {
            network.getNameInfoCallback?.invoke(Result(NameInfo(JSONObject(nameInfo))))
        }

        fun getNameInfoFailure(error: String) {
            network.getNameInfoCallback?.invoke(Result(null, error))
        }
        fun getNameInfoResult(nameInfo: String) {
            network.getNameInfoCallback?.invoke(Result(NameInfo(JSONObject(nameInfo))))
        }

        fun getNameInfoFailure(error: String) {
            network.getNameInfoCallback?.invoke(Result(null, error))
        }
        fun getNameInfoResult(nameInfo: String) {
            network.getNameInfoCallback?.invoke(Result(NameInfo(JSONObject(nameInfo))))
        }

        fun getNameInfoFailure(error: String) {
            network.getNameInfoCallback?.invoke(Result(null, error))
        }
        fun getNameInfoResult(nameInfo: String) {
            network.getNameInfoCallback?.invoke(Result(NameInfo(JSONObject(nameInfo))))
        }

        fun getNameInfoFailure(error: String) {
            network.getNameInfoCallback?.invoke(Result(null, error))
        }
        fun getNameInfoResult(nameInfo: String) {
            network.getNameInfoCallback?.invoke(Result(NameInfo(JSONObject(nameInfo))))
        }

        fun getNameInfoFailure(error: String) {
            network.getNameInfoCallback?.invoke(Result(null, error))
        }
        fun getNameInfoResult(nameInfo: String) {
            network.getNameInfoCallback?.invoke(Result(NameInfo(JSONObject(nameInfo))))
        }

        fun getNameInfoFailure(error: String) {
            network.getNameInfoCallback?.invoke(Result(null, error))
        }
        fun getNameInfoResult(nameInfo: String) {
            network.getNameInfoCallback?.invoke(Result(NameInfo(JSONObject(nameInfo))))
        }

        fun getNameInfoFailure(error: String) {
            network.getNameInfoCallback?.invoke(Result(null, error))
        }

        fun getNameInfoResult(nameInfo: String) {
            network.getNameInfoCallback?.invoke(Result(NameInfo(JSONObject(nameInfo))))
        }

        fun getNameInfoFailure(error: String) {
            network.getNameInfoCallback?.invoke(Result(null, error))
        }
    }

}