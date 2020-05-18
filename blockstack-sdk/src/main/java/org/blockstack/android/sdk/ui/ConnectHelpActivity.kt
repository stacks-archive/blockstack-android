package org.blockstack.android.sdk.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_connect_help.*
import org.blockstack.android.sdk.R

class ConnectHelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect_help)
        setSupportActionBar(toolbar)
    }
}
