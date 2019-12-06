package org.blockstack.collection

import android.util.Log
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JSON
import org.blockstack.android.sdk.BLOCKSTACK_SESSION
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.COLLECTION_KEY_FILENAME
import org.blockstack.android.sdk.SessionStore
import org.blockstack.android.sdk.model.PutFileOptions
import org.blockstack.collection.test.TestActivity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.json.JSONObject
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactsTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    @Test
    fun testContactScope() {
        assertThat(Contact.scope.name,`is`("collection.Contact"))
    }

    @Test
    fun testContacts() {
        runBlocking {
            val session = JSONObject(A_VALID_BLOCKSTACK_SESSION_JSON)
            val userData = session.getJSONObject("userData")
            val encryptionKey = userData.getString("appPrivateKey")
            val hubUrl = userData.getString("hubUrl")

            val sessionStore = sessionStoreforIntegrationTests(rule)
            val userSession = BlockstackSession(sessionStore)

            val collectionHubConfig = userSession.hub.connectToGaia(hubUrl, encryptionKey, null)
            val keys = JSONObject().put("Contact", JSONObject()
                    .put("encryptionKey", encryptionKey)
                    .put("hubConfig", JSONObject()
                            .put("address", collectionHubConfig.address)
                            .put("token", collectionHubConfig.token)
                            .put("server", collectionHubConfig.server)
                            .put("url_prefix", collectionHubConfig.urlPrefix))
            )
            userSession.putFile(COLLECTION_KEY_FILENAME, keys.toString() , PutFileOptions())

            var contact = Contact(ContactAttr("1.0", "BlockStack", "Block", "Stack",
                    "blockstack.id", null, null, null, null, null, null, null, null, null))

            var result = contact.save(userSession)
            assertThat(result.error, nullValue())

            contact = Contact.get(contact.attrs.identifier, userSession) as Contact
            assertThat(contact.attrs.toString(), `is`("ContactAttr(schemaVersion=1.0, identifier=BlockStack, firstName=Block, lastName=Stack, blockstackID=blockstack.id, email=null, website=null, address=null, telephone=null, organization=null, createdAt=0, updatedAt=0, signingKeyId=null, _id=null)"))

            val contact2 = Contact(ContactAttr("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", 11, 12, "13", "14"))
            result = contact2.delete(userSession)
            assertThat(result.error!!.message, `is`("Failed to delete file: 404"))
        }
    }
}


