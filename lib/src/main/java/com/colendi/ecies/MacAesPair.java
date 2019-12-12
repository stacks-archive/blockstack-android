package com.colendi.ecies;

public class MacAesPair {
    String macKey;
    String encKeyAES;

    public MacAesPair(String macKey, String encKeyAES) {
        this.macKey = macKey;
        this.encKeyAES = encKeyAES;
    }

    public String getMacKey() {
        return macKey;
    }

    public void setMacKey(String macKey) {
        this.macKey = macKey;
    }

    public String getEncKeyAES() {
        return encKeyAES;
    }

    public void setEncKeyAES(String encKeyAES) {
        this.encKeyAES = encKeyAES;
    }
}
