package org.blockstack.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import org.blockstack.android.sdk.model.Entity
import org.blockstack.android.sdk.model.Profile
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.notNullValue
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"

private val TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJqdGkiOiJhMzU4MzdmNS1jOGNmLTRiYTMtYjk4ZS03OTY5YTUzZmVjNDgiLCJpYXQiOiIyMDE3LTAzLTA0VDE2OjEzOjA2Ljc5MFoiLCJleHAiOiIyMDE4LTAzLTA0VDE2OjEzOjA2Ljc5MFoiLCJzdWJqZWN0Ijp7InB1YmxpY0tleSI6IjAzZTdhNGQ3OTgzMzY5ZDMzZWQxMzAyMDg4NTk4NWQ2OGY4YjA1ZGVlNjE2OGY3NWY5ZDk3ZTFhMDcyY2RmY2RjNSJ9LCJpc3N1ZXIiOnsicHVibGljS2V5IjoiMDNlN2E0ZDc5ODMzNjlkMzNlZDEzMDIwODg1OTg1ZDY4ZjhiMDVkZWU2MTY4Zjc1ZjlkOTdlMWEwNzJjZGZjZGM1In0sImNsYWltIjp7IkBjb250ZXh0IjoiaHR0cDovL3NjaGVtYS5vcmcvIiwiQHR5cGUiOiJDcmVhdGl2ZVdvcmsiLCJuYW1lIjoiQmFsbG9vbiBEb2ciLCJjcmVhdG9yIjpbeyJAdHlwZSI6IlBlcnNvbiIsIkBpZCI6InRoZXJlYWxqZWZma29vbnMuaWQiLCJuYW1lIjoiSmVmZiBLb29ucyJ9XSwiZGF0ZUNyZWF0ZWQiOiIxOTk0LTA1LTA5VDAwOjAwOjAwLTA0MDAiLCJkYXRlUHVibGlzaGVkIjoiMjAxNS0xMi0xMFQxNDo0NDoyNi0wNTAwIn19.MF7ru91rk8IIKNEEqo9wjLHkvW3jSlcDJmeZZeOVSj9KlXApBp67q_3ke0-LzSO_YyYsUnGOplMYiNxY1XynAA"
private val TOKEN_PUBLIC_KEY = "03e7a4d7983369d33ed13020885985d68f8b05dee6168f75f9d97e1a072cdfcdc5"
private val TOKEN_PUBLIC_KEY_2 = "0354a4b971100da0c0be348fc62a30757d115cb4044adfed3d32e48298f9bcfd0c"
private val TOKEN_PROFILE_CONTENT = "{\"@context\":\"http:\\/\\/schema.org\\/\",\"@type\":\"CreativeWork\",\"name\":\"Balloon Dog\",\"creator\":[{\"@type\":\"Person\",\"@id\":\"therealjeffkoons.id\",\"name\":\"Jeff Koons\"}],\"dateCreated\":\"1994-05-09T00:00:00-0400\",\"datePublished\":\"2015-12-10T14:44:26-0500\"}"
private val DECODED_TOKEN = "{\"header\":{\"typ\":\"JWT\",\"alg\":\"ES256K\"},\"payload\":{\"jti\":\"a35837f5-c8cf-4ba3-b98e-7969a53fec48\",\"iat\":\"2017-03-04T16:13:06.790Z\",\"exp\":\"2018-03-04T16:13:06.790Z\",\"subject\":{\"publicKey\":\"03e7a4d7983369d33ed13020885985d68f8b05dee6168f75f9d97e1a072cdfcdc5\"},\"issuer\":{\"publicKey\":\"03e7a4d7983369d33ed13020885985d68f8b05dee6168f75f9d97e1a072cdfcdc5\"},\"claim\":{\"@context\":\"http:\\/\\/schema.org\\/\",\"@type\":\"CreativeWork\",\"name\":\"Balloon Dog\",\"creator\":[{\"@type\":\"Person\",\"@id\":\"therealjeffkoons.id\",\"name\":\"Jeff Koons\"}],\"dateCreated\":\"1994-05-09T00:00:00-0400\",\"datePublished\":\"2015-12-10T14:44:26-0500\"}},\"signature\":\"MF7ru91rk8IIKNEEqo9wjLHkvW3jSlcDJmeZZeOVSj9KlXApBp67q_3ke0-LzSO_YyYsUnGOplMYiNxY1XynAA\"}"
private val ZONE_FILE_CONTENT = "\$ORIGIN friedger.id\n\$TTL 3600\n_http._tcp URI 10 1 \"https://gaia.blockstack.org/hub/1Maw8BjWgj6MWrBCfupqQuWANthMhefb2v/0/profile.json\"\n"
private val ADDRESS = "1Maw8BjWgj6MWrBCfupqQuWANthMhefb2v"

