package com.colendi.ecies;

import org.junit.Test;

public class EncryptionApplicationTests {

	@Test
	public void contextLoads() {
	    Encryption encryption = new Encryption();

		String privateKey = "7a7480972a756b1f117faadd23f9af00bdb309d3553e47b3b5d7f2756df620b3";
	    String publicKey = "04327453891187123d8a122c47ac5a98ff9a1cbc0dd28ce6fae2183a51a7b8aeaaea8b75c7ac46fbc2434c0fe8b8fecb5fee1be8b52bff3072046fe26ca3652279";
	    String message = "Colendi";


	    EncryptedResult encryptedResult = encryption.encryptWithPublicKey(message, publicKey);

	    EncryptedResultForm formData = new EncryptedResultForm();

		formData.setPrivateKey(privateKey);
	    formData.setCiphertext(encryptedResult.getCiphertext());
		formData.setEphemPublicKey(encryptedResult.getEphemPublicKey());
		formData.setIv(encryptedResult.getIv());
		formData.setMac(encryptedResult.getMac());

	    String result = encryption.decryptWithPrivateKey(formData);
        assert(result.equals(message));
	}

}

