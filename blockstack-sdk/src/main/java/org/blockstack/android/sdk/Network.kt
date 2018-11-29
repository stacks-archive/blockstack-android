package org.blockstack.android.sdk

import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import org.json.JSONObject

/**
 * Object giving access to information about the blockstack network.
 */
class Network internal constructor(private val v8networkAndroid: V8Object, val v8: V8) {
    
    private var getNameInfoCallback: ((Result<NameInfo>) -> Unit)? = null

    init {
        registerJSNetworkBridgeMethods()
    }

    private fun registerJSNetworkBridgeMethods() {
        val network = JSNetworkBridge(this)
        v8networkAndroid.registerJavaMethod(network, "getNameInfoResult", "getNameInfoResult", arrayOf<Class<*>>(String::class.java))
        v8networkAndroid.registerJavaMethod(network, "getNameInfoFailure", "getNameInfoFailure", arrayOf<Class<*>>(String::class.java))
    }

    fun getNameInfo(fullyQualifiedName: String, callback: (Result<NameInfo>) -> Unit) {
        getNameInfoCallback = callback
        val v8params = V8Array(v8)
                .push(fullyQualifiedName)
        v8networkAndroid.executeVoidFunction("getNameInfo", v8params)
        v8params.release()
    }

    private class JSNetworkBridge (private val network: Network) {
        fun getNameInfoResult(nameInfo: String) {
            network.getNameInfoCallback?.invoke(Result(NameInfo(JSONObject(nameInfo))))
        }

        fun getNameInfoFailure(error: String) {
            network.getNameInfoCallback?.invoke(Result(null, error))
        }
    }

}