package org.blockstack.android.sdk;

import android.content.Context
import android.preference.PreferenceManager
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.eclipsesource.v8.V8ScriptExecutionException
import kotlinx.coroutines.experimental.runBlocking
import org.blockstack.android.sdk.model.Profile
import org.blockstack.android.sdk.model.Proof
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.notNullValue
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch


private val TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJqdGkiOiJhMzU4MzdmNS1jOGNmLTRiYTMtYjk4ZS03OTY5YTUzZmVjNDgiLCJpYXQiOiIyMDE3LTAzLTA0VDE2OjEzOjA2Ljc5MFoiLCJleHAiOiIyMDE4LTAzLTA0VDE2OjEzOjA2Ljc5MFoiLCJzdWJqZWN0Ijp7InB1YmxpY0tleSI6IjAzZTdhNGQ3OTgzMzY5ZDMzZWQxMzAyMDg4NTk4NWQ2OGY4YjA1ZGVlNjE2OGY3NWY5ZDk3ZTFhMDcyY2RmY2RjNSJ9LCJpc3N1ZXIiOnsicHVibGljS2V5IjoiMDNlN2E0ZDc5ODMzNjlkMzNlZDEzMDIwODg1OTg1ZDY4ZjhiMDVkZWU2MTY4Zjc1ZjlkOTdlMWEwNzJjZGZjZGM1In0sImNsYWltIjp7IkBjb250ZXh0IjoiaHR0cDovL3NjaGVtYS5vcmcvIiwiQHR5cGUiOiJDcmVhdGl2ZVdvcmsiLCJuYW1lIjoiQmFsbG9vbiBEb2ciLCJjcmVhdG9yIjpbeyJAdHlwZSI6IlBlcnNvbiIsIkBpZCI6InRoZXJlYWxqZWZma29vbnMuaWQiLCJuYW1lIjoiSmVmZiBLb29ucyJ9XSwiZGF0ZUNyZWF0ZWQiOiIxOTk0LTA1LTA5VDAwOjAwOjAwLTA0MDAiLCJkYXRlUHVibGlzaGVkIjoiMjAxNS0xMi0xMFQxNDo0NDoyNi0wNTAwIn19.MF7ru91rk8IIKNEEqo9wjLHkvW3jSlcDJmeZZeOVSj9KlXApBp67q_3ke0-LzSO_YyYsUnGOplMYiNxY1XynAA"
private val TOKEN_PUBLIC_KEY = "03e7a4d7983369d33ed13020885985d68f8b05dee6168f75f9d97e1a072cdfcdc5"
private val TOKEN_PUBLIC_KEY_2 = "0354a4b971100da0c0be348fc62a30757d115cb4044adfed3d32e48298f9bcfd0c"
private val TOKEN_PROFILE_CONTENT = "{\"@context\":\"http:\\/\\/schema.org\\/\",\"@type\":\"CreativeWork\",\"name\":\"Balloon Dog\",\"creator\":[{\"@type\":\"Person\",\"@id\":\"therealjeffkoons.id\",\"name\":\"Jeff Koons\"}],\"dateCreated\":\"1994-05-09T00:00:00-0400\",\"datePublished\":\"2015-12-10T14:44:26-0500\"}"
private val DECODED_TOKEN = "{\"header\":{\"typ\":\"JWT\",\"alg\":\"ES256K\"},\"payload\":{\"jti\":\"a35837f5-c8cf-4ba3-b98e-7969a53fec48\",\"iat\":\"2017-03-04T16:13:06.790Z\",\"exp\":\"2018-03-04T16:13:06.790Z\",\"subject\":{\"publicKey\":\"03e7a4d7983369d33ed13020885985d68f8b05dee6168f75f9d97e1a072cdfcdc5\"},\"issuer\":{\"publicKey\":\"03e7a4d7983369d33ed13020885985d68f8b05dee6168f75f9d97e1a072cdfcdc5\"},\"claim\":{\"@context\":\"http:\\/\\/schema.org\\/\",\"@type\":\"CreativeWork\",\"name\":\"Balloon Dog\",\"creator\":[{\"@type\":\"Person\",\"@id\":\"therealjeffkoons.id\",\"name\":\"Jeff Koons\"}],\"dateCreated\":\"1994-05-09T00:00:00-0400\",\"datePublished\":\"2015-12-10T14:44:26-0500\"}},\"signature\":\"MF7ru91rk8IIKNEEqo9wjLHkvW3jSlcDJmeZZeOVSj9KlXApBp67q_3ke0-LzSO_YyYsUnGOplMYiNxY1XynAA\"}"
private val ZONE_FILE_CONTENT = "\$ORIGIN friedger.id\n\$TTL 3600\n_http._tcp URI 10 1 \"https://gaia.blockstack.org/hub/1Maw8BjWgj6MWrBCfupqQuWANthMhefb2v/0/profile.json\"\n"

