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
package org.apache.usergrid.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.crypto.RsaProvider;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.NewOrgAppAdminRule;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.security.sso.ApigeeSSO2Provider;
import org.apache.usergrid.security.tokens.TokenInfo;
import org.apache.usergrid.security.tokens.exceptions.BadTokenException;
import org.junit.*;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import static org.apache.commons.codec.binary.Base64.decodeBase64;


/**
 * Created by Dave Johnson (snoopdave@apache.org) on 10/25/16.
 */
public class ApigeeSSO2ProviderIT {
    private static final Logger logger = LoggerFactory.getLogger(ApigeeSSO2ProviderIT.class);

    @ClassRule
    public static final ServiceITSetup setup = new ServiceITSetupImpl();


    @Test
    public void testBasicOperation() throws Exception {

        // create keypair
        KeyPair kp = RsaProvider.generateKeyPair(1024);
        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();

        // create provider with private key
        ApigeeSSO2Provider provider = new MockApigeeSSO2Provider();
        provider.setManagement( setup.getMgmtSvc() );
        provider.setPublicKey( publicKey );

        // create user, claims and a token for those things
        User user = createUser();
        long exp = System.currentTimeMillis() + 10000;
        Map<String, Object> claims = createClaims( user.getUsername(), user.getEmail(), exp );
        String token = Jwts.builder().setClaims(claims).signWith( SignatureAlgorithm.RS256, privateKey).compact();

        // test that provider can validate the token, get user, return token info
        TokenInfo tokenInfo = provider.validateAndReturnTokenInfo( token, 86400L );
        Assert.assertNotNull( tokenInfo );
    }


    @Test
    public void testExpiredToken() throws Exception {

        // create keypair
        KeyPair kp = RsaProvider.generateKeyPair(1024);
        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();

        // create provider with private key
        ApigeeSSO2Provider provider = new MockApigeeSSO2Provider();
        provider.setManagement( setup.getMgmtSvc() );
        provider.setPublicKey( publicKey );

        // create user, claims and a token for those things
        User user = createUser();
        long exp = System.currentTimeMillis() - 1500;
        Map<String, Object> claims = createClaims( user.getUsername(), user.getEmail(), exp );
        String token = Jwts.builder()
            .setClaims(claims)
            .setExpiration( new Date() )
            .signWith( SignatureAlgorithm.RS256, privateKey)
            .compact();

        Thread.sleep(500); // wait for claims to timeout

        // test that token is expired
        try {
            provider.validateAndReturnTokenInfo( token, 86400L );
            Assert.fail("Should have failed due to expired token");

        } catch ( BadTokenException e ) {
            Assert.assertTrue( e.getCause() instanceof ExpiredJwtException );
        }
    }


    @Test
    public void testMalformedToken() throws Exception {

        // create keypair
        KeyPair kp = RsaProvider.generateKeyPair(1024);
        PublicKey publicKey = kp.getPublic();

        // create provider with private key
        ApigeeSSO2Provider provider = new MockApigeeSSO2Provider();
        provider.setManagement( setup.getMgmtSvc() );
        provider.setPublicKey( publicKey );

        // test that token is malformed
        try {
            provider.getClaims( "{;aklsjd;fkajsd;fkjasd;lfkj}" );
            Assert.fail("Should have failed due to malformed token");

        } catch ( BadTokenException e ) {
            Assert.assertTrue( e.getCause() instanceof MalformedJwtException );
        }
    }

    @Test
    public void testNewPublicKeyFetch() throws Exception {

        // create old keypair
        KeyPair kp = RsaProvider.generateKeyPair(1024);
        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();

        // create new keypair
        KeyPair kpNew = RsaProvider.generateKeyPair(1024);
        PublicKey publicKeyNew = kpNew.getPublic();
        PrivateKey privateKeyNew = kpNew.getPrivate();

        // create mock provider with old and old key
        MockApigeeSSO2ProviderNewKey provider = new MockApigeeSSO2ProviderNewKey( publicKey, publicKeyNew );
        provider.setManagement( setup.getMgmtSvc() );

        // create user, claims and a token for those things. Sign with new public key
        User user = createUser();
        long exp = System.currentTimeMillis() + 10000;
        Map<String, Object> claims = createClaims( user.getUsername(), user.getEmail(), exp );
        String token = Jwts.builder().setClaims(claims).signWith( SignatureAlgorithm.RS256, privateKeyNew).compact();

        // test that provider can validate the token, get user, return token info
        TokenInfo tokenInfo = provider.validateAndReturnTokenInfo( token, 86400L );
        Assert.assertNotNull( tokenInfo );

        // assert that provider called for new key
        Assert.assertTrue( provider.isGetPublicKeyCalled() );


        // try it again, but this time it should fail due to freshness value

        provider.setPublicKey( publicKey ); // set old key

        // test that signature exception thrown
        try {
            provider.validateAndReturnTokenInfo( token, 86400L );
            Assert.fail("Should have failed due to bad signature");

        } catch ( BadTokenException e ) {
            Assert.assertTrue( e.getCause() instanceof SignatureException );
        }

    }


    @Test
    public void testBadSignature() throws Exception {

        // create old keypair
        KeyPair kp = RsaProvider.generateKeyPair(1024);
        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();

        // create new keypair
        KeyPair kpNew = RsaProvider.generateKeyPair(1024);
        PrivateKey privateKeyNew = kpNew.getPrivate();

        // create mock provider with old public key
        ApigeeSSO2Provider provider = new MockApigeeSSO2ProviderNewKey( publicKey, publicKey );
        provider.setManagement( setup.getMgmtSvc() );

        // create user, claims and a token for those things. Sign with new public key
        User user = createUser();
        long exp = System.currentTimeMillis() + 10000;
        Map<String, Object> claims = createClaims( user.getUsername(), user.getEmail(), exp );
        String token = Jwts.builder().setClaims(claims).signWith( SignatureAlgorithm.RS256, privateKeyNew).compact();

        // test that signature exception thrown
        try {
            provider.validateAndReturnTokenInfo( token, 86400L );
            Assert.fail("Should have failed due to bad signature");

        } catch ( BadTokenException e ) {
            Assert.assertTrue( e.getCause() instanceof SignatureException );
        }

    }

    private User createUser() throws Exception {
        String rando = RandomStringUtils.randomAlphanumeric( 10 );
        String username = "user_" + rando;
        String email = username + "@example.com";
        Map<String, Object> properties = new HashMap<String, Object>() {{
            put( "username", username );
            put( "email", email );
        }};
        EntityManager em = setup.getEmf().getEntityManager( setup.getEmf().getManagementAppId() );
        Entity entity = em.create( "user", properties );

        return em.get( new SimpleEntityRef( User.ENTITY_TYPE, entity.getUuid() ), User.class );
    }


    private Map<String, Object> createClaims(final String username, final String email, long exp ) {
        return new HashedMap<String, Object>() {{
                put("jti","c7df0339-3847-450b-a925-628ef237953a");
                put("sub","b6d62259-217b-4e96-8f49-e00c366e4fed");
                put("scope","size = 5");
                put("client_id", "dummy1");
                put("azp","dummy2");
                put("grant_type" ,"password");
                put("user_id","b6d62259-217b-4e96-8f49-e00c366e4fed");
                put("origin","usergrid");
                put("user_name", username );
                put("email", email);
                put("rev_sig","dfe5d0d3");
                put("exp", exp);
                put("iat", System.currentTimeMillis());
                put("iss", "https://jwt.example.com/token");
                put("zid","uaa");
                put("aud"," size = 6");
            }};
    }
}

class MockApigeeSSO2Provider extends ApigeeSSO2Provider {
    private static final Logger logger = LoggerFactory.getLogger(MockApigeeSSO2Provider.class);

    @Override
    public PublicKey getPublicKey(String keyUrl ) {
        return publicKey;
    }

    @Override
    public void setPublicKey( PublicKey publicKey ) {
        this.publicKey = publicKey;
    }
}


class MockApigeeSSO2ProviderNewKey extends ApigeeSSO2Provider {
    private static final Logger logger = LoggerFactory.getLogger(MockApigeeSSO2Provider.class);

    private PublicKey newKey;
    private boolean getPublicKeyCalled = false;

    public MockApigeeSSO2ProviderNewKey( PublicKey oldKey, PublicKey newKey ) {
        this.publicKey = oldKey;
        this.newKey = newKey;
        this.properties = new Properties();
    }

    @Override
    public PublicKey getPublicKey( String keyUrl ) {
        getPublicKeyCalled = true;
        return newKey;
    }

    public boolean isGetPublicKeyCalled() {
        return getPublicKeyCalled;
    }
}


