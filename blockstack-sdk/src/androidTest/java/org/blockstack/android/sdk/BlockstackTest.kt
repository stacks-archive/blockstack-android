package org.blockstack.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
private val ADDRESS = "1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"

private val TAG = BlockstackTest::class.java.simpleName


@RunWith(AndroidJUnit4::class)
class BlockstackTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)
    private lateinit var session: BlockstackSession


    @Before
    fun setup() {
        session = BlockstackSession(rule.activity,
                "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray()),
                sessionStore = sessionStoreforIntegrationTests(rule),
                executor = IntegrationTestExecutor(rule))
    }

    @Test
    fun getPublicKeyFromPrivateReturnsCorrectKey() {
        assertThat(session.getPublicKeyFromPrivate(PRIVATE_KEY), `is`(PUBLIC_KEY))
    }

    @Test
    fun makeECPrivateKeyDoesNotThrow() {
        session.makeECPrivateKey()
        // ok, no exception thrown
    }

    @Test
    fun publicKeyToAddressReturnsCorrectAddress() {
        assertThat(session.publicKeyToAddress(PUBLIC_KEY), `is`(ADDRESS))
    }
}
