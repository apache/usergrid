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
import org.usergrid.persistence.entities.User;

/**
 * 
 * @author tnine
 *
 */
public interface EncryptionService {

  /**
   * Using the credentials and the input secret provided, return a base 64 encoded representation of the encrypted secret
   * @param inputSecret
   * @param creds
   * @param user
   * @param applicationId
   * @return
   */
  public String encryptSecret(String inputSecret, CredentialsInfo creds, User user, UUID applicationId);
  
  /**
   * Generate a plain text credentials info with the given type.  Used for storing oAuth tokens and prehashes mongo passwords etc
   * @param text
   * @return
   */
  public CredentialsInfo plainTextCredentials(String secret,  User user, UUID applicationId);
  
  /**
   * Generate credentials info using the system default encryption command.  Used for passwords and other types
   * @param input
   * @return
   */
  public CredentialsInfo defaultEncryptedCredentials(String secret,  User user, UUID applicationId);
  
  
  
}
