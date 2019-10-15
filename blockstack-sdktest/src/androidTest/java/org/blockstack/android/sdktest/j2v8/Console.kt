package org.blockstack.android.sdktest.j2v8

import android.util.Log
import com.eclipsesource.v8.V8Object


interface Console {
    fun error(msg: Any)
    fun warn(msg: String)
    fun debug(msg: String)
    fun log(msg: String)
}

class LogConsole : Console {
    private val TAG = LogConsole::class.simpleName

    override fun error(msg: Any) {
        if (msg is V8Object) {
            Log.e(TAG, msg.toString())
        } else if (msg is String) {
            Log.e(TAG, msg)
        } else {
            Log.e(TAG, msg.toString())
        }

    }

    override fun warn(msg: String) {
        Log.w(TAG, msg)
    }

    override fun log(msg: String) {
        Log.i(TAG, msg)
    }

    override fun debug(msg: String) {
        Log.d(TAG, msg)
    }
}