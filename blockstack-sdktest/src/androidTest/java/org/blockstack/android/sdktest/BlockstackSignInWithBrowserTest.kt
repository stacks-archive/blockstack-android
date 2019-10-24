package org.blockstack.android.sdktest

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.UriMatchers.hasHost
import androidx.test.espresso.intent.matcher.UriMatchers.hasParamWithName
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.blockstack.android.sdk.BlockstackSignIn
import org.blockstack.android.sdk.SessionStore
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdktest.test.TestActivity
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
        signIn = BlockstackSignIn(sessionStore, "https://example.com".toBlockstackConfig(emptyArray()))
    }

    @Test
    fun testRedirect() {
        runBlocking {
            signIn.redirectUserToSignIn(rule.activity)
            delay(500)
            InstrumentationRegistry.getInstrumentation().uiAutomation
                    .performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        }
        intended(allOf(hasAction(Intent.ACTION_VIEW),
                hasData(allOf(
                        hasHost("browser.blockstack.org"),
                        hasParamWithName("authRequest")
                ))))
    }
}
