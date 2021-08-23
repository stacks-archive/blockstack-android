package org.blockstack.android.sdk.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import kotlinx.android.synthetic.main.activity_connect.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import org.blockstack.android.sdk.BlockstackConnect
import org.blockstack.android.sdk.R

class BlockstackConnectActivity : AppCompatActivity() {

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Action Bar Close Icon & Title
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)

        //Check if BlockstackConnect Config has a custom theme
        setTheme(
            intent?.getIntExtra(
                EXTRA_CUSTOM_THEME,
                R.style.Theme_Blockstack
            ) ?: R.style.Theme_Blockstack
        )

        setContentView(R.layout.activity_connect)

        //App customization
        // Icon
        connect_app_icon.setImageResource(applicationInfo.icon)
        //Replace Strings with the APP name
        connect_title.text =
            getString(R.string.connect_dialog_title, getString(applicationInfo.labelRes))
        connect_tracking_desc_title.text =
            getString(R.string.connect_activity_tracking_desc, getString(applicationInfo.labelRes))


        //Button Listeners
        val registerSubdomain = intent.getBooleanExtra(EXTRA_REGISTER_SUBDOMAIN, false)

        connect_get_secret_key.setOnClickListener {
            lifecycle.coroutineScope.launch(Dispatchers.IO) {
                BlockstackConnect.redirectUserToSignIn(
                    this@BlockstackConnectActivity,
                    sendToSignIn = false,
                    registerSubdomain = registerSubdomain
                )
                this@BlockstackConnectActivity.finish()
            }
        }

        connect_sign_in.setOnClickListener {
            lifecycle.coroutineScope.launch(Dispatchers.IO) {
                BlockstackConnect.redirectUserToSignIn(
                    this@BlockstackConnectActivity,
                    sendToSignIn = true,
                    registerSubdomain = registerSubdomain
                )
                this@BlockstackConnectActivity.finish()
            }
        }

        connect_how_it_works.setOnClickListener {
            startActivityForResult(
                Intent(this, ConnectHowItWorksActivity::class.java),
                REQUEST_HOW_IT_WORKS
            )
        }

    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //Intercept Get Started click from ConnectHowItWorks
        if (resultCode == RESULT_OK && requestCode == REQUEST_HOW_IT_WORKS) {
            lifecycle.coroutineScope.launch {
                BlockstackConnect.redirectUserToSignIn(
                    this@BlockstackConnectActivity,
                    sendToSignIn = false
                )
                this@BlockstackConnectActivity.finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish() //Action Bar Close instead of Navigate up
        return false
    }

    companion object {
        private val REQUEST_HOW_IT_WORKS = 1
        val EXTRA_CUSTOM_THEME = "styleResCustomTheme"
        val EXTRA_REGISTER_SUBDOMAIN = "registerSubdomain"

        fun getIntent(
            context: Context,
            registerSubdomain: Boolean = false,
            @StyleRes theme: Int? = null
        ): Intent {
            return Intent(context, BlockstackConnectActivity::class.java)
                .putExtra(EXTRA_CUSTOM_THEME, theme)
                .putExtra(EXTRA_REGISTER_SUBDOMAIN, registerSubdomain)
        }
    }

}