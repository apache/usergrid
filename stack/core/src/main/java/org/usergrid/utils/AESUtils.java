package org.usergrid.utils;

import static org.apache.commons.lang.StringUtils.rightPad;
import static org.apache.commons.lang.StringUtils.substring;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AESUtils {

	private static final Logger logger = LoggerFactory
			.getLogger(AESUtils.class);

	private static byte[] linebreak = {};
	private static Cipher cipher;
	private static Base64 coder;

	static {
		try {
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
			coder = new Base64(32, linebreak, true);
		} catch (Throwable t) {
			logger.error("Cipher error", t);
		}
	}

	public static synchronized String encrypt(String secret, String plainText) {
		secret = substring(rightPad(secret, 16), 0, 16);
		SecretKey key = new SecretKeySpec(secret.getBytes(), "AES");
		try {
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] cipherText = cipher.doFinal(plainText.getBytes());
			return new String(coder.encode(cipherText));
		} catch (Exception e) {
			logger.error("Encryption error", e);
		}
		return null;
	}

	public static synchronized String decrypt(String secret, String codedText) {
		secret = substring(rightPad(secret, 16), 0, 16);
		SecretKey key = new SecretKeySpec(secret.getBytes(), "AES");
		byte[] encypted = coder.decode(codedText.getBytes());
		try {
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] decrypted = cipher.doFinal(encypted);
			return new String(decrypted);
		} catch (Exception e) {
			logger.error("Decryption error", e);
		}
		return null;
	}
}
