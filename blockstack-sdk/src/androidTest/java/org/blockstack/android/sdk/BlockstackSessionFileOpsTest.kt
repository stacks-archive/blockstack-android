package org.blockstack.android.sdk

import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Assert.fail
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

    lateinit var session: BlockstackSession

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getContext()
        val latch = CountDownLatch(1)
        val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(arrayOf())

        // sign in if needed
        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config) {
                session.makeAuthResponse(PRIVATE_KEY) { authResponse ->
                    if (authResponse.hasValue) {
                        session.handlePendingSignIn(authResponse.value!!) {
                            if (it.hasValue) {
                                Log.d("FileOpsTest", it.value!!.json.toString())
                                latch.countDown()
                            } else {
                                failTest("sign in failed")
                            }
                        }
                    }
                }
            }
        }

        latch.await()
    }


    /*
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
                } else {
                    rule.activity.runOnUiThread {
                        session.getFile(path, GetFileOptions(false)) {
                            if (!it.hasValue) {
                                failTest("failed to get file " + it.error)
                            } else {

                                fileContent = it.value as String?
                                latch.countDown()
                            }
                        }
                    }
                }
            }
        }

        latch.await()
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
                } else {
                    rule.activity.runOnUiThread {
                        session.getFile(path, GetFileOptions(false)) {
                            if (!it.hasValue) {
                                failTest("failed to get file " + it.error)
                            } else {
                                fileContent = it.value as ByteArray
                                latch.countDown()
                            }
                        }
                    }
                }
            }
        }

        latch.await()
        assertThat(fileContent, notNullValue())

        assertThat(String(fileContent!!), `is`(content))
    }

*/

    @Test
    fun plainTextFileCanNotBeUploadedWithBadAppPrivateKey() {

        var latch = CountDownLatch(1)
        var error: String? = null

        val path = "test1.txt"
        val content = "Test content"
        rule.activity.runOnUiThread {
            session.putFile(path, content, PutFileOptions(false)) { putFileResult ->
                if (!putFileResult.hasErrors) {
                    failTest("should fail with 'failed to fetch' error")
                } else {
                    error = putFileResult.error!!.message
                    latch.countDown()
                }
            }
        }

        latch.await()
        assertThat(error, notNullValue())
        assertThat(error as String, `is`("TypeError: Failed to fetch"))
    }


    private fun failTest(msg: String) {
        rule.activity.runOnUiThread {
            fail(msg)
        }
    }

}
