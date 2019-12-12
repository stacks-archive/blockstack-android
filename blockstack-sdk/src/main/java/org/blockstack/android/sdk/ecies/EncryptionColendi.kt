package org.blockstack.android.sdk.ecies


import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.BigIntegers
import org.bouncycastle.util.encoders.Hex
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class EncryptionColendi {
    init {
        Security.addProvider(BouncyCastleProvider())
        curveInit()
    }

    fun encryptWithPublicKey(plainText: String, pubKey: String): EncryptedResult {
        val ecPoint = CURVE!!.curve.decodePoint(BigIntegers.asUnsignedByteArray(BigInteger(pubKey, 16)))

        try {
            return this.encrypt(ecPoint, plainText)
        } catch (e: Exception) {
            return EncryptedResult("", "", "", "")
        }

    }

    fun decryptWithPrivateKey(formData: EncryptedResult, privateKey: String): String {
        val privKey = BigInteger(privateKey, 16)

        return decrypt(privKey, formData.ivString, formData.ephemPubString, formData.encryptedText, formData.macString)
    }

    @Throws(Exception::class)
    private fun encrypt(toPub: ECPoint, plainText: String): EncryptedResult {
        val eGen = ECKeyPairGenerator()
        val random = SecureRandom()
        val gParam = ECKeyGenerationParameters(CURVE!!, random)

        eGen.init(gParam)

        val ephemPair = eGen.generateKeyPair()
        val ephemPrivatep = (ephemPair.private as ECPrivateKeyParameters).d
        val ephemPub = (ephemPair.public as ECPublicKeyParameters).q

        val macAesPair = getMacKeyAndAesKey(ephemPrivatep, toPub)

        val IV = ByteArray(16)
        SecureRandom().nextBytes(IV)

        val encryptedMsg = encryptAES256CBC(plainText, macAesPair.encKeyAES, IV)

        val ephemPubBytes = ephemPub.getEncoded(true)

        val dataToMac = generateMAC(IV, ephemPubBytes, encryptedMsg)

        val HMac = getHMAC(Hex.decode(macAesPair.macKey), dataToMac)

        val ephemPubString = String(Hex.encode(ephemPubBytes))
        val ivString = String(Hex.encode(IV))
        val macString = String(Hex.encode(HMac))
        val encryptedText = String(Hex.encode(encryptedMsg))


        return EncryptedResult(ephemPubString, ivString, macString, encryptedText)
    }

    companion object {

        private val CURVE_NAME = "secp256k1"
        private val PROVIDER = "BC"

        private var CURVE: ECDomainParameters? = null

        private fun curveInit() {
            try {
                Class.forName("org.bouncycastle.asn1.sec.SECNamedCurves")
            } catch (e: ClassNotFoundException) {
                throw IllegalStateException(
                        "BouncyCastle is not available on the classpath, see https://www.bouncycastle.org/latest_releases.html")
            }

            val x9ECParameters = SECNamedCurves.getByName(CURVE_NAME)
            CURVE = ECDomainParameters(x9ECParameters.curve, x9ECParameters.g, x9ECParameters.n, x9ECParameters.h)

        }

        private fun calculateKeyAgreement(privKey: BigInteger, theirPubKey: ECPoint): BigInteger {

            val privKeyP = ECPrivateKeyParameters(privKey, CURVE)
            val pubKeyP = ECPublicKeyParameters(theirPubKey, CURVE!!)

            val agreement = ECDHBasicAgreement()
            agreement.init(privKeyP)
            return agreement.calculateAgreement(pubKeyP)
        }

        @Throws(Exception::class)
        private fun encryptAES256CBC(plaintext: String, encKey: String, IV: ByteArray): ByteArray {

            val secretKeySpec = SecretKeySpec(Hex.decode(encKey), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, IvParameterSpec(IV))
            return cipher.doFinal(plaintext.toByteArray())
        }


        @Throws(IOException::class)
        private fun generateMAC(IV: ByteArray, ephemPublicKey: ByteArray, ciphertext: ByteArray): ByteArray {
            val bos = ByteArrayOutputStream()

            bos.write(IV)
            bos.write(ephemPublicKey)
            bos.write(ciphertext)

            return bos.toByteArray()

        }

        private fun getHMAC(macKey: ByteArray, dataToMac: ByteArray): ByteArray {

            val hmac = HMac(SHA256Digest())
            val resBuf = ByteArray(hmac.macSize)
            hmac.init(KeyParameter(macKey))
            hmac.update(dataToMac, 0, dataToMac.size)
            hmac.doFinal(resBuf, 0)

            return resBuf

        }

        private fun decrypt(privKey: BigInteger, IV: String, ephemPublicKey: String, ciphertext: String, mac: String): String {

            try {
                val ecPoint = CURVE!!.curve.decodePoint(BigIntegers.asUnsignedByteArray(BigInteger(ephemPublicKey, 16)))

                val macAesPair = getMacKeyAndAesKey(privKey, ecPoint)


                val ephemPubBytes = ecPoint.getEncoded(true)

                val dataToMac = generateMAC(Hex.decode(IV), ephemPubBytes, Hex.decode(ciphertext))

                val HMac = getHMAC(Hex.decode(macAesPair.macKey), dataToMac)

                return if (MessageDigest.isEqual(HMac, Hex.decode(mac))) {
                    decryptAES256CBC(Hex.decode(ciphertext), macAesPair.encKeyAES, Hex.decode(IV))
                } else {
                    "BAD-MAC"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                return ""
            }

        }

        @Throws(Exception::class)
        private fun decryptAES256CBC(ciphertext: ByteArray, encKey: String, IV: ByteArray): String {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKeySpec = SecretKeySpec(Hex.decode(encKey), "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(IV))
            return String(cipher.doFinal(ciphertext))
        }

        @Throws(Exception::class)
        private fun getMacKeyAndAesKey(privKey: BigInteger, ecPoint: ECPoint): MacAesPair {
            val mda = MessageDigest.getInstance("SHA-512", PROVIDER)

            val derivedKey = calculateKeyAgreement(privKey, ecPoint)


            val derivedKeyInBytes = BigIntegers.asUnsignedByteArray(derivedKey)
            val digestKey = ByteArray(32)
            System.arraycopy(derivedKeyInBytes, 0, digestKey, 0, 32)

            val digested = mda.digest(digestKey)

            val strDigested = String(Hex.encode(digested))

            val encKeyAES = strDigested.substring(0, 64)
            val macKey = strDigested.substring(64)

            return MacAesPair(macKey, encKeyAES)
        }
    }
}

data class EncryptedResult(val ephemPubString: String, val ivString: String, val macString: String, val encryptedText: String)

data class MacAesPair(val macKey: String, val encKeyAES: String)

