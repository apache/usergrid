package org.usergrid.persistence;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.usergrid.utils.AESUtils.decrypt;
import static org.usergrid.utils.PasswordUtils.computeHash;
import static org.usergrid.utils.PasswordUtils.mongoPassword;

import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.utils.AESUtils;
import org.usergrid.utils.BCrypt;

@XmlRootElement
public class CredentialsInfo {

	boolean recoverable;
	boolean encrypted;
	String cipher;
	String key;
	String secret;

	protected Map<String, Object> properties = new TreeMap<String, Object>(
			String.CASE_INSENSITIVE_ORDER);

	public CredentialsInfo() {
	}

	public static CredentialsInfo plainTextCredentials(String secret) {
		CredentialsInfo credentials = new CredentialsInfo();
		credentials.setRecoverable(true);
		credentials.setSecret(secret);
		return credentials;
	}

	public static CredentialsInfo encryptedCredentials(String salt,
			String secret) {
		CredentialsInfo credentials = new CredentialsInfo();
		credentials.setRecoverable(true);
		credentials.setCipher("aes");
		credentials.setEncryptedSecret("aes", salt, secret);
		return credentials;
	}

	public static CredentialsInfo hashedCredentials(String salt, String secret) {
		CredentialsInfo credentials = new CredentialsInfo();
		credentials.setRecoverable(false);
		credentials.setCipher("sha-1");
		credentials.setEncryptedSecret("sha-1", salt, secret);
		return credentials;
	}

	public static CredentialsInfo passwordCredentials(String secret) {
		return hashedCredentials(null, secret);
	}

	public static CredentialsInfo mongoPasswordCredentials(String username,
			String password) {
		return plainTextCredentials(mongoPassword(username, password));
	}

	public boolean getRecoverable() {
		return recoverable;
	}

	public void setRecoverable(boolean recoverable) {
		this.recoverable = recoverable;
	}

	public boolean getEncrypted() {
		return encrypted;
	}

	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getCipher() {
		return cipher;
	}

	public void setCipher(String cipher) {
		this.cipher = cipher;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public static String getCredentialsSecret(CredentialsInfo credentials) {
		if (credentials == null) {
			return null;
		}
		return credentials.getSecret();
	}

	@JsonAnyGetter
	public Map<String, Object> getProperties() {
		return properties;
	}

	@JsonAnySetter
	public void setProperty(String key, Object value) {
		properties.put(key, value);
	}

	public String getUnencryptedSecret(String salt) {
		if (!recoverable) {
			return null;
		}
		if (!encrypted) {
			return secret;
		}
		if (isBlank(cipher)) {
			return secret;
		}
		if ("plaintext".equals(cipher)) {
			return secret;
		} else if ("bcrypt".equals(cipher)) {
			return null;
		} else if ("md5".equals(cipher)) {
			return null;
		} else if ("sha-1".equals(cipher)) {
			return null;
		} else if ("aes".equals(cipher)) {
			return decrypt(salt, secret);
		}
		return null;
	}

	public boolean compareSecret(String cipher, String salt, String secret) {
		if (secret == null) {
			return false;
		}
		if (this.secret == null) {
			return false;
		}
		if ("bcrypt".equals(cipher)) {
			return BCrypt.checkpw(secret, this.secret);
		}
		return this.secret.equals(encrypt(cipher, salt, secret));
	}

	public boolean compareSha1Secret(String secret) {
		return compareSecret("sha-1", null, secret);
	}

	public boolean compareSha1Secret(String salt, String secret) {
		return compareSecret("sha-1", salt, secret);
	}

	public static boolean checkPassword(String password,
			CredentialsInfo credentials) {
		if (credentials == null) {
			return false;
		}
		if (password == null) {
			return false;
		}
		return credentials.compareSha1Secret(password);
	}

	public boolean compareBCryptSecret(String secret) {
		return compareSecret("bcrypt", null, secret);
	}

	public void setEncryptedSecret(String cipher, String salt, String secret) {
		encrypted = true;
		recoverable = ("aes".equals(cipher) || "plaintext".equals(cipher) || (cipher == null));
		this.cipher = cipher;
		this.secret = encrypt(cipher, salt, secret);
	}

	public String encrypt(String cipher, String salt, String secret) {
		if ("plaintext".equals(cipher)) {
			return secret;
		} else if ("bcrypt".equals(cipher)) {
			return BCrypt.hashpw(secret, BCrypt.gensalt());
		} else if ("sha-1".equals(cipher)) {
			return encodeBase64URLSafeString(computeHash(secret));
		} else if ("md5".equals(cipher)) {
			return DigestUtils.md5Hex(secret);
		} else if ("aes".equals(cipher)) {
			return AESUtils.encrypt(salt, secret);
		}
		return secret;
	}

}
