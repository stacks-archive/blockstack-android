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

@RunWith(AndroidJUnit4::class)
class AppLinkVerifierTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)
    private lateinit var appLinkVerifier: AppLinkVerifier

    @Before
    fun setup() {
        appLinkVerifier = AppLinkVerifier(rule.activity, "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(arrayOf()))
    }

    @Test
    fun testCallOnMainThread() {
        rule.runOnUiThread {
            val warning = appLinkVerifier.verify()
            assertThat(warning, `is`("Failed to check verified app links. android.os.NetworkOnMainThreadException"))
        }

    }

    @Test
    fun testNotExistingDigitalAssetLinksFile() {
        val warning = appLinkVerifier.verify()
        assertThat(warning, `is`("Digital Asset Links file for https://flamboyant-darwin-d11c17.netlify.com does not contain a fingerprint for this app org.blockstack.android.sdk.test.\nPlease verify https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://flamboyant-darwin-d11c17.netlify.com&relation=delegate_permission/common.handle_all_urls"))
    }

    @Test
    fun testInvalidFingerprintsWithExistingDigitalAssetLinksFile() {
        appLinkVerifier = AppLinkVerifier(rule.activity, "https://app.afari.io".toBlockstackConfig(arrayOf()))
        val warning = appLinkVerifier.verify()
        assertThat(warning, `is`("Digital Asset Links file for https://app.afari.io does not contain a fingerprint for this app org.blockstack.android.sdk.test.\nPlease verify https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://app.afari.io&relation=delegate_permission/common.handle_all_urls"))
    }
}