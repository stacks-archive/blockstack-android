package org.blockstack.android.sdk;

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test

class ScopesUnitTest {
    @Test
    fun testScopesToString() {
        val scopes: Array<Scope> = arrayOf(Scope.Email, Scope.PublishData, Scope.StoreWrite)
        val result = Scope.scopesArrayToJSONString(scopes)
        assertEquals("[\"email\", \"publish_data\", \"store_write\"]", result)
    }

    @Test
    fun testFromJSName() {
        assertThat(Scope.StoreWrite, `is`(Scope.fromJSName("store_write")))
    }

    @Test
    fun testBadJSName() {
        try {
            Scope.fromJSName("xyz")
        } catch (e: Exception) {
            assertThat(e.message, `is`("scope 'xyz' not defined, available scopes: store_write, publish_data, email"))
        }
    }
}