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
package org.apache.usergrid.rest;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.java.client.Client;
import static org.apache.usergrid.utils.JsonUtils.mapToFormattedJsonString;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import org.junit.AfterClass;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for testing Usergrid Jersey-based REST API. Implementations should model the paths mapped, not the method
 * names. For example, to test the the "password" mapping on applications.users.UserResource for a PUT method, the test
 * method(s) should following the following naming convention: test_[HTTP verb]_[action mapping]_[ok|fail][_[specific
 * failure condition if multiple]
 */
@Concurrent()
public abstract class AbstractRestIT extends JerseyTest {
    private static final Logger LOG = LoggerFactory.getLogger( AbstractRestIT.class );
    private static boolean usersSetup = false;


    private static ClientConfig clientConfig = new DefaultClientConfig();

    protected static String access_token;

    protected static String adminAccessToken;

    protected static Client client;

    protected static final AppDescriptor descriptor;

    @ClassRule
    public static ITSetup setup = new ITSetup( RestITSuite.cassandraResource );

    private static final URI baseURI = setup.getBaseURI();

    protected ObjectMapper mapper = new ObjectMapper();


    static {
        clientConfig.getFeatures().put( JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE );
        descriptor = new WebAppDescriptor.Builder( "org.apache.usergrid.rest" ).clientConfig( clientConfig ).build();
        dumpClasspath( AbstractRestIT.class.getClassLoader() );
    }


    @AfterClass
    public static void teardown() {
        access_token = null;
        usersSetup = false;
        adminAccessToken = null;
    }


    /** Hook to get the token for our base user */
    @Before
    public void acquireToken() throws Exception {

        setupUsers();

        reindex("test-organization", "test-app");

        LOG.info( "acquiring token" );
        access_token = userToken( "ed@anuff.com", "sesame" );
        LOG.info( "with token: {}", access_token );

        loginClient();

        reindex("test-organization", "test-app");
    }


    public void reindex( UUID appId ) {
        resource().path( "/testreindex" )
            .queryParam( "app_id", appId.toString() )
            .accept( MediaType.APPLICATION_JSON )
            .post();
    }


    public void reindex( String orgName, String appName ) {
        resource().path( "/testreindex" )
            .queryParam( "org_name", orgName )
            .queryParam( "app_name", appName )
            .accept( MediaType.APPLICATION_JSON )
            .post();
    }


    public static void dumpClasspath( ClassLoader loader ) {
        System.out.println( "Classloader " + loader + ":" );

        if ( loader instanceof URLClassLoader ) {
            URLClassLoader ucl = ( URLClassLoader ) loader;
            System.out.println( "\t" + Arrays.toString( ucl.getURLs() ) );
        }
        else {
            System.out.println( "\t(cannot display components as not a URLClassLoader)" );
        }

        if ( loader.getParent() != null ) {
            dumpClasspath( loader.getParent() );
        }
    }


    public AbstractRestIT() throws TestContainerException {
        super( descriptor );
    }


    protected void setupUsers() {

        if ( usersSetup ) {
            return;
        }

        //
        createUser( "edanuff", "ed@anuff.com", "sesame", "Ed Anuff" ); // client.setApiUrl(apiUrl);

        usersSetup = true;
    }


    public void loginClient() throws Exception {
        // now create a client that logs in ed

        // TODO T.N. This is a filthy hack and I should be ashamed of it (which
        // I am). There's a bug in the grizzly server when it's restarted per
        // test, and until we can upgrade versions this is the workaround. Backs
        // off with each attempt to allow the server to catch up


        setUserPassword( "ed@anuff.com", "sesame" );

        client = new Client( "test-organization", "test-app" ).withApiUrl(
                UriBuilder.fromUri( "http://localhost/" ).port( setup.getTomcatPort() ).build().toString() );

        org.apache.usergrid.java.client.response.ApiResponse response = client.authorizeAppUser( "ed@anuff.com", "sesame" );

        assertTrue( response != null && response.getError() == null );
    }


    @Override
    protected TestContainerFactory getTestContainerFactory() {
        // return new
        // com.sun.jersey.test.framework.spi.container.grizzly2.web.GrizzlyWebTestContainerFactory();
        return new com.sun.jersey.test.framework.spi.container.external.ExternalTestContainerFactory();
    }


    @Override
    protected URI getBaseURI() {
        return baseURI;
    }


    public static void logNode( JsonNode node ) {
        if ( LOG.isInfoEnabled() ) // - protect against unnecessary call to formatter
        {
            LOG.info("Node: " + mapToFormattedJsonString( node ) );
        }
    }


    protected String userToken( String name, String password ) throws Exception {

        setUserPassword( "ed@anuff.com", "sesame" );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/token" ).queryParam( "grant_type", "password" )
                .queryParam( "username", name ).queryParam( "password", password ).accept( MediaType.APPLICATION_JSON )
                .get( String.class ));

        String userToken = node.get( "access_token" ).textValue();
        LOG.info( "returning user token: {}", userToken );
        return userToken;
    }


    public void createUser( String username, String email, String password, String name ) {

        try {
            JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/token" )
                .queryParam( "grant_type", "password" )
                .queryParam( "username", username )
                .queryParam( "password", password )
                .accept( MediaType.APPLICATION_JSON )
                .get( String.class ));
            if ( getError( node ) == null ) {
                return;
            }
        }
        catch ( Exception ex ) {
            LOG.error( "Miss on user. Creating." );
        }

        adminToken();

        Map<String, Object> payload = (Map<String, Object>)
            hashMap( "email", (Object)email )
            .map( "username", username )
            .map( "name", name )
            .map( "password", password )
            .map( "pin", "1234" );

        resource().path( "/test-organization/test-app/users" )
            .queryParam( "access_token", adminAccessToken )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON )
            .post( payload );
    }


    public void setUserPassword( String username, String password ) throws IOException {
        Map<String, String> data = new HashMap<String, String>();
        data.put( "newpassword", password );

        adminToken();

        // change the password as admin. The old password isn't required
        JsonNode node = mapper.readTree( resource().path( String.format( "/test-organization/test-app/users/%s/password", username ) )
                .queryParam( "access_token", adminAccessToken ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, data ));

        assertNull( getError( node ) );
    }


    /** Acquire the management token for the test@usergrid.com user with the default password */
    protected String adminToken() {
        adminAccessToken = mgmtToken( "test@usergrid.com", "test" );
        return adminAccessToken;
    }


    /** Get the super user's access token */
    protected String superAdminToken() {
        return mgmtToken( "superuser", "superpassword" );
    }


    /** Acquire the management token for the test@usergrid.com user with the given password */
    protected String mgmtToken( String user, String password ) {

        ObjectMapper mapper = new ObjectMapper();

        JsonNode node;
        try {
            node = mapper.readTree( resource().path( "/management/token" )
                .queryParam( "grant_type", "password" )
                .queryParam( "username", user )
                .queryParam( "password", password )
                .accept( MediaType.APPLICATION_JSON )
                .get( String.class ));

        } catch (IOException ex) {
            throw new RuntimeException("Unable to parse response", ex);
        }

        String mgmToken = node.get( "access_token" ).textValue();
        LOG.info( "got mgmt token: {}", mgmToken );
        return mgmToken;
    }


    /** Get the entity from the entity array in the response */
    protected JsonNode getEntity( JsonNode response, int index ) {
        if ( response == null ) {
            return null;
        }

        JsonNode entities = response.get( "entities" );

        if ( entities == null ) {
            return null;
        }

        int size = entities.size();

        if ( size <= index ) {
            return null;
        }

        return entities.get( index );
    }


    /** Get the entity from the entity array in the response */
    protected JsonNode getEntity( JsonNode response, String name ) {
        return response.get( "entities" ).get( name );
    }


    /** Get the uuid from the entity at the specified index */
    protected UUID getEntityId( JsonNode response, int index ) {
        return UUID.fromString( getEntity( response, index ).get( "uuid" ).asText() );
    }


    /** Get the error response */
    protected JsonNode getError( JsonNode response ) {
        return response.get( "error" );
    }


    /** convenience to return a ready WebResource.Builder in a single call */
    protected WebResource.Builder appPath( String path ) {
        return resource().path( "/test-organization/test-app/" + path )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE );
    }


    /** convenience to return a ready WebResource.Builder in a single call */
    protected WebResource.Builder path( String path ) {
        return resource().path( path )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE );
    }


    /** Sets a management service property locally and remotely. */
    public void setTestProperty( String key, String value ) {

        // set the value locally (in the Usergrid instance here in the JUnit classloader
        setup.getMgmtSvc().getProperties().setProperty( key, value );

        // set the value remotely (in the Usergrid instance running in Jetty classloader)
        Map<String, String> props = new HashMap<String, String>();
        props.put( key, value );
        resource().path( "/testproperties" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( props );
    }


    /** Set a management service properties locally and remotely. */
    public void setTestProperties( Map<String, String> props ) {

        // set the values locally (in the Usergrid instance here in the JUnit classloader
        for ( String key : props.keySet() ) {
            setup.getMgmtSvc().getProperties().setProperty( key, props.get( key ) );
        }

        // set the values remotely (in the Usergrid instance running in Jetty classloader)
        resource().path( "/testproperties" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( props );
    }


    /** Get all management service properties from th Jetty instance of the service. */
    public Map<String, String> getRemoteTestProperties() {
        return resource().path( "/testproperties" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( Map.class );
    }
}
