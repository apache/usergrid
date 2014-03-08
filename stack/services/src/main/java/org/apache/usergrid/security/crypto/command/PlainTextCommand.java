/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.security.crypto.command;


import java.util.UUID;

import org.springframework.stereotype.Component;
import org.apache.usergrid.persistence.CredentialsInfo;


/**
 * A no-op provider
 *
 * @author tnine
 */
@Component("org.apache.usergrid.security.crypto.command.PlainTextCommand")
public class PlainTextCommand extends SaltedHasherCommand {

    /* (non-Javadoc)
     * @see org.apache.usergrid.security.crypto.command.EncryptionCommand#hash(byte[],
     * org.apache.usergrid.persistence.CredentialsInfo, java.util.UUID, java.util.UUID)
     */
    @Override
    public byte[] hash( byte[] input, CredentialsInfo info, UUID userId, UUID applicationId ) {
        return input;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.security.crypto.command.EncryptionCommand#auth(byte[],
     * org.apache.usergrid.persistence.CredentialsInfo, org.apache.usergrid.persistence.entities.User, java.util.UUID)
     */
  /* (non-Javadoc)
   * @see org.apache.usergrid.security.crypto.command.EncryptionCommand#auth(byte[],
   * org.apache.usergrid.persistence.CredentialsInfo, java.util.UUID, java.util.UUID)
   */
    @Override
    public byte[] auth( byte[] input, CredentialsInfo info, UUID userId, UUID applicationId ) {
        return input;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.security.crypto.command.EncryptionCommand#getName()
     */
    @Override
    public String getName() {
        return PLAINTEXT;
    }
}
