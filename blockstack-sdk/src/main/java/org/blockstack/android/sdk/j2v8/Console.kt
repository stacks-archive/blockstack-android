package org.blockstack.android.sdk.j2v8

import android.util.Log


interface Console {
    fun error(msg: String)
    fun warn(msg: String)
    fun debug(msg: String)
    fun log(msg: String)
}

class LogConsole : Console {
    private val TAG = LogConsole::class.simpleName

    override fun error(msg: String) {
        Log.e(TAG, msg)
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