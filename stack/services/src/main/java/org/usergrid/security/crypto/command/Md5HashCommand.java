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
import org.usergrid.persistence.entities.User;

/**
 * @author tnine
 *
 */
@Component
public class Md5HashCommand extends SaltedHasherCommand {

  
  @Override
  public byte[] hash(byte[] input, CredentialsInfo info, User user, UUID applicationId) {
     byte[]  data = maybeSalt(input, applicationId, user.getUuid());
     
     return DigestUtils.md5(data);
  
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
    return MD5;
  }

 
}
