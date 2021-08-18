package org.blockstack.android.sdk.extensions

import me.uport.sdk.core.hexToByteArray
import org.kethereum.crypto.getCompressedPublicKey
import org.kethereum.extensions.toBytesPadded
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PUBLIC_KEY_SIZE
import org.kethereum.model.PublicKey
import org.komputing.kbase58.encodeToBase58String
import org.komputing.khash.ripemd160.extensions.digestRipemd160
import org.komputing.khash.sha256.extensions.sha256
import org.komputing.khex.extensions.toNoPrefixHexString

fun ECKeyPair.toHexPublicKey64(): String {
    return this.getCompressedPublicKey().toNoPrefixHexString()
}

fun String.toStxAddress(sPrefix: Boolean = false): String {
    val sha256 = hexToByteArray().sha256()
    val hash160 = sha256.digestRipemd160()
    val extended = "b0${hash160.toNoPrefixHexString()}"
    val cs = checksum("16${hash160.toNoPrefixHexString()}")

    val prefix = if(sPrefix) "S" else ""
    return prefix + (extended + cs).hexToByteArray().encodeCrockford32()
}

fun ECKeyPair.toStxAddress(sPrefix: Boolean = false): String {
    val sha256 = toHexPublicKey64().hexToByteArray().sha256()
    val hash160 = sha256.digestRipemd160()
    val extended = "b0${hash160.toNoPrefixHexString()}"
    val cs = checksum("16${hash160.toNoPrefixHexString()}")
    val prefix = if(sPrefix) "S" else ""
    return prefix + (extended + cs).hexToByteArray().encodeCrockford32()
    // current       b0 3c8045956db97437913676c6adc770e0ccb927fc 2b371f2d
    // should be    cd bc8045956db97437913676c6adc770e0ccb927fc 2b371f2d
}

fun ECKeyPair.toTestNetStxAddress(sPrefix: Boolean = false) : String {
    val sha256 = toHexPublicKey64().hexToByteArray().sha256()
    val hash160 = sha256.digestRipemd160()
    val extended = "d0${hash160.toNoPrefixHexString()}"
    val cs = checksum("1a${hash160.toNoPrefixHexString()}")
    val prefix = if(sPrefix) "S" else ""
    return prefix + (extended + cs).hexToByteArray().encodeCrockford32()
}

fun String.toBtcAddress(): String {
    val sha256 = hexToByteArray().sha256()
    val hash160 = sha256.digestRipemd160()
    val extended = "00${hash160.toNoPrefixHexString()}"
    val checksum = checksum(extended)
    return(extended + checksum).hexToByteArray().encodeToBase58String()
}

fun ECKeyPair.toBtcAddress(): String {
    val publicKey = toHexPublicKey64()
    return publicKey.toBtcAddress()
}

fun PublicKey.toBtcAddress(): String {
    //add the uncompressed prefix
    val ret = this.key.toBytesPadded(PUBLIC_KEY_SIZE + 1)
    ret[0] = 4
    val point = org.kethereum.crypto.CURVE.decodePoint(ret)
    val compressedPublicKey = point.encoded(true).toNoPrefixHexString()
    val sha256 = compressedPublicKey.hexToByteArray().sha256()
    val hash160 = sha256.digestRipemd160()
    val extended = "00${hash160.toNoPrefixHexString()}"
    val checksum = checksum(extended)
    return (extended + checksum).hexToByteArray().encodeToBase58String()
}

private fun checksum(extended: String): String {
    val checksum = extended.hexToByteArray().sha256().sha256()
    val shortPrefix = checksum.slice(0..3)
    return shortPrefix.toNoPrefixHexString()
}