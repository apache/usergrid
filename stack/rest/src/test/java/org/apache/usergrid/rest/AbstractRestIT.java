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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.usergrid.java.client.Client;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;

import org.apache.usergrid.persistence.exceptions.ApplicationAlreadyExistsException;
import org.junit.AfterClass;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.usergrid.utils.JsonUtils.mapToFormattedJsonString;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Base class for testing Usergrid Jersey-based REST API. Implementations should model the
 * paths mapped, not the method names. For example, to test the the "password" mapping on
 * applications.users.UserResource for a PUT method, the test method(s) should following the
 * following naming convention: test_[HTTP verb]_[action mapping]_[ok|fail][_[specific
 * failure condition if multiple]
 */
//@ArquillianSuiteDeployment
//@RunWith(Arquillian.class)
public abstract class AbstractRestIT extends JerseyTest {
    private static final Logger LOG = LoggerFactory.getLogger( AbstractRestIT.class );
    private static boolean usersSetup = false;

    protected static TomcatRuntime tomcatRuntime = TomcatRuntime.getInstance();

    protected static final ITSetup setup = ITSetup.getInstance();

    private static ClientConfig clientConfig = new DefaultClientConfig();

    protected static String access_token;

    protected static String adminAccessToken;

    protected static Client client;

    protected static final AppDescriptor descriptor;


    //private static final URI baseURI = setup.getBaseURI();

    protected ObjectMapper mapper = new ObjectMapper();


    public AbstractRestIT() {
        super( descriptor );
    }


    static {
        clientConfig.getFeatures().put( JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE );
        descriptor = new WebAppDescriptor.Builder( "org.apache.usergrid.rest" )
                .clientConfig( clientConfig ).build();
        dumpClasspath( AbstractRestIT.class.getClassLoader() );
    }


//    // We set testable = false so we deploy the archive to the server and test it locally
//    @org.jboss.arquillian.container.test.api.Deployment( testable = false )
//    public static WebArchive createTestArchive() {
//
//        // we use the MavenImporter from shrinkwrap to just produce whatever maven would build then test with it
//
//        // set maven to be in offline mode
//
//        System.setProperty( "org.apache.maven.offline", "true" );
//        return ShrinkWrap.create(MavenImporter.class)
//            .loadPomFromFile( "pom.xml", "arquillian-tomcat" )
//            .importBuildOutput()
//            .as( WebArchive.class );
//    }


    @AfterClass
    public static void teardown() {
        access_token = null;
        usersSetup = false;
        adminAccessToken = null;
    }


    public ApplicationInfo appInfo = null;
    public OrganizationInfo orgInfo = null;
    public String orgAppPath = null;
    public String username = null;
    public String userEmail = null;

    /** Quick fix to get old style test working again. We need them! */
    @Before
    public void setupOrgApp() throws Exception {

        setup.getMgmtSvc().setup();

        String rand = RandomStringUtils.randomAlphanumeric(5);

        orgInfo = setup.getMgmtSvc().getOrganizationByName("test-organization");
        if ( orgInfo == null  ) {
            OrganizationOwnerInfo orgOwnerInfo = setup.getMgmtSvc().createOwnerAndOrganization(
                "test-organization" + rand, "test", "test", "test@usergrid.org", rand, false, false);
            orgInfo = orgOwnerInfo.getOrganization();
        }

        String appname =  "app-" + rand;
        try {
            appInfo = setup.getMgmtSvc().createApplication(orgInfo.getUuid(),appname);
        }catch(ApplicationAlreadyExistsException e){
            LOG.error("Failed to create application"+appname+", maybe this is ok", e);
        }
        refreshIndex( orgInfo.getName(), appInfo.getName() );

        orgAppPath = appInfo.getName() + "/";
        adminToken();

        setupUsers();
        refreshIndex( orgInfo.getName(), appInfo.getName() );

        loginClient();
        refreshIndex( orgInfo.getName(), appInfo.getName() );

        LOG.info( "acquiring token" );
        access_token = userToken( userEmail, "sesame" );
        LOG.info( "with token: {}", access_token );

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


    protected void setupUsers() throws Exception {

        LOG.info("Entering setupUsers");

//        if ( usersSetup ) {
//            LOG.info("Leaving setupUsers: already setup");
//            return;
//        }

        String rand = RandomStringUtils.randomAlphanumeric(5);
        username = "user-" + rand;
        userEmail = username + "@example.com";

        createUser( username, userEmail, "sesame", "User named " + rand);

        //usersSetup = true;
        LOG.info("Leaving setupUsers, setup user: " + userEmail );
    }


    public void loginClient() throws Exception {

        String appNameOnly = appInfo.getName().split("/")[1];

        client = new Client( "test-organization", appNameOnly ).withApiUrl(
                UriBuilder.fromUri( "http://localhost/" ).port(tomcatRuntime.getPort() ).build().toString() );

        org.apache.usergrid.java.client.response.ApiResponse response =
            client.authorizeAppUser( userEmail, "sesame" );

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
        try {
            return new URI("http://localhost:" + tomcatRuntime.getPort());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error determining baseURI", e);
        }
    }


    public static void logNode( JsonNode node ) {
        if ( LOG.isInfoEnabled() ) // - protect against unnecessary call to formatter
        {
            LOG.info("Node: " + mapToFormattedJsonString( node ) );
        }
    }


    protected String userToken( String name, String password ) throws Exception {

        try {
            JsonNode node = mapper.readTree( resource().path( orgAppPath + "token" )
                    .queryParam("grant_type", "password")
                    .queryParam( "username", name )
                    .queryParam( "password", password ).accept( MediaType.APPLICATION_JSON )
                    .get( String.class ));

            String userToken = node.get( "access_token" ).textValue();
            LOG.info( "returning user token: {}", userToken );
            return userToken;

        } catch ( Exception e ) {
            LOG.debug("Error getting user token", e);
            throw e;
        }
    }


    public void createUser( String username, String email, String password, String name ) {

        try {
            JsonNode node = mapper.readTree( resource().path( orgAppPath + "token" )
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

        resource().path( orgAppPath + "users" )
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
        JsonNode node = mapper.readTree(
            resource().path( String.format( orgAppPath + "users/%s/password", username ) )
                .queryParam("access_token", adminAccessToken)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(String.class, data));
    }


    /** Acquire the management token for the test@usergrid.com user with the default password */
    protected String adminToken() {
        adminAccessToken = mgmtToken( "test@usergrid.com", "test" );
        return adminAccessToken;
    }


    /** Get the super user's access token */
    protected String superAdminToken() {
        return mgmtToken( "superuser", "test" );
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


    /**
     * Get the property "name" from the entity at the specified index
     * @param response
     * @param index
     * @return
     */
    protected String getEntityName(JsonNode response, int index){
        return getEntity(response, index).get( "name" ).asText();
    }


    /** Get the error response */
    protected JsonNode getError( JsonNode response ) {
        return response.get( "error" );
    }


    /** convenience to return a ready WebResource.Builder in a single call */
    protected WebResource.Builder appPath( String path ) {
        return resource().path( orgAppPath + "" + path )
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

        // set the value remotely (in the Usergrid instance running in Tomcat classloader)
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

        // set the values remotely (in the Usergrid instance running in Tomcat classloader)
        resource().path( "/testproperties" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( props );
    }


    /** Get all management service properties from the Tomcat instance of the service. */
    public Map<String, String> getRemoteTestProperties() {
        return resource().path( "/testproperties" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( Map.class );
    }


    protected void refreshIndex( UUID appId ) {

        LOG.debug("Refreshing index for appId {}", appId );

        try {

            resource().path( "/refreshindex" )
                .queryParam( "app_id", appId.toString() )
                .accept( MediaType.APPLICATION_JSON )
                .post();

        } catch ( Exception e) {
            LOG.debug("Error refreshing index", e);
            return;
        }

        LOG.debug("Refreshed index for appId {}", appId );
    }

    //TODO: move refresh index into context so that we automatically refresh the indexs without needing to call
    //different values of context.
    public void refreshIndex( String orgName, String appName ) {

        LOG.debug("Refreshing index for app {}/{}", orgName, appName );

        // be nice if somebody accidentally passed in orgName/appName
        appName = appName.contains("/") ? appInfo.getName().split("/")[1] : appName;

        try {

            resource().path( "/refreshindex" )
                .queryParam( "org_name", orgName )
                .queryParam( "app_name", appName )
                .accept( MediaType.APPLICATION_JSON )
                .post();

        } catch ( Exception e) {
            LOG.debug("Error refreshing index", e);
            return;
        }

        LOG.debug("Refreshed index for app {}/{}", orgName, appName );
    }

}
