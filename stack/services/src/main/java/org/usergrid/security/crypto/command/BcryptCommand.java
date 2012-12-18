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

import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.persistence.entities.User;
import org.usergrid.utils.BCrypt;

/**
 * Simple bcrypt command.  Runtime encoding of bytes is expected to convert to a UTF8 string
 * @author tnine
 *
 */
@Component
public class BcryptCommand extends EncryptionCommand {

  private static final Charset UTF8 = Charset.forName("UTF-8");
  
  
  @Override
  public byte[] hash(byte[] input, CredentialsInfo info, User user, UUID applicationId) {
    return BCrypt.hashpw(new String(input, UTF8), BCrypt.gensalt()).getBytes();
  }


  /* (non-Javadoc)
   * @see org.usergrid.security.crypto.command.EncryptionCommand#getName()
   */
  @Override
  public String getName() {
    return BCRYPT;
  }

}
