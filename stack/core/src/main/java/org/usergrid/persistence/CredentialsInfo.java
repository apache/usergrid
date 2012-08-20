/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
	String hashType;

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

	public static CredentialsInfo hashedCredentials(String salt, String secret, String hashType) {
		CredentialsInfo credentials = new CredentialsInfo();
		credentials.setRecoverable(false);
		credentials.setCipher("sha-1");
    credentials.setHashType(hashType);
		credentials.setEncryptedSecret("sha-1", salt, secret);
		return credentials;
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

	/**
	 * @return the hashType
	 */
	public String getHashType() {
		return hashType;
	}

	/**
   * Used for handling legacy passwords encrypted in md5 or similar.
	 * @param hashType
	 *            the hashType to set
	 */
	public void setHashType(String hashType) {
		this.hashType = hashType;
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


  /**
   * If hashType on this object is set, we will do a first-pass call to {@link #encrypt(String, String, String)}
   * with that hashType. The primary use case is to support imported legacy data with weaker password hashing
   * such as vanilla md5.
   * @param cipher
   * @param salt
   * @param secret
   */
	public void setEncryptedSecret(String cipher, String salt, String secret) {
		encrypted = true;
		recoverable = ("aes".equals(cipher) || "plaintext".equals(cipher) || (cipher == null));
		this.cipher = cipher;
    if ( this.hashType != null ) {
      secret = encrypt(this.hashType, "", secret);
    }
		this.secret = encrypt(cipher, salt, secret);
	}

	public String encrypt(String cipher, String salt, String secret) {
		if ("plaintext".equals(cipher)) {
			return secret;
		} else if ("bcrypt".equals(cipher)) {
			return BCrypt.hashpw(secret, BCrypt.gensalt());
		} else if ("sha-1".equals(cipher)) {
			return encodeBase64URLSafeString(computeHash((isBlank(salt) ? secret : salt + secret)));
		} else if ("md5".equals(cipher)) {
			return DigestUtils.md5Hex(secret);
		} else if ("aes".equals(cipher)) {
			return AESUtils.encrypt(salt, secret);
		}
		return secret;
	}

  /**
   * Main entry point for password equivalency comparrison. Compares the output
   * of {@link #getSecret()} for this object and the provided object.
   * @param other
   * @return
   */
  public boolean compare(CredentialsInfo other) {
    return this.getSecret().equals(other.getSecret());
  }
}
