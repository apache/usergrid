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
package org.apache.usergrid.persistence;


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.RandomStringUtils;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.utils.JsonUtils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;



public class EntityDictionaryIT extends AbstractCoreIT {
    private static final Logger LOG = LoggerFactory.getLogger( EntityDictionaryIT.class );


    public EntityDictionaryIT() {
        super();
    }


    @Test
    public void testApplicationDictionaries() throws Exception {
        Application.OAuthProvider provider = new Application.OAuthProvider();
        provider.setClientId( "123456789012.apps.googleusercontent.com" );
        provider.setClientSecret( "abcdefghijklmnopqrstuvwx" );
        provider.setRedirectUris( "https://www.example.com/oauth2callback" );
        provider.setJavaScriptOrigins( "https://www.example.com" );
        provider.setAuthorizationEndpointUrl( "https://accounts.google.com/o/oauth2/auth" );
        provider.setAccessTokenEndpointUrl( "https://accounts.google.com/o/oauth2/token" );
        provider.setVersion( "2.0" );

        LOG.info( "EntityDictionaryIT.testApplicationDictionaries" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        em.addToDictionary( em.getApplicationRef(), "oauthproviders", "google", provider );

        Object o = em.getDictionaryElementValue( em.getApplicationRef(), "oauthproviders", "google" );
        LOG.info( JsonUtils.mapToFormattedJsonString( o ) );
    }


    @Test
    public void testUserDictionaries() throws Exception {
        LOG.info( "EntityDictionaryIT.testUserDictionaries" );



        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );

        Entity user = em.create( "user", properties );
        assertNotNull( user );

        // test plaintext

        CredentialsInfo credentials = new CredentialsInfo();
        credentials.setSecret( "test" );
        credentials.setEncrypted( false );
        credentials.setRecoverable( true );
        credentials.setCryptoChain( new String[] { "plaintext" } );

        em.addToDictionary( user, "credentials", "plaintext", credentials );

        Object o = em.getDictionaryElementValue( user, "credentials", "plaintext" );
        LOG.info( JsonUtils.mapToFormattedJsonString( o ) );

        assertEquals( CredentialsInfo.class, o.getClass() );

        CredentialsInfo returned = ( CredentialsInfo ) o;

        assertEquals( credentials.getSecret(), returned.getSecret() );
        assertEquals( credentials.getEncrypted(), returned.getEncrypted() );
        assertEquals( credentials.getRecoverable(), returned.getRecoverable() );
        assertArrayEquals( credentials.getCryptoChain(), returned.getCryptoChain() );

        // test encrypted but recoverable

        credentials = new CredentialsInfo();
        credentials.setEncrypted( true );
        credentials.setRecoverable( false );
        credentials.setSecret( "salt" );
        credentials.setCryptoChain( new String[] { "sha-1" } );

        em.addToDictionary( user, "credentials", "encrypted", credentials );

        o = em.getDictionaryElementValue( user, "credentials", "encrypted" );
        LOG.info( JsonUtils.mapToFormattedJsonString( o ) );

        assertEquals( CredentialsInfo.class, o.getClass() );
        returned = ( CredentialsInfo ) o;


        assertEquals( credentials.getSecret(), returned.getSecret() );
        assertEquals( credentials.getEncrypted(), returned.getEncrypted() );
        assertEquals( credentials.getRecoverable(), returned.getRecoverable() );
        assertArrayEquals( credentials.getCryptoChain(), returned.getCryptoChain() );
    }

    @Test
    public void testRemoveFromDictionary() throws Exception {
        LOG.info( "EntityDictionaryIT.testRemoveFromDictionary" );

        Application.OAuthProvider provider = new Application.OAuthProvider();
        provider.setClientId( "123456789012.apps.googleusercontent.com" );
        provider.setClientSecret( "abcdefghijklmnopqrstuvwx" );
        provider.setRedirectUris( "https://www.example.com/oauth2callback" );
        provider.setJavaScriptOrigins( "https://www.example.com" );
        provider.setAuthorizationEndpointUrl( "https://accounts.google.com/o/oauth2/auth" );
        provider.setAccessTokenEndpointUrl( "https://accounts.google.com/o/oauth2/token" );
        provider.setVersion( "2.0" );

        LOG.info( "EntityDictionaryIT.testApplicationDictionaries" );


        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        em.addToDictionary( em.getApplicationRef(), "oauthproviders", "google", provider );

        Object o = em.getDictionaryElementValue( em.getApplicationRef(), "oauthproviders", "google" );

        assertNotNull( o );

        em.removeFromDictionary( em.getApplicationRef(),"oauthproviders","google" );

        o = em.getDictionaryElementValue( em.getApplicationRef(), "oauthproviders", "google" );
        assertNull( o );


    }

    @Test
    public void testGetDictionaries() throws Exception {
        LOG.info( "EntityDictionaryIT.testGetDictionaries" );


        Application.OAuthProvider provider = new Application.OAuthProvider();
        provider.setClientId( "123456789012.apps.googleusercontent.com" );
        provider.setClientSecret( "abcdefghijklmnopqrstuvwx" );
        provider.setRedirectUris( "https://www.example.com/oauth2callback" );
        provider.setJavaScriptOrigins( "https://www.example.com" );
        provider.setAuthorizationEndpointUrl( "https://accounts.google.com/o/oauth2/auth" );
        provider.setAccessTokenEndpointUrl( "https://accounts.google.com/o/oauth2/token" );
        provider.setVersion( "2.0" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        em.addToDictionary( em.getApplicationRef(), "oauthproviders", "google", provider );

        Object o = em.getDictionaryElementValue( em.getApplicationRef(), "oauthproviders", "google" );

        assertNotNull( o );
        Set<String> set = em.getDictionaryNames( em.getApplicationRef() );

        assertTrue( set.contains( "oauthproviders" ) );

    }
    @Test
    public void testAddMapToDictionaries() throws Exception {
        LOG.info( "EntityDictionaryIT.testAddMapToDictionaries" );

        Map<String,Object> testMap = new HashMap<String,Object>();

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        testMap.put( "testName","testval" );

        em.addMapToDictionary( em.getApplicationRef(), "testProvider",testMap );

        Object o = em.getDictionaryElementValue( em.getApplicationRef(), "testProvider","testName" );
        assertEquals("testval" , o.toString() );

    }
}
