package org.blockstack.android.sdk

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.UriMatchers.hasHost
import androidx.test.espresso.intent.matcher.UriMatchers.hasParamWithName
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.blockstack.android.sdk.model.BlockstackConfig
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BlockstackSessionLoginWithBrowserTest {
    private val TIMEOUT = 3000L

    @get:Rule
    val rule = IntentsTestRule(TestActivity::class.java)

    private lateinit var signIn: BlockstackSignIn
    private lateinit var sessionStore: SessionStore

    @Before
    fun setup() {
        sessionStore = sessionStoreforIntegrationTests(rule)
        signIn = BlockstackSignIn("https://example.com".toBlockstackConfig(emptyArray()), sessionStore)
    }

    @Test
    fun testRedirect() {
        runBlocking {
            signIn.redirectToSignIn(rule.activity)
        }
        intended(allOf(hasAction(Intent.ACTION_VIEW),
                hasData(allOf(
                        hasHost("browser.blockstack.org"),
                        hasParamWithName("authRequest")
                ))))
        InstrumentationRegistry.getInstrumentation().uiAutomation
                .performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }
}
