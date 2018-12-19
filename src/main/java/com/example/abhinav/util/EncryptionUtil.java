package com.example.abhinav.util;

import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.apache.commons.io.FileUtils.*;
import static org.apache.commons.io.FileUtils.readFileToByteArray;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;

public class EncryptionUtil {

	String encryptedText = "{\"organization\":\"NEU\",\"scope\":\"readOnly\",\"creationDate\":\"2018-11-02\",\"user\":\"Abhinav Srivastava\",\"TTL\":\"2018-11-17\"}";
	private static String algorithm = "DESede";
	public static String KEY = "src/main/resources/" + "key";

	public boolean decrypt(String authKey) {
		String decryptedText = null;
		try {
			SecretKey spec = loadKey(new File(KEY));
			Cipher c = Cipher.getInstance(algorithm);
			byte[] decoded = Base64.getDecoder().decode(authKey);
			decryptedText = decryptF(decoded, spec, c);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException
				| IllegalBlockSizeException | NullPointerException e) {
			e.printStackTrace();
			return false;
		}
		// System.out.println("encryptedText:: "+encryptedText);
		// System.out.println("decryptedText:: "+decryptedText);
		return encryptedText.equalsIgnoreCase(decryptedText) ? true : false;
	}

	static String decryptF(byte[] encryptionBytes, Key pkey, Cipher c)
			throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
		c.init(Cipher.DECRYPT_MODE, pkey);
		byte[] decrypt = c.doFinal(encryptionBytes);
		return new String(decrypt);
	}

	public static SecretKey loadKey(File file) throws IOException {
		String data = new String(readFileToByteArray(file));
		byte[] encoded;
		try {
			encoded = decodeHex(data.toCharArray());
		} catch (DecoderException e) {
			e.printStackTrace();
			return null;
		}
		return new SecretKeySpec(encoded, algorithm);
	}

}
