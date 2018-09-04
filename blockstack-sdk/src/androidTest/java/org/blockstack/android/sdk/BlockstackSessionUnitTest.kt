package org.blockstack.android.sdk;

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test;
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockstackSessionUnitTest {
    private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"


    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    @Test
    fun testMakeAuthResponse() {
        val session = BlockstackSession(rule.activity, "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray()))
        val token = session.makeAuthResponse(PRIVATE_KEY)
        assertThat(token, `is`(notNullValue()))
    }
}