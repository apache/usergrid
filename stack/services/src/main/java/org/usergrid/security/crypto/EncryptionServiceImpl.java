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
package org.usergrid.security.crypto;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.CharSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.persistence.entities.User;
import org.usergrid.security.crypto.command.EncryptionCommand;

import java.util.UUID;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;

/**
 * @author tnine
 * 
 */
public class EncryptionServiceImpl implements EncryptionService {

  private static final Charset UTF8 = Charset.forName("UTF-8");
  private Map<String, EncryptionCommand> commands;

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.security.crypto.EncryptionService#encryptSecret(java.lang.
   * String, org.usergrid.persistence.CredentialsInfo,
   * org.usergrid.persistence.entities.User, com.eaio.uuid.UUID)
   */
  @Override
  public String encryptSecret(String inputSecret, CredentialsInfo creds, User user, UUID applicationId) {
    String[] storedCommands = null;

    if (creds.getCryptoChain() != null && creds.getCryptoChain().length > 0) {
      storedCommands = creds.getCryptoChain();
    }
    // no chain was set, fall back to try to use the hashType and our default
    // (legacy support)
    else if (creds.getHashType() != null) {
      storedCommands = new String[] { creds.getHashType(), creds.getCipher() };
    }
    // use the default cipher
    else {
      storedCommands = new String[] { creds.getCipher() };
    }

    byte[] encrypted = inputSecret.getBytes(UTF8);

    // run the bytes through each command sequentially to generate our
    // acceptable hashcode
    for (String commandName : storedCommands) {
      EncryptionCommand command = commands.get(commandName);

      // verify we have a command to load
      Assert
          .notNull(
              command,
              String
                  .format(
                      "No command implementat for name %s exists, yet it is persisted on a user's credentials info.  This means their credentials either need removed, or this command needs supported",
                      commandName));

      encrypted = command.hash(encrypted, creds, user, applicationId);
    }

    return encodeBase64URLSafeString(encrypted);

  }

  /**
   * @param wiredCommands
   *          the wiredCommands to set
   */
  @Autowired
  public void setCommands(List<EncryptionCommand> inputCommands) {

    /**
     * Create the map by name so we can reference them later.
     */
    for (EncryptionCommand command : inputCommands) {
      String name = command.getName();

      Assert.notNull(name, "Encryption command name cannot be null");

      EncryptionCommand existing = commands.get(name);

      Assert.isNull(existing, String.format(
          "Both class %s and %s implement command %s.  This is a wiring bug, and not allowed", command.getClass()
              .getName(), existing.getClass().getName(), name));

      commands.put(name, command);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.security.crypto.EncryptionService#plainTextCredentials(java
   * .lang.String)
   */
  @Override
  public CredentialsInfo plainTextCredentials(String secret) {

    CredentialsInfo credentials = new CredentialsInfo();
    credentials.setRecoverable(true);
    credentials.setSecret(secret);
    credentials.setCryptoChain(new String[]{EncryptionCommand.PLAINTEXT});
    return credentials;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.security.crypto.EncryptionService#defaultEncryptedCredentials
   * (java.lang.String)
   */
  @Override
  public CredentialsInfo defaultEncryptedCredentials(String input) {
    return null;
  }

}
