package org.blockstack.android.sdk

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.blockstack.android.sdk.ecies.EncryptionColendi
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"

@RunWith(AndroidJUnit4::class)
class EncryptionColendiKotlinTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    @Test
    fun testEncryptDecryptWorks() {
        val encryption = EncryptionColendi()

        val plainText = "a"
        val cipher = encryption.encryptWithPublicKey("04327453891187123d8a122c47ac5a98ff9a1cbc0dd28ce6fae2183a51a7b8aeaaea8b75c7ac46fbc2434c0fe8b8fecb5fee1be8b52bff3072046fe26ca3652279", plainText)
        Log.d(TAG, cipher.toString())
        val decryptedText = encryption.decryptWithPrivateKey(cipher, "7a7480972a756b1f117faadd23f9af00bdb309d3553e47b3b5d7f2756df620b3")

        assertThat(decryptedText, `is`(plainText))
    }

    companion object {
        val TAG = EncryptionColendiKotlinTest.javaClass.simpleName
    }
}