@RunWith(AndroidJUnit4::class)
class BlockstackSessionTokenTest {


    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var blockstack: Blockstack

    @Before
    fun setup() {
        blockstack = Blockstack()
    }


    @Test
    fun extractProfileReturnsCorrectObject() {
        val profileToken = blockstack.extractProfile(TOKEN, null)
        assertThat(profileToken?.json.toString(), `is`("{\"@context\":\"http:\\/\\/schema.org\\/\",\"@type\":\"CreativeWork\",\"name\":\"Balloon Dog\",\"creator\":[{\"@type\":\"Person\",\"@id\":\"therealjeffkoons.id\",\"name\":\"Jeff Koons\"}],\"dateCreated\":\"1994-05-09T00:00:00-0400\",\"datePublished\":\"2015-12-10T14:44:26-0500\"}"))
    }

    @Test
    fun extractProfileAlsoVerifiesToken() {
        val profileToken = blockstack.extractProfile(TOKEN, TOKEN_PUBLIC_KEY)
        assertThat(profileToken?.json.toString(), `is`(TOKEN_PROFILE_CONTENT))
    }

    @Test
    fun extractProfileThrowsForWrongOwner() {
        val result = kotlin.runCatching {
            blockstack.extractProfile(TOKEN, TOKEN_PUBLIC_KEY_2)
        }
        assertThat(result.exceptionOrNull()?.message, endsWith("Token issuer public key does not match the verifying value"))

    }

    @Test
    fun wrapProfileTokenReturnsTokenPair() {
        val profileTokenPair = blockstack.wrapProfileToken(TOKEN)
        assertThat(profileTokenPair.json.getJSONObject("decodedToken").toString(), `is`(DECODED_TOKEN))
        assertThat(profileTokenPair.json.getString("token"), `is`(TOKEN))
    }

    @Test
    fun verifyProfileTokenReturnsToken() {
        runBlocking {
            val profileToken = blockstack.verifyProfileToken(TOKEN, TOKEN_PUBLIC_KEY)
            assertThat(profileToken.json.toString(), `is`(DECODED_TOKEN))
        }
    }

    @Test
    fun verifyProfileTokenThrowsForWrongOwner() {
        val result = kotlin.runCatching {
            runBlocking {
                blockstack.verifyProfileToken(TOKEN, TOKEN_PUBLIC_KEY_2)
            }
        }
        assertThat(result.exceptionOrNull()?.message, endsWith("Token issuer public key does not match the verifying value"))
    }

    @Test
    fun parseZoneFileReturnsCorrectObject() {
        val zoneFile = parseZoneFile(ZONE_FILE_CONTENT)
        assertThat(zoneFile.json.toString(), `is`("{\"txt\":[],\"ns\":[],\"a\":[],\"aaaa\":[],\"cname\":[],\"mx\":[],\"ptr\":[],\"srv\":[],\"spf\":[],\"uri\":[\"URIType(name=_http._tcp, target=https:\\/\\/gaia.blockstack.org\\/hub\\/1Maw8BjWgj6MWrBCfupqQuWANthMhefb2v\\/0\\/profile.json, priority=10, weight=1, ttl=null)\"],\"ttl\":3600}"))
    }

    @Test
    fun resolveZoneFileToProfileReturnsCorrectObject() {
        var result: Profile? = blockstack.resolveZoneFileToProfile(ZONE_FILE_CONTENT, ADDRESS)

        assertThat(result, notNullValue()) // content is dynamic
    }

    @Test
    fun signProfileTokenReturnsSignedToken() {
        val tokenPair = runBlocking {
            blockstack.signProfileToken(Profile(JSONObject(TOKEN_PROFILE_CONTENT)), PRIVATE_KEY, Entity.withKey(TOKEN_PUBLIC_KEY), Entity.withKey(TOKEN_PUBLIC_KEY_2))
        }
        assertThat(tokenPair.decodedToken?.signature, notNullValue())
    }
}
