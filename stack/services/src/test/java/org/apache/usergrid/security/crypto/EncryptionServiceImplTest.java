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
package org.apache.usergrid.security.crypto;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.apache.usergrid.persistence.CredentialsInfo;
import org.apache.usergrid.security.crypto.command.EncryptionCommand;
import org.apache.usergrid.security.crypto.command.Md5HashCommand;
import org.apache.usergrid.security.crypto.command.Sha1HashCommand;

import static org.junit.Assert.assertTrue;


/** @author tnine */
public class EncryptionServiceImplTest {

    @Test(expected = IllegalArgumentException.class)
    public void duplicateCommand() {
        final String duplicate = "foo";

        EncryptionCommand command1 = new EncryptionCommand() {

            @Override
            public byte[] hash( byte[] input, CredentialsInfo info, UUID userId, UUID applicationId ) {
                return null;
            }


            @Override
            public String getName() {
                return duplicate;
            }


            @Override
            public byte[] auth( byte[] input, CredentialsInfo info, UUID userId, UUID applicationId ) {
                return null;
            }
        };

        EncryptionCommand command2 = new EncryptionCommand() {

            @Override
            public byte[] hash( byte[] input, CredentialsInfo info, UUID userId, UUID applicationId ) {
                return null;
            }


            @Override
            public String getName() {
                return duplicate;
            }


            @Override
            public byte[] auth( byte[] input, CredentialsInfo info, UUID userId, UUID applicationId ) {
                return null;
            }
        };

        List<EncryptionCommand> commands = new ArrayList<EncryptionCommand>();
        commands.add( command1 );
        commands.add( command2 );

        EncryptionServiceImpl service = new EncryptionServiceImpl();
        service.setCommands( commands );
        service.init();
    }


    @Test(expected = IllegalArgumentException.class)
    public void missingCryptoCommand() {
        final String duplicate = "foo";

        EncryptionCommand command1 = new EncryptionCommand() {

            @Override
            public byte[] hash( byte[] input, CredentialsInfo info, UUID userId, UUID applicationId ) {
                return null;
            }


            @Override
            public String getName() {
                return duplicate;
            }


            @Override
            public byte[] auth( byte[] input, CredentialsInfo info, UUID userId, UUID applicationId ) {
                return null;
            }
        };

        List<EncryptionCommand> commands = new ArrayList<EncryptionCommand>();
        commands.add( command1 );


        EncryptionServiceImpl service = new EncryptionServiceImpl();

        service.setCommands( commands );
        service.init();


        CredentialsInfo info = new CredentialsInfo();
        info.setCryptoChain( new String[] { "doesnotexist" } );

        service.verify( "irrelevant", info, null, null );
    }


    @Test(expected = IllegalArgumentException.class)
    public void noCommands() {

        EncryptionServiceImpl service = new EncryptionServiceImpl();
        service.init();
    }


    /** Tests legacy md5 support for old imported md5 -> sha-1 passwords */
    @Test
    public void legacyMd5Support() {
        EncryptionServiceImpl impl = new EncryptionServiceImpl();

        Md5HashCommand md5 = new Md5HashCommand();
        Sha1HashCommand sha1 = new Sha1HashCommand();

        List<EncryptionCommand> commands = new ArrayList<EncryptionCommand>( 2 );
        commands.add( md5 );
        commands.add( sha1 );

        impl.setCommands( commands );
        impl.setDefaultCommandName( sha1.getName() );
        impl.init();

        //now encrypt
        String password = "secret";

        CredentialsInfo creds = new CredentialsInfo();
        creds.setHashType( "md5" );
        creds.setEncrypted( true );
        creds.setCipher( "sha-1" );

        //set the secret into the creds statically for the legacy test
        creds.setSecret( "8rpwQiXFx-5nbzIB6iVr9XeeaHc" );


        boolean result = impl.verify( password, creds, null, null );

        assertTrue( "Legacy password verified", result );
    }
}
