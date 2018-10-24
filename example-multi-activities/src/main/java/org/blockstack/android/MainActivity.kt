package org.blockstack.android


import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.UserData
import org.jetbrains.anko.coroutines.experimental.bg
import java.net.URL

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName

    private var _blockstackSession: BlockstackSession? = null
    private var userData: UserData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        _blockstackSession = BlockstackSession(this, defaultConfig)
        checkLogin()
    }

    private fun checkLogin() {
        val signedIn = blockstackSession().isUserSignedIn()
        if (signedIn) {
            userData = blockstackSession().loadUserData()
            progressBar.visibility = GONE
            if (userData != null) {
                runOnUiThread {
                    onSignIn(userData!!)
                }
            }
        } else {
            navigateToAccount()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_blockstackSession?.loaded == true) {
            checkLogin()
        }
    }

    private fun onSignIn(userData: UserData) {
        userDataTextView.text = "Signed in as ${userData.profile?.name} (${userData.decentralizedID}) with ${userData.profile?.email}"
        showUserAvatar(userData.profile?.avatarImage)
        this.userData = userData
    }

    private fun showUserAvatar(avatarImage: String?) {
        if (avatarImage != null) {
            // use whatever suits your app architecture best to asynchronously load the avatar
            // better use a image loading library than the code below
            async(UI) {
                val avatar = bg {
                    try {
                        BitmapDrawable.createFromStream(URL(avatarImage).openStream(), "src")
                    } catch (e: Exception) {
                        Log.d(TAG, e.toString())
                        null
                    }
                }.await()
                avatarView.setImageDrawable(avatar)
            }
        } else {
            avatarView.setImageResource(R.drawable.default_avatar)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")

        if (intent?.action == Intent.ACTION_MAIN) {
            val userData = blockstackSession().loadUserData()
            runOnUiThread {
                if (userData != null) {
                    onSignIn(userData!!)
                }
            }
        }
    }

    private fun navigateToAccount() {
        startActivity(Intent(this, AccountActivity::class.java))
    }

    private fun navigateToCipher() {
        startActivity(Intent(this, CipherActivity::class.java).putExtra("json", this.userData?.json.toString()))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_account -> {
                navigateToAccount()
                true
            }
            R.id.action_cipher -> {
                navigateToCipher()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun blockstackSession(): BlockstackSession {
        val session = _blockstackSession
        if (session != null) {
            return session
        } else {
            throw IllegalStateException("No session.")
        }
    }

}
