package org.blockstack.android

import android.app.IntentService
import android.content.Intent
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.PutFileOptions

class BlockstackService : IntentService("BlockstackExample") {
    private val TAG: String = "BlockstackService"
    private val CHANNEL_ID = "warnings"
    private lateinit var _blockstackSession: BlockstackSession
    private lateinit var handler: Handler

    override fun onCreate() {
        super.onCreate()
        handler = Handler()
    }

    override fun onHandleIntent(intent: Intent?) {

        /* this will throw an exception
            java.lang.IllegalStateException: Calling View methods on another thread than the UI thread.
            at com.android.webview.chromium.WebViewChromium.b(PG:102)
            at com.android.webview.chromium.WebViewChromium.c(PG:106)
            at com.android.webview.chromium.WebViewChromium.init(PG:40)
            at android.webkit.WebView.<init>(WebView.java:648)
         */
        runOnUIThread {
            _blockstackSession = BlockstackSession(this, defaultConfig)
            putFileFromService()
        }
    }

    fun runOnUIThread(runnable: () -> Unit) {
        handler.post(runnable)
    }

    private fun putFileFromService() {

        val signedIn = _blockstackSession.isUserSignedIn()
        if (signedIn) {
            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(org.blockstack.android.sdk.R.drawable.org_blockstack_logo)
                    .setContentTitle("Blockstack Service")
                    .setContentText("Uploading file")
                    .setProgress(100, 50, true)
                    .build()
            NotificationManagerCompat.from(this).notify(0, notif)
            // make it take looong
            Thread.sleep(10000)
            _blockstackSession.putFile("fromService.txt", "Hello Android from Service", PutFileOptions()) { result ->
                Log.d(TAG, "File stored at: ${result.value}")
                val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(org.blockstack.android.sdk.R.drawable.org_blockstack_logo)
                        .setContentTitle("Blockstack Service")
                        .setContentText("File stored at: ${result.value}")
                        .build()
                NotificationManagerCompat.from(this).notify(0, notif)
            }
        } else {
            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Not logged In")
                    .build()
            NotificationManagerCompat.from(this).notify(0, notif)
        }

    }
}