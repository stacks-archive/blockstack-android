package org.blockstack.android.sdk.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import kotlinx.android.synthetic.main.fragment_connect_dialog.*
import kotlinx.coroutines.*
import org.blockstack.android.sdk.*

class ConnectActivity : AppCompatActivity() {

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_connect_dialog)

        connect_dialog_get_started.setOnClickListener {
            lifecycle.coroutineScope.launch(Dispatchers.IO) {
                BlockstackConnect.redirectUserToSignIn(this@ConnectActivity, sendToSignIn = false)
            }
        }
    }

}