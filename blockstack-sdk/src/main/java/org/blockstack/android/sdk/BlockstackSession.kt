package org.blockstack.android.sdk

import android.content.Context
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

/**
 * Created by larry on 3/25/18.
 */

class BlockstackSession(context: Context) {
    private val context = context
    private val TAG = BlockstackSession::class.qualifiedName
    private var userData: JSONObject? = null
    private var signInCallback: ((JSONObject) -> Unit)? = null
    init {
        Log.d(TAG, context.toString())
    }
    private val webView = WebView(context)
    init {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = BlockstackWebViewClient(context)
        webView.addJavascriptInterface(JavascriptInterfaceObject(this),"android")
        webView.loadUrl("file:///android_res/raw/signin.html")
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
        val javascript = "redirectToSignIn()"
        webView.evaluateJavascript(javascript, { result: String ->

        })
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
        customTabsIntent.launchUrl(context, Uri.parse("http://192.168.188.88:3000/auth?authRequest=${authRequestToken}"))
        Log.d(TAG,"here")
        return true
    }
}
