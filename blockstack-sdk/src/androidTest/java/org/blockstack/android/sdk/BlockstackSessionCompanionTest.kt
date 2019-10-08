package org.blockstack.android.sdk;

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.isEmptyOrNullString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJqdGkiOiJhMzU4MzdmNS1jOGNmLTRiYTMtYjk4ZS03OTY5YTUzZmVjNDgiLCJpYXQiOiIyMDE3LTAzLTA0VDE2OjEzOjA2Ljc5MFoiLCJleHAiOiIyMDE4LTAzLTA0VDE2OjEzOjA2Ljc5MFoiLCJzdWJqZWN0Ijp7InB1YmxpY0tleSI6IjAzZTdhNGQ3OTgzMzY5ZDMzZWQxMzAyMDg4NTk4NWQ2OGY4YjA1ZGVlNjE2OGY3NWY5ZDk3ZTFhMDcyY2RmY2RjNSJ9LCJpc3N1ZXIiOnsicHVibGljS2V5IjoiMDNlN2E0ZDc5ODMzNjlkMzNlZDEzMDIwODg1OTg1ZDY4ZjhiMDVkZWU2MTY4Zjc1ZjlkOTdlMWEwNzJjZGZjZGM1In0sImNsYWltIjp7IkBjb250ZXh0IjoiaHR0cDovL3NjaGVtYS5vcmcvIiwiQHR5cGUiOiJDcmVhdGl2ZVdvcmsiLCJuYW1lIjoiQmFsbG9vbiBEb2ciLCJjcmVhdG9yIjpbeyJAdHlwZSI6IlBlcnNvbiIsIkBpZCI6InRoZXJlYWxqZWZma29vbnMuaWQiLCJuYW1lIjoiSmVmZiBLb29ucyJ9XSwiZGF0ZUNyZWF0ZWQiOiIxOTk0LTA1LTA5VDAwOjAwOjAwLTA0MDAiLCJkYXRlUHVibGlzaGVkIjoiMjAxNS0xMi0xMFQxNDo0NDoyNi0wNTAwIn19.MF7ru91rk8IIKNEEqo9wjLHkvW3jSlcDJmeZZeOVSj9KlXApBp67q_3ke0-LzSO_YyYsUnGOplMYiNxY1XynAA"

@RunWith(AndroidJUnit4::class)
class BlockstackSessionCompanionTest {
    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    @Test
    fun verifyAuthResponseReturnsNullForValidAuthResponse() {
        assertThat(BlockstackSession.verifyAuthResponse(TOKEN)?.message, isEmptyOrNullString())
    }

    @Test
    fun verifyAuthResponseReturnsErrorForInValidAuthResponse() {
        assertThat(BlockstackSession.verifyAuthResponse("a.b.c")?.message, `is`("The authResponse parameter is an invalid base64 encoded token\nbad base-64\nAuth response: a.b.c"))
    }
}
