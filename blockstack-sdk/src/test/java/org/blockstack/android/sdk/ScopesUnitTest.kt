package org.blockstack.android.sdk;

import org.junit.Test;

import org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ScopesUnitTest {
    @Test
    fun testScopesToString() {
        val scopes: Array<Scope> = arrayOf<Scope>(Scope.Email, Scope.PublishData, Scope.StoreWrite)
        val result = Scope.scopesArrayToJSONString(scopes)
        assertEquals("", result)

    }
}