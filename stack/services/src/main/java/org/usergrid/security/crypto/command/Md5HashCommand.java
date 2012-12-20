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

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import org.usergrid.persistence.CredentialsInfo;

/**
 * @author tnine
 *
 */
@Component("org.usergrid.security.crypto.command.Md5HashCommand")
public class Md5HashCommand extends SaltedHasherCommand {

  
  /* (non-Javadoc)
   * @see org.usergrid.security.crypto.command.EncryptionCommand#hash(byte[], org.usergrid.persistence.CredentialsInfo, java.util.UUID, java.util.UUID)
   */
  @Override
  public byte[] hash(byte[] input, CredentialsInfo info,  UUID userId, UUID applicationId) {
     byte[]  data = maybeSalt(input, applicationId, userId);
     
     return DigestUtils.md5(data);
  
  }
  

  /* (non-Javadoc)
   * @see org.usergrid.security.crypto.command.EncryptionCommand#auth(byte[], org.usergrid.persistence.CredentialsInfo, java.util.UUID, java.util.UUID)
   */
  @Override
  public byte[] auth(byte[] input, CredentialsInfo info, UUID userId, UUID applicationId) {
    return hash(input, info, userId, applicationId);
  }


  /* (non-Javadoc)
   * @see org.usergrid.security.crypto.command.EncryptionCommand#getName()
   */
  @Override
  public String getName() {
    return MD5;
  }

 
}
