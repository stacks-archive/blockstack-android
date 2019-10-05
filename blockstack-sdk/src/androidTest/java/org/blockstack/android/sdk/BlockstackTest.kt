package org.blockstack.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class BlockstackTest {


    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var blockstack: Blockstack

    @Before
    fun setup() {
        blockstack = Blockstack()
    }

    @Test
    fun testLookupProfile() {

        val profile = runBlocking {
            blockstack.lookupProfile("public_profile_for_testing.id.blockstack", null)
        }
        assertThat(profile.json.toString(), `is`("{\"@type\":\"Person\",\"@context\":\"http:\\/\\/schema.org\",\"apps\":{\"https:\\/\\/banter.pub\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/1DkuAChufYjTkTCejJgSztuqp5KdykpWap\\/\"},\"api\":{\"gaiaHubConfig\":{\"url_prefix\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/\"},\"gaiaHubUrl\":\"https:\\/\\/hub.blockstack.org\"}}"))
    }
}

