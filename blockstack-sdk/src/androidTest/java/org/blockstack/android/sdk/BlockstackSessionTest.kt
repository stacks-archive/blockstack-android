package org.blockstack.android.sdk

import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.*
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

    private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
    private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
    private val DECENTRALIZED_ID = "did:btc-addr:1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"

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
            session = BlockstackSession(context, config)
        }

        latch.await()
        assertThat(session.loaded, `is`(true))
    }

    @Test
    fun signInCallbackIsCalledAfterHandlingPendingSignIn() {
        val context = InstrumentationRegistry.getContext()
        val latch = CountDownLatch(1)
        var decentralizedID:String? = null
        val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())
        lateinit var session: BlockstackSession

        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config)
        }

        latch.await()
        assertThat(decentralizedID, `is`(DECENTRALIZED_ID))
    }

    @Test
    fun loadUserDataIsNullAfterSignOut() {
        val context = InstrumentationRegistry.getContext()
        var latch = CountDownLatch(1)
        val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())
        lateinit var session: BlockstackSession

        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config)
        }

        latch.await()

    }

    private fun failTest(msg: String) {
        rule.activity.runOnUiThread {
            fail(msg)
        }
    }
}
