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

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.security.crypto.command.EncryptionCommand;

/**
 * @author tnine
 * 
 */
@Service("encryptionService")
public class EncryptionServiceImpl implements EncryptionService {

  private String defaultCommandName = EncryptionCommand.BCRYPT;
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private Map<String, EncryptionCommand> commands;
  private List<EncryptionCommand> inputCommands;
  private EncryptionCommand defaultCommand;

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.security.crypto.EncryptionService#encryptSecret(java.lang.
   * String, org.usergrid.persistence.CredentialsInfo, java.util.UUID,
   * java.util.UUID)
   */
  @Override
  public boolean verify(String inputSecret, CredentialsInfo creds, UUID userId, UUID applicationId) {

    
    String[] storedCommands = null;

    // We have the new format of crypto chain. read them and apply them
    if (creds.getCryptoChain() != null && creds.getCryptoChain().length > 0) {
      storedCommands = creds.getCryptoChain();
    }

    // no chain was set, fall back to try to use the hashType and our default
    // (legacy support)
    else if (creds.getHashType() != null) {
      storedCommands = new String[] { creds.getHashType(), creds.getCipher() };
    }
    // use the default cipher on the creds
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

      encrypted = command.auth(encrypted, creds, userId, applicationId);
    }


    return encode(encrypted).equals(creds.getSecret());

  }

  /**
   * @param wiredCommands
   *          the wiredCommands to set
   */
  @Autowired
  public void setCommands(List<EncryptionCommand> inputCommands) {
    this.inputCommands = inputCommands;
  }
  
  @PostConstruct
  public void init(){
    if(inputCommands == null || inputCommands.size() == 0){
      throw new IllegalArgumentException(String.format("You must provide %s implementations for this service to function properly", EncryptionCommand.class));
    }
    
    commands = new HashMap<String, EncryptionCommand>();

    /**
     * Create the map by name so we can reference them later.
     */
    for (EncryptionCommand command : inputCommands) {
      String name = command.getName();

      Assert.notNull(name, "Encryption command name cannot be null");

      EncryptionCommand existing = commands.get(name);

      if (existing != null) {
        throw new IllegalArgumentException(String.format(
            "Both class %s and %s implement command '%s'.  This is a wiring bug, and not allowed.  Each instance must define it's own type", command.getClass()
                .getName(), existing.getClass().getName(), name));
      }

      commands.put(name, command);
    }

    defaultCommand = commands.get(defaultCommandName);

    Assert.notNull(defaultCommand, "Encryption command for type " + defaultCommandName + " must be present");
  }
    

  /**
   * @param defaultCommandName
   *          the defaultCommandName to set
   */
  public void setDefaultCommandName(String defaultCommandName) {
    this.defaultCommandName = defaultCommandName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.security.crypto.EncryptionService#plainTextCredentials(java
   * .lang.String, org.usergrid.persistence.entities.User, java.util.UUID)
   */
  @Override
  public CredentialsInfo plainTextCredentials(String secret, UUID userId, UUID applicationId) {

    CredentialsInfo credentials = new CredentialsInfo();
    credentials.setRecoverable(true);
    credentials.setSecret(secret);
    credentials.setCryptoChain(new String[] { EncryptionCommand.PLAINTEXT });
    return credentials;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.security.crypto.EncryptionService#defaultEncryptedCredentials
   * (java.lang.String, org.usergrid.persistence.entities.User, java.util.UUID)
   */
  @Override
  public CredentialsInfo defaultEncryptedCredentials(String input, UUID userId, UUID applicationId) {
    CredentialsInfo credentials = new CredentialsInfo();
    credentials.setRecoverable(false);
    credentials.setEncrypted(true);
    credentials.setCryptoChain(new String[] { defaultCommand.getName() });

    credentials.setSecret(encode(defaultCommand.hash(input.getBytes(UTF8), credentials, userId, applicationId)));

    return credentials;

  }
  

  /* (non-Javadoc)
   * @see org.usergrid.security.crypto.EncryptionService#getCommand(java.lang.String)
   */
  @Override
  public EncryptionCommand getCommand(String name) {
    return commands.get(name);
  }

  /* (non-Javadoc)
   * @see org.usergrid.security.crypto.EncryptionService#getDefaultCommand()
   */
  @Override
  public EncryptionCommand getDefaultCommand() {
    return defaultCommand;
  }


  protected String encode(byte[] bytes) {
    return encodeBase64URLSafeString(bytes);
  }

}
