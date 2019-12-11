package org.blockstack.android

import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.UriMatchers
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.blockstack.android.sdk.BaseScope
import org.blockstack.android.sdk.Blockstack
import org.blockstack.android.sdk.SessionStore
import org.blockstack.android.sdk.model.BlockstackAccount
import org.blockstack.android.sdk.model.BlockstackIdentity
import org.blockstack.android.sdk.toHexPublicKey64
import org.hamcrest.Matchers
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kethereum.bip32.generateChildKey
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed
import org.kethereum.bip44.BIP44Element
import org.kethereum.crypto.toECKeyPair
import org.kethereum.model.PrivateKey

private val SEED_PHRASE = "sound idle panel often situate develop unit text design antenna vendor screen opinion balcony share trigger accuse scatter visa uniform brass update opinion media"
private val TRANSIT_PRIVATE_KEY = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"

/**
 * Test all buttons
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val rule = IntentsTestRule(MainActivity::class.java, false, false)

    @Test
    fun afterSignedIn() {
        val blockstack = Blockstack()

        val words = MnemonicWords(SEED_PHRASE)
        val identity = BlockstackIdentity(words.toSeed().toKey("m/888'/0'"))
        val keys = identity.identityKeys.generateChildKey(BIP44Element(true, 0))
        val transitKeyPair = PrivateKey(TRANSIT_PRIVATE_KEY).toECKeyPair()

        val sessionStore = SessionStore(PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext))
                sessionStore.setTransitPrivateKey(TRANSIT_PRIVATE_KEY)
        sessionStore.sessionData = sessionStore.sessionData // side-effect: stored on preferences

        val account = BlockstackAccount(null, keys, identity.salt)
        val payload = JSONObject()
                .put("public_keys", JSONArray(arrayOf(transitKeyPair.toHexPublicKey64())))
                .put("domain_name", "flamboyant-darwin-d11c17.netlify.com")

        val authResponse = runBlocking {
            blockstack.makeAuthResponse(payload, account, arrayOf(BaseScope.StoreWrite.scope))
        }
        val authResponseIntent = Intent(Intent.ACTION_VIEW)
        authResponseIntent.data = Uri.parse("blockstacksample:?authResponse=$authResponse")

        rule.launchActivity(authResponseIntent)

        Espresso.onIdle()
        onView(withId(R.id.userDataTextView)).check(matches(withText("Signed in as did:btc-addr:1JeTQ5cQjsD57YGcsVFhwT7iuQUXJR6BSk")))

        onView(withId(R.id.putStringFileButton)).perform(click())
        Espresso.onIdle()
        onView(withId(R.id.readURLTextView)).check(matches(withText("Uploading...")))
    }

    @Test
    fun signIn() {
        rule.launchActivity(Intent(Intent.ACTION_MAIN))

        onView(withId(R.id.signInButton)).perform(click())

        Intents.intended(Matchers.allOf(
                IntentMatchers.hasAction(Intent.ACTION_VIEW),
                IntentMatchers.hasData(UriMatchers.hasHost("browser.blockstack.org"))
        ))
    }
}