@RunWith(AndroidJUnit4::class)
class BlockstackSessionTokenTest {
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

    @After
    fun teardown() {
        session.release()
    }

    @Test
    fun extractProfileReturnsCorrectObject() {
        val profileToken = session.extractProfile(TOKEN)
        assertThat(profileToken.json.toString(), `is`("{\"@context\":\"http:\\/\\/schema.org\\/\",\"@type\":\"CreativeWork\",\"name\":\"Balloon Dog\",\"creator\":[{\"@type\":\"Person\",\"@id\":\"therealjeffkoons.id\",\"name\":\"Jeff Koons\"}],\"dateCreated\":\"1994-05-09T00:00:00-0400\",\"datePublished\":\"2015-12-10T14:44:26-0500\"}"))
    }

    @Test
    fun extractProfileAlsoVerifiesToken() {
        val profileToken = session.extractProfile(TOKEN, TOKEN_PUBLIC_KEY)
        assertThat(profileToken.json.toString(), `is`(TOKEN_PROFILE_CONTENT))
    }

    @Test
    fun extractProfileThrowsForWrongOwner() {
        try {
            session.extractProfile(TOKEN, TOKEN_PUBLIC_KEY_2)
            throw AssertionError("should throw an error")
        } catch (e: V8ScriptExecutionException) {
            assertThat(e.message, `is`("undefined:7917: Error: Token issuer public key does not match the verifying value"))
        }
    }

    @Test
    fun wrapProfileTokenReturnsTokenPair() {
        val profileTokenPair = session.wrapProfileToken(TOKEN)
        assertThat(profileTokenPair.json.getJSONObject("decodedToken").toString(), `is`(DECODED_TOKEN))
        assertThat(profileTokenPair.json.getString("token"), `is`(TOKEN))
    }

    @Test
    fun verifyProfileTokenReturnsToken() {
        val profileToken = session.verifyProfileToken(TOKEN, TOKEN_PUBLIC_KEY)
        assertThat(profileToken.json.toString(), `is`(DECODED_TOKEN))
    }

    @Test
    fun verifyProfileTokenThrowsForWrongOwner() {
        try {
            session.verifyProfileToken(TOKEN, TOKEN_PUBLIC_KEY_2)
            throw AssertionError("should throw an error")
        } catch (e: V8ScriptExecutionException) {
            assertThat(e.message, `is`("undefined:7917: Error: Token issuer public key does not match the verifying value"))
        }
    }

    @Test
    fun parseZoneFileReturnsCorrectObject() {
        val zoneFile = session.parseZoneFile(ZONE_FILE_CONTENT)
        assertThat(zoneFile.json.toString(), `is`("{\"\$origin\":\"friedger.id\",\"\$ttl\":3600,\"uri\":[{\"name\":\"_http._tcp\",\"target\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/1Maw8BjWgj6MWrBCfupqQuWANthMhefb2v\\/0\\/profile.json\",\"priority\":10,\"weight\":1}]}"))
    }

    @Test
    fun resolveZoneFileToProfileReturnsCorrectObject() {
        var result: Profile? = null
        val latch = CountDownLatch(1)
        session.resolveZoneFileToProfile(ZONE_FILE_CONTENT) {
            latch.countDown()
            result = it.value
        }

        assertThat(result, notNullValue()) // content is dynamic
    }
}
