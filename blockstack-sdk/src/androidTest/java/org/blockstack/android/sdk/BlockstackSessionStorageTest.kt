package org.blockstack.android.sdk

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import org.blockstack.android.sdk.model.*
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.net.URL
import java.util.concurrent.CountDownLatch


private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
private val DECENTRALIZED_ID = "did:btc-addr:1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"
private val BITCOIN_ADDRESS = "1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"


@RunWith(AndroidJUnit4::class)
class BlockstackSessionStorageTest {
    private lateinit var blockstack: Blockstack
    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var session: BlockstackSession

    @Before
    fun setup() {
        blockstack = Blockstack()
        session = BlockstackSession(
                appConfig = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray()),
                sessionStore = sessionStoreforIntegrationTests(rule),
                blockstack = blockstack)
        val gaiaHubConfig = runBlocking {
            session.getOrSetLocalGaiaHubConnection()
        }
        Log.d(BlockstackSessionStorageTest::class.java.simpleName, gaiaHubConfig.toString())
    }


    @Test
    fun testPutStringFileTwice() {
        var result1: String? = null
        var result2: String? = null
        runBlocking {

            if (session.isUserSignedIn()) {
                var it = session.putFile("testPutStringFileTwice.txt", "Hello Test", PutFileOptions(false))
                if (it.value is String) {
                    result1 = it.value as String
                }

                it = session.putFile("testPutStringFileTwice.txt", "Hello Test2", PutFileOptions(false))
                if (it.value != null) {
                    result2 = it.value as String
                }


            }
        }
        assertThat(result1, `is`(notNullValue()))
        assertThat(result2, `is`(notNullValue()))
    }

    @Test
    fun testEncryptDecryptString() {
        assertTrue(session.isUserSignedIn())
        val options = CryptoOptions(publicKey = PUBLIC_KEY, privateKey = PRIVATE_KEY)
        val plainContent = "hello from test"
        val encResult = blockstack.encryptContent(plainContent, options = options)
        assertTrue(encResult.hasValue)
        val decResult = blockstack.decryptContent(encResult.value!!.json.toString(), false, options)
        assertThat(decResult.value as String, `is`(plainContent))
    }

    @Test
    fun testEncryptDecryptBinary() {
        assertTrue(session.isUserSignedIn())
        val binaryContent = getImageBytes()
        val options = CryptoOptions(publicKey = PUBLIC_KEY, privateKey = PRIVATE_KEY)
        val encResult = blockstack.encryptContent(binaryContent, options = options)
        assertTrue(encResult.hasValue)
        val decResult = blockstack.decryptContent(encResult.value!!.json.toString(), true, options)
        assertThat((decResult.value as ByteArray).size, `is`(binaryContent.size))
    }

    @Test
    fun testGetFileFor404File() {
        var result: Result<out Any>? = null
        val latch = CountDownLatch(1)

        runBlocking {
            if (session.isUserSignedIn()) {
                result = session.getFile("404file.txt", GetFileOptions(false))
            }
        }
        assertThat(result, `is`(notNullValue()))
        assertThat(result?.value, `is`(nullValue()))
        assertThat(result?.error?.message, `is`("Error when loading from Gaia hub, status:404"))
    }

    @Test
    fun testPutGetStringFile() {
        var result: String? = null

        runBlocking {
            if (session.isUserSignedIn()) {
                val urlResult = session.putFile("testPutGetStringFile.txt", "Hello Test", PutFileOptions(false))
                assertThat(urlResult.value, `is`("https://gaia.blockstack.org/hub/19Usb4TCn8mWhjvLxmC2eEiXC9xJfwRkAy/testPutGetStringFile.txt"))
                val it = session.getFile("testPutGetStringFile.txt", GetFileOptions(false))
                if (it.value is String) {
                    result = it.value as String
                }
            }

        }
        assertThat(result, `is`("Hello Test"))
    }

    @Test
    fun testPutGetStringFileWithContentType() {
        var result: Any? = null

        runBlocking {
            if (session.isUserSignedIn()) {
                val it = session.putFile("testPutGetStringFileWithContentType.txt", "Hello Test", PutFileOptions(false, contentType = "application/x.foo"))
                val u = URL(it.value).openConnection()
                u.connect()
                result = u.contentType
            }
        }
        assertThat(result as String, `is`("application/x.foo"))
    }

    @Test
    fun testPutGetEncryptedStringFile() {
        var result: String? = null

        runBlocking {

            if (session.isUserSignedIn()) {
                session.putFile("testPutGetEncryptedStringFile.txt", "Hello Test", PutFileOptions(true))


                val it = session.getFile("testPutGetEncryptedStringFile.txt", GetFileOptions(true))
                if (it.value is String) {
                    result = it.value as String
                }
            }

        }


        assertThat(result, `is`("Hello Test"))
    }

    @Test
    fun testPutGetBinaryFile() {
        val bitMapData = getImageBytes()

        var result: ByteArray? = null

        val latch = CountDownLatch(1)

        runBlocking {

            if (session.isUserSignedIn()) {
                session.putFile("testPutGetBinaryFile.txt", bitMapData, PutFileOptions(false))

                val it = session.getFile("testPutGetBinaryFile.txt", GetFileOptions(false))
                if (it.value is ByteArray) {
                    result = it.value as ByteArray
                }
            }
        }
        assertThat(result?.size, `is`(bitMapData.size))
    }

    @Test
    fun testPutGetEncryptedBinaryFile() {
        val bitMapData = getImageBytes()

        var result: ByteArray? = null

        runBlocking {

            if (session.isUserSignedIn()) {
                session.putFile("testPutGetEncryptedBinaryFile.txt", bitMapData, PutFileOptions(true))

                val it = session.getFile("testPutGetEncryptedBinaryFile.txt", GetFileOptions(true))
                if (it.value is ByteArray) {
                    result = it.value as ByteArray
                }
            }
        }

        assertThat(result?.size, `is`(bitMapData.size))
    }

    @Test
    fun testPutGetFileSigned() {
        var result: String? = null
        runBlocking {
            if (session.isUserSignedIn()) {
                session.putFile("testPutGetFileSigned.txt", "all work and no play makes jack a dull boy", PutFileOptions(false, sign = true))

                val it = session.getFile("testPutGetFileSigned.txt", GetFileOptions(false, verify = true))
                if (!it.hasErrors) {
                    Log.d("blockstack test", it.value as String)
                    result = it.value as String
                } else {
                    result = it.error!!.message
                }
            }
        }

        assertThat(result, `is`("all work and no play makes jack a dull boy"))
    }

    @Test
    fun testPutGetFileMissingSignature() {
        var result: String? = null

        runBlocking {
            if (session.isUserSignedIn()) {
                session.putFile("testPutGetFileMissingSignature.txt", "Hello Test", PutFileOptions(false, sign = true))
                val it = session.deleteFile("testPutGetFileMissingSignature.txt.sig", DeleteFileOptions())
                if (!it.hasErrors) {

                    val it2 = session.getFile("testPutGetFileMissingSignature.txt", GetFileOptions(false, verify = true))

                    if (!it2.hasErrors) {
                        result = it2.value as String
                    } else {
                        result = it2.error!!.message
                    }
                } else {
                    result = "error while deleting signature ${it.error}"
                }
            }
        }


        assertThat(result, startsWith("Failed to verify signature: Failed to obtain signature for file: testPutGetFileMissingSignature.txt"))
    }

    @Test
    fun testPutGetFileSignedEncrypted() {
        var result: JSONObject? = null
        runBlocking {
            if (session.isUserSignedIn()) {
                session.putFile("testPutGetFileSignedEncrypted.txt", "Hello Test", PutFileOptions(true, sign = true))
                val it = session.getFile("testPutGetFileSignedEncrypted.txt", GetFileOptions(decrypt = false))
                if (!it.hasErrors) {
                    result = JSONObject(it.value as String)
                }
            }
        }

        assertThat(result, `is`(notNullValue()))
        assertThat(result!!.getString("signature"), `is`(notNullValue()))
    }

    @Test
    fun testPutGetFileInvalidSigned() {
        var result: String? = null

        val invalidSignedEncryptedText = "{\"signature\":\"INVALID_SIGNATURE\",\"publicKey\":\"024634ee1d4ff57f2e0ec7a847e1705ec562949f84a83d1f5fdb5956220a9775e0\",\"cipherText\":\"{\\\"iv\\\":\\\"329acaeffe36e8ae58365b56b31af640\\\",\\\"ephemeralPK\\\":\\\"0333fde58c40196efa696dde93fb615e8e960bf52d78ab883d67fb56d4b9a10c5a\\\",\\\"cipherText\\\":\\\"143df68fd1542b29febe2b0843e723af\\\",\\\"mac\\\":\\\"68c3e439c26a2be400aeb278ed7061a8802b0366bf79a1d64a7a6e10e4710047\\\",\\\"wasString\\\":true}\"}"
        if (session.isUserSignedIn()) {
            runBlocking {
                session.putFile("testPutGetFileInvalidSigned.txt", invalidSignedEncryptedText, PutFileOptions(false))
                val it = session.getFile("testPutGetFileInvalidSigned.txt", GetFileOptions(true, verify = true))
                if (it.hasErrors) {
                    result = it.error!!.message
                }
            }
        }
        assertThat(result, `is`("hex-string must have an even number of digits (nibbles)"))
    }


    @Test
    fun getUserAppFileUrlReturnsCorrectUrl() {
        val url = runBlocking {
            blockstack.getUserAppFileUrl("message.txt", "dev_android_sdk.id.blockstack", "https://flamboyant-darwin-d11c17.netlify.com", null)
        }
        assertThat(url, `is`("https://gaia.blockstack.org/hub/1CroEcMCtJUuvCLyjeqNToRURB8DjH2Uo2/message.txt"))

    }

    @Test
    fun getUserAppFileUrlReturns_NO_URL_forNonPublicFile() {
        val url = runBlocking {
            blockstack.getUserAppFileUrl("non_public_file.txt", "friedger.id", "https://blockstack-todos.appartisan.com/", null)
        }
        assertThat(url, `is`("NO_URL"))

    }

    @Test
    fun getAppBucketUrlReturnsUrl() {
        val url = runBlocking {
            blockstack.getAppBucketUrl("https://hub.blockstack.org", PRIVATE_KEY)
        }

        assertThat(url, `is`("https://gaia.blockstack.org/hub/1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U/"))
    }

    @Test
    fun getGaiaAddressReturnsCorrectAddress() {
        val address = runBlocking {
            session.getGaiaAddress("https://www.stealthy.im", "muneeb.id")
        }
        assertThat(address, `is`("1KJmwBRzF4A8CaMbAh2EjUwG3BhHiSfTAM"))
    }


    @Test
    fun testListFilesWithAtLeastOneFile() {
        var fileCount: Result<Int>? = null
        runBlocking {
            session.putFile("testListFilesWithAtLeastOneFile.text", "Hello Test", PutFileOptions())
            fileCount =
                    session.listFiles {
                        true
                    }

        }

        assertThat(fileCount?.value, `is`(Matchers.greaterThanOrEqualTo(1)))
    }


    @Test
    fun listFilesReturnsCorrectNumberOfFiles() {
        var count = 0
        val countResult = runBlocking {
            session.listFiles {
                count++
                true
            }
        }
        assertThat(countResult.value, `is`(count))
    }

    @Test
    fun listFilesCanHandleErrorInCallback() {
        val countResult = runBlocking {
            session.listFiles {
                throw RuntimeException("I want to make the API crash!")
            }
        }
        assertThat(countResult.error?.message, `is`("I want to make the API crash!"))
    }

    @Test
    fun testGetFileUrlForEncryptedFile() {
        var urlResult: Result<String>? = null
        runBlocking {
            if (session.isUserSignedIn()) {
                session.putFile("testGetFileUrlForEncryptedFile.txt", "Hello Test", PutFileOptions(true))
                urlResult = session.getFileUrl("testGetFileUrlForEncryptedFile.txt", GetFileOptions(true))

            }
        }

        assertThat(urlResult?.value, notNullValue())
        assertThat(URL(urlResult!!.value).readText(), startsWith("{\"iv\":"))
    }

    @Test
    fun testGetFileUrlForUnencryptedFile() {
        var urlResult: Result<String>? = null
        runBlocking {
            if (session.isUserSignedIn()) {
                session.putFile("testGetFileUrlForUnencryptedFile.txt", "Hello Test", PutFileOptions(false))
                urlResult = session.getFileUrl("testGetFileUrlForUnencryptedFile.txt", GetFileOptions(false))

            }
        }
        assertThat(urlResult?.value, notNullValue())
        assertThat(URL(urlResult!!.value).readText(), `is`("Hello Test"))
    }

    @Test
    fun testGetFileUrlFor404File() {
        val result = kotlin.runCatching {
            val urlResult = runBlocking {
                session.getFileUrl("404file.txt", GetFileOptions(false))
            }

            assertThat("bad urlResult ${urlResult.error}", urlResult.value, notNullValue())
            try {
                URL(urlResult.value).readText()
                fail("Should throw FileNotFoundException")
            } catch (e: FileNotFoundException) {
                // success
            }
        }
        assertThat(result.exceptionOrNull(), `is`(nullValue()))
    }

    @Test
    fun testDeleteFile() {
        var result: Result<out Any>? = null

        runBlocking {
            if (session.isUserSignedIn()) {
                session.putFile("testDeleteFile.txt", "Hello Test", PutFileOptions(false))
                session.deleteFile("testDeleteFile.txt")

                result = session.getFile("testDeleteFile.txt", GetFileOptions(false))
            }
        }


        assertThat(result, `is`(notNullValue()))
        assertThat(result?.value, `is`(nullValue()))
        assertThat(result?.error?.message, `is`("Error when loading from Gaia hub, status:404"))
    }

    private fun getImageBytes(): ByteArray {
        val drawable: BitmapDrawable = rule.activity.resources.getDrawable(org.blockstack.android.sdk.test.R.drawable.blockstackteam) as BitmapDrawable

        val bitmap = drawable.bitmap
        val stream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bitMapData = stream.toByteArray()
        return bitMapData
    }
}