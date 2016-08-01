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

package org.apache.usergrid.rest.management;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.RsaProvider;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.RestClient;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.security.sso.ApigeeSSO2Provider;
import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.security.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by ayeshadastagiri on 7/20/16.
 */
@Ignore("Need to figure out a way to set the public key for Mock server.")
public class ExternalSSOEnabledIT extends AbstractRestIT {

    Key key;
    PublicKey publicKey;
    PrivateKey privateKey;
    String compactJws;
    String username = "SSOadminuser" + UUIDUtils.newTimeUUID();
    ApigeeSSO2Provider apigeeSSO2ProviderTest;
    //SSO2 implementation
    public static final String USERGRID_EXTERNAL_SSO_ENABLED = "usergrid.external.sso.enabled";
    public static final String USERGRID_EXTERNAL_PROVIDER =    "usergrid.external.sso.provider";

    public ExternalSSOEnabledIT() throws Exception {

    }

    @Before
    public void setup() throws NoSuchAlgorithmException {
        generateKey();
    }

    private void generateKey() {
        KeyPair kp = RsaProvider.generateKeyPair(1024);
        publicKey = kp.getPublic();
        privateKey = kp.getPrivate();
    }

    private String genrateToken(){
        Map<String, Object> claims = new HashedMap<String, Object>();
        claims.put("jti","c7df0339-3847-450b-a925-628ef237953a");
        claims.put("sub","b6d62259-217b-4e96-8f49-e00c366e4fed");
        claims.put("scope","size = 5");
        claims.put("client_id", "edgecli");
        claims.put("azp","edgecli");
        claims.put("grant_type" ,"password");
        claims.put("user_id","b6d62259-217b-4e96-8f49-e00c366e4fed");
        claims.put( "origin","usergrid");
        claims.put("user_name","AyeshaSSOUser");
        claims.put("email", "adastagiri+ssotesting@apigee.com");
        claims.put( "rev_sig","dfe5d0d3");
        claims.put("iat","1466550862");
        claims.put("exp", System.currentTimeMillis() + 1000);
        claims.put("iss", "https://login.apigee.com/oauth/token");
        claims.put( "zid","uaa");
        claims.put( "aud"," size = 6");
        claims.put("grant_type","password");

        String jwt = Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.RS256, privateKey).compact();
        return jwt;

    }

    @Test
    public void SuperUserTestsFor() throws NoSuchAlgorithmException {

        // create a admin user.
        RestClient restClient = clientSetup.getRestClient();

        //Create adminUser values
        Entity adminUserPayload = new Entity();
        adminUserPayload.put("username", "TestUser");
        adminUserPayload.put("name", username);
        adminUserPayload.put("email", "adastagiri+ssotesting@apigee.com");
        adminUserPayload.put("password", username);

        //create adminUser
        ApiResponse adminUserEntityResponse = management().orgs().org(clientSetup.getOrganizationName()).users().post(ApiResponse.class, adminUserPayload);

        Entity adminUserResponse = new Entity(adminUserEntityResponse);
        //verify that the response contains the correct data
        assertNotNull(adminUserResponse);
        assertEquals("TestUser", adminUserResponse.get("username"));

        Map<String, String> props = new HashMap<String, String>();

        props.put( USERGRID_EXTERNAL_SSO_ENABLED, "true" );
        props.put( USERGRID_EXTERNAL_PROVIDER, "apigee" );
        pathResource( "testproperties" ).post( props );

        // /management/me --> superuser and query params --> Generate a super usesr token.
        Map<String, Object> loginInfo = new HashMap<String, Object>() {{
            put( "username", "superuser" );
            put( "password", "superpassword" );
            put( "grant_type", "password" );
        }};
        ApiResponse postResponse2 = pathResource( "management/token" ).post( false,ApiResponse.class,loginInfo );
        assertTrue(postResponse2.getAccessToken() != null );


        // /orgs  create an org with superuser credentials.
        // /management/me --> superuser and query params --> Generate a super usesr token.
        Map<String, Object> orgDetails = new HashMap<String, Object>() {{
            put( "email", "adastagiri+ssotesting@apigee.com" );
            put( "name", "testuser" );
            put( "organization", username );
        }};

        context().getToken().put("access_token",postResponse2.getAccessToken());
        postResponse2 = pathResource( "management/orgs" ).post( true,ApiResponse.class,orgDetails);
        assertTrue(postResponse2.getData() != null);

        postResponse2 = pathResource("management/orgs").get(ApiResponse.class,true);
        assertTrue(postResponse2 != null);


        compactJws = genrateToken();

        SpringResource.getInstance().getAppContext().getBean(ApigeeSSO2Provider.class).setPublicKey( publicKey  );
        context().getToken().put("access_token",compactJws);
        // /management/me --> admin user and jwt token. Return the user information and "token" should have jwt token.
        JsonNode responseToken = management().me().get(JsonNode.class,true);
        assertTrue(responseToken.get("access_token") != null);


        // /management/me --> admin and query params --> Generate a super usesr token.
        Map<String, Object> loginInfo1 = new HashMap<String, Object>() {{
            put( "username", "TestUser" );
            put( "password", username );
            put( "grant_type", "password" );
        }};

        // /managment/token -> adminusername and password --> should fail.
        ApiResponse postResponse1 = pathResource("management/token").post(false, ApiResponse.class,loginInfo1);
//        fail( "SSO Integration is enabled, Admin users must login via provider: "+ USERGRID_EXTERNAL_SSO_PROVIDER_URL);




    }
}
