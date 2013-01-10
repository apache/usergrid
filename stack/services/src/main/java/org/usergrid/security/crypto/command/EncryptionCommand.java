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
package org.usergrid.security.crypto.command;

import java.util.UUID;

import org.usergrid.persistence.CredentialsInfo;

/**
 * Simple chain of responsibility algorithm for password cryptography.  
 * 
 * @author tnine
 *
 */
public abstract class EncryptionCommand {

  /**
   * The default implementations provided by subclasses
   */
  public static final String SHA1 = "sha-1";
  public static final String MD5 = "md5";
  public static final String PLAINTEXT = "plaintext";
  public static final String BCRYPT = "bcrypt";

  
  /**
   * Perform the required hash on the input bytes, using the CredentialsInfo and the user provided.  Subclasses
   * should invoke the next in the chain if required.
   * 
   * @param input
   * @param info
   * @param userId
   * @param applicationId
   * @return
   */
  public abstract byte[] hash(byte[] input, CredentialsInfo info, UUID userId, UUID applicationId);
  
  /**
   * Perform authentication from the given input bytes.  Return the bytes that should be used for comparison
   * @param input
   * @param info
   * @param userId
   * @param applicationId
   * @return
   */
  public abstract byte[] auth(byte[] input, CredentialsInfo info, UUID userId, UUID applicationId);
  
  /**
   * Get the name of this encryption command
   * @return
   */
  public abstract String getName();
  

}
