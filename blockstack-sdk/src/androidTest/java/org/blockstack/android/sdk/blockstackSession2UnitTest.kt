package org.blockstack.android.sdk;

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.preference.PreferenceManager
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import kotlinx.coroutines.experimental.runBlocking
import org.blockstack.android.sdk.test.R
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch

val A_VALID_BLOCKSTACK_SESSION_JSON = "{\"transitKey\":\"000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f\",\"userData\":{\"username\":null,\"profile\":{\"@type\":\"Person\",\"@context\":\"http:\\/\\/schema.org\",\"apps\":{\"https:\\/\\/app.graphitedocs.com\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/1Fuzd6sJXqtXhgoFpb8W19Ao4BCG3EHQGf\\/\",\"https:\\/\\/app.misthos.io\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/1CkGhNN71a1dpgXQjncunYZpXakJvwrpmc\\/\"},\"image\":[{\"@type\":\"ImageObject\",\"name\":\"avatar\",\"contentUrl\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/12Gpr9kYXJJn4fWec4nRVwHLSrrTieeywt\\/\\/avatar-0\"}],\"name\":\"fm\"},\"decentralizedID\":\"did:btc-addr:12Gpr9kYXJJn4fWec4nRVwHLSrrTieeywt\",\"identityAddress\":\"12Gpr9kYXJJn4fWec4nRVwHLSrrTieeywt\",\"appPrivateKey\":\"89f92476f13f5b173e53926ad7d6e22baf78c6b1dcdf200c38dc73d2bf47d43b\",\"coreSessionToken\":null,\"authResponseToken\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJqdGkiOiI3MWI3NWY1Yi0xNDJkLTQxMzQtOGZkYy0zYWUyMWRmZTkzNjAiLCJpYXQiOjE1MzcxNjkxODYsImV4cCI6MTUzOTc2MTE4NSwiaXNzIjoiZGlkOmJ0Yy1hZGRyOjEyR3ByOWtZWEpKbjRmV2VjNG5SVndITFNyclRpZWV5d3QiLCJwcml2YXRlX2tleSI6IjdiMjI2OTc2MjIzYTIyMzEzOTMzMzM2NTM5MzYzNTY2NjMzNzM5NjM2MjYyMzAzNjY0NjMzMjM0MzIzNzY2NjY2MjYzMzczMjMyMzgzOTIyMmMyMjY1NzA2ODY1NmQ2NTcyNjE2YzUwNGIyMjNhMjIzMDMyMzQzMTY1MzY2NjYzMzEzODM0MzAzMDMyMzQzMTY0NjYzNzMzMzMzNTY1NjM2MjY2MzQzMzM0NjY2NDM5MzU2MjY0NjUzMjY2MzIzNTMxNjU2MzM3MzczNjMxMzQzNDM0MzAzMjY2NjUzNTM3NjE2MzYzMzA2MjY2MzQzNjYxMzQyMjJjMjI2MzY5NzA2ODY1NzI1NDY1Nzg3NDIyM2EyMjM5MzkzMTYxMzM2MzM1NjYzOTY2NjI2MzM2MzYzMzY2MzMzMDY0MzgzNzMxMzgzNzMyNjIzODM5MzMzMTYxMzMzMTMyNjUzMjM0NjM2MjM4MzAzNzM1NjQzODY0MzkzNzM1Mzg2MjM3NjEzMDY1NjMzMTM0Mzg2MTM2MzEzMjM2MzIzMjY1MzYzMjYyMzQ2NjY1NjYzNjMzNjMzMTYzNjYzMjMzNjEzMTM3MzM2NTM0NjUzOTMwMzIzMzM5MzQ2NDY1NjEzMTMxMzQzOTYzNjE2NTYxMzMzMzY2MzMzODY2NjIzMTMwMzczNTM0MzU2NDM1MzUzNDM3NjYzNjY2NjMzODMwMzg2MTM2MzIzNjM2MzAzMDMzNjMzNzM5MzMzNzM4MzYzMzM4MzMzNDMyNjEzNjYzMzMzOTM4MzM2MjM5MjIyYzIyNmQ2MTYzMjIzYTIyNjYzODMwMzQ2MjMzMzY2MjY0MzgzMjM2NjQzNTM3MzIzMzYxMzQzOTM4MzczOTYzMzAzMDYzNjQ2MzMzNjYzMDYxNjQzNzY0NjE2MTYyNjE2NDM0MzY2NDMzMzczNjY0MzUzMTYxNjI2NjYzNjYzMTM4MzM2MTM4MzAzNDMwMzgyMjJjMjI3NzYxNzM1Mzc0NzI2OTZlNjcyMjNhNzQ3Mjc1NjU3ZCIsInB1YmxpY19rZXlzIjpbIjAyODcyNmQyMGZhMTI4Yjc1OWViOGIwOWZjY2Y2ODU2ZTQzMjM1YmI5OTkxNjI0OWNmNjNhM2NiMjA0MTY0Y2I4ZSJdLCJwcm9maWxlIjpudWxsLCJ1c2VybmFtZSI6bnVsbCwiY29yZV90b2tlbiI6bnVsbCwiZW1haWwiOiJmcmllZGdlckBnbWFpbC5jb20iLCJwcm9maWxlX3VybCI6Imh0dHBzOi8vZ2FpYS5ibG9ja3N0YWNrLm9yZy9odWIvMTJHcHI5a1lYSkpuNGZXZWM0blJWd0hMU3JyVGllZXl3dC9wcm9maWxlLmpzb24iLCJodWJVcmwiOiJodHRwczovL2h1Yi5ibG9ja3N0YWNrLm9yZyIsInZlcnNpb24iOiIxLjIuMCJ9.QM8wx-EDjZrHk1ubK99divaejN5XAcCn_OkYAiPXQUNtzENUy8ogyrHzB4xC7mwfMckzFsMxEOdzwVW_Xcjqqg\",\"hubUrl\":\"https:\\/\\/hub.blockstack.org\"}}"

