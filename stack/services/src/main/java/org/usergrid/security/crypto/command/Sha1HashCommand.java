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

import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.persistence.entities.User;

/**
 * @author tnine
 *
 */
@Component
public class Sha1HashCommand extends SaltedHasherCommand {

  private static final Logger logger = LoggerFactory.getLogger(Sha1HashCommand.class);
  
  
  @Override
  public byte[] hash(byte[] input, CredentialsInfo info, User user, UUID applicationId) {
      java.security.MessageDigest d = null;
      try {
        d = java.security.MessageDigest.getInstance("SHA-1");
      } catch (NoSuchAlgorithmException e) {
        logger.error( "Unable to get SHA1 algorithm", e);
        throw new RuntimeException(e);
      }
      d.reset();
      
      d.update(maybeSalt(input, applicationId, user.getUuid()));
      return d.digest();
  
  }
  
  
  /* (non-Javadoc)
   * @see org.usergrid.security.crypto.command.EncryptionCommand#auth(byte[], org.usergrid.persistence.CredentialsInfo, org.usergrid.persistence.entities.User, java.util.UUID)
   */
  @Override
  public byte[] auth(byte[] input, CredentialsInfo info, User user, UUID applicationId) {
    return hash(input, info, user, applicationId);
  }


  /* (non-Javadoc)
   * @see org.usergrid.security.crypto.command.EncryptionCommand#getName()
   */
  @Override
  public String getName() {
    return SHA1;
  }

}
