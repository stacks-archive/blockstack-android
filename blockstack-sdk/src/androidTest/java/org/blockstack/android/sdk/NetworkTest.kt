package org.blockstack.android.sdk

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4;
import org.blockstack.android.sdk.model.network.NameInfo
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule

import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class NetworkTest {

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
    fun getNameInfoReturnsCorrectInfo() {
        val latch = CountDownLatch(1)
        var result: Result<NameInfo>? = null
        session.network.getNameInfo("dev_android_sdk.id.blockstack") {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value?.json.toString(), `is`("{\"address\":\"1Akc4hagxfYfDq9suMp1wjjyC5RwxJ7D3H\",\"blockchain\":\"bitcoin\",\"did\":\"did:stack:v0:SX3c6YMqh2jrk8wLSno6VdtXqrfNjmF81U-0\",\"last_txid\":\"536e596a4f6bf96656e3db202ea6842eb3f427143e8010cdf388399265ac6568\",\"status\":\"registered_subdomain\",\"zonefile\":\"\$ORIGIN dev_android_sdk.id.blockstack\\n\$TTL 3600\\n_http._tcp\\tIN\\tURI\\t10\\t1\\t\\\"https:\\/\\/gaia.blockstack.org\\/hub\\/1Akc4hagxfYfDq9suMp1wjjyC5RwxJ7D3H\\/profile.json\\\"\\n\\n\",\"zonefile_hash\":\"c146ad093f6152d67233b871f4fa181d98754f9f\"}"))
    }

    @Test
    fun getNameInfoReturnsNoInfoForBadName() {
        val latch = CountDownLatch(1)
        var result: Result<NameInfo>? = null
        session.network.getNameInfo("a_name_that_is_not_valid_and_does_not_exist.id.blockstack") {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Name not found"))
    }

}
