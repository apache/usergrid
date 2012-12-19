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

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.junit.Test;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.persistence.entities.User;

/**
 * @author tnine
 *
 */
public class Md5HashCommandTest {

  @Test
  public void hashAndAuthCorrect() throws UnsupportedEncodingException {
    
    String test = "I'm a  test password";
    
    byte[] hashed = DigestUtils.md5(test.getBytes("UTF-8"));
    
    Md5HashCommand command = new  Md5HashCommand();
    
    CredentialsInfo info = new CredentialsInfo();
    
    User user = new User();
    
    UUID applicationId = UUID.randomUUID();
    
    byte[] results = command.hash(test.getBytes("UTF-8"), info, user, applicationId);
    
    assertArrayEquals(hashed, results);
    
    byte[] authed = command.auth(test.getBytes("UTF-8"), info, user, applicationId);
    
    assertArrayEquals(results, authed);
    
  }

}
