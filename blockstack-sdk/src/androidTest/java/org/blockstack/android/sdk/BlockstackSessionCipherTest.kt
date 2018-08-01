package org.blockstack.android.sdk

import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
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
class BlockstackSessionCipherTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
    private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
    private val DECENTRALIZED_ID = "did:btc-addr:1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"

    lateinit var session: BlockstackSession

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getContext()
        val latch = CountDownLatch(1)
        val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(arrayOf())

        // sign in if needed
        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config) {
                session.makeAuthResponse(PUBLIC_KEY) { authResponse ->
                    if (authResponse.hasValue) {
                        session.isUserSignedIn { signedIn ->
                            if (signedIn) {
                                latch.countDown()
                            } else {
                                session.handlePendingSignIn(authResponse.value!!) {
                                    if (it.hasValue) {
                                        latch.countDown()
                                    } else {
                                        failTest("sign in failed")
                                    }
                                }
                            }
                        }
                    } else {
                        failTest("makeAuthResponse failed")
                    }
                }
            }
        }

        latch.await()
    }

    private fun failTest(msg: String) {
        rule.activity.runOnUiThread {
            fail(msg)
        }
    }

    @Test
    fun encryptedStringCanBeDecrypted() {


        // verify user data
        var latch = CountDownLatch(1)
        var decryptedContent: Any? = null

        val plainText = "PlainText"
        val cryptoOptions = CryptoOptions(PUBLIC_KEY, PRIVATE_KEY)
        rule.activity.runOnUiThread {
            session.encryptContent(plainText, cryptoOptions) { cipherObjectResult ->
                assertThat(cipherObjectResult.value, notNullValue())

                session.decryptContent(cipherObjectResult.value!!.json.toString(), cryptoOptions) {
                    decryptedContent = it.value
                    latch.countDown()
                }
            }
        }

        latch.await()
        assertThat(decryptedContent, notNullValue())
        assertThat(decryptedContent as String, `is`(plainText))
    }

    @Test
    fun encryptedByteArrayCanBeDecrypted() {


        // verify user data
        var latch = CountDownLatch(1)
        var decryptedContent: Any? = null

        val plainText = "PlainText"
        val plainByteArray = plainText.toByteArray()
        val cryptoOptions = CryptoOptions(PUBLIC_KEY, PRIVATE_KEY)
        rule.activity.runOnUiThread {
            session.encryptContent(plainByteArray, cryptoOptions) { cipherObjectResult ->
                assertThat(cipherObjectResult.value, notNullValue())

                session.decryptContent(cipherObjectResult.value!!.json.toString(), cryptoOptions) {
                    decryptedContent = it.value
                    latch.countDown()
                }
            }
        }

        latch.await()
        assertThat(decryptedContent, notNullValue())
        assertThat(String(decryptedContent as ByteArray), `is`(plainText))
    }
}
