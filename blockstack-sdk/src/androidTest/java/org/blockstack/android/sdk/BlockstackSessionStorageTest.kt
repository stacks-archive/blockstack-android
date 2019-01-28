package org.blockstack.android.sdk;

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.blockstack.android.sdk.model.CryptoOptions
import org.blockstack.android.sdk.model.GetFileOptions
import org.blockstack.android.sdk.model.PutFileOptions
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
private val DECENTRALIZED_ID = "did:btc-addr:1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"
private val BITCOIN_ADDRESS = "1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"


@RunWith(AndroidJUnit4::class)
class BlockstackSessionStorageTest {
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
    fun testPutStringFileTwice() {
        var result1: String? = null
        var result2: String? = null
        val latch = CountDownLatch(2)

        if (session.isUserSignedIn()) {
            session.putFile("try.txt", "Hello Test", PutFileOptions(false), {
                result1 = it.value as String
                latch.countDown()
            })
            session.putFile("try.txt", "Hello Test2", PutFileOptions(false), {
                result2 = it.value as String
                latch.countDown()
            })
        } else {
            latch.countDown()
            latch.countDown()
        }
        latch.await()
        assertThat(result1, `is`(notNullValue()))
        assertThat(result2, `is`(notNullValue()))
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
    fun testGetFileFor404File() {
        var result: Result<Any>? = null
        val latch = CountDownLatch(1)

        if (session.isUserSignedIn()) {
            session.getFile("404file.txt", GetFileOptions(false)) {
                result = it
                latch.countDown()
            }
        } else {
            latch.countDown()
        }
        latch.await()
        assertThat(result, `is`(notNullValue()))
        assertThat(result?.value, `is`(nullValue()))
        assertThat(result?.error, `is`(nullValue()))
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
    fun testPutGetStringFileWithContentType() {
        var result: Any? = null
        val latch = CountDownLatch(1)

        if (session.isUserSignedIn()) {
            session.putFile("try.txt", "Hello Test", PutFileOptions(false, "application/x.foo")) {
                val u = URL(it.value).openConnection()
                u.connect()
                result = u.contentType
            }
        } else {
            latch.countDown()
        }
        latch.await()
        assertThat(result as String, `is`("application/x.foo"))
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


    @Test
    fun getUserAppFileUrlReturns_NO_URL_forNonPublicFile() {
        val latch = CountDownLatch(1)
        var url: String? = null

        session.getUserAppFileUrl("non_public_file.txt", "friedger.id", "https://blockstack-todos.appartisan.com/", null) {
            if (it.hasValue) {
                url = it.value
                latch.countDown()
            } else {
                latch.countDown()
            }
        }

        latch.await()
        assertThat(url, `is`("NO_URL"))
    }

    @Test
    fun getAppBucketUrlReturnsUrl() {
        val latch = CountDownLatch(1)
        var url: String? = null

        session.getAppBucketUrl("https://hub.blockstack.org", PRIVATE_KEY) {
            if (it.hasValue) {
                url = it.value
                latch.countDown()
            } else {
                latch.countDown()
            }
        }

        latch.await()
        assertThat(url, `is`("https://gaia.blockstack.org/hub/1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U/"))
    }

    @Test
    fun listFilesReturnsCorrectNumberOfFiles() {
        val latch = CountDownLatch(1)
        var count = 0
        var countResult: Result<Int>? = null
        session.listFiles({ fileResult ->
            assertTrue(fileResult.hasValue)
            count++
            true
        }, {
            countResult = it
            latch.countDown()
        })
        latch.await(1, TimeUnit.MINUTES)
        assertThat(countResult?.value!!, `is`(count))
    }

    @Test
    fun listFilesCanHandleErrorInCallback() {
        val latch = CountDownLatch(1)
        var countResult: Result<Int>? = null
        session.listFiles({ fileResult ->
            throw RuntimeException("I want to make the API crash!")
        }, {
            countResult = it
            latch.countDown()
        })
        latch.await(1, TimeUnit.MINUTES)
        assertThat(countResult?.error, `is`("I want to make the API crash!"))
    }

    private fun getImageBytes(): ByteArray {
        val drawable: BitmapDrawable = rule.activity.resources.getDrawable(org.blockstack.android.sdk.test.R.drawable.blockstackteam) as BitmapDrawable

        val bitmap = drawable.getBitmap()
        val stream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bitMapData = stream.toByteArray()
        return bitMapData
    }
}