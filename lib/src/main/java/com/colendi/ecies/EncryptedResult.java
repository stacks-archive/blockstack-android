package com.colendi.ecies;

public class EncryptedResult {

    public EncryptedResult() { }

    public EncryptedResult(String ephemPublicKey, String iv, String mac, String ciphertext) {
        this.ephemPublicKey = ephemPublicKey;
        this.iv = iv;
        this.mac = mac;
        this.ciphertext = ciphertext;
    }

    protected String ephemPublicKey;
    protected String iv;
    protected String mac;
    protected String ciphertext;

    public String getEphemPublicKey() {
        return ephemPublicKey;
    }

    public void setEphemPublicKey(String ephemPublicKey) {
        this.ephemPublicKey = ephemPublicKey;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public void setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
    }
}
