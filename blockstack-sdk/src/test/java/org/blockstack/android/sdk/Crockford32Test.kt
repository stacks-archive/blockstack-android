package org.blockstack.android.sdk

import org.blockstack.android.sdk.extensions.decodeCrockford32
import org.blockstack.android.sdk.extensions.encodeCrockford32
import org.junit.Assert
import org.junit.Test

class Crockford32Test {

    val strings = listOf(
        "a46ff88886c2ef9762d970b4d2c63678835bd39d",
        "",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000001",
        "1000000000000000000000000000000000000001",
        "1000000000000000000000000000000000000000",
        "1",
        "22",
        "001",
        "0001",
        "00001",
        "000001",
        "0000001",
        "00000001",
        "10",
        "100",
        "1000",
        "10000",
        "100000",
        "1000000",
        "10000000",
        "100000000"
    )

    val c32Strings = listOf(
        "C4T3CSK670W3GE1PCCS6ASHS6WV34S1S6WR64D3469HKCCSP6WW3GCSNC9J36EB4",
        "",
        "60R30C1G60R30C1G60R30C1G60R30C1G60R30C1G60R30C1G60R30C1G60R30C1G",
        "60R30C1G60R30C1G60R30C1G60R30C1G60R30C1G60R30C1G60R30C1G60R30C1H",
        "64R30C1G60R30C1G60R30C1G60R30C1G60R30C1G60R30C1G60R30C1G60R30C1H",
        "64R30C1G60R30C1G60R30C1G60R30C1G60R30C1G60R30C1G60R30C1G60R30C1G",
        "64",
        "68S0",
        "60R32",
        "60R30C8",
        "60R30C1H",
        "60R30C1G64",
        "60R30C1G60RG",
        "60R30C1G60R32",
        "64R0",
        "64R30",
        "64R30C0",
        "64R30C1G",
        "64R30C1G60",
        "64R30C1G60R0",
        "64R30C1G60R30",
        "64R30C1G60R30C0"
    )

    @Test
    fun encodeTest() {
        strings.forEachIndexed { index, string ->
            Assert.assertEquals(c32Strings[index], string.encodeCrockford32())
        }
    }

    @Test
    fun decodeTest() {
        c32Strings.forEachIndexed { index, string ->
            Assert.assertEquals(strings[index], string.decodeCrockford32())
        }
    }

    @Test
    fun crockford32Test() {
        val encoded = "something very very big and complex".encodeCrockford32()
        Assert.assertEquals("something very very big and complex", encoded.decodeCrockford32())
    }
}