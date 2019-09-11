package org.blockstack.android.sdk.ecies


import android.util.Base64
import org.blockstack.android.sdk.model.CipherObject
import org.json.JSONObject
import org.kethereum.crypto.*
import org.kethereum.crypto.api.ec.CurvePoint
import org.kethereum.crypto.impl.hashing.DigestParams
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey
import org.walleth.khex.hexToByteArray
import org.walleth.khex.toNoPrefixHexString
import java.math.BigInteger
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesCBC(var key: ByteArray, var iv: ByteArray) {

    @Throws(Exception::class)
    fun encrypt(plainText: ByteArray): String {
        val secretKey = SecretKeySpec(key, ALGORITHM)
        val ivParameterSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(AES_256_CDC)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
        val data = cipher.doFinal(plainText)
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    @Throws(Exception::class)
    fun decrypt(cipherText: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(key, ALGORITHM)
        val ivParameterSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(AES_256_CDC)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
        return cipher.doFinal(Base64.decode(cipherText, Base64.NO_WRAP))
    }


    companion object {
        private val AES_256_CDC = "AES/CBC/PKCS5Padding"
        private val ALGORITHM = "AES"
    }
}

data class Keys(
        val encryptionKey: ByteArray,
        val hmacKey: ByteArray
)

fun sharedSecretToKeys(sharedSecret: ByteArray): Keys {
    // generate mac and encryption key from shared secret
    val hashedSecret = CryptoAPI.hmac.init(sharedSecret).generate(sharedSecret)
    return Keys(hashedSecret.sliceArray(0..31), hashedSecret.sliceArray(32..63))
}

val iv = byteArrayOf(1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6)

fun encryptECIES(publicKey: String, content: Any, privateKey: String): CipherObject {

    val isString = (content is String)
// always copy to buffer
    val plainText = if (content is ByteArray) content else (content as String).toByteArray(Charsets.UTF_8)

    //  const ecPK = ecurve.keyFromPublic(publicKey, 'hex').getPublic() as BN
    val ecPK = CURVE.decodePoint(publicKey.hexToByteArray())
    //  const ephemeralSK = ecurve.genKeyPair()
    val ephemeralSK = CryptoAPI.keyPairGenerator.generate()
    //  const ephemeralPK = ephemeralSK.getPublic()
    //  const sharedSecret = ephemeralSK.derive(ecPK) as BN
    val sharedSecret = ephemeralSK.derive(ecPK)
    //
    //  const sharedSecretHex = getHexFromBN(sharedSecret)
    //  const sharedKeys = sharedSecretToKeys(
    //    Buffer.from(sharedSecretHex, 'hex')
    //  )
    val sharedSecretHex = sharedSecret.toByteArray().toNoPrefixHexString()
    val sharedKeys = sharedSecretToKeys(sharedSecretHex.toByteArray(Charsets.US_ASCII))

    var initializationVector = ByteArray(16).apply {
        SecureRandomUtils.secureRandom().nextBytes(this)
    }
    initializationVector = iv

    // const cipherText = aes256CbcEncrypt(
    //    initializationVector, sharedKeys.encryptionKey, plainText
    //  )
    val cipherText = AesCBC(sharedKeys.encryptionKey, initializationVector).encrypt(plainText)

    // const macData = Buffer . concat ([initializationVector,
    //        Buffer.from(ephemeralPK.encodeCompressed()),
    //        cipherText])
    val macData = initializationVector + ephemeralSK.getCompressedPublicKey() + cipherText.toByteArray(Charsets.US_ASCII)

    // const mac = hmacSha256 (sharedKeys.hmacKey, macData)
    val mac = CryptoAPI.hmac.init(sharedKeys.hmacKey, DigestParams.Sha256).generate(macData)


    // decrypt *******
    val ecSK = PrivateKey(privateKey.hexToByteArray()).toECKeyPair()
    val ephemeralPk = CURVE.decodePoint(ephemeralSK.getCompressedPublicKey())
    val sharedSecret2 = ecSK.derive(ephemeralPk)
    val sharedSecretHex2 = sharedSecret2.toByteArray().toNoPrefixHexString()
    val sharedKeys2 = sharedSecretToKeys(sharedSecretHex2.toByteArray(Charsets.US_ASCII))
    val plainText2 = AesCBC(sharedKeys2.encryptionKey, initializationVector).decrypt(cipherText.toByteArray(Charsets.UTF_8))

    val cipherObject= CipherObject(JSONObject()
            .put("iv", initializationVector.toNoPrefixHexString())
            .put("ephemeralPK", ephemeralSK.getCompressedPublicKey().toNoPrefixHexString())
            .put("cipherText", cipherText)
            .put("mac", mac.toNoPrefixHexString())
            .put("wasString", isString)
    )

    val ephemeralPk2 = CURVE.decodePoint(cipherObject.json.getString("ephemeralPK").hexToByteArray())
    val cipherTextBuffer = cipherObject.json.getString("cipherText").toByteArray(Charsets.UTF_8)

    val plainTex3 = AesCBC(sharedKeys2.encryptionKey, initializationVector).decrypt(cipherTextBuffer)

    return cipherObject

}

fun decryptECIES(privateKey: String, cipherObject: CipherObject): Any {
//  const ecSK = ecurve.keyFromPrivate(privateKey, 'hex')
    val ecSK = PrivateKey(privateKey.hexToByteArray()).toECKeyPair()
//  const ephemeralPK = ecurve.keyFromPublic(cipherObject.ephemeralPK, 'hex').getPublic()
    val ephemeralPk = CURVE.decodePoint(cipherObject.json.getString("ephemeralPK").hexToByteArray())
//  const sharedSecret = ecSK.derive(ephemeralPK)
    val sharedSecret = ecSK.derive(ephemeralPk)

//  const sharedSecretBuffer = Buffer.from(getHexFromBN(sharedSecret), 'hex')
//  const sharedKeys = sharedSecretToKeys(sharedSecretBuffer)
    val sharedSecretHex = sharedSecret.toByteArray().toNoPrefixHexString()
    val sharedKeys = sharedSecretToKeys(sharedSecretHex.toByteArray(Charsets.US_ASCII))

//
//  const ivBuffer = Buffer.from(cipherObject.iv, 'hex')
    val ivBuffer = cipherObject.json.getString("iv").hexToByteArray()
//  const cipherTextBuffer = Buffer.from(cipherObject.cipherText, 'hex')
    val cipherTextBuffer = cipherObject.json.getString("cipherText").toByteArray(Charsets.UTF_8)

//  const macData = Buffer.concat([ivBuffer,
//                                 Buffer.from(ephemeralPK.encode('array', true) as Buffer),
//                                 cipherTextBuffer])
    val macData = ivBuffer + ephemeralPk.encoded() + cipherTextBuffer
//  const actualMac = hmacSha256(sharedKeys.hmacKey, macData)
    val actualMac = CryptoAPI.hmac.init(sharedKeys.hmacKey, DigestParams.Sha256).generate(macData)

//  const expectedMac = Buffer.from(cipherObject.mac, 'hex')
    val expectedMac = cipherObject.json.getString("mac").hexToByteArray()
//  if (!equalConstTime(expectedMac, actualMac)) {
//    throw new Error('Decryption failed: failure in MAC check')
//  }
    // TODO verify mac
//  const plainText = aes256CbcDecrypt(
//    ivBuffer, sharedKeys.encryptionKey, cipherTextBuffer
//  )
    val plainText = AesCBC(sharedKeys.encryptionKey, ivBuffer).decrypt(cipherTextBuffer)

    if (cipherObject.json.getBoolean("wasString")) {
        return plainText.toString(Charsets.UTF_8)
    } else {
        return plainText
    }

}

private fun ECKeyPair.derive(pk: CurvePoint): BigInteger {
    return pk.mul(this.privateKey.key).x
}
