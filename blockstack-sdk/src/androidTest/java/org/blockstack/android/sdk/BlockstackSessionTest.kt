package org.blockstack.android.sdk

import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class BlockstackSessionTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

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
        val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())
        val authResponse = ""
        lateinit var session: BlockstackSession

        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config) {
                session.handlePendingSignIn(authResponse) {
                    latch.await()
                }
            }
        }

        latch.await()
        assertThat(session.loaded, `is`(true))
    }

    @Test
    fun loadUserDataIsNullAfterSignOut() {
        val context = InstrumentationRegistry.getContext()
        var latch = CountDownLatch(1)
        val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())
        val authResponse = ""
        lateinit var session: BlockstackSession

        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config) {
                session.handlePendingSignIn(authResponse) {
                    session.signUserOut {
                        latch.await()
                    }
                }
            }
        }

        latch.await()

        // verify user data
        latch = CountDownLatch(1)
        var userData: UserData? = null
        session.loadUserData {
            userData = it
            latch.countDown()
        }

        latch.await()
        assertThat(userData, `is`(nullValue()))
    }

}