@RunWith(AndroidJUnit4::class)
class BlockstackSession2UnitTest {
    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var sessionStore: SessionStore
    private lateinit var executor: Executor
    private lateinit var session: BlockstackSession

    @Before
    fun setup() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(rule.activity)
        prefs.edit().putString("blockstack_session", A_VALID_BLOCKSTACK_SESSION_JSON)
                .apply()
        sessionStore = SessionStore(prefs)

        executor = object : Executor {
            override fun onMainThread(function: (Context) -> Unit) {
                function(rule.activity)
            }

            override fun onWorkerThread(function: suspend () -> Unit) {
                runBlocking {
                    function()
                }
            }
        }
        session = BlockstackSession(rule.activity, "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray()),
                sessionStore = sessionStore,
                executor = executor)
    }

    @Test
    fun loadedIsTrueAfterSessionCreated() {
        Assert.assertThat(session.loaded, Matchers.`is`(true))
    }

    @Test
    fun testHandlingPendingSignInWithInvalidToken() {
        val latch = CountDownLatch(1)
        var error: String? = null
        session.handlePendingSignIn("authResponse") {
            latch.countDown()
            error = it.error
        }
        latch.await()
        Assert.assertThat(error, Matchers.`is`("SyntaxError: Unexpected token j in JSON at position 0"))
    }

    @Test
    fun loadUserDataIsNullAfterSignOut() {
        session.signUserOut()
        assertThat(session.loadUserData(), `is`(nullValue()))
    }

    @Test
    fun testEncryptDecryptString() {
        assertTrue(session.isUserSignedIn())
        val options = CryptoOptions()
        val plainContent = "hello from test"
        val encResult = session.encryptContent(plainContent, options = options)
        assertTrue(encResult.hasValue)
        val decResult = session.decryptContent(encResult.value!!.json.toString(), false, options)
        assertThat(decResult.value as String, `is`(plainContent))
    }

    @Test
    fun testEncryptDecryptBinary() {
        assertTrue(session.isUserSignedIn())
        val binaryContent = getImageBytes()
        val options = CryptoOptions()
        val encResult = session.encryptContent(binaryContent, options = options)
        assertTrue(encResult.hasValue)
        val decResult = session.decryptContent(encResult.value!!.json.toString(), true, options)
        assertThat((decResult.value as ByteArray).size, `is`(binaryContent.size))
    }

    @Test
    fun testPutGetStringFile() {
        var result: String? = null
        val latch = CountDownLatch(1)

        if (session.isUserSignedIn()) {
            session.putFile("try.txt", "Hello Test", PutFileOptions(false), {
                session.getFile("try.txt", GetFileOptions(false)) {
                    result = it.value as String
                    latch.countDown()
                }
            })
        } else {
            latch.countDown()
        }
        latch.await()
        assertThat(result, `is`("Hello Test"))
    }

    @Test
    fun testPutGetEncryptedStringFile() {
        var result: String? = null
        val latch = CountDownLatch(1)

        if (session.isUserSignedIn()) {
            session.putFile("try.txt", "Hello Test", PutFileOptions(true), {
                session.getFile("try.txt", GetFileOptions(true)) {
                    result = it.value as String
                    latch.countDown()
                }
            })
        } else {
            latch.countDown()
        }
        latch.await()
        assertThat(result, `is`("Hello Test"))
    }

    @Test
    fun testPutGetBinaryFile() {
        val bitMapData = getImageBytes()

        var result: ByteArray? = null

        val latch = CountDownLatch(1)

        if (session.isUserSignedIn()) {
            session.putFile("try.txt", bitMapData, PutFileOptions(false), {
                session.getFile("try.txt", GetFileOptions(false)) {
                    result = it.value as ByteArray
                    latch.countDown()
                }
            })
        } else {
            latch.countDown()
        }
        latch.await()
        assertThat(result?.size, `is`(bitMapData.size))
    }

    @Test
    fun testPutGetEncryptedBinaryFile() {
        val bitMapData = getImageBytes()

        var result: ByteArray? = null

        val latch = CountDownLatch(1)

        if (session.isUserSignedIn()) {
            session.putFile("try.txt", bitMapData, PutFileOptions(true), {
                session.getFile("try.txt", GetFileOptions(true)) {
                    result = it.value as ByteArray
                    latch.countDown()
                }
            })
        } else {
            latch.countDown()
        }
        latch.await()
        assertThat(result?.size, `is`(bitMapData.size))

    }


    private fun getImageBytes(): ByteArray {
        val drawable: BitmapDrawable = rule.activity.resources.getDrawable(R.drawable.blockstackteam) as BitmapDrawable

        val bitmap = drawable.getBitmap()
        val stream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bitMapData = stream.toByteArray()
        return bitMapData
    }
}