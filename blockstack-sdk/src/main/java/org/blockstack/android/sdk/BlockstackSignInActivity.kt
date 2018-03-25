package org.blockstack.android.sdk

import android.content.Context
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient


class BlockstackSignInActivity : AppCompatActivity() {
    private val TAG = BlockstackSignInActivity::class.qualifiedName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blockstack_sign_in)

//
//        Log.d(TAG, "Loading sign in webview...")
//        val webView: WebView = findViewById<WebView>(R.id.webView) as WebView
//        // Load transaction generation code into webview
//        webView.settings.javaScriptEnabled = true
//        webView.settings.domStorageEnabled = true
//        webView.webViewClient = BlockstackSignInWebViewClient(this)
//        webView.loadUrl("file:///android_res/raw/signin.html")


    }
}

//private class BlockstackSignInWebViewClient(context: Context) : WebViewClient() {
//    val context = context
//    private val TAG = BlockstackSignInWebViewClient::class.qualifiedName
//
//    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
//        // initially overriding a function that's deprecated in API 27 in order to support API 15
//        Log.d(TAG, "Navigation detected in sign in webview")
//        Log.d(TAG, url)
//
//        val authRequestToken = url.split(':')[1]
//        Log.d(TAG, authRequestToken)
//
//        val customTabsIntent = CustomTabsIntent.Builder().build()
//        // on redirect load the following with
//        // TODO: handle lack of custom tabs support
//        customTabsIntent.launchUrl(context, Uri.parse("http://192.168.188.88:3000/auth?authRequest=${authRequestToken}"))
//        Log.d(TAG,"here")
//        return true
//    }
//}

