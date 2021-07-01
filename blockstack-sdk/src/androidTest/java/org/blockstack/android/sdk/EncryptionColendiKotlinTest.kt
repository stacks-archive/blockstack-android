package org.blockstack.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.blockstack.android.sdk.ecies.EncryptedResult
import org.blockstack.android.sdk.ecies.EncryptionColendi
import org.blockstack.android.sdk.test.TestActivity
import org.junit.Ignore
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
    @Ignore("Test not passing on 0.6.2, no changes made here in 0.6.3 marked as ignored until fixes are made")
    fun testEncryptDecryptWorks() {
        val encryption = EncryptionColendi()

        val message = "Colendi"


        val encryptedResult = encryption.encryptWithPublicKey(message, PUBLIC_KEY)

        val formData = EncryptedResult(encryptedResult.ephemPubString,
                encryptedResult.ivString,
                encryptedResult.macString,
                encryptedResult.encryptedText)

        val result = encryption.decryptWithPrivateKey(formData, PRIVATE_KEY)
        assert(result == message)
    }

    companion object {
        val TAG = EncryptionColendiKotlinTest.javaClass.simpleName
    }
}
