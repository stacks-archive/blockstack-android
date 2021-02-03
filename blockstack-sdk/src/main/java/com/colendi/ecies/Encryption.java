package com.colendi.ecies;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class Encryption {

    private static final String CURVE_NAME = "secp256k1";
    private static final String PROVIDER = "BC";

    private static ECDomainParameters CURVE;

    public Encryption() {
        Security.addProvider(new BouncyCastleProvider());
        curveInit();
    }

    public EncryptedResult encryptWithPublicKey(byte[] plainText, String pubKey) {
        ECPoint ecPoint = CURVE.getCurve().decodePoint(BigIntegers.asUnsignedByteArray(new BigInteger(pubKey, 16)));

        try {
            return this.encrypt(ecPoint, plainText);
        } catch (Exception e) {
            return new EncryptedResult("", "", "", "");
        }
    }

    public byte[] decryptWithPrivateKey(EncryptedResultForm formData) {
        BigInteger privateKey = new BigInteger(formData.getPrivateKey(), 16);

        return decryptWithPrivateKey(formData, privateKey);
    }

    public byte[] decryptWithPrivateKey(EncryptedResult formData, BigInteger privateKey) {
        return decrypt(privateKey, formData.getIv(), formData.getEphemPublicKey(), formData.getCiphertext(), formData.getMac());
    }

    private static void curveInit() {
        try {
            Class.forName("org.bouncycastle.asn1.sec.SECNamedCurves");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "BouncyCastle is not available on the classpath, see https://www.bouncycastle.org/latest_releases.html");
        }
        X9ECParameters x9ECParameters = SECNamedCurves.getByName(CURVE_NAME);
        CURVE = new ECDomainParameters(x9ECParameters.getCurve(), x9ECParameters.getG(), x9ECParameters.getN(), x9ECParameters.getH());

    }

    private EncryptedResult encrypt(ECPoint toPub, byte[] plainText) throws Exception {
        ECKeyPairGenerator eGen = new ECKeyPairGenerator();
        SecureRandom random = new SecureRandom();
        KeyGenerationParameters gParam = new ECKeyGenerationParameters(CURVE, random);

        eGen.init(gParam);

        AsymmetricCipherKeyPair ephemPair = eGen.generateKeyPair();
        BigInteger ephemPrivatep = ((ECPrivateKeyParameters) ephemPair.getPrivate()).getD();
        ECPoint ephemPub = ((ECPublicKeyParameters) ephemPair.getPublic()).getQ();

        MacAesPair macAesPair = getMacKeyAndAesKey(ephemPrivatep, toPub);

        byte[] IV = new byte[16];
        new SecureRandom().nextBytes(IV);

        byte[] encryptedMsg = encryptAES256CBC(plainText, macAesPair.getEncKeyAES(), IV);

        byte[] ephemPubBytes = ephemPub.getEncoded(true);

        byte[] dataToMac = generateMAC(IV, ephemPubBytes, encryptedMsg);

        byte[] HMac = getHMAC(Hex.decode(macAesPair.getMacKey()), dataToMac);

        String ephemPubString = new String(Hex.encode(ephemPubBytes));
        String ivString = new String(Hex.encode(IV));
        String macString = new String(Hex.encode((HMac)));
        String encryptedText = new String(Hex.encode(encryptedMsg));


        return new EncryptedResult(ephemPubString, ivString, macString, encryptedText);
    }

    private static BigInteger calculateKeyAgreement(BigInteger privKey, ECPoint theirPubKey) {

        ECPrivateKeyParameters privKeyP =
                new ECPrivateKeyParameters(privKey, CURVE);
        ECPublicKeyParameters pubKeyP = new ECPublicKeyParameters(theirPubKey, CURVE);

        ECDHBasicAgreement agreement = new ECDHBasicAgreement();
        agreement.init(privKeyP);
        return agreement.calculateAgreement(pubKeyP);
    }

    static byte[] encryptAES256CBC(byte[] plaintext, String encKey, byte[] IV) throws Exception {

        SecretKeySpec secretKeySpec = new SecretKeySpec(Hex.decode(encKey), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(IV));
        return cipher.doFinal(plaintext);
    }


    private static byte[] generateMAC(byte[] IV, byte[] ephemPublicKey, byte[] ciphertext) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        bos.write(IV);
        bos.write(ephemPublicKey);
        bos.write(ciphertext);

        byte[] dataToMac = bos.toByteArray();

        return dataToMac;

    }

    public static byte[] getHMAC(byte[] macKey, byte[] dataToMac) {
        HMac hmac = new HMac(new SHA256Digest());
        byte[] resBuf = new byte[hmac.getMacSize()];
        hmac.init(new KeyParameter(macKey));
        hmac.update(dataToMac, 0, dataToMac.length);
        hmac.doFinal(resBuf, 0);

        return resBuf;

    }

    private static byte[] decrypt(BigInteger privKey, String IV, String ephemPublicKey, String ciphertext, String mac) {

        try {
            ECPoint ecPoint = CURVE.getCurve().decodePoint(BigIntegers.asUnsignedByteArray(new BigInteger(ephemPublicKey, 16)));

            MacAesPair macAesPair = getMacKeyAndAesKey(privKey, ecPoint);


            byte[] ephemPubBytes = ecPoint.getEncoded(true);

            byte[] dataToMac = generateMAC(Hex.decode(IV), ephemPubBytes, Hex.decode(ciphertext));

            byte[] HMac = getHMAC(Hex.decode(macAesPair.getMacKey()), dataToMac);

            if (MessageDigest.isEqual(HMac, Hex.decode(mac))) {
                return decryptAES256CBC(Hex.decode(ciphertext), macAesPair.getEncKeyAES(), Hex.decode(IV));
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decryptAES256CBC(byte[] ciphertext, String encKey, byte[] IV) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(Hex.decode(encKey), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(IV));
        return cipher.doFinal(ciphertext);
    }

    private static MacAesPair getMacKeyAndAesKey(BigInteger privKey, ECPoint ecPoint) throws Exception {
        MessageDigest mda = MessageDigest.getInstance("SHA-512");

        BigInteger derivedKey = calculateKeyAgreement(privKey, ecPoint);


        byte[] derivedKeyInBytes = BigIntegers.asUnsignedByteArray(derivedKey);
        return sharedSecretToKeys(mda, derivedKeyInBytes);
    }

    public static MacAesPair sharedSecretToKeys(MessageDigest mda, byte[] derivedKeyInBytes) {
        byte[] digestKey = new byte[32];
        if (derivedKeyInBytes.length < 32) {
            System.arraycopy(derivedKeyInBytes, 0, digestKey, 32 - derivedKeyInBytes.length, derivedKeyInBytes.length);
        } else {
            System.arraycopy(derivedKeyInBytes, 0, digestKey, 0, 32);
        }

        byte[] digested = mda.digest(digestKey);

        String strDigested = new String(Hex.encode(digested));

        String encKeyAES = strDigested.substring(0, 64);
        String macKey = strDigested.substring(64);

        return new MacAesPair(macKey, encKeyAES);
    }
}

