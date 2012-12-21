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

import java.util.UUID;

import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.security.crypto.command.EncryptionCommand;

/**
 * 
 * @author tnine
 *
 */
public interface EncryptionService {

  /**
   * Using the credentials and the input secret provided, return a CredentialsInfo for comparison with the existing credentials info
   * 
   * @param inputSecret The secret to use for authentication
   * @param creds The credentials read from the data store
   * @param userId The id of the user
   * @param applicationId The application id of the parent application
   * @return True if the input secret is correct, false otherwise
   */
  public boolean verify(String inputSecret, CredentialsInfo creds, UUID userId, UUID applicationId);
  
  
  /**
   * Generate a plain text credentials info with the given type.  Used for storing oAuth tokens and prehashes mongo passwords etc
   * @param secret The secret to store.  Note this WILL NOT perform any encryption and or hashing on the secret 
   * @param userId The user's id (optional)
   * @param applicationId The application id (optional)
   * @return
   */
  public CredentialsInfo plainTextCredentials(String secret,  UUID userId, UUID applicationId);
  
  /**
   * Generate credentials info using the system default encryption command.  Used for passwords and other types
   * @param secret The secret to encrypt
   * @param userId The user's id (optional)
   * @param applicationId The application id (optional)
   * @return
   */
  public CredentialsInfo defaultEncryptedCredentials(String secret,  UUID userId, UUID applicationId);
  
    
  /**
   * Get the command supplied by name.  Could return null.  Should only be used by tools that are VERY sure what they're doing
   * @param name
   * @return
   */
  public EncryptionCommand getCommand(String name);
  
  /**
   * Return the default encryption name
   * @return
   */
  public EncryptionCommand getDefaultCommand();
  
  
  
}
