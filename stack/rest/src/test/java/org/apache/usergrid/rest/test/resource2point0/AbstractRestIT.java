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
package org.apache.usergrid.rest.test.resource2point0;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import org.apache.usergrid.rest.TomcatRuntime;
import org.apache.usergrid.rest.test.resource2point0.endpoints.ApplicationsResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.OrganizationResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt.ManagementResource;
import org.apache.usergrid.rest.test.resource2point0.model.Token;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;
import org.junit.Rule;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;



/**
 * Base class for REST tests.
 */
//@RunWith( Arquillian.class )
public class AbstractRestIT extends JerseyTest {

    private static ClientConfig clientConfig = new DefaultClientConfig();

    public static TomcatRuntime tomcatRuntime = TomcatRuntime.getInstance();



    @Rule
    public ClientSetup clientSetup = new ClientSetup( this.getBaseURI().toString() );

    protected static final AppDescriptor descriptor;

    public AbstractRestIT() {
        super( descriptor );
    }


    protected ObjectMapper mapper = new ObjectMapper();

    static {
        clientConfig.getFeatures().put( JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE );
        descriptor = new WebAppDescriptor.Builder( "org.apache.usergrid.rest" )
                .clientConfig( clientConfig ).build();
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

    @Override
    protected URI getBaseURI() {
        try {
            return new URI("http://localhost:" + tomcatRuntime.getPort());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error determining baseURI", e);
        }
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new com.sun.jersey.test.framework.spi.container.external.ExternalTestContainerFactory();
    }

    ///myorg/
    protected OrganizationResource org(){
        return clientSetup.restClient.org( clientSetup.getOrganization().getName() );
    }

    //myorg/myapp
    protected ApplicationsResource app(){
        return clientSetup.restClient.org(clientSetup.getOrganization().getName()).app(clientSetup.getAppName());

    }

    protected ManagementResource management(){
        return clientSetup.restClient.management();
    }

    protected ClientContext context(){
        return this.clientSetup.getRestClient().getContext();
    }


    protected Token getAppUserToken(String username, String password){
        return this.app().token().post(new Token(username,password));
    }

    public void refreshIndex() {
        //TODO: add error checking and logging
        clientSetup.refreshIndex();
    }


    /**
     * Takes in the expectedStatus message and the expectedErrorMessage then compares it to the UniformInterfaceException
     * to make sure that we got what we expected.
     * @param expectedStatus
     * @param expectedErrorMessage
     * @param uie
     */
    public void errorParse(int expectedStatus, String expectedErrorMessage, UniformInterfaceException uie){
        assertEquals(expectedStatus,uie.getResponse().getStatus());
        JsonNode errorJson = uie.getResponse().getEntity( JsonNode.class );
        assertEquals( expectedErrorMessage, errorJson.get( "error" ).asText() );

    }


    protected Token getAdminToken(String username, String password){
        return this.clientSetup.getRestClient().management().token().post(Token.class,
                new Token(username, password)
        );
    }

    protected Token getAdminToken(){
        return this.clientSetup.getRestClient().management().token().post(Token.class,
                new Token(this.clientSetup.getUsername(),this.clientSetup.getUsername())
        );
    }
}
