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

import static org.junit.Assert.assertArrayEquals;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.usergrid.persistence.CredentialsInfo;

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
    
    
    byte[] results = command.hash(test.getBytes("UTF-8"), info, null, null);
    
    assertArrayEquals(hashed, results);
    
    byte[] authed = command.auth(test.getBytes("UTF-8"), info, null, null);
    
    assertArrayEquals(results, authed);
    
  }

}
