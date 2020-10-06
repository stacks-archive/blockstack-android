package org.blockstack.android.sdk.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_connect_help.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.blockstack.android.sdk.BlockstackSignIn
import org.blockstack.android.sdk.R

class ConnectHelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect_help)

        button_get_started.setOnClickListener {
            setResult(RESULT_OK)
            this.finish()
        }

        power_by_blockstack.setOnClickListener {
            val locationUri = Uri.parse("https://www.blockstack.org/")
            if (BlockstackSignIn.shouldLaunchInCustomTabs) {
                val builder = CustomTabsIntent.Builder()
                val options = BitmapFactory.Options()
                options.outWidth = 24
                options.outHeight = 24
                options.inScaled = true
                val backButton = BitmapFactory.decodeResource(resources, R.drawable.ic_arrow_back, options)
                builder.setCloseButtonIcon(backButton)
                builder.setToolbarColor(ContextCompat.getColor(this, R.color.org_blockstack_purple_50_logos_types))
                builder.setToolbarColor(ContextCompat.getColor(this, R.color.org_blockstack_purple_85_lines))
                builder.setShowTitle(true)
                val customTabsIntent = builder.build()
                customTabsIntent.launchUrl(this, locationUri)
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, locationUri).addCategory(Intent.CATEGORY_BROWSABLE))
            }

        }
    }
}
