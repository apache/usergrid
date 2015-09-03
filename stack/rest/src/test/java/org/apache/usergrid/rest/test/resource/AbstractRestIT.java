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
package org.apache.usergrid.rest.test.resource;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.rest.TomcatRuntime;
import org.apache.usergrid.rest.test.resource.endpoints.ApplicationsResource;
import org.apache.usergrid.rest.test.resource.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource.endpoints.OrganizationResource;
import org.apache.usergrid.rest.test.resource.endpoints.mgmt.ManagementResource;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.apache.usergrid.rest.test.resource.state.ClientContext;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Rule;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;



/**
 * Base class for REST tests.
 */
//@RunWith( Arquillian.class )
public class AbstractRestIT extends JerseyTest {

    private static ClientConfig clientConfig = new ClientConfig();

    public static TomcatRuntime tomcatRuntime = TomcatRuntime.getInstance();

    @Rule
    public ClientSetup clientSetup = new ClientSetup( this.getBaseURI().toString() );

    protected static final Application descriptor = new Application();


    @Override
    protected Application configure() {
        return descriptor;
    }

    protected ObjectMapper mapper = new ObjectMapper();

    static {
        clientConfig.register( new JacksonFeature() );
        dumpClasspath( AbstractRestIT.class.getClassLoader() );
    }


//    //We set testable = false so we deploy the archive to the server and test it locally
//    @Deployment( testable = false )
//    public static WebArchive createTestArchive() {
//
//        //we use the MavenImporter from shrinkwrap to just produce whatever maven would build then test with it
//
//        //set maven to be in offline mode
//
//        System.setProperty( "org.apache.maven.offline", "true" );
//
//        return ShrinkWrap.create( MavenImporter.class ).loadPomFromFile( "pom.xml", "arquillian-tomcat" )
//                         .importBuildOutput().as( WebArchive.class );
//    }

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

    protected URI getBaseURI() {
        try {
            return new URI("http://localhost:" + tomcatRuntime.getPort());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error determining baseURI", e);
        }
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        final URI baseURI = getBaseURI();
        return (uri, deploymentContext) -> new TestContainer() {
            @Override
            public ClientConfig getClientConfig() {
                return clientConfig;
            }

            @Override
            public URI getBaseUri() {
                return baseURI;
            }

            @Override
            public void start() {
                // noop
            }

            @Override
            public void stop() {
                // noop
            }
        };
    }

    ///myorg/
    protected OrganizationResource org(){
        return clientSetup.restClient.org( clientSetup.getOrganization().getName() );
    }

    //myorg/myapp
    protected ApplicationsResource app(){
        return clientSetup.restClient.org(clientSetup.getOrganization().getName()).app( clientSetup.getAppName() );

    }

    protected ManagementResource management(){
        return clientSetup.restClient.management();
    }

    protected NamedResource pathResource(String path){ return clientSetup.restClient.pathResource( path );}

    protected String getOrgAppPath(String additionalPath){
        return clientSetup.orgName + "/" + clientSetup.appName + "/" + (additionalPath !=null ? additionalPath : "");
    }

    protected ClientContext context(){
        return this.clientSetup.getRestClient().getContext();
    }


    protected Token getAppUserToken(String username, String password){
        return this.app().token().post( new Token( username, password ) );
    }

    public void refreshIndex() {
        //TODO: add error checking and logging
        clientSetup.refreshIndex();
    }


    /**
     * Takes in the expectedStatus message and the expectedErrorMessage then compares it to the ClientErrorException
     * to make sure that we got what we expected.
     * @param expectedStatus
     * @param expectedErrorMessage
     * @param uie
     */
    public void errorParse(int expectedStatus, String expectedErrorMessage, ClientErrorException uie){
        assertEquals(expectedStatus,uie.getResponse().getStatus());
        JsonNode errorJson = uie.getResponse().readEntity( JsonNode.class );
        assertEquals( expectedErrorMessage, errorJson.get( "error" ).asText() );

    }


    protected Token getAdminToken(String username, String password) {
        Token token = new Token(username, password);
        return this.clientSetup.getRestClient().management().token()
            .post( false, Token.class, token, null, false );
    }

    protected Token getAdminToken() {
        Token token = new Token(this.clientSetup.getUsername(), this.clientSetup.getPassword());
        return this.clientSetup.getRestClient().management().token()
            .post( false, Token.class, token, null, false);
    }
    public Map<String, Object> getRemoteTestProperties() {
        return clientSetup.getRestClient().testPropertiesResource().get().getProperties();
    }

    /**
     * Sets a management service property locally and remotely.
     */
    public void setTestProperty(String key, Object value) {
        // set the value remotely (in the Usergrid instance running in Tomcat classloader)
        Entity props = new Entity();
        props.put(key, value);
        clientSetup.getRestClient().testPropertiesResource().post(props);

    }

    public void setTestProperties(Map<String, Object> props) {
        Entity properties = new Entity();
        // set the values locally (in the Usergrid instance here in the JUnit classloader
        for (String key : props.keySet()) {
            properties.put(key, props.get(key));

        }

        // set the values remotely (in the Usergrid instance running in Tomcat classloader)
        clientSetup.getRestClient().testPropertiesResource().post(properties);
    }


}
