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

import static org.apache.commons.codec.binary.Base64.decodeBase64;

import java.nio.charset.Charset;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.utils.BCrypt;

/**
 * Simple bcrypt command.  Runtime encoding of bytes is expected to convert to a UTF8 string
 * @author tnine
 *
 */
@Component("org.usergrid.security.crypto.command.BcryptCommand")
public class BcryptCommand extends EncryptionCommand {

  private static final Charset UTF8 = Charset.forName("UTF-8");
  
  /**
   * 2^12 seems to be far too slow on modern processors.  Approximately 1.8 seconds per crypt on a 2.2 GHZ intel i7.  2^11 is less than 1 second
   */
  private int defaultIterations = 2^11;
  
  
  /* (non-Javadoc)
   * @see org.usergrid.security.crypto.command.EncryptionCommand#hash(byte[], org.usergrid.persistence.CredentialsInfo, java.util.UUID, java.util.UUID)
   */
  @Override
  public byte[] hash(byte[] input, CredentialsInfo info, UUID userId, UUID applicationId) {
    return  BCrypt.hashpw(new String(input, UTF8), BCrypt.gensalt(defaultIterations)).getBytes();
  }


 

  /* (non-Javadoc)
   * @see org.usergrid.security.crypto.command.EncryptionCommand#auth(byte[], org.usergrid.persistence.CredentialsInfo, java.util.UUID, java.util.UUID)
   */
  public byte[] auth(byte[] input, CredentialsInfo info, UUID userId, UUID applicationId){
    //our existing has the salt in it, extract it and re-use it
    
    String infoSecret = info.getSecret();
    
    Assert.notNull(infoSecret, "The credentials info must have a bcrypt compatible secret to perform auth");
    
    String existing = new String(decodeBase64(infoSecret), UTF8);
    
    return BCrypt.hashpw(new String(input, UTF8), existing).getBytes(UTF8);
  }
  
  /**
   * Set the number of default iterations to use.  If the password was previously hashed, the number of iterations 
   * will be in the CredentialsInfo.  Otherwise the default is used
   * @param defaultIterations the defaultIterations to set
   */
  public void setDefaultIterations(int defaultIterations) {
    this.defaultIterations = defaultIterations;
  }


  /* (non-Javadoc)
   * @see org.usergrid.security.crypto.command.EncryptionCommand#getName()
   */
  @Override
  public String getName() {
    return BCRYPT;
  }

}
