package org.blockstack.android.sdk

import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import org.blockstack.android.sdk.model.BlockstackConfig
import org.blockstack.android.sdk.test.TestActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BlockstackSessionLoginTest {
    private val TIMEOUT = 3000L

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var signIn: BlockstackSignIn
    private lateinit var sessionStore: SessionStore
    private lateinit var config: BlockstackConfig

    @Before
    fun setup() {

    }

    @Test
    fun testRedirect() {
        runBlocking {
            signIn.redirectToSignIn()
        }
    }
}
