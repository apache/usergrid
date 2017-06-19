/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.token;

import net.jcip.annotations.NotThreadSafe;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.token.impl.TokenSerializationImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;

import com.google.inject.Inject;

import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.Assert.assertEquals;


@RunWith( ITRunner.class )
@UseModules( { TestTokenModule.class } )
@NotThreadSafe
public class TokenSerializationTest {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    public TokenSerialization tokenSerialization;

    @Test
    public void putTokenInfo() {

        UUID uuid = UUIDGenerator.newTimeUUID();
        Map<String, Object> tokenDetails = new HashMap<>();
        tokenDetails.put(TokenSerializationImpl.TOKEN_TYPE, "test-token");

        ByteBuffer principalKeyBuffer = ByteBuffer.wrap("test-principal".getBytes());

        tokenSerialization.putTokenInfo(uuid, tokenDetails, principalKeyBuffer, 60);

    }

    @Test
    public void getTokenInfo() {

        UUID uuid = UUIDGenerator.newTimeUUID();
        Map<String, Object> tokenDetails = new HashMap<>();
        tokenDetails.put(TokenSerializationImpl.TOKEN_TYPE, "test-token");

        ByteBuffer principalKeyBuffer = ByteBuffer.wrap("test-principal".getBytes());

        tokenSerialization.putTokenInfo(uuid, tokenDetails, principalKeyBuffer, 60);

        Map<String, Object> returnedDetails = tokenSerialization.getTokenInfo(uuid);

        assertEquals(
            tokenDetails.get(TokenSerializationImpl.TOKEN_TYPE),
            returnedDetails.get(TokenSerializationImpl.TOKEN_TYPE)
        );
    }

    @Test
    public void updateAccessTokenTime() {

        UUID uuid = UUIDGenerator.newTimeUUID();
        Map<String, Object> tokenDetails = new HashMap<>();
        tokenDetails.put(TokenSerializationImpl.TOKEN_TYPE, "test-token");

        long accessedTime = System.currentTimeMillis()-1000000;
        long inactiveTime = accessedTime+2000000;

        tokenDetails.put(TokenSerializationImpl.TOKEN_ACCESSED, accessedTime);
        tokenDetails.put(TokenSerializationImpl.TOKEN_INACTIVE, inactiveTime);

        ByteBuffer principalKeyBuffer = ByteBuffer.wrap("test-principal".getBytes());

        tokenSerialization.putTokenInfo(uuid, tokenDetails, principalKeyBuffer, 60);

        Map<String, Object> returnedDetails = tokenSerialization.getTokenInfo(uuid);

        assertEquals(
            accessedTime,
            returnedDetails.get(TokenSerializationImpl.TOKEN_ACCESSED)
        );

        long newAccessedTime = System.currentTimeMillis();
        long newInactiveTime = newAccessedTime+1000000;

        tokenSerialization.updateTokenAccessTime(uuid, newAccessedTime,newInactiveTime, 1200);

        returnedDetails = tokenSerialization.getTokenInfo(uuid);

        assertEquals(
            newAccessedTime,
            returnedDetails.get(TokenSerializationImpl.TOKEN_ACCESSED)
        );
    }

    @Test
    public void deleteTokens() {

        UUID uuid = UUIDGenerator.newTimeUUID();
        Map<String, Object> tokenDetails = new HashMap<>();
        tokenDetails.put(TokenSerializationImpl.TOKEN_TYPE, "test-token");

        ByteBuffer principalKeyBuffer = ByteBuffer.wrap("test-principal".getBytes());

        tokenSerialization.putTokenInfo(uuid, tokenDetails, principalKeyBuffer, 60);

        Map<String, Object> returnedDetails = tokenSerialization.getTokenInfo(uuid);

        assertEquals(
            tokenDetails.get(TokenSerializationImpl.TOKEN_TYPE),
            returnedDetails.get(TokenSerializationImpl.TOKEN_TYPE)
        );

        tokenSerialization.deleteTokens(Collections.singletonList(uuid), principalKeyBuffer);

        returnedDetails = tokenSerialization.getTokenInfo(uuid);

        assertEquals(
            0,
            returnedDetails.size()
        );
    }

    @Test
    public void revokeToken() {

        UUID uuid = UUIDGenerator.newTimeUUID();
        Map<String, Object> tokenDetails = new HashMap<>();
        tokenDetails.put(TokenSerializationImpl.TOKEN_TYPE, "test-token");

        ByteBuffer principalKeyBuffer = ByteBuffer.wrap("test-principal".getBytes());

        tokenSerialization.putTokenInfo(uuid, tokenDetails, principalKeyBuffer, 60);

        Map<String, Object> returnedDetails = tokenSerialization.getTokenInfo(uuid);

        assertEquals(
            tokenDetails.get(TokenSerializationImpl.TOKEN_TYPE),
            returnedDetails.get(TokenSerializationImpl.TOKEN_TYPE)
        );

        tokenSerialization.revokeToken(uuid, principalKeyBuffer);

        returnedDetails = tokenSerialization.getTokenInfo(uuid);

        assertEquals(
            0,
            returnedDetails.size()
        );
    }

    @Test
    public void getTokensForPrincipal() {

        UUID uuid = UUIDGenerator.newTimeUUID();
        Map<String, Object> tokenDetails = new HashMap<>();
        tokenDetails.put(TokenSerializationImpl.TOKEN_TYPE, "test-token");

        String principal = "test-principal-"+System.currentTimeMillis();

        ByteBuffer principalKeyBuffer = ByteBuffer.wrap(principal.getBytes());

        tokenSerialization.putTokenInfo(uuid, tokenDetails, principalKeyBuffer, 60);

        principalKeyBuffer = ByteBuffer.wrap(principal.getBytes());

        List<UUID> returnTokens = tokenSerialization.getTokensForPrincipal(principalKeyBuffer);

        assertEquals(
            returnTokens.get(0),
            uuid
        );
    }

}
