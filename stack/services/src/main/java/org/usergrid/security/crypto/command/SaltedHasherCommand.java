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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.security.salt.SaltProvider;

/**
 * Class that provides salt options
 * 
 * @author tnine
 * 
 */
public abstract class SaltedHasherCommand extends EncryptionCommand {

  private static final Charset UTF8 = Charset.forName("UTF-8");

  @Autowired
  private SaltProvider provider;

  /**
   * Possibly salt the input bytes if the salt provider gives us back data.
   * 
   * @param input
   * @param applicationId
   * @param userId
   * @return
   */
  protected byte[] maybeSalt(byte[] input, UUID applicationId, UUID userId) {
    String salt = provider.getSalt(applicationId, userId);

    /**
     * Nothing to do
     */
    if (salt == null || salt.length() == 0) {
      return input;
    }

    byte[] saltBytes = salt.getBytes(UTF8);

    byte[] outputBytes = new byte[input.length + saltBytes.length];

    // wrap it for ease of use
    ByteBuffer buff = ByteBuffer.wrap(outputBytes);

    buff.put(saltBytes);
    buff.put(input);

    return outputBytes;
  }

}
