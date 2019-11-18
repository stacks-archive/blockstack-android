package org.blockstack.android.sdk

import org.blockstack.android.sdk.ecies.fromDER
import org.blockstack.android.sdk.ecies.signContent
import org.blockstack.android.sdk.ecies.toDER
import org.blockstack.android.sdk.ecies.verify
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.kethereum.crypto.signMessageHash
import org.kethereum.crypto.toECKeyPair
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.extensions.toHexStringNoPrefix
import org.kethereum.hashes.sha256
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey
import org.kethereum.model.PublicKey
import org.komputing.khex.extensions.toNoPrefixHexString
import java.math.BigInteger

private const val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private const val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"

class SignatureTest {

    private val msg = "all work and no play makes jack a dull boy"
    private val keyPair = PrivateKey(PRIVATE_KEY.hexToBigInteger()).toECKeyPair()
    private val contentHash = msg.toByteArray().sha256()

    @Test
    fun testData() {
        assertThat(keyPair.toHexPublicKey64(), `is`("027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"))
        assertThat(contentHash.toNoPrefixHexString(), `is`("b2621295ffe7a9c5b522a0e70e902e4f491554b5d0322068e20ba42d02410393"))
    }

    @Test
    fun testSignatureToDERReturnsCorrectValue() {
        val sigData = signMessageHash(contentHash, keyPair, false)

        assertThat(sigData.s, `is`(BigInteger("51270512107919556030784457855866735147191587478042333754425793328616058206206")))
        assertThat(sigData.r, `is`(BigInteger("16154742971936886808708269743413573628406694544833939252852419405686873402168")))
        assertThat(sigData.v, `is`(BigInteger("28")))

        assertThat(sigData.toDER(), `is`("3044022023b742aff15d1b2ee47b7bb5ff8b1c9e4627201c627f6ae7995f5e00b82e2b380220715a14dc988c549e72cf06660c850c623aa56b9a35e0bb452a8b6410e39137fe"))
    }

    @Test
    fun testSignatureFromDERReturnsCorrectValues() {

        val sigData = signMessageHash(contentHash, keyPair, false).toDER().fromDER()
        assertThat(sigData.s, `is`(BigInteger("51270512107919556030784457855866735147191587478042333754425793328616058206206")))
        assertThat(sigData.r, `is`(BigInteger("16154742971936886808708269743413573628406694544833939252852419405686873402168")))
        assertThat(sigData.v, `is`(BigInteger("0")))
    }

    @Test
    fun testVerifyWorks() {
        val signature = signMessageHash(contentHash, keyPair, false).toDER()
        val isValid = ECKeyPair(PrivateKey(0.toBigInteger()), PublicKey(PUBLIC_KEY)).verify(contentHash, signature)
        assertThat(isValid, `is`(true))
    }

    @Test
    fun testVerifyFails() {
        val signature = signMessageHash(contentHash, keyPair, false).toDER()
        val keyPair2 = ECKeyPair(PrivateKey(0.toBigInteger()), PublicKey("02413d7c51118104cfe1b41e540b6c2acaaf91f1e2e22316df7448fb6070d582ec"))
        val isValid = keyPair2.verify(contentHash, signature)
        assertThat(isValid, `is`(false))
    }

    @Test
    fun testSignContent() {
        val signedContent = signContent(msg, PRIVATE_KEY)
        assertThat(signedContent.signature, `is`("3044022023b742aff15d1b2ee47b7bb5ff8b1c9e4627201c627f6ae7995f5e00b82e2b380220715a14dc988c549e72cf06660c850c623aa56b9a35e0bb452a8b6410e39137fe"))

        val isValid = keyPair.verify(contentHash, signedContent.signature)
        assertThat(isValid, `is`(true))
    }

    @Test
    fun testSignShortContent() {
        val privateKey = "89f92476f13f5b173e53926ad7d6e22baf78c6b1dcdf200c38dc73d2bf47d43b"
        val message = "all work and no play makes jack a dull boy" // TODO use "Hello Test"
        val signedContent = signContent(message, privateKey)
        val keyPair = ECKeyPair(PrivateKey(0.toBigInteger()), PublicKey(signedContent.publicKey))
        val isValid = keyPair.verify(message.toByteArray().sha256(), signedContent.signature)
        assertThat(isValid, `is`(true))
    }
}