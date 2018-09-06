package org.blockstack.android.sdk

import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

/**
 * Instrumented tests around encryption and decryption.
 */
@RunWith(AndroidJUnit4::class)
class BlockstackSessionFileOpsTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
    private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
    private val RANDOM_APP_PRIVATE_KEY ="4865cadc9e36781684eb1bda49fc12528b8559469349f86ea70c0484eca6c6be"

    lateinit var session: BlockstackSession
    private var lastError: String? = null
    private val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(arrayOf(Scope.StoreWrite, Scope.PublishData))

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getContext()
        val latch = CountDownLatch(1)

        // sign in if needed
        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config) {
                session.makeAuthResponse(PRIVATE_KEY, RANDOM_APP_PRIVATE_KEY) { authResponse ->
                    if (authResponse.hasValue) {
                        session.handlePendingSignIn(authResponse.value!!) {
                            if (it.hasValue) {
                                Log.d("FileOpsTest", it.value!!.json.toString())
                            } else {
                                failTest("sign in failed")
                            }
                            latch.countDown()
                        }
                    }
                }
            }
        }

        latch.await()
        assertThat(lastError, nullValue())
    }



    @Test
    fun uploadedPlainTextFileCanBeDownloaded() {

        var latch = CountDownLatch(1)
        var fileContent: String? = null

        val path = "test1.txt"
        val content = "Test content"
        rule.activity.runOnUiThread {
            session.putFile(path, content, PutFileOptions(false)) { putFileResult ->
                if (!putFileResult.hasValue) {
                    failTest("failed to put file " + putFileResult.error)
                    latch.countDown()
                } else {
                    rule.activity.runOnUiThread {
                        session.getFile(path, GetFileOptions(false)) {
                            if (!it.hasValue) {
                                failTest("failed to get file " + it.error)
                            } else {
                                fileContent = it.value as String?
                            }
                            latch.countDown()
                        }
                    }
                }
            }
        }

        latch.await()
        assertThat(lastError, nullValue())
        assertThat(fileContent, notNullValue())

        assertThat(fileContent as String, `is`(content))
    }

    @Test
    fun uploadedPlainBinaryFileCanBeDownloaded() {

        var latch = CountDownLatch(1)
        var fileContent: ByteArray? = null

        val path = "test1.txt"
        val content = "Test content"
        rule.activity.runOnUiThread {
            session.putFile(path, content.toByteArray(), PutFileOptions(false)) { putFileResult ->
                if (!putFileResult.hasValue) {
                    failTest("failed to put file " + putFileResult.error)
                    latch.countDown()
                } else {
                    rule.activity.runOnUiThread {
                        session.getFile(path, GetFileOptions(false)) {
                            if (!it.hasValue) {
                                failTest("failed to get file " + it.error)
                            } else {
                                fileContent = it.value as ByteArray
                            }
                            latch.countDown()
                        }
                    }
                }
            }
        }

        latch.await()
        assertThat(lastError, nullValue())
        assertThat(fileContent, notNullValue())

        assertThat(String(fileContent!!), `is`(content))
    }


    @Test
    fun getUserAppFileUrlReturns_NO_URL_forNonPublicFile() {

        val context = InstrumentationRegistry.getContext()
        var latch = CountDownLatch(1)
        lateinit var session: BlockstackSession
        var url: String? = null

        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config) {
                session.getUserAppFileUrl("non_public_file.txt", "friedger.id", "https://blockstack-todos.appartisan.com/", null) {
                    if (it.hasValue) {
                        url = it.value
                        latch.countDown()
                    } else {
                        failTest("failed to get user app file url: " + it.error)
                        latch.countDown()
                    }
                }
            }
        }

        latch.await()
        assertThatTestDidNotFail()
        assertThat(url, `is`("NO_URL"))
    }


    @Test
    fun getAppBucketUrlReturnsUrl() {
        val latch = CountDownLatch(1)
        var url: String? = null

        rule.activity.runOnUiThread {
            session.getAppBucketUrl("https://hub.blockstack.org", PRIVATE_KEY) {
                if (it.hasValue) {
                    url = it.value
                    latch.countDown()
                } else {
                    failTest("failed to get App bucket url: " + it.error)
                    latch.countDown()
                }
            }
        }

        latch.await()
        assertThatTestDidNotFail()
        assertThat(url, `is`("https://gaia.blockstack.org/hub/1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U/"))
    }

    private fun failTest(msg: String) {
        rule.finishActivity()
        lastError = msg
    }

    private fun assertThatTestDidNotFail() {
        assertThat(lastError, nullValue())
    }
}
