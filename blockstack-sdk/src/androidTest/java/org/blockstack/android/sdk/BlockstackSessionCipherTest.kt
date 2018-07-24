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

    lateinit var session: BlockstackSession

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getContext()
        val latch = CountDownLatch(1)
        val config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(arrayOf())
        val authResponse = ""

        // sign in if needed
        rule.activity.runOnUiThread {
            session = BlockstackSession(context, config) {
                session.isUserSignedIn { signedIn ->
                    if (signedIn) {
                        latch.countDown()
                    } else {
                        session.handlePendingSignIn(authResponse, failureCallback = {
                            failTest("sign in failed")
                        }) {
                            latch.await()
                        }
                    }
                }
            }
        }

        latch.await()
    }

    private fun failTest(msg:String) {
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
        val privateKey = "pk"
        val cryptoOptions = CryptoOptions(privateKey)
        rule.activity.runOnUiThread {
            session.encryptContent(plainText, cryptoOptions) { cipherObject ->
                assertThat(cipherObject, notNullValue())

                session.decryptContent(cipherObject!!, cryptoOptions) {
                    decryptedContent = it
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
        val privateKey = "pk"
        val cryptoOptions = CryptoOptions(privateKey)
        rule.activity.runOnUiThread {
            session.encryptContent(plainByteArray, cryptoOptions) { cipherObject ->
                assertThat(cipherObject, notNullValue())

                session.decryptContent(cipherObject!!, cryptoOptions) {
                    decryptedContent = it
                    latch.countDown()
                }
            }
        }

        latch.await()
        assertThat(decryptedContent, notNullValue())
        assertThat(String(decryptedContent as ByteArray), `is`(plainText))
    }
}
