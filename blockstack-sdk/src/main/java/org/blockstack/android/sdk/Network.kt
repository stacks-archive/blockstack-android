package org.blockstack.android.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.Response
import org.blockstack.android.sdk.model.network.AccountStatus
import org.blockstack.android.sdk.model.network.Denomination
import org.blockstack.android.sdk.model.network.NameInfo
import org.blockstack.android.sdk.model.network.NamespaceInfo
import org.json.JSONArray
import org.json.JSONObject
import org.kethereum.hashes.Sha256
import org.kethereum.hashes.ripemd160
import org.komputing.khex.extensions.toNoPrefixHexString
import java.math.BigInteger

/**
 * Object giving access to information about the blockstack network.
 */
class Network(private val blockstackAPIUrl: String,
              private val callFactory: Call.Factory = OkHttpClient()) {

    /**
     * Get the price to pay for registering a name.
     * @param fullyQualifiedName can be a name or subdomain name.
     * @result result object that contains the denomination (units, amount) of the requested price
     * or error if the request failed.
     */
    suspend fun getNamePrice(fullyQualifiedName: String): Result<Denomination> {
        val response = fetchPrivate("${this.blockstackAPIUrl}/v2/prices/names/${fullyQualifiedName}")
        if (response.isSuccessful) {
            val result = JSONObject(response.body()!!.string())
            val namePrice = result.getJSONObject("name_price")
            val denomination = Denomination(namePrice)
            if ("BTC" == denomination.units) {
                val amount = denomination.amount
                        ?: return Result(null, ResultError(ErrorCode.InvalidAmountError, "missing amount"))
                if (amount < DUST_MINIMUM) {
                    denomination.json.put("amount", DUST_MINIMUM.toString())
                }
            }
            return Result(denomination)
        } else {
            return Result(null, ResultError(ErrorCode.RemoteServiceError, "failed to get name price v2"))
        }
    }


    /**
     * Get the price to pay for registering a namesapce.
     * @param fullyQualifiedName can be a name or subdomain name.
     * @result result object that contains the denomination (units, amount) of the requested price
     * or error if the request failed.
     */
    suspend fun getNamespacePrice(namespaceID: String): Result<Denomination> {
        val response = fetchPrivate("${this.blockstackAPIUrl}/v2/prices/namespaces/${namespaceID}")
        if (response.isSuccessful) {
            val namespacePrice = JSONObject(response.body()!!.string())
            val denomination = Denomination(namespacePrice)
            if ("BTC" == denomination.units) {
                val amount = denomination.amount
                        ?: return Result(null, ResultError(ErrorCode.InvalidAmountError, "missing amount"))
                if (amount < DUST_MINIMUM) {
                    denomination.json.put("amount", DUST_MINIMUM.toString())
                }
            }
            return Result(denomination)
        } else {
            return Result(null, ResultError(ErrorCode.RemoteServiceError, "failed to get namespace price v2"))
        }
    }

    /**
     * Get the number of blocks that can pass between a name expiring and the name being able to be re-registered by a different owner.
     * @result result object that contains the number of blocks or error if the request failed.
     */
    fun getGracePeriod(): Result<Int> {
        return Result(5000)
    }

    private var getNamesOwnedCallback: ((Result<List<String>>) -> Unit)? = null

    /**
     * Get the names -- both on-chain and off-chain -- owned by an address.
     * @param address the blockchain address (the hash of the owner public key)
     * @param callback called with a result object that contains the list of names or error if the request failed.
     */
    suspend fun getNamesOwned(address: String): Result<List<String>> {
        val networkAddress = this.coerceAddress(address)
        val response = fetchPrivate("${this.blockstackAPIUrl}/v1/addresses/bitcoin/${networkAddress}")
        val result = JSONObject(response.body()!!.string())
        return Result(result.getJSONArray("names").toStringList())

    }

    /**
     * Get the blockchain address to which a name's registration fee must be sent (the address will depend on the namespace in which it is registered.).
     * @param namespace the namespace ID
     * @result result object that contains the address as string or error if the request failed.
     */
    suspend fun getNamespaceBurnAddress(namespace: String): Result<String> {

        val response = fetchPrivate("${this.blockstackAPIUrl}/v1/namespaces/${namespace}")
        val blockHeight = this.getBlockHeight()


        val namespaceInfo = if (response.code() == 404) {
            return Result(null, ResultError(ErrorCode.UnknownError, "No such namespace '${namespace}'"))
        } else {
            JSONObject(response.body()!!.string())
        }

        var address = this.getDefaultBurnAddress()
        if (namespaceInfo.getInt("version") == 2) {
            // pay-to-namespace-creator if this namespace is less than 1 year old
            if (namespaceInfo.getInt("reveal_block") + 52595 >= blockHeight) {
                address = namespaceInfo.getString("address")
            }
        }
        return Result(coerceAddress(address))
    }

    private fun getDefaultBurnAddress(): String {
        return this.coerceAddress("1111111111111111111114oLvT2")

    }

    private fun getBlockHeight(): Int {
        throw NotImplementedError("getBlockHeight")
    }

    /**
     * Get WHOIS-like information for a name, including the address that owns it, the block at which it expires, and the zone file anchored to it (if available).
     * @param fullyQualifiedName the name to query. Can be on-chain of off-chain.
     * @result result object that contains the WHOIS-like name information or error if the request failed.
     */
    suspend fun getNameInfo(fullyQualifiedName: String): Result<NameInfo> {
        val response = fetchPrivate("${this.blockstackAPIUrl}/v1/names/${fullyQualifiedName}")

        var nameInfo = if (response.code() == 404) {
            throw Error("Name not found")
        } else if (response.code() != 200) {
            throw Error("Bad response status: ${response.code()}")
        } else {
            response.json()
        }
        val address = nameInfo.optStringOrNull("address")
        if (address != null) {
            nameInfo.put("address", coerceAddress(address))
        }
        return Result(NameInfo(nameInfo))
    }

    /**
     *Get the pricing parameters and creation history of a namespace.
     * @param namespaceId the namespace to query
     * @result result object that contains the namespace information or error if the request failed.
     */
    suspend fun getNamespaceInfo(namespaceId: String): Result<NamespaceInfo> {
        val resp = fetchPrivate("${this.blockstackAPIUrl}/v1/namespaces/${namespaceId}")
        val nameSpaceInfo = if (resp.code() == 404) {
            throw  Error("Namespace not found")
        } else if (resp.code() != 200) {
            throw  Error("Bad response status: ${resp.code()}")
        } else {
            resp.json()
        }
        val address = nameSpaceInfo.optStringOrNull("address")
        val recipientAddress = nameSpaceInfo.optStringOrNull("recipient_address")
        if (address != null && recipientAddress != null) {
            nameSpaceInfo.put("address", coerceAddress(address))
            nameSpaceInfo.put("recipient_address", coerceAddress(recipientAddress))
        }
        return Result(NamespaceInfo(nameSpaceInfo))
    }

    /**
     * Get a zone file, given its hash.
     * @param zonefileHash the ripemd160(sha256) hash of the zone file.
     * @param callback called with a result object that contains the zone file's text
     * or error if the request failed or the zone file obtained does not match the hash.
     */
    suspend fun getZonefile(zonefileHash: String): Result<String> {
        val resp = fetchPrivate("${this.blockstackAPIUrl}/v1/zonefiles/${zonefileHash}")
        if (resp.code() == 200) {
            val body = resp.body()!!.string()

            val sha256 = Sha256.digest(body.toByteArray())
            val h = sha256.ripemd160().toNoPrefixHexString()
            if (h !== zonefileHash) {
                return Result(null, ResultError(ErrorCode.UnknownError, "Zone file contents hash to ${h}, not ${zonefileHash}"))
            }
            return Result(body)
        } else {
            throw Error("Bad response status: ${resp.code()}")
        }

    }


    /**
     * Get the status of an account for a particular token holding. This includes its total number of expenditures and credits, lockup times, last txid, and so on.
     * @param address  the account's address
     * @param tokenType the token type to query
     * @param callback called with a result object that contains the state of the account for this token
     * or error if the request failed
     */
    suspend fun getAccountStatus(address: String, tokenType: String): Result<AccountStatus> {
        val resp = fetchPrivate("${this.blockstackAPIUrl}/v1/accounts/${address}/${tokenType}/status")

        val accountStatus = if (resp.code() == 404) {
            throw  Error("Account not found")
        } else if (resp.code() != 200) {
            throw  Error("Bad response status: ${resp.code()}")
        } else {
            resp.json()
        }
        // coerce all addresses, and convert credit/debit to biginteger
        val address = coerceAddress(accountStatus.getString("address"))
        val debitValue = BigInteger(accountStatus.getString("debit_value"))
        val creditValue = BigInteger(accountStatus.getString("credit_value"))
        accountStatus.put("address", address)
                .put("debit_value", debitValue)
                .put("credit_value", creditValue)
        return Result(AccountStatus(accountStatus))
    }

    private var getAccountHistoryPageCallback: ((Result<List<AccountStatus>>) -> Unit)? = null

    /**
     * Get a page of an account's transaction history.
     * @param address the account's address
     * @param page the page number. Page 0 contains the most recent transactions
     * @result result object that contains a list of account statuses at various block heights (e.g. prior balances, txids, etc)
     */
    suspend fun getAccountHistoryPage(address: String, page: Int, callback: (Result<List<AccountStatus>>) -> Unit): Result<List<AccountStatus>> {
        val resp = fetchPrivate("${this.blockstackAPIUrl}/v1/accounts/${address}/history?page=${page}")

        val historyList = if (resp.code() == 404) {
            throw  Error("Account not found")
        } else if (resp.code() != 200) {
            throw  Error("Bad response status: ${resp.code()}")
        } else {
            val jsonString = resp.body()!!.string()
            if (jsonString.startsWith("{")) {
                val error = JSONObject(jsonString)
                val historyListError = error.optStringOrNull("error")
                return Result(null, ResultError(ErrorCode.UnknownError, "Unable to get account history page: ${historyListError}"))
            } else {
                JSONArray(jsonString)
            }
        }

        val result = arrayListOf<AccountStatus>()
        for (
        arrayIndex in 0 until historyList.length()) {
            val histEntry = historyList.getJSONObject(arrayIndex)
            val addr = coerceAddress(histEntry.getString("address"))
            histEntry.put("address", addr)
            result.add(AccountStatus(histEntry))
        }
        return Result(result)
    }

    /**
     * Get the state(s) of an account at a particular block height. This includes the state of the account
     * beginning with this block's transactions,
     * as well as all of the states the account passed through when this block was processed (if any).
     * @param address the accounts's address.
     * @param blockHeight the block to query.
     * @result result object that contains the account states of the account at this block
     * or error if the request failed
     */
    suspend fun getAccountAt(address: String, blockHeight: Int): Result<ArrayList<AccountStatus>> {
        val resp = fetchPrivate("${this.blockstackAPIUrl}/v1/accounts/${address}/history/${blockHeight}")

        val historyList = if (resp.code() == 404) {
            throw  Error("Account not found")
        } else if (resp.code() != 200) {
            throw  Error("Bad response status: ${resp.code()}")
        } else {
            val jsonString = resp.body()!!.string()
            if (jsonString.startsWith("{")) {
                val error = JSONObject(jsonString)
                val historyListError = error.optStringOrNull("error")
                return Result(null, ResultError(ErrorCode.UnknownError, "Unable to get historical account state: ${historyListError}"))
            } else {
                JSONArray(jsonString)
            }
        }

        val result = arrayListOf<AccountStatus>()
        for (
        arrayIndex in 0 until historyList.length()) {
            val histEntry = historyList.getJSONObject(arrayIndex)
            val addr = coerceAddress(histEntry.getString("address"))
            histEntry.put("address", addr)
            result.add(AccountStatus(histEntry))
        }
        return Result(result)
    }

    /**
     * Get the set of token types that this account owns
     * @param address the accounts's address
     * @result result object that contains the list of types of token this account holds (excluding the underlying blockchain's tokens)
     * or error if the request failed
     */
    suspend fun getAccountTokens(address: String): Result<ArrayList<String>> {
        val resp = fetchPrivate("${this.blockstackAPIUrl}/v1/accounts/${address}/tokens")

        val tokenList = if (resp.code() == 404) {
            throw  Error("Account not found")
        } else if (resp.code() != 200) {
            throw Error("Bad response status: ${resp.code()}")
        } else {
            val jsonString = resp.body()!!.string()
            if (jsonString.startsWith("{")) {
                val error = JSONObject(jsonString)
                val tokenListError = error.optStringOrNull("error")
                return Result(null, ResultError(ErrorCode.UnknownError, "Unable to get token list: ${tokenListError}"))
            } else {
                JSONArray(jsonString)
            }
        }

        val result = arrayListOf<String>()
        for (
        arrayIndex in 0 until tokenList.length()) {
            val token = tokenList.getString(arrayIndex)
            result.add(token)
        }
        return Result(result)
    }


    /**
     * Get the number of tokens owned by an account. If the account does not exist or has no tokens of this type, then 0 will be returned.
     * @param address the account's address.
     * @param tokenType the type of token to query.
     * @param callback called with a result object that contains the number of tokens held by this account in
     * the smallest denomination.
     */
    suspend fun getAccountBalance(address: String, tokenType: String): Result<BigInteger> {
        val resp = fetchPrivate("${this.blockstackAPIUrl}/v1/accounts/${address}/${tokenType}/balance")
        val tokenBalance = if (resp.code() == 404) {
            // talking to an older blockstack core node without the accounts API
            return Result(BigInteger("0"))
        } else if (resp.code() != 200) {
            throw  Error("Bad response status: ${resp.code()}")
        } else {
            val jsonString = resp.body()!!.string()
            if (jsonString.startsWith("{")) {
                val error = JSONObject(jsonString)
                val balanceError = error.optStringOrNull("error")
                return Result(null, ResultError(ErrorCode.UnknownError, "Unable to get account balance: ${balanceError}"))
            } else {
                JSONObject(jsonString)
            }
        }

        val balance = tokenBalance.optString("balance") ?: "0"
        return Result(BigInteger(balance))
    }


    private suspend fun fetchPrivate(url: String): Response {
        val request = Builder().url(url)
                .addHeader("Referrer-Policy", "no-referrer")
                .build()
        return withContext(Dispatchers.IO) {
            callFactory.newCall(request).execute()
        }
    }

    fun coerceAddress(address: String): String {
        // TODO coerce to network address
        return address
    }


    companion object {
        private val DUST_MINIMUM: BigInteger = BigInteger("5500")
    }

}


private fun Response.json(): JSONObject {
    return this.body()!!.string().let {
        JSONObject(it)
    }
}

private fun JSONArray.toStringList(): List<String> {
    val result = arrayListOf<String>()
    var i = 0
    val size = this.length()
    while (i < size) {
        result.add(this.getString(i))
        i++
    }
    return result
}
