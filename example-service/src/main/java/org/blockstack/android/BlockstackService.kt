package org.blockstack.android

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.SessionStore
import org.blockstack.android.sdk.model.PutFileOptions


class BlockstackService : IntentService("BlockstackExample") {

    private lateinit var sessionStore: SessionStore
    private val TAG: String = "BlockstackService"
    private val CHANNEL_ID = "progress"
    private lateinit var _blockstackSession: BlockstackSession
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler

    override fun onCreate() {
        super.onCreate()
        handlerThread = HandlerThread("BlockstackService")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        sessionStore = SessionStore(PreferenceManager.getDefaultSharedPreferences(this))

    }

    override fun onHandleIntent(intent: Intent?) {
        _blockstackSession = BlockstackSession(sessionStore, defaultConfig)
        CoroutineScope(Dispatchers.IO).launch {
            putFileFromService()
        }
    }

    fun initNotifChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return
        }
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID,
                "Progress",
                NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Progress messages for file operations"
        notificationManager.createNotificationChannel(channel)
    }

    private suspend fun putFileFromService() {
        initNotifChannel()
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
            val result = _blockstackSession.putFile("fromService.txt", "Hello Android from Service", PutFileOptions())
            Log.d(TAG, "File stored at: ${result.value}")
            val notif2 = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(org.blockstack.android.sdk.R.drawable.org_blockstack_logo)
                    .setContentTitle("Blockstack Service")
                    .setContentText("File stored at: ${result.value}")
                    .build()

            NotificationManagerCompat.from(this).notify(0, notif2)
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_DONE))

        } else {
            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Not logged In")
                    .setSmallIcon(R.drawable.org_blockstack_logo)
                    .build()
            NotificationManagerCompat.from(this).notify(0, notif)
        }

    }

    companion object {
        val ACTION_DONE = "org.blockstack.intent.action.DONE"
    }
}
