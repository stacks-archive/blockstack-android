package org.blockstack.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.colendi.ecies.EncryptedResultForm
import com.colendi.ecies.Encryption
import com.colendi.ecies.Encryption.getHMAC
import me.uport.sdk.core.hexToByteArray
import org.blockstack.android.sdk.model.CipherObject
import org.blockstack.android.sdk.model.CryptoOptions
import org.blockstack.android.sdk.test.TestActivity
import org.bouncycastle.util.encoders.Hex
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.komputing.khex.extensions.hexToByteArray
import java.security.MessageDigest

private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"

@RunWith(AndroidJUnit4::class)
class EncryptionColendiLibTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    @Test
    fun testEncryptDecryptWorks() {
        val encryption = Encryption()

        val privateKey = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
        val publicKey = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
        val message = "Colendi"


        val encryptedResult = encryption.encryptWithPublicKey(message.toByteArray(), publicKey)

        val formData = EncryptedResultForm()

        formData.privateKey = privateKey
        formData.ciphertext = encryptedResult.ciphertext
        formData.ephemPublicKey = encryptedResult.ephemPublicKey
        formData.iv = encryptedResult.iv
        formData.mac = encryptedResult.mac

        val result = String(encryption.decryptWithPrivateKey(formData))
        assert(result == message)
    }

    @Test
    fun testEncryptColendiDescryptBlockstackJS() {
        val encryption = Encryption()

        val privateKey = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
        val publicKey = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
        val message = "Colendi"


        val encryptedResult = encryption.encryptWithPublicKey(message.toByteArray(), publicKey)

        val result = Blockstack()
                .decryptContent(CipherObject(encryptedResult.iv, encryptedResult.ephemPublicKey, encryptedResult.ciphertext, encryptedResult.mac, true).json.toString(), false,
                        CryptoOptions(publicKey, privateKey))
        assertThat(result.value as String, `is`(message))

    }


    @Test
    fun testEncryptBlockstackJSDescryptColendi() {
        val encryption = Encryption()

        val privateKey = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
        val publicKey = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
        val message = "all work and no play makes jack a dull boy"


        val cipherObject = JSONObject("{cipherText : \"c77fded9b2013ed08409b5f6a69f53e78d4ef0ec1cca6380d6b0aa8bd927c454135dd1a5c54adc0f3e0aa9748fec3fb5\",\n" +
                "    ephemeralPK: \"02df2bc402b134631b2afaa31316392e3ded63728cd588e4f8bc152b39f8a6deb4\",\n" +
                "    iv: \"f0f56df1978d5c5d65e8e5b3ff8ad1fc\",\n" +
                "    mac: \"4210125fb1c7f9c47a8e4ab7995980e4eeebbccbd6fe888beec809062e1b33da\",\n" +
                "    wasString: true\n" +
                "    }")

        val encryptedResult = CipherObject(cipherObject)

        val result = encryption.decryptWithPrivateKey(EncryptedResultForm(encryptedResult.ephemeralPK, encryptedResult.iv, encryptedResult.mac, encryptedResult.cipherText, privateKey))

        assertThat(String(result), `is`(message))

    }

    @Test
    fun testDecryptAes256CDReturnsCorrectValue() {
        val publicKey = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"

        val cipher = "c5a777b2daac1c7f50f2007af02f517d"
        val plainText = Encryption.decryptAES256CBC(cipher.hexToByteArray(), publicKey.slice(0..63), byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6))
        assertThat(String(plainText), `is`("abc"))

    }

    @Test
    fun testSharedSecretToKeysReturnCorrectValues() {
        val mda = MessageDigest.getInstance("SHA-512")

        val sharedSecret = Hex.decode("dd585e51548fea14df7114ea366ffd1372abdf8cf6c771da2ff0285522951001")
        val keys = Encryption.sharedSecretToKeys(mda, sharedSecret)
        assertThat(keys.encKeyAES, `is`("a32fb7bbf65d0a6f6c2d05c49cc5e477b8616b517562f1494f16a106190c74e4"))
        assertThat(keys.macKey, `is`("dcbb3058a9fb295b181f483dc32962949551e648125f351eafdffd053ed2761f"))

    }

    @Test
    fun testHmacReturnsCorrectValue() {
        val key = Hex.decode("dcbb3058a9fb295b181f483dc32962949551e648125f351eafdffd053ed2761f")
        val content = Hex.decode("f0f56df1978d5c5d65e8e5b3ff8ad1fc02df2bc402b134631b2afaa31316392e3ded63728cd588e4f8bc152b39f8a6deb4c77fded9b2013ed08409b5f6a69f53e78d4ef0ec1cca6380d6b0aa8bd927c454135dd1a5c54adc0f3e0aa9748fec3fb5")
        val hmac = getHMAC(key, content)
        assertThat(String(Hex.encode(hmac)), `is`("4210125fb1c7f9c47a8e4ab7995980e4eeebbccbd6fe888beec809062e1b33da"))

    }

    companion object {
        val TAG = EncryptionColendiLibTest.javaClass.simpleName
    }
}
