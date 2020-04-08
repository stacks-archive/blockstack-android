package org.blockstack.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import org.blockstack.android.sdk.model.CryptoOptions
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI


private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
private val ADDRESS = "1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"

private val TAG = BlockstackTest::class.java.simpleName


@RunWith(AndroidJUnit4::class)
class BlockstackTest {


    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var blockstack: Blockstack

    @Before
    fun setup() {
        blockstack = Blockstack()
    }

    @Test
    fun getPublicKeyFromPrivateReturnsCorrectKey() {
        assertThat(blockstack.getPublicKeyFromPrivate(PRIVATE_KEY), `is`(PUBLIC_KEY))
    }

    @Test
    fun makeECPrivateKeyDoesNotThrow() {
        blockstack.makeECPrivateKey()
        // ok, no exception thrown
    }

    @Test
    fun publicKeyToAddressReturnsCorrectAddress() {
        assertThat(blockstack.publicKeyToAddress(PUBLIC_KEY), `is`(ADDRESS))
    }

    @Test
    fun testLookupProfile() {

        val profile = runBlocking {
            blockstack.lookupProfile("public_profile_for_testing.id.blockstack", null)
        }
        // Note that this can fail due to updates on the profile (the secret key is publicly available
        assertThat(profile.json.toString(), startsWith("{\"@type\":\"Person\",\"@context\":\"http:\\/\\/schema.org\",\"apps\":{"))
    }


    @Test
    fun testEncryptThenDecrypt() {
        val message = "Hello Test"
        val result = blockstack.encryptContent(message, CryptoOptions(publicKey = PUBLIC_KEY))
        val plainText = blockstack.decryptContent(result.value!!.json.toString(), false, CryptoOptions(privateKey = PRIVATE_KEY))
        assertThat(plainText.value as String, `is`(message))
    }

    @Test
    fun testOriginOrUri() {
        assertThat(URI("https://blockstack.org:443/wiki").getOrigin(), `is`("https://blockstack.org:443"))
        assertThat(URI("https://blockstack.org:443").getOrigin(), `is`("https://blockstack.org:443"))
        assertThat(URI("https://blockstack.org").getOrigin(), `is`("https://blockstack.org"))
        assertThat(URI("https://user@blockstack.org").getOrigin(), `is`("https://blockstack.org"))
        assertThat(URI("blockstack://blockstack.id").getOrigin(), `is`("blockstack://blockstack.id"))
    }
}

