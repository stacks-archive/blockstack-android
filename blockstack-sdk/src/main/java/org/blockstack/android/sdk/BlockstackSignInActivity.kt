package org.blockstack.android.sdk

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent


class BlockstackSignInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_blockstack_sign_in)
        val customTabsIntent = CustomTabsIntent.Builder().build()

        // Load transaction generation code into webview

        // on redirect load the following with
        // TODO: handle lack of custom tabs support
        customTabsIntent.launchUrl(this, Uri.parse("https://blockstack.org"))
    }
}
