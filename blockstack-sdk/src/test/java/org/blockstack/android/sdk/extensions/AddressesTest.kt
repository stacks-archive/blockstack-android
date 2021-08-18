package org.blockstack.android.sdk.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.blockstack.android.sdk.model.BlockstackIdentity
import org.junit.Assert
import org.junit.Test
import org.kethereum.bip32.generateChildKey
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed
import org.kethereum.extensions.toHexStringNoPrefix
import org.komputing.kbip44.BIP44Element
import org.komputing.khex.extensions.toHexString
import org.komputing.khex.extensions.toNoPrefixHexString

class AddressesTest {

    private val SEED_PHRASE =
        "float myth tuna chuckle estate recipe canoe equal sport matter zebra vanish pyramid this veteran oppose festival lava economy uniform open zoo shrug fade"
    private val PRIVATE_KEY =
        "9f6da87aa7a214d484517394ca0689a38faa8b3497bb9bf491bd82c31b5af796" //01
    private val PUBLIC_KEY =
        "023064b1fa3c279cd7c8eca2f41c3aa33dc48741819f38b740975af1e8fef61fe4"
    private val BTC_ADDRESS_MAINNET = "1Hu5PUAGWqaokbusF7ZUTpfnejwKbAeGUd"
    private val STX_ADDRESS_MAINNET = "SP2WNPKGHNM1PKE1D95KGADR1X5MWXTJHD8EJ1HHK"

    // Test environment
    private val STX_ADDRESS_TESTNET = "ST2WNPKGHNM1PKE1D95KGADR1X5MWXTJHDAYBBZPG"

    @Test
    fun customStxTest() = runBlocking {
        val keys = generateLegacyWalletKeysFromMnemonicWords(SEED_PHRASE).keyPair

        Assert.assertEquals("SPY80HCNDPWQ8DWH6SVCDBE7E3GCSE97ZGNKE7SD", keys.toStxAddress(true))
    }

    @Test
    fun customDecode(): Unit = runBlocking {
       "SPY80HCNDPWQ8DWH6SVCDBE7E3GCSE97ZGNKE7SD".decodeCrockford32ToByteArray().toNoPrefixHexString()
    }

    @Test
    fun stxAddressMainnetTest() = runBlocking {
        // Arrange
        val keys = generateWalletKeysFromMnemonicWords(SEED_PHRASE)

        // Act / Assert
        Assert.assertEquals(PUBLIC_KEY, keys.keyPair.toHexPublicKey64())
        Assert.assertEquals(PRIVATE_KEY, keys.keyPair.privateKey.key.toHexStringNoPrefix())
        Assert.assertEquals(BTC_ADDRESS_MAINNET, keys.keyPair.toBtcAddress())
        Assert.assertEquals(STX_ADDRESS_MAINNET, "S${keys.keyPair.toStxAddress()}")
        Assert.assertEquals(STX_ADDRESS_MAINNET, keys.keyPair.toStxAddress(true))
    }


    @Test
    fun stxAddressTestnetTest() = runBlocking {
        // Arrange
        val keys = generateWalletKeysFromMnemonicWords(SEED_PHRASE)

        // Act Assert
        Assert.assertEquals(STX_ADDRESS_TESTNET, "S${keys.keyPair.toTestNetStxAddress()}")
        Assert.assertEquals(STX_ADDRESS_TESTNET, keys.keyPair.toTestNetStxAddress(true))
    }

}


private suspend fun generateWalletKeysFromMnemonicWords(seedPhrase: String) = withContext(
    Dispatchers.IO
) {
    val words = MnemonicWords(seedPhrase)
    val stxKeys = BlockstackIdentity(words.toSeed().toKey("m/44'/5757'/0'/0"))
    return@withContext stxKeys.identityKeys.generateChildKey(BIP44Element(false, 0))
}

private suspend fun generateLegacyWalletKeysFromMnemonicWords(seedPhrase: String) = withContext(
    Dispatchers.IO
) {
    val words = MnemonicWords("spray forum chronic innocent exercise market ice pact foster twice glory account")
    val stxKeys = BlockstackIdentity(words.toSeed().toKey("m/888'/0'"))
    return@withContext stxKeys.identityKeys.generateChildKey(BIP44Element(false, 0))
}
