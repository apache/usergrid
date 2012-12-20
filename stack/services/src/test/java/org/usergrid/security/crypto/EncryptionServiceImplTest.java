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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.security.crypto.command.EncryptionCommand;

/**
 * @author tnine
 * 
 */
public class EncryptionServiceImplTest {

  @Test(expected = IllegalArgumentException.class)
  public void duplicateCommand() {
    final String duplicate = "foo";

    EncryptionCommand command1 = new EncryptionCommand() {

      @Override
      public byte[] hash(byte[] input, CredentialsInfo info, UUID userId, UUID applicationId) {
        return null;
      }

      @Override
      public String getName() {
        return duplicate;
      }

      @Override
      public byte[] auth(byte[] input, CredentialsInfo info, UUID userId, UUID applicationId) {
        return null;
      }
    };

    EncryptionCommand command2 = new EncryptionCommand() {

      @Override
      public byte[] hash(byte[] input, CredentialsInfo info, UUID userId, UUID applicationId) {
        return null;
      }

      @Override
      public String getName() {
        return duplicate;
      }

      @Override
      public byte[] auth(byte[] input, CredentialsInfo info, UUID userId, UUID applicationId) {
        return null;
      }
    };

    List<EncryptionCommand> commands = new ArrayList<EncryptionCommand>();
    commands.add(command1);
    commands.add(command2);

    EncryptionServiceImpl service = new EncryptionServiceImpl();
    service.setCommands(commands);
    service.init();
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingCryptoCommand() {
    final String duplicate = "foo";

    EncryptionCommand command1 = new EncryptionCommand() {

      @Override
      public byte[] hash(byte[] input, CredentialsInfo info, UUID userId, UUID applicationId) {
        return null;
      }

      @Override
      public String getName() {
        return duplicate;
      }

      @Override
      public byte[] auth(byte[] input, CredentialsInfo info, UUID userId, UUID applicationId) {
        return null;
      }
    };

    List<EncryptionCommand> commands = new ArrayList<EncryptionCommand>();
    commands.add(command1);
    

    EncryptionServiceImpl service = new EncryptionServiceImpl();

    service.setCommands(commands);
    service.init();
    
    
    CredentialsInfo info = new CredentialsInfo();
    info.setCryptoChain(new String[]{"doesnotexist"});
    
    service.verify("irrelevant", info, null, null);
  }

  
  @Test(expected = IllegalArgumentException.class)
  public void noCommands() {
    
    EncryptionServiceImpl service = new EncryptionServiceImpl();
    service.init();

  }

}
