package org.blockstack.android.sdk.extensions

import org.json.JSONObject
import org.junit.Test


class JSONObjectKtTest {

    @Test
    fun testGetNullableNull() {
        // Arrange
        val json = JSONObject("{\"associationToken\":null,\"version\":\"1.3.1\",\"iss\":\"did:btc-addr:1KjvynGKa7tuZyH4JVNKjBxkfXugk9wyhL\"}")

        // Act
        val token = json.getStringOrNull("associationToken")

        // Assert
        assert(token == null)
    }

    @Test
    fun testGetNullableStringNull() {
        // Arrange
        val json = JSONObject("{\"associationToken\":\"null\",\"version\":\"1.3.1\",\"iss\":\"did:btc-addr:1KjvynGKa7tuZyH4JVNKjBxkfXugk9wyhL\"}")

        // Act
        val token = json.getStringOrNull("associationToken")

        // Assert
        assert(token == null)
    }

    @Test
    fun testGetNullableValue() {
        // Arrange
        val json = JSONObject("{\"associationToken\":\"123\",\"version\":\"1.3.1\",\"iss\":\"did:btc-addr:1KjvynGKa7tuZyH4JVNKjBxkfXugk9wyhL\"}")

        // Act
        val token = json.getStringOrNull("associationToken")

        // Assert
        assert(token == "123")
    }
}