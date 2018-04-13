package org.blockstack.android.sdk

import android.content.Context
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.*

/**
 * Created by larry on 3/25/18.
 */

class BlockstackSession(private val context: Context,
                        private val appDomain: URI,
                        private val redirectURI: URI,
                        private val manifestURI: URI,
                        private val scopes: Array<String>) {

    private val TAG = BlockstackSession::class.qualifiedName
    private var userData: JSONObject? = null
    private var signInCallback: ((JSONObject) -> Unit)? = null
    private val getFileCallbacks = HashMap<String, ((String) -> Unit)>()
    private val putFileCallbacks = HashMap<String, ((String) -> Unit)>()

    init {
        Log.d(TAG, context.toString())
    }
    private val webView = WebView(context)
    init {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = BlockstackWebViewClient(context)
        webView.addJavascriptInterface(JavascriptInterfaceObject(this),"android")
        webView.loadUrl("file:///android_res/raw/webview.html")
    }

    fun handlePendingSignIn(authResponse: String) {
        Log.d(TAG, "handlePendingSignIn")
        val javascript = "handlePendingSignIn('${authResponse}')"
        webView.evaluateJavascript(javascript, { result: String ->

        })
    }

    fun redirectUserToSignIn(signInCallback: (JSONObject) -> Unit ) {
        this.signInCallback = signInCallback
        Log.d(TAG, "redirectUserToSignIn")
        val scopesString = JSONArray(scopes).toString()
        val javascript = "redirectToSignIn('${appDomain}', '${redirectURI}', '${manifestURI}', ${scopesString})"
        webView.evaluateJavascript(javascript, { result: String ->
            // no op
        })
    }

    /* Public storage methods */

    // getAppBucketUrl

    // getUserAppFileUrl


    fun getFile(path: String, options: GetFileOptions, callback: ((String) -> Unit)) {
        Log.d(TAG, "getFile: path: ${path} options: ${options}")
        val uniqueIdentifier = addGetFileCallback(callback)
        val javascript = "getFile('${path}', ${options}, '${uniqueIdentifier}')"
        webView.evaluateJavascript(javascript, { result: String ->
            // no op
        })
    }

    fun putFile(path: String, content: String, options: PutFileOptions, callback: ((String) -> Unit)) {
        Log.d(TAG, "putFile: path: ${path} options: ${options}")
        val uniqueIdentifier = addPutFileCallback(callback)
        val javascript = "putFile('${path}', '${content}', ${options}, '${uniqueIdentifier}')"
        webView.evaluateJavascript(javascript, { result: String ->
            // no op
        })
    }



    private fun addGetFileCallback(callback: (String) -> Unit): String {
        val uniqueIdentifier = UUID.randomUUID().toString()
        getFileCallbacks[uniqueIdentifier] = callback
        return uniqueIdentifier
    }

    private fun addPutFileCallback(callback: (String) -> Unit): String {
        val uniqueIdentifier = UUID.randomUUID().toString()
        putFileCallbacks[uniqueIdentifier] = callback
        return uniqueIdentifier
    }

    private class JavascriptInterfaceObject(session: BlockstackSession) {
        private val session = session

        @JavascriptInterface
        fun signInSuccess(userDataString: String) {
            Log.d(session.TAG, "signInSuccess" )
            val userData = JSONObject(userDataString)
            session.userData = userData
            Log.d(session.TAG, session.userData.toString() )
            session.signInCallback?.invoke(userData)
        }

        @JavascriptInterface
        fun getFileResult(content: String, uniqueIdentifier: String) {
            Log.d(session.TAG, "putFileResult" )
            session.getFileCallbacks[uniqueIdentifier]?.invoke(content)
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

private class BlockstackWebViewClient(context: Context) : WebViewClient() {
    val context = context
    private val TAG = BlockstackWebViewClient::class.qualifiedName

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
