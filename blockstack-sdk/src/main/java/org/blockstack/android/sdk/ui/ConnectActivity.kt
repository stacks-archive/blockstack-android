package org.blockstack.android.sdk.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import kotlinx.android.synthetic.main.activity_connect.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import org.blockstack.android.sdk.BlockstackConnect
import org.blockstack.android.sdk.R

class ConnectActivity : AppCompatActivity() {

    private val LAUNCH_HOW_IT_WORKS_ACTIVITY = 1

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        //Action Bar Close Icon & Title
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)

        //Check if BlockstackConnect Config has a custom theme
        setTheme(savedInstanceState?.getInt(
                BlockstackConnect.CUSTOM_THEME,
                R.style.Theme_Blockstack) ?: R.style.Theme_Blockstack)

        //App customization
        // Icon
        connect_app_icon.setImageResource(applicationInfo.icon)
        //Replace Strings with the APP name
        connect_title.text = getString(R.string.connect_dialog_title, getString(applicationInfo.labelRes))
        connect_tracking_desc_title.text = getString(R.string.connect_activity_tracking_desc, getString(applicationInfo.labelRes))


        //Button Listeners
        connect_get_secret_key.setOnClickListener {
            lifecycle.coroutineScope.launch(Dispatchers.IO) {
                BlockstackConnect.redirectUserToSignIn(this@ConnectActivity, sendToSignIn = false)
                this@ConnectActivity.finish()
            }
        }

        connect_sign_in.setOnClickListener {
            lifecycle.coroutineScope.launch(Dispatchers.IO) {
                BlockstackConnect.redirectUserToSignIn(this@ConnectActivity, sendToSignIn = true)
                this@ConnectActivity.finish()
            }
        }

        connect_how_it_works.setOnClickListener {
            startActivityForResult(Intent(this, ConnectHowItWorksActivity::class.java), LAUNCH_HOW_IT_WORKS_ACTIVITY)
        }

    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //Intercept Get Started click from ConnectHowItWorks
        if (resultCode == RESULT_OK && requestCode == LAUNCH_HOW_IT_WORKS_ACTIVITY) {
            lifecycle.coroutineScope.launch(Dispatchers.IO) {
                BlockstackConnect.redirectUserToSignIn(this@ConnectActivity, sendToSignIn = false)
                this@ConnectActivity.finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish() //Action Bar Close instead of Navigate up
        return false
    }

}