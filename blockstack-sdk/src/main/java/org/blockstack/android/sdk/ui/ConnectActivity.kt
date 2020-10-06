package org.blockstack.android.sdk.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import kotlinx.android.synthetic.main.fragment_connect_dialog.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import org.blockstack.android.sdk.BlockstackConnect
import org.blockstack.android.sdk.R

class ConnectActivity : AppCompatActivity() {

    val LAUNCH_HOW_IT_WORKS_ACTIVITY = 1


    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_connect_dialog)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)

        setTheme(savedInstanceState?.getInt(
                BlockstackConnect.CUSTOM_THEME,
                R.style.Theme_Blockstack) ?: R.style.Theme_Blockstack)

        connect_dialog_title.text = getString(R.string.connect_dialog_title, getString(applicationInfo.labelRes))
        connect_dialog_app_icon.setImageResource(applicationInfo.icon)

        connect_dialog_get_started.setOnClickListener {
            lifecycle.coroutineScope.launch(Dispatchers.IO) {
                BlockstackConnect.redirectUserToSignIn(this@ConnectActivity, sendToSignIn = false)
                this@ConnectActivity.finish()
            }
        }

        connect_dialog_restore.setOnClickListener {
            lifecycle.coroutineScope.launch(Dispatchers.IO) {
                BlockstackConnect.redirectUserToSignIn(this@ConnectActivity, sendToSignIn = true)
                this@ConnectActivity.finish()
            }
        }

        connect_dialog_help.setOnClickListener {
            startActivityForResult(Intent(this, ConnectHelpActivity::class.java), LAUNCH_HOW_IT_WORKS_ACTIVITY)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == LAUNCH_HOW_IT_WORKS_ACTIVITY) {
            lifecycle.coroutineScope.launch(Dispatchers.IO) {
                BlockstackConnect.redirectUserToSignIn(this@ConnectActivity, sendToSignIn = false)
                this@ConnectActivity.finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish() // close this activity as oppose to navigating up
        return false
    }

}