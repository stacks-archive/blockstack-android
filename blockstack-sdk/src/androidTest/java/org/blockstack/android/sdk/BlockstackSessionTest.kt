package org.blockstack.android.sdk

import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import kotlin.emptyArray

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class BlockstackSessionTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
    private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
    private val DECENTRALIZED_ID = "did:btc-addr:1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"
    private val BITCOIN_ADDRESS = "1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"
    private var lastError: String? = null

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("org.blockstack.android.sdk.test", appContext.packageName)
    }

    @Test
    fun onLoadedCallbackIsCalledAfterSessionCreated() {
        val context = InstrumentationRegistry.getContext()
        val latch = CountDownLatch(1)
        val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())

        lateinit var session: BlockstackSession

        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config) {
                latch.countDown()
            }
        }

        latch.await()
        assertThat(session.loaded, `is`(true))
    }

    @Test
    fun signInCallbackIsCalledAfterHandlingPendingSignIn() {
        val context = InstrumentationRegistry.getContext()
        val latch = CountDownLatch(1)
        var decentralizedID: String? = null
        val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())
        lateinit var session: BlockstackSession

        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config) {
                session.makeAuthResponse(PRIVATE_KEY, null) { authResponse ->
                    if (authResponse.hasValue) {
                        session.handlePendingSignIn(authResponse.value!!) {
                            if (it.hasValue) {
                                decentralizedID = it.value!!.decentralizedID
                                latch.countDown()
                            } else {
                                failTest("Should validate auth response")
                                latch.countDown()
                            }
                        }
                    } else {
                        failTest("Should create valid auth response")
                        latch.countDown()
                    }
                }
            }
        }

        latch.await()
        assertThatTestDidNotFail()
        assertThat(decentralizedID, `is`(DECENTRALIZED_ID))
    }

    @Test
    fun loadUserDataIsNullAfterSignOut() {
        val context = InstrumentationRegistry.getContext()
        var latch = CountDownLatch(1)
        val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())
        lateinit var session: BlockstackSession

        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config) {
                session.makeAuthResponse(PRIVATE_KEY, null) { authResponse ->
                    if (authResponse.hasValue) {
                        session.handlePendingSignIn(authResponse.value!!) {
                            if (it.hasValue) {
                                rule.activity.runOnUiThread {
                                    session.signUserOut {
                                        latch.countDown()
                                    }
                                }
                            } else {
                                failTest("should sign in")
                                latch.countDown()
                            }
                        }
                    } else {
                        failTest("should create valid auth response")
                        latch.countDown()
                    }
                }
            }
        }

        latch.await()
        assertThatTestDidNotFail()

        // verify user data
        latch = CountDownLatch(1)
        var userData: UserData? = null

        rule.activity.runOnUiThread {
            session.loadUserData {
                userData = it
                latch.countDown()
            }
        }

        latch.await()
        assertThat(userData, `is`(nullValue()))
    }


    @Test
    fun verifyProofsReturnsEmptyListForEmptyProfile() {
        val context = InstrumentationRegistry.getContext()
        var latch = CountDownLatch(1)
        val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())
        lateinit var session: BlockstackSession
        var proofList: ArrayList<Proof>? = null

        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config) {
                session.validateProofs(Profile(JSONObject("{}")), BITCOIN_ADDRESS, null) { proofs ->
                    if (proofs.hasValue) {
                        proofList = proofs.value
                        latch.countDown()
                    } else {
                        failTest("no proofs")
                        assertThatTestDidNotFail()
                    }
                }
            }
        }

        latch.await()
        assertThatTestDidNotFail()
        assertThat(proofList, notNullValue())
        assertThat(proofList!!.size, `is`(0))
    }

    @Test
    fun verifyProofsReturnsAllProofsForFriedger() {
        val context = InstrumentationRegistry.getContext()
        var latch = CountDownLatch(1)
        val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())
        lateinit var session: BlockstackSession
        var proofList: ArrayList<Proof>? = null
        val profile = "{\"@type\":\"Person\",\"@context\":\"http://schema.org\",\"name\":\"Friedger Müffke\",\"description\":\"Entredeveloper in Europe\",\"image\":[{\"@type\":\"ImageObject\",\"name\":\"avatar\",\"contentUrl\":\"https://gaia.blockstack.org/hub/1Maw8BjWgj6MWrBCfupqQuWANthMhefb2v/0/avatar-0\"}],\"account\":[{\"@type\":\"Account\",\"placeholder\":false,\"service\":\"twitter\",\"identifier\":\"fmdroid\",\"proofType\":\"http\",\"proofUrl\":\"https://twitter.com/fmdroid/status/927285474854670338\"},{\"@type\":\"Account\",\"placeholder\":false,\"service\":\"facebook\",\"identifier\":\"friedger.mueffke\",\"proofType\":\"http\",\"proofUrl\":\"https://www.facebook.com/friedger.mueffke/posts/10155370909214191\"},{\"@type\":\"Account\",\"placeholder\":false,\"service\":\"github\",\"identifier\":\"friedger\",\"proofType\":\"http\",\"proofUrl\":\"https://gist.github.com/friedger/d789f7afd1aa0f23dd3f87eb40c2673e\"},{\"@type\":\"Account\",\"placeholder\":false,\"service\":\"bitcoin\",\"identifier\":\"1MATdc1Xjen4GUYMhZW5nPxbou24bnWY1v\",\"proofType\":\"http\",\"proofUrl\":\"\"},{\"@type\":\"Account\",\"placeholder\":false,\"service\":\"pgp\",\"identifier\":\"5371148B3FC6B5542CADE04F279B3081B173CFD0\",\"proofType\":\"http\",\"proofUrl\":\"\"},{\"@type\":\"Account\",\"placeholder\":false,\"service\":\"ethereum\",\"identifier\":\"0x73274c046ae899b9e92EaAA1b145F0b5f497dd9a\",\"proofType\":\"http\",\"proofUrl\":\"\"}],\"apps\":{\"https://app.graphitedocs.com\":\"https://gaia.blockstack.org/hub/17Qhy4ob8EyvScU6yiP6sBdkS2cvWT9FqE/\",\"https://www.stealthy.im\":\"https://gaia.blockstack.org/hub/1KyYJihfZUjYyevfPYJtCEB8UydxqQS67E/\",\"https://www.chat.hihermes.co\":\"https://gaia.blockstack.org/hub/1DbpoUCdEpyTaND5KbZTMU13nhNeDfVScD/\",\"https://app.travelstack.club\":\"https://gaia.blockstack.org/hub/1QK5n11Xn1p5aP74xy14NCcYPndHxnwN5y/\"}}"

        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config) {
                session.validateProofs(Profile(JSONObject(profile)), "1Maw8BjWgj6MWrBCfupqQuWANthMhefb2v", "friedger.id") { proofs ->
                    if (proofs.hasValue) {
                        proofList = proofs.value
                        latch.countDown()
                    } else {
                        failTest("no proofs")
                        latch.countDown()
                    }
                }
            }
        }

        latch.await()
        assertThatTestDidNotFail()
        assertThat(proofList, notNullValue())
        assertThat(proofList!!.size, `is`(3))
    }

    private fun failTest(msg: String) {
        rule.finishActivity()
        lastError = msg
    }

    private fun assertThatTestDidNotFail() {
        assertThat(lastError, nullValue())
    }
}
