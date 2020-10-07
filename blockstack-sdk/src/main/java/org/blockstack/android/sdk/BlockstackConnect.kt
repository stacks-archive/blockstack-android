package org.blockstack.android.sdk

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.withContext
import org.blockstack.android.sdk.model.BlockstackConfig
import org.blockstack.android.sdk.model.UserData
import org.blockstack.android.sdk.ui.ConnectActivity
import java.lang.Exception

@FlowPreview
@ExperimentalCoroutinesApi
object BlockstackConnect {

    private val TAG = BlockstackConnect::class.java.simpleName

    private var blockstackSession: BlockstackSession? = null
    private var blockstackSignIn: BlockstackSignIn? = null

    @StyleRes private var theme : Int? = null


    fun config(blockstackConfig: BlockstackConfig, sessionStore: ISessionStore, appDetails: AppDetails? = null, @StyleRes theme: Int? = null): BlockstackConnect {
        blockstackSession = BlockstackSession(sessionStore, blockstackConfig)
        blockstackSignIn = BlockstackSignIn(sessionStore, blockstackConfig, appDetails)
        this.theme = theme
        return this
    }

    fun connect(context: Context) {
        if (blockstackSignIn == null) {
            throw BlockstackConnectInvalidConfiguration(
                    "Cannot establish connection without a valid configuration"
            )
        }
        context.startActivity(ConnectActivity.getIntent(context, theme))
    }

    suspend fun handleAuthResponse(intent: Intent): Result<UserData> {
        if (blockstackSession == null) {
            throw BlockstackConnectInvalidConfiguration(
                    "Cannot establish connection without a valid configuration"
            )
        }

        val response = intent.data?.query
        return handleAuthResponse(response)
    }

    private suspend fun handleAuthResponse(response: String?): Result<UserData> {
        var result = errorResult
        Log.d(TAG, "response $response")
        if (response != null) {
            val authResponseTokens = response.split('=')

            if (authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]
                Log.d(TAG, "authResponse: $authResponse")
                withContext(Dispatchers.IO) {
                    val userDataResult = blockstackSession?.handlePendingSignIn(authResponse)
                            ?: errorResult
                    result = if (userDataResult.hasValue) {
                        val userData = userDataResult.value!!
                        Log.d(TAG, "signed in!")
                        Result(userData)
                    } else {
                        Result(null, userDataResult.error)
                    }
                }
            }
        }
        return result
    }

    private inline val errorResult: Result<UserData>
        get() = Result(null, ResultError(
                ErrorCode.UnknownError,
                "Unable to process response "))

    suspend fun redirectUserToSignIn(context: AppCompatActivity, sendToSignIn: Boolean) {
        if (blockstackSignIn == null) {
            throw BlockstackConnectInvalidConfiguration(
                    "Cannot establish connection without a valid configuration"
            )
        }
        blockstackSignIn?.redirectUserToSignIn(context, sendToSignIn)
    }

    class BlockstackConnectInvalidConfiguration(message: String) : Exception(message) {}
}