val A_VALID_BLOCKSTACK_SESSION_JSON = "{\"transitKey\":\"000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f\",\"userData\":{\"username\":null,\"profile\":{\"@type\":\"Person\",\"@context\":\"http:\\/\\/schema.org\",\"apps\":{\"https:\\/\\/app.graphitedocs.com\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/1Fuzd6sJXqtXhgoFpb8W19Ao4BCG3EHQGf\\/\",\"https:\\/\\/app.misthos.io\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/1CkGhNN71a1dpgXQjncunYZpXakJvwrpmc\\/\"},\"image\":[{\"@type\":\"ImageObject\",\"name\":\"avatar\",\"contentUrl\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/12Gpr9kYXJJn4fWec4nRVwHLSrrTieeywt\\/\\/avatar-0\"}],\"name\":\"fm\"},\"decentralizedID\":\"did:btc-addr:12Gpr9kYXJJn4fWec4nRVwHLSrrTieeywt\",\"identityAddress\":\"12Gpr9kYXJJn4fWec4nRVwHLSrrTieeywt\",\"appPrivateKey\":\"89f92476f13f5b173e53926ad7d6e22baf78c6b1dcdf200c38dc73d2bf47d43b\",\"coreSessionToken\":null,\"authResponseToken\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJqdGkiOiI3MWI3NWY1Yi0xNDJkLTQxMzQtOGZkYy0zYWUyMWRmZTkzNjAiLCJpYXQiOjE1MzcxNjkxODYsImV4cCI6MTUzOTc2MTE4NSwiaXNzIjoiZGlkOmJ0Yy1hZGRyOjEyR3ByOWtZWEpKbjRmV2VjNG5SVndITFNyclRpZWV5d3QiLCJwcml2YXRlX2tleSI6IjdiMjI2OTc2MjIzYTIyMzEzOTMzMzM2NTM5MzYzNTY2NjMzNzM5NjM2MjYyMzAzNjY0NjMzMjM0MzIzNzY2NjY2MjYzMzczMjMyMzgzOTIyMmMyMjY1NzA2ODY1NmQ2NTcyNjE2YzUwNGIyMjNhMjIzMDMyMzQzMTY1MzY2NjYzMzEzODM0MzAzMDMyMzQzMTY0NjYzNzMzMzMzNTY1NjM2MjY2MzQzMzM0NjY2NDM5MzU2MjY0NjUzMjY2MzIzNTMxNjU2MzM3MzczNjMxMzQzNDM0MzAzMjY2NjUzNTM3NjE2MzYzMzA2MjY2MzQzNjYxMzQyMjJjMjI2MzY5NzA2ODY1NzI1NDY1Nzg3NDIyM2EyMjM5MzkzMTYxMzM2MzM1NjYzOTY2NjI2MzM2MzYzMzY2MzMzMDY0MzgzNzMxMzgzNzMyNjIzODM5MzMzMTYxMzMzMTMyNjUzMjM0NjM2MjM4MzAzNzM1NjQzODY0MzkzNzM1Mzg2MjM3NjEzMDY1NjMzMTM0Mzg2MTM2MzEzMjM2MzIzMjY1MzYzMjYyMzQ2NjY1NjYzNjMzNjMzMTYzNjYzMjMzNjEzMTM3MzM2NTM0NjUzOTMwMzIzMzM5MzQ2NDY1NjEzMTMxMzQzOTYzNjE2NTYxMzMzMzY2MzMzODY2NjIzMTMwMzczNTM0MzU2NDM1MzUzNDM3NjYzNjY2NjMzODMwMzg2MTM2MzIzNjM2MzAzMDMzNjMzNzM5MzMzNzM4MzYzMzM4MzMzNDMyNjEzNjYzMzMzOTM4MzM2MjM5MjIyYzIyNmQ2MTYzMjIzYTIyNjYzODMwMzQ2MjMzMzY2MjY0MzgzMjM2NjQzNTM3MzIzMzYxMzQzOTM4MzczOTYzMzAzMDYzNjQ2MzMzNjYzMDYxNjQzNzY0NjE2MTYyNjE2NDM0MzY2NDMzMzczNjY0MzUzMTYxNjI2NjYzNjYzMTM4MzM2MTM4MzAzNDMwMzgyMjJjMjI3NzYxNzM1Mzc0NzI2OTZlNjcyMjNhNzQ3Mjc1NjU3ZCIsInB1YmxpY19rZXlzIjpbIjAyODcyNmQyMGZhMTI4Yjc1OWViOGIwOWZjY2Y2ODU2ZTQzMjM1YmI5OTkxNjI0OWNmNjNhM2NiMjA0MTY0Y2I4ZSJdLCJwcm9maWxlIjpudWxsLCJ1c2VybmFtZSI6bnVsbCwiY29yZV90b2tlbiI6bnVsbCwiZW1haWwiOiJmcmllZGdlckBnbWFpbC5jb20iLCJwcm9maWxlX3VybCI6Imh0dHBzOi8vZ2FpYS5ibG9ja3N0YWNrLm9yZy9odWIvMTJHcHI5a1lYSkpuNGZXZWM0blJWd0hMU3JyVGllZXl3dC9wcm9maWxlLmpzb24iLCJodWJVcmwiOiJodHRwczovL2h1Yi5ibG9ja3N0YWNrLm9yZyIsInZlcnNpb24iOiIxLjIuMCJ9.QM8wx-EDjZrHk1ubK99divaejN5XAcCn_OkYAiPXQUNtzENUy8ogyrHzB4xC7mwfMckzFsMxEOdzwVW_Xcjqqg\",\"hubUrl\":\"https:\\/\\/hub.blockstack.org\"}}"

fun sessionStoreforIntegrationTests(rule: ActivityTestRule<*>): SessionStore {
    val prefs = PreferenceManager.getDefaultSharedPreferences(rule.activity)
    prefs.edit().putString(BLOCKSTACK_SESSION, A_VALID_BLOCKSTACK_SESSION_JSON)
            .apply()
    return SessionStore(prefs)
}
