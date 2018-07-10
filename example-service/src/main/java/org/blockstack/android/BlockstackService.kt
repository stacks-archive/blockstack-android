package org.blockstack.android

import android.app.IntentService
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.PutFileOptions

class BlockstackService : IntentService("BlockstackExample") {
    private val TAG: String = "BlockstackService"
    private val CHANNEL_ID = "warnings"
    private lateinit var _blockstackSession: BlockstackSession

    override fun onHandleIntent(intent: Intent?) {
        _blockstackSession = BlockstackSession(this, defaultConfig) {
            putFileFromService()
        }
    }

    private fun putFileFromService() {

        _blockstackSession.isUserSignedIn { signedIn ->
            if (signedIn) {
                val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Blockstack Service")
                        .setContentText("Uploading file")
                        .setProgress(100,50, true)
                        .build()
                NotificationManagerCompat.from(this).notify(0, notif)
                // make it take looong
                Thread.sleep(10000)
                _blockstackSession.putFile("fromService.txt", "Hello Android from Service", PutFileOptions()) { readURL: String ->
                    Log.d(TAG, "File stored at: ${readURL}")
                    val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                            .setContentTitle("Blockstack Service")
                            .setContentText("File stored at: ${readURL}")
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
}