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


import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.junit.Test;
import org.apache.usergrid.persistence.CredentialsInfo;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import com.yammer.metrics.reporting.ConsoleReporter;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.junit.Assert.assertArrayEquals;


/** @author tnine */
public class BcryptCommandTest {

    /** Tests bcrypt hashing with a default number of rounds */
    @Test
    public void hashWithNoExistingImpl() throws UnsupportedEncodingException {

        int cryptIterations = 2 ^ 4;

        BcryptCommand command = new BcryptCommand();
        command.setDefaultIterations( cryptIterations );

        String baseString = "I am a test password for hashing";

        CredentialsInfo info = new CredentialsInfo();


        byte[] result = command.hash( baseString.getBytes( "UTF-8" ), info, null, null );


        String stringResults = encodeBase64URLSafeString( result );


        info.setSecret( stringResults );

        //now check we can auth with the same phrase
        byte[] authed = command.auth( baseString.getBytes( "UTF-8" ), info, null, null );


        assertArrayEquals( result, authed );
    }


    /** Tests bcrypt hashing then auth.  This should fail if there's no secret. */
    @Test(expected = IllegalArgumentException.class)
    public void authNoSecret() throws UnsupportedEncodingException {

        int cryptIterations = 2 ^ 4;

        BcryptCommand command = new BcryptCommand();
        command.setDefaultIterations( cryptIterations );

        String baseString = "I am a test password for hashing";

        CredentialsInfo info = new CredentialsInfo();


        command.hash( baseString.getBytes( "UTF-8" ), info, null, null );


        //now check we can't auth since the CI doesn't have any secret on it
        command.auth( baseString.getBytes( "UTF-8" ), info, null, null );
    }


    /** Tests bcrypt hashing fails when the existing secret is wrong */
    @Test(expected = IllegalArgumentException.class)
    public void authInvalidSecret() throws UnsupportedEncodingException {

        int cryptIterations = 2 ^ 4;

        BcryptCommand command = new BcryptCommand();
        command.setDefaultIterations( cryptIterations );

        String baseString = "I am a test password for hashing";

        CredentialsInfo info = new CredentialsInfo();


        byte[] result = command.hash( baseString.getBytes( "UTF-8" ), info, null, null );

        info.setSecret( "I'm a junk secret that's not bcrypted" );

        //now check we can auth with the same phrase
        byte[] authed = command.auth( baseString.getBytes( "UTF-8" ), info, null, null );


        assertArrayEquals( result, authed );
    }


    /**
     * Tests bcrypt hashing with a default number of rounds.  Note that via the console output, this test should take
     * about 5 seconds to run since we want to force 500 ms per authentication attempt with bcrypt
     */
    @Test
    public void testHashRoundSpeed() throws UnsupportedEncodingException {

        int cryptIterations = 2 ^ 11;
        int numberOfTests = 10;

        BcryptCommand command = new BcryptCommand();
        command.setDefaultIterations( cryptIterations );

        String baseString = "I am a test password for hashing";

        CredentialsInfo info = new CredentialsInfo();


        UUID user = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        byte[] result = command.hash( baseString.getBytes( "UTF-8" ), info, user, applicationId );


        String stringResults = encodeBase64URLSafeString( result );

        info.setSecret( stringResults );

        Timer timer = Metrics.newTimer( BcryptCommandTest.class, "hashtimer" );

        for ( int i = 0; i < numberOfTests; i++ ) {
            TimerContext timerCtx = timer.time();

            //now check we can auth with the same phrase
            byte[] authed = command.auth( baseString.getBytes( "UTF-8" ), info, user, applicationId );

            timerCtx.stop();


            assertArrayEquals( result, authed );
        }

        /**
         * Print out the data
         */
        ConsoleReporter reporter = new ConsoleReporter( Metrics.defaultRegistry(), System.out, MetricPredicate.ALL );

        reporter.run();
    }
}
