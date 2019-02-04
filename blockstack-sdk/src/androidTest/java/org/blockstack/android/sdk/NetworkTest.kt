package org.blockstack.android.sdk

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import org.blockstack.android.sdk.model.network.AccountStatus
import org.blockstack.android.sdk.model.network.Denomination
import org.blockstack.android.sdk.model.network.NameInfo
import org.blockstack.android.sdk.model.network.NamespaceInfo
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.startsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigInteger
import java.util.concurrent.CountDownLatch

private val username = "dev_android_sdk.id.blockstack"
private val userAddress = "1Akc4hagxfYfDq9suMp1wjjyC5RwxJ7D3H"
private val stackAddress = "SM3KJBA4RZ7Z20KD2HBXNSXVPCR1D3CRAV6Q05MKT"
private val invalidUsername = "a_name_that_is_not_valid_and_does_not_exist.id.blockstack"
private val invalidAddress = "1234567890"

private val TAG = NetworkTest::class.java.simpleName

private val STACKS_TYPE = "STACKS"


@RunWith(AndroidJUnit4::class)
class NetworkTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)
    private lateinit var session: BlockstackSession

    @Before
    fun setup() {
        session = BlockstackSession(rule.activity,
                "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray()),
                sessionStore = sessionStoreforIntegrationTests(rule),
                executor = IntegrationTestExecutor(rule))
    }

    @Test
    fun getNamePriceReturnsCorrectPrice() {
        val latch = CountDownLatch(1)
        var result: Result<Denomination>? = null
        session.network.getNamePrice("id.blockstack") {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value?.json.toString(), `is`("{\"units\":\"BTC\",\"amount\":{\"0\":10240000,\"1\":0,\"t\":1,\"s\":0}}"))
    }

    @Test
    fun getNamePriceReturnsErrorForSubdomainName() {
        val latch = CountDownLatch(1)
        var result: Result<Denomination>? = null
        session.network.getNamePrice(username) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Failed to query name price for dev_android_sdk.id.blockstack"))
    }

    @Test
    fun getNamespacePriceReturnsCorrectPrice() {
        val latch = CountDownLatch(1)
        var result: Result<Denomination>? = null
        session.network.getNamespacePrice("blockstack") {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value?.json.toString(), `is`("{\"units\":\"STACKS\",\"amount\":{\"0\":36020224,\"1\":9,\"t\":2,\"s\":0}}"))
    }

    @Test
    fun getNamespacePriceReturnsErrorForIncorrectNamespace() {
        val latch = CountDownLatch(1)
        var result: Result<Denomination>? = null
        session.network.getNamespacePrice(username) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Failed to query name price for dev_android_sdk.id.blockstack"))
    }

    @Test
    fun getGracePeriodReturnsAValue() {
        val latch = CountDownLatch(1)
        var result: Result<Int>? = null
        session.network.getGracePeriod {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value, `is`(5000))
    }

    @Test
    fun getNamesOwnedReturnsCorrectNames() {
        val latch = CountDownLatch(1)
        var result: Result<List<String>>? = null
        session.network.getNamesOwned("1Akc4hagxfYfDq9suMp1wjjyC5RwxJ7D3H") {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value?.joinToString(), `is`(username))
    }

    @Test
    fun getNamesOwnedReturnsErrorForIncorrectNames() {
        val latch = CountDownLatch(1)
        var result: Result<List<String>>? = null
        session.network.getNamesOwned(invalidAddress) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Non-base58 character"))
    }

    @Test
    fun getNamespaceBurnAddressReturnsCorrectAddress() {

        val latch = CountDownLatch(1)
        var result: Result<String>? = null
        session.network.getNamespaceBurnAddress("id") {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value, `is`("\"1111111111111111111114oLvT2\""))
    }

    @Test
    fun getNamespaceBurnAddressReturnsErrorForIncorrectNamespace() {

        val latch = CountDownLatch(1)
        var result: Result<String>? = null
        session.network.getNamespaceBurnAddress(invalidUsername) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: No such namespace 'a_name_that_is_not_valid_and_does_not_exist.id.blockstack'"))
    }

    @Test
    fun getNameInfoReturnsCorrectInfo() {
        val latch = CountDownLatch(1)
        var result: Result<NameInfo>? = null
        session.network.getNameInfo(username) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value?.json.toString(), `is`("{\"address\":\"1Akc4hagxfYfDq9suMp1wjjyC5RwxJ7D3H\",\"blockchain\":\"bitcoin\",\"did\":\"did:stack:v0:SX3c6YMqh2jrk8wLSno6VdtXqrfNjmF81U-0\",\"last_txid\":\"536e596a4f6bf96656e3db202ea6842eb3f427143e8010cdf388399265ac6568\",\"status\":\"registered_subdomain\",\"zonefile\":\"\$ORIGIN dev_android_sdk.id.blockstack\\n\$TTL 3600\\n_http._tcp\\tIN\\tURI\\t10\\t1\\t\\\"https:\\/\\/gaia.blockstack.org\\/hub\\/1Akc4hagxfYfDq9suMp1wjjyC5RwxJ7D3H\\/profile.json\\\"\\n\\n\",\"zonefile_hash\":\"c146ad093f6152d67233b871f4fa181d98754f9f\"}"))
    }

    @Test
    fun getNameInfoReturnsNoInfoForBadName() {
        val latch = CountDownLatch(1)
        var result: Result<NameInfo>? = null
        session.network.getNameInfo(invalidUsername) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Name not found"))
    }

    @Test
    fun getNamespaceInfoReturnsCorrectInfo() {
        val latch = CountDownLatch(1)
        var result: Result<NamespaceInfo>? = null
        session.network.getNamespaceInfo("blockstack") {
            result = it
            latch.countDown()
        }

        latch.await()
        Log.d("networtest", "info " + result?.error)
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value?.json.toString(), `is`("{\"address\":\"1KSqiHikdbc7NnR5nMNk2omipbbkEbXdD4\",\"base\":4,\"block_number\":524343,\"buckets\":\"[7, 6, 5, 4, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]\",\"coeff\":250,\"history\":{\"524343\":[{\"address\":\"1KSqiHikdbc7NnR5nMNk2omipbbkEbXdD4\",\"block_number\":524343,\"burn_address\":\"1111111111111111111114oLvT2\",\"consensus_hash\":\"bd2d8778b8a5f86be904b5f73d4d2bec\",\"op\":\"*\",\"op_fee\":4000000,\"opcode\":\"NAMESPACE_PREORDER\",\"preorder_hash\":\"1e03292114b6c88118a894e671764a7cd14065c5\",\"sender\":\"76a914ca555365c094561f7763c9eb5959017891c74b3288ac\",\"sender_pubkey\":\"03aa4478a40571ba799ae60d280061d7034fc5e6f33ab849cbb597133fb4f19771\",\"token_fee\":\"0\",\"token_units\":\"BTC\",\"txid\":\"45ba98a2c9c43587338ad83e1f0fd0a2e46821246d293886dc327b52da08600c\",\"vtxindex\":433}],\"524363\":[{\"address\":\"1KSqiHikdbc7NnR5nMNk2omipbbkEbXdD4\",\"base\":4,\"block_number\":524343,\"buckets\":[7,6,5,4,3,2,1,1,1,1,1,1,1,1,1,1],\"coeff\":250,\"lifetime\":4294967295,\"namespace_id\":\"blockstack\",\"no_vowel_discount\":4,\"nonalpha_discount\":4,\"op\":\"&\",\"op_fee\":4000000,\"opcode\":\"NAMESPACE_REVEAL\",\"preorder_hash\":\"1e03292114b6c88118a894e671764a7cd14065c5\",\"ready_block\":0,\"recipient\":\"76a914b2b887ab5a7cef6e7fb9a7ff6a0ebbd186bb80f588ac\",\"recipient_address\":\"1HHzLWhhQuaedRe1Wi8XC4qsyWJ76X5oSR\",\"reveal_block\":524363,\"sender\":\"76a914ca555365c094561f7763c9eb5959017891c74b3288ac\",\"sender_pubkey\":\"03aa4478a40571ba799ae60d280061d7034fc5e6f33ab849cbb597133fb4f19771\",\"token_fee\":\"0\",\"txid\":\"e42f23299c8352c1ba397f955ca3b3a7b819a8a20de02ae7f6f7523ab71e086d\",\"version\":1,\"vtxindex\":589}],\"524393\":[{\"address\":\"1KSqiHikdbc7NnR5nMNk2omipbbkEbXdD4\",\"base\":4,\"block_number\":524343,\"buckets\":[7,6,5,4,3,2,1,1,1,1,1,1,1,1,1,1],\"coeff\":250,\"lifetime\":4294967295,\"namespace_id\":\"blockstack\",\"no_vowel_discount\":4,\"nonalpha_discount\":4,\"op\":\"!\",\"op_fee\":4000000,\"opcode\":\"NAMESPACE_READY\",\"preorder_hash\":\"1e03292114b6c88118a894e671764a7cd14065c5\",\"ready_block\":524393,\"recipient\":\"76a914b2b887ab5a7cef6e7fb9a7ff6a0ebbd186bb80f588ac\",\"recipient_address\":\"1HHzLWhhQuaedRe1Wi8XC4qsyWJ76X5oSR\",\"reveal_block\":524363,\"sender\":\"76a914b2b887ab5a7cef6e7fb9a7ff6a0ebbd186bb80f588ac\",\"sender_pubkey\":\"03aa4478a40571ba799ae60d280061d7034fc5e6f33ab849cbb597133fb4f19771\",\"token_fee\":\"0\",\"txid\":\"05ce49c5a6ea1d2fc9ab0215c46242483f8a927c73e49b3f88d673a72e195578\",\"version\":1,\"vtxindex\":689}]},\"lifetime\":4294967295,\"namespace_id\":\"blockstack\",\"no_vowel_discount\":4,\"nonalpha_discount\":4,\"op\":\"!\",\"op_fee\":4000000,\"preorder_hash\":\"1e03292114b6c88118a894e671764a7cd14065c5\",\"ready\":true,\"ready_block\":524393,\"recipient\":\"76a914b2b887ab5a7cef6e7fb9a7ff6a0ebbd186bb80f588ac\",\"recipient_address\":\"1HHzLWhhQuaedRe1Wi8XC4qsyWJ76X5oSR\",\"reveal_block\":524363,\"sender\":\"76a914b2b887ab5a7cef6e7fb9a7ff6a0ebbd186bb80f588ac\",\"sender_pubkey\":\"03aa4478a40571ba799ae60d280061d7034fc5e6f33ab849cbb597133fb4f19771\",\"token_fee\":\"0\",\"txid\":\"05ce49c5a6ea1d2fc9ab0215c46242483f8a927c73e49b3f88d673a72e195578\",\"version\":1,\"vtxindex\":689}"))
    }


    @Test
    fun getNamespaceInfoReturnsErrorForInvalidNamespace() {
        val latch = CountDownLatch(1)
        var result: Result<NamespaceInfo>? = null
        session.network.getNamespaceInfo("id.blockstack") {
            result = it
            latch.countDown()
        }

        latch.await()
        Log.d("networtest", "info " + result?.error)
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Bad response status: 400"))
    }

    @Test
    fun getNamespaceInfoReturnsNoInfoForBadNamespace() {
        val latch = CountDownLatch(1)
        var result: Result<NamespaceInfo>? = null
        session.network.getNamespaceInfo(invalidUsername) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Namespace not found"))
    }

    /* enable test as soon as core.blockstack.org exposes zonefiles.

    @Test
    fun getZonefileReturnsCorrectContent() {
        val latch = CountDownLatch(1)
        var result: Result<String>? = null
        val hash = "6454d4e7052279480fadca7ebd01a97b7a9ad26f"
        session.network.getZonefile(hash) {
            result = it
            latch.countDown()
        }

        latch.await()
        Log.d("networktest", result?.error)
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value, `is`("kjk"))
    }

    */


    @Test
    fun getZonefileReturnsErrorForHashMismatch() {
        val latch = CountDownLatch(1)
        var result: Result<String>? = null
        val opReturn = "69642b65cb791d68bbafab0fffea7f8a32499c3bd68d67eea0a621bf2b789d1ac97271afdfb8e1" // from https://www.blocktrail.com/BTC/tx/e2029990fa75e9fc642f149dad196ac6b64b9c4a6db254f23a580b7508fc34d7
        val hash = opReturn.substring(19 * 2)
        session.network.getZonefile(hash) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Zone file contents hash to 7825a1f3f844c2936d0e23ef21f17b52ce7565c0, not 3bd68d67eea0a621bf2b789d1ac97271afdfb8e1"))
    }

    @Test
    fun getZonefileReturnsErrorForInvalidHash() {
        val latch = CountDownLatch(1)
        var result: Result<String>? = null
        session.network.getZonefile("invalid hash") {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Bad response status: 404"))
    }

    @Test
    fun getAccountStatusReturnsCorrectStatus() {
        val latch = CountDownLatch(1)
        var result: Result<AccountStatus>? = null
        session.network.getAccountStatus(stackAddress, STACKS_TYPE) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value?.json.toString(), startsWith("{\"address\":\"3NmD8RN7N5ryJumThxa5pq3UUy1LUEfmir\",\"block_id\":"))
    }


    @Test
    fun getAccountStatusReturnsErrorForInvalidAccount() {
        val latch = CountDownLatch(1)
        var result: Result<AccountStatus>? = null
        session.network.getAccountStatus(userAddress, STACKS_TYPE) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Account not found"))
    }


    @Test
    fun getAccountAtReturnsCorrectState() {
        val latch = CountDownLatch(1)
        var result: Result<List<AccountStatus>>? = null
        session.network.getAccountAt(stackAddress, 493493) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value?.joinToString { state -> state.json.toString() }, `is`("{\"address\":\"3NmD8RN7N5ryJumThxa5pq3UUy1LUEfmir\",\"block_id\":373601,\"credit_value\":{\"0\":0,\"t\":0,\"s\":0},\"debit_value\":{\"0\":0,\"t\":0,\"s\":0},\"lock_transfer_block_id\":543805,\"txid\":\"049417c2dbf26d3dc168bcf40bb83b5d7719e49205ee97c4feac42636e3d2cc8\",\"type\":\"STACKS\",\"vtxindex\":0}"))
    }

    @Test
    fun getAccountAtReturnsEmptyStateForBadAddress() {
        val latch = CountDownLatch(1)
        var result: Result<List<AccountStatus>>? = null
        session.network.getAccountAt(userAddress, 488115) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value?.joinToString(), `is`(""))
    }

    @Test
    fun getAccountAtReturnsErrorForInvalidAddress() {
        val latch = CountDownLatch(1)
        var result: Result<List<AccountStatus>>? = null
        session.network.getAccountAt(invalidAddress, 488115) {
            result = it
            latch.countDown()
        }

        latch.await()
        Log.d("networktest", " " + result?.error)
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Bad response status: 400"))
    }

    @Test
    fun getAccountTokensReturnsAllTokens() {
        val latch = CountDownLatch(1)
        var result: Result<List<String>>? = null
        session.network.getAccountTokens(stackAddress) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value?.joinToString(), `is`("STACKS"))
    }

    @Test
    fun getAccountTokensReturnsErrorForInvalidAddress() {
        val latch = CountDownLatch(1)
        var result: Result<List<String>>? = null
        session.network.getAccountTokens(invalidAddress) {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Bad response status: 400"))
    }

    @Test
    fun getAccountBalanceReturnsCorrectBalance() {
        val latch = CountDownLatch(1)
        var result: Result<BigInteger>? = null
        session.network.getAccountBalance(userAddress, STACKS_TYPE) {
            result = it
            latch.countDown()
        }

        latch.await()
        Log.d("networktest", " " + result?.error)
        assertThat(result?.hasValue, `is`(true))
        assertThat(result?.value, `is`(BigInteger.ZERO))
    }

    @Test
    fun getAccountBalanceReturnsErrorForInvalidAddress() {
        val latch = CountDownLatch(1)
        var result: Result<BigInteger>? = null
        session.network.getAccountBalance(invalidAddress, STACKS_TYPE) {
            result = it
            latch.countDown()
        }

        latch.await()
        Log.d("networktest", " " + result?.error)
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Bad response status: 400"))
    }

    @Test
    fun getAccountBalanceReturnsErrorForInvalidTokenType() {
        val latch = CountDownLatch(1)
        var result: Result<BigInteger>? = null
        session.network.getAccountBalance(userAddress, "BTC") {
            result = it
            latch.countDown()
        }

        latch.await()
        assertThat(result?.hasValue, `is`(false))
        assertThat(result?.error, `is`("Error: Bad response status: 400"))
    }
}
