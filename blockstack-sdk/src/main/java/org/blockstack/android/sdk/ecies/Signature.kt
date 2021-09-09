package org.blockstack.android.sdk.ecies

import me.uport.sdk.core.hexToByteArray
import me.uport.sdk.signer.getUncompressedPublicKeyWithPrefix
import org.blockstack.android.sdk.extensions.toHexPublicKey64
import org.blockstack.android.sdk.model.SignatureObject
import org.blockstack.android.sdk.model.SignedCipherObject
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.kethereum.crypto.signMessageHash
import org.kethereum.crypto.toECKeyPair
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey
import org.kethereum.model.SignatureData
import org.komputing.khash.sha256.extensions.sha256
import org.komputing.khex.extensions.toNoPrefixHexString
import org.komputing.khex.model.HexString
import java.math.BigInteger
import java.security.InvalidParameterException
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.ln
import kotlin.math.log10


internal val CURVE by lazy { CustomNamedCurves.getByName("secp256k1")!! }
internal val DOMAIN_PARAMS = CURVE.run { ECDomainParameters(curve, g, n, h) }


fun signContent(content: Any, privateKey: String): SignatureObject {
    val contentBuffer = if (content is ByteArray) {
        content
    } else {
        (content as String).toByteArray()
    }
    val keyPair = PrivateKey(HexString(privateKey)).toECKeyPair()
    val sigData = signMessageHash(contentBuffer.sha256(), keyPair, false)

    val signatureString = sigData.toDER()

    return SignatureObject(signatureString,
            keyPair.toHexPublicKey64()
    )
}

fun signEncryptedContent(content: String, privateKey: String): SignedCipherObject {
    val signatureObject = signContent(content, privateKey)
    return SignedCipherObject(signatureObject.signature,
            signatureObject.publicKey, content)
}

fun ECKeyPair.verify(contentHash: ByteArray, signature: String): Boolean {
    val sig: SignatureData = signature.fromDER()

    val publicKeyBytes = publicKey.getUncompressedPublicKeyWithPrefix()

    val ecPoint = CURVE.curve.decodePoint(publicKeyBytes)
    val verifier = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))

    val ecPubKeyParams = ECPublicKeyParameters(ecPoint, DOMAIN_PARAMS)
    verifier.init(false, ecPubKeyParams)

    return verifier.verifySignature(contentHash, sig.r, sig.s)
}

data class Position(var place: Int)

fun String.fromDER(): SignatureData {
    val data = this.hexToByteArray()
    val p = Position(0)
    if (data[p.place++] != 0x30.toByte()) {
        throw InvalidParameterException()
    }
    val len = getLength(data, p)
    if ((len + p.place) != data.size) {
        throw InvalidParameterException()
    }
    if (data[p.place++] != 0x02.toByte()) {
        throw InvalidParameterException()
    }
    val rlen = getLength(data, p)
    var r = data.sliceArray(p.place until rlen + p.place)
    p.place += rlen
    if (data[p.place++] != 0x02.toByte()) {
        throw InvalidParameterException()
    }
    val slen = getLength(data, p)
    if (data.size != slen + p.place) {
        throw InvalidParameterException()
    }
    var s = data.sliceArray(p.place until slen + p.place)
    /* BigInteger is dealing with leading zero correctly
    if (r[0] == ZERO && (r[1] and LENGTH) != ZERO) {
        r = r.sliceArray(1 until r.size)
    }
    if (s[0] == ZERO && (s[1] and LENGTH) != ZERO) {
        s = s.sliceArray(1 until s.size)
    }
   */

    return SignatureData(BigInteger(r), BigInteger(s), BigInteger.ZERO)
}

const val ZERO = 0.toByte()
const val LENGTH = 0x80.toByte() // 128


fun addSize(arr: MutableList<Byte>, len: Int) {
    if (len < 128) {
        arr.add(len.toByte())
        return
    }
    val l = (log10(len.toDouble()) / ln(2.toDouble())).toInt() ushr 3
    var octets = 1 + l
    arr.add(octets.toByte() or LENGTH)
    octets -= 1
    while (octets.toByte() != ZERO) {
        arr.add(((len ushr (octets shl 3)) and 0xff).toByte())
    }
    arr.add(len.toByte())
}


fun getLength(buf: ByteArray, p: Position): Byte {
    val initial = buf[p.place++]
    if (initial and LENGTH == ZERO) {
        return initial
    }
    val octetLen = initial and 0xf.toByte()
    var value = 0
    var off = p.place
    for (i in 0 until octetLen) {
        value = value shl 8
        value = value or buf[off].toInt()
        off += 1
    }
    p.place = off
    return value.toByte()
}

fun rmPadding(buf: ByteArray): ByteArray {
    var i = 0
    val len = buf.size - 1
    while (buf[i] == ZERO && (buf[i + 1] and LENGTH) == ZERO && i < len) {
        i++
    }
    if (i == 0) {
        return buf
    }
    return buf.sliceArray(i until buf.size)
}

fun SignatureData.toDER(): String {
    var r = this.r.toByteArray()
    var s = this.s.toByteArray()

    // Pad values
    if (r[0] and LENGTH != ZERO) {
        r = byteArrayOf(0) + r
    }
    // Pad values
    if (s[0] and LENGTH != ZERO) {
        s = byteArrayOf(0) + s
    }

    r = rmPadding(r)
    s = rmPadding(s)

    while (s[0] == ZERO && (s[1] and LENGTH) == ZERO) {
        s = s.sliceArray(1 until s.size)
    }
    val arr = mutableListOf<Byte>(0x02)
    addSize(arr, r.size)
    arr.addAll(r.toTypedArray())

    arr.add(0x02.toByte())
    addSize(arr, s.size)
    arr.addAll(s.toTypedArray())

    val res = mutableListOf<Byte>(0x30)
    addSize(res, arr.size)
    res.addAll(arr)

    return res.toNoPrefixHexString()
}
