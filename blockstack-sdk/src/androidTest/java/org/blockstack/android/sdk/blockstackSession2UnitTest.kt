package org.blockstack.android.sdk;

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.preference.PreferenceManager
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import kotlinx.coroutines.experimental.runBlocking
import org.blockstack.android.sdk.test.R
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch

val A_VALID_BLOCKSTACK_SESSION_JSON = ""

@RunWith(AndroidJUnit4::class)
class BlockstackSession2UnitTest {
    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var sessionStore: SessionStore
    private lateinit var executor: Executor
    private lateinit var session: BlockstackSession2

    @Before
    fun setup() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(rule.activity)
        prefs.edit().putString("blockstack_session", A_VALID_BLOCKSTACK_SESSION_JSON)
                .apply()
        sessionStore = SessionStore(prefs)

        executor = object : Executor {
            override fun onMainThread(function: () -> Unit) {
                function()
            }

            override fun onWorkerThread(function: suspend () -> Unit) {
                runBlocking {
                    function()
                }
            }
        }
        session = BlockstackSession2(rule.activity, "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray()),
                sessionStore = sessionStore,
                executor = executor)
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