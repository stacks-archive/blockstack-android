package org.blockstack.android.sdk

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.blockstack.android.sdk.ecies.AesCBC
import org.blockstack.android.sdk.ecies.decryptECIES
import org.blockstack.android.sdk.ecies.encryptECIES
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"

@RunWith(AndroidJUnit4::class)
class EncryptionEthereumTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    @Test
    fun testAesEncryptDecryptWorks() {
        val initializationVector = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6)
        val aesCBC = AesCBC("ABCDEFGHIHGEFTFD".toByteArray(Charsets.US_ASCII), initializationVector)

        val plainText = "ABC"
        val cipherText = aesCBC.encrypt(plainText.toByteArray(Charsets.UTF_8))
        assertThat(cipherText, `is`("tQxHGjJFtHVvsMYHwLIJQg=="))
        val decryptedText = aesCBC.decrypt(cipherText.toByteArray(Charsets.UTF_8))
        assertThat(decryptedText.toString(Charsets.UTF_8), `is`(plainText))
    }

    @Test
    fun testEncryptDecryptWorks() {
        val plainText = "a"
        val cipher = encryptECIES(PUBLIC_KEY, plainText, PRIVATE_KEY)
        Log.d(TAG, cipher.json.toString())
        val decryptedText = decryptECIES(PRIVATE_KEY, cipher)

        assertThat(decryptedText as String, `is`(plainText))
    }

    companion object {
        val TAG = EncryptionEthereumTest.javaClass.simpleName
    }
}
