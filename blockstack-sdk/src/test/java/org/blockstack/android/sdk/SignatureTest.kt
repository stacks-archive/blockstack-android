package org.blockstack.android.sdk

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.kethereum.crypto.signMessageHash
import org.kethereum.crypto.toECKeyPair
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.hashes.sha256
import org.kethereum.model.PrivateKey
import org.komputing.khex.extensions.toNoPrefixHexString
import java.math.BigInteger

private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"

class SignatureTest {


    @Test
    fun sign() {
        val msg = "all work and no play makes jack a dull boy"
        val keyPair = PrivateKey(PRIVATE_KEY.hexToBigInteger()).toECKeyPair()
        assertThat(keyPair.toHexPublicKey64(), `is`("027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"))
        val contentHash =msg.toByteArray().sha256()
        assertThat(contentHash.toNoPrefixHexString(),`is`("b2621295ffe7a9c5b522a0e70e902e4f491554b5d0322068e20ba42d02410393"))
        val sigData = signMessageHash(contentHash, keyPair, false)

        assertThat(sigData.s, `is`(BigInteger("51270512107919556030784457855866735147191587478042333754425793328616058206206")))
        assertThat(sigData.r, `is`(BigInteger("16154742971936886808708269743413573628406694544833939252852419405686873402168")))
        assertThat(sigData.v, `is`(BigInteger("28")))

        assertThat(sigData.toDER(), `is`("3044022023b742aff15d1b2ee47b7bb5ff8b1c9e4627201c627f6ae7995f5e00b82e2b380220715a14dc988c549e72cf06660c850c623aa56b9a35e0bb452a8b6410e39137fe"))
    }

}