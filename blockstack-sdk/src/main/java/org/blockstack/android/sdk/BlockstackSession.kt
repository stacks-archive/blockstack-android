package org.blockstack.android.sdk

import android.content.Context
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.*

private val AUTH_URL_STRING = "file:///android_res/raw/webview.html"

/**
 * Created by larry on 3/25/18.
 */

class BlockstackSession(private val context: Context,
                        private val appDomain: URI,
                        private val redirectURI: URI,
                        private val manifestURI: URI,
                        private val scopes: Array<String>,
                        onLoadedCallback: () -> Unit = {}) {

    private val TAG = BlockstackSession::class.qualifiedName
    private var userData: JSONObject? = null
    private var signInCallback: ((UserData) -> Unit)? = null
    private var userDataLoaded: ((UserData) -> Unit)? = null
    private val getFileCallbacks = HashMap<String, ((Any) -> Unit)>()
    private val putFileCallbacks = HashMap<String, ((String) -> Unit)>()

    init {
        Log.d(TAG, context.toString())
    }
    private val webView = WebView(context)
    init {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = BlockstackWebViewClient(context, onLoadedCallback)
        webView.addJavascriptInterface(JavascriptInterfaceObject(this),"android")
        webView.loadUrl(AUTH_URL_STRING)
    }

    fun handlePendingSignIn(authResponse: String, signInCallback: ((UserData) -> Unit)? = this.signInCallback) {
        this.signInCallback = signInCallback
        Log.d(TAG, "handlePendingSignIn")
        val javascript = "handlePendingSignIn('${authResponse}')"
        webView.evaluateJavascript(javascript, { result: String ->

        })
    }

    fun redirectUserToSignIn(signInCallback: (UserData) -> Unit ) {
        this.signInCallback = signInCallback
        Log.d(TAG, "redirectUserToSignIn")
        val scopesString = JSONArray(scopes).toString()
        val javascript = "redirectToSignIn('${appDomain}', '${redirectURI}', '${manifestURI}', ${scopesString})"
        webView.evaluateJavascript(javascript, { result: String ->
            // no op
        })
    }

    fun loadUserData(callback: (UserData) -> Unit) {
        userDataLoaded = callback
        val javascript = "loadUserData()"
        webView.evaluateJavascript(javascript, {result ->
            // no op
        })
    }

    /* Public storage methods */

    // getAppBucketUrl

    // getUserAppFileUrl


    fun getFile(path: String, options: GetFileOptions, callback: ((Any) -> Unit)) {
        Log.d(TAG, "getFile: path: ${path} options: ${options}")
        val uniqueIdentifier = addGetFileCallback(callback)
        val javascript = "getFile('${path}', ${options}, '${uniqueIdentifier}')"
        webView.evaluateJavascript(javascript, { result: String ->
            // no op
        })
    }

    fun putFile(path: String, content: Any, options: PutFileOptions, callback: ((String) -> Unit)) {
        Log.d(TAG, "putFile: path: ${path} options: ${options}")

        val valid = content is String || content is ByteArray
        if(!valid) {
            throw IllegalArgumentException("putFile content only supports String or ByteArray")
        }

        val isBinary = content is ByteArray

        val uniqueIdentifier = addPutFileCallback(callback)
        if(isBinary) {
            val contentString = Base64.encodeToString(content as ByteArray, Base64.NO_WRAP)
            val javascript = "putFile('${path}', '${contentString}', ${options}, '${uniqueIdentifier}', true)"
            webView.evaluateJavascript(javascript, { result: String ->
                // no op
            })
        } else {
            val javascript = "putFile('${path}', '${content}', ${options}, '${uniqueIdentifier}', false)"
            webView.evaluateJavascript(javascript, { result: String ->
                // no op
            })
        }

    }

    private fun addGetFileCallback(callback: (Any) -> Unit): String {
        val uniqueIdentifier = UUID.randomUUID().toString()
        getFileCallbacks[uniqueIdentifier] = callback
        return uniqueIdentifier
    }

    private fun addPutFileCallback(callback: (String) -> Unit): String {
        val uniqueIdentifier = UUID.randomUUID().toString()
        putFileCallbacks[uniqueIdentifier] = callback
        return uniqueIdentifier
    }

    private class JavascriptInterfaceObject(private val session: BlockstackSession) {

        @JavascriptInterface
        fun signInSuccess(userDataString: String) {
            Log.d(session.TAG, "signInSuccess" )
            val userData = JSONObject(userDataString)
            session.userData = userData
            Log.d(session.TAG, session.userData.toString() )
            session.signInCallback?.invoke(UserData(userData))
        }


        @JavascriptInterface
        fun userDataLoaded(userDataString: String) {
            Log.d(session.TAG, "userDataLoaded" )
            val userData = JSONObject(userDataString)
            session.userData = userData
            Log.d(session.TAG, session.userData.toString() )
            session.userDataLoaded?.invoke(UserData(userData))
        }

        @JavascriptInterface
        fun getFileResult(content: String, uniqueIdentifier: String, isBinary: Boolean) {
            Log.d(session.TAG, "putFileResult" )

            if (isBinary) {
                val binaryContent: ByteArray = Base64.decode(content as String, Base64.DEFAULT)
                session.getFileCallbacks[uniqueIdentifier]?.invoke(binaryContent)
            } else {
                session.getFileCallbacks[uniqueIdentifier]?.invoke(content)
            }
            session.getFileCallbacks.remove(uniqueIdentifier)
        }

        @JavascriptInterface
        fun putFileResult(readURL: String, uniqueIdentifier: String) {
            Log.d(session.TAG, "putFileResult" )
            session.putFileCallbacks[uniqueIdentifier]?.invoke(readURL)
            session.putFileCallbacks.remove(uniqueIdentifier)
        }

    }

}

private class BlockstackWebViewClient(val context: Context, val onLoadedCallback: () -> Unit ) : WebViewClient() {
    private val TAG = BlockstackWebViewClient::class.qualifiedName

    override fun onPageFinished(view: WebView?, url: String?) {
        Log.d(TAG, "url loaded:" + url)
        if (AUTH_URL_STRING == url) {
            onLoadedCallback()
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        // initially overriding a function that's deprecated in API 27 in order to support API 15
        Log.d(TAG, "Navigation detected in sign in webview")
        Log.d(TAG, url)

        val authRequestToken = url.split(':')[1]
        Log.d(TAG, authRequestToken)

        val customTabsIntent = CustomTabsIntent.Builder().build()
        // on redirect load the following with
        // TODO: handle lack of custom tabs support
        customTabsIntent.launchUrl(context, Uri.parse("https://browser.blockstack.org/auth?authRequest=${authRequestToken}"))
        return true
    }
}
