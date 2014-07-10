/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.webapp;


import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.chop.api.BaseResult;
import org.apache.usergrid.chop.api.RestParams;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.RunnerBuilder;
import org.apache.usergrid.chop.webapp.coordinator.rest.*;
import org.safehaus.jettyjam.utils.TestParams;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.*;


/**
 * Common tests run in unit and test mode.
 */
class ChopUiTestUtils {

    private final static Map<String, String> WRONG_USER_PARAMS = new HashMap<String, String>() {
        {
            put( "user", "user" );
            put( "pwd", "foo" );
        }
    };

    private final static Map<String, String> QUERY_PARAMS = new HashMap<String, String>() {
        {
            put( RestParams.USERNAME, "user" );
            put( RestParams.PASSWORD, "pass" );
            put( RestParams.COMMIT_ID, UUID.randomUUID().toString() );
            put( RestParams.MODULE_VERSION, "2.0.0-SNAPSHOT" );
            put( RestParams.MODULE_ARTIFACTID, "chop-example" );
            put( RestParams.MODULE_GROUPID, "org.apache.usergrid.chop" );
            put( RestParams.VCS_REPO_URL, "git@github.com:usergrid/usergrid.git" );
            put( RestParams.TEST_PACKAGE, "org.apache.usergrid.chop.example" );
        }
    };


    static void testRunManagerNext( TestParams testParams ) {
        Integer next = testParams.addQueryParameters( QUERY_PARAMS )
                .setEndpoint( RunManagerResource.ENDPOINT )
                .newWebResource()
                .path( "/next" )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .accept( MediaType.APPLICATION_JSON )
                .get( Integer.class );

        assertEquals( 1, next.intValue() );
    }


    static void testRunnerRegistryList(TestParams testParams) {
        List<Runner> runnerList = testParams.addQueryParameters(QUERY_PARAMS)
                .setEndpoint(RunnerRegistryResource.ENDPOINT)
                .newWebResource()
                .path("/list")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<Runner>>() {
                });

        assertNotNull(runnerList);
        assertEquals(0, runnerList.size());
    }


    static void testRunnerRegistryUnregister(TestParams testParams) {
        Boolean result = testParams.addQueryParameters(QUERY_PARAMS)
                .setEndpoint(RunnerRegistryResource.ENDPOINT)
                .newWebResource()
                .path("/unregister")
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Boolean.class);

        assertFalse(result);
    }


    static void testRunnerRegistryRegister(TestParams testParams) {
        /*
         * Even though in test mode the runner is not used, a runner must
         * be sent over because the method is expecting the Runner as JSON.
         */
        RunnerBuilder builder = new RunnerBuilder();
        builder.setTempDir(".")
                .setServerPort(19023)
                .setUrl("https://localhost:19023")
                .setHostname("foobar")
                .setIpv4Address("127.0.0.1");

        Boolean result = testParams.addQueryParameters(QUERY_PARAMS)
                .setEndpoint(RunnerRegistryResource.ENDPOINT)
                .newWebResource()
                .path("/register")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Boolean.class, builder.getRunner());

        assertFalse(result);
    }


    static void testRunnerRegistrySequence(TestParams testParams) {
        /*
         * ------------------------------------------------------------
         * Let's register a runner first before we query for it
         * ------------------------------------------------------------
         */

        String commitId = UUID.randomUUID().toString();
        String hostname = RandomStringUtils.randomAlphabetic(8);

        RunnerBuilder builder = new RunnerBuilder();
        builder.setTempDir(".")
                .setServerPort(19023)
                .setUrl("https://localhost:19023")
                .setHostname(hostname)
                .setIpv4Address("127.0.0.1");

        Boolean result = testParams.addQueryParameters(QUERY_PARAMS)
                .setEndpoint(RunnerRegistryResource.ENDPOINT)
                .newWebResource(null)
                .queryParam(RestParams.COMMIT_ID, commitId)
                .path("/register")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Boolean.class, builder.getRunner());

        assertTrue(result);

        /*
         * ------------------------------------------------------------
         * Let's see if we can get the runner back from the registry
         * ------------------------------------------------------------
         */
        List<Runner> runnerList = testParams
                .setEndpoint(RunnerRegistryResource.ENDPOINT)
                .newWebResource(null)
                .queryParam(RestParams.COMMIT_ID, commitId)
                .path("/list")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<Runner>>() {
                });

        assertNotNull(runnerList);
        assertEquals(1, runnerList.size());

        Runner runner = runnerList.get(0);
        assertEquals(19023, runner.getServerPort());
        assertEquals("https://localhost:19023", runner.getUrl());
        assertEquals(hostname, runner.getHostname());
        assertEquals("127.0.0.1", runner.getIpv4Address());
        assertEquals(".", runner.getTempDir());

        /*
         * ------------------------------------------------------------
         * Let's unregister the runner from the registry and check
         * ------------------------------------------------------------
         */
        result = testParams
                .newWebResource(null)
                .queryParam(RestParams.RUNNER_URL, runner.getUrl())
                .path("/unregister")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Boolean.class);

        assertTrue(result);

        /*
         * ------------------------------------------------------------
         * Let's make sure we do NOT get the runner from the registry
         * ------------------------------------------------------------
         */
        runnerList.clear();
        runnerList = testParams
                .setEndpoint(RunnerRegistryResource.ENDPOINT)
                .newWebResource(null)
                .queryParam(RestParams.COMMIT_ID, commitId)
                .path("/list")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<Runner>>() {
                });

        assertNotNull(runnerList);
        assertEquals(0, runnerList.size());
    }


    static void testSetup(TestParams testParams) {
        ClientResponse response = testParams.addQueryParameters(QUERY_PARAMS)
                .setEndpoint(SetupResource.ENDPOINT)
                .newWebResource()
                .queryParam(RestParams.RUNNER_COUNT, "5")
                .path("/stack")
                .type(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        assertEquals("\"JarNotFound\"", response.getEntity(String.class));
    }


    static void testDestroy( TestParams testParams ) {
        ClientResponse response = testParams.addQueryParameters( QUERY_PARAMS )
                .setEndpoint( DestroyResource.ENDPOINT )
                .newWebResource()
                .path( "/stack" )
                .type( MediaType.APPLICATION_JSON )
                .accept( MediaType.APPLICATION_JSON )
                .post( ClientResponse.class );

        assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );

        assertEquals( "\"JarNotFound\"", response.getEntity( String.class ) );
    }


    static void testStart(TestParams testParams) {
        BaseResult result = testParams
                .setEndpoint(StartResource.ENDPOINT)
                .newWebResource()
                .accept(MediaType.APPLICATION_JSON)
                .post(BaseResult.class);

        assertEquals(result.getEndpoint(), StartResource.ENDPOINT);
    }


    static void testReset(TestParams testParams) {
        BaseResult result = testParams
                .setEndpoint(ResetResource.ENDPOINT)
                .newWebResource()
                .accept(MediaType.APPLICATION_JSON)
                .post(BaseResult.class);

        assertEquals(result.getEndpoint(), ResetResource.ENDPOINT);
    }


    static void testStop(TestParams testParams) {
        BaseResult result = testParams
                .setEndpoint(StopResource.ENDPOINT)
                .newWebResource()
                .accept(MediaType.APPLICATION_JSON)
                .post(BaseResult.class);

        assertEquals(result.getEndpoint(), StopResource.ENDPOINT);
    }


    static void testUpload(TestParams testParams) throws Exception {

        FormDataMultiPart part = new FormDataMultiPart();

        part.field( RestParams.COMMIT_ID, QUERY_PARAMS.get( RestParams.COMMIT_ID ) );
        part.field( RestParams.MODULE_GROUPID, QUERY_PARAMS.get( RestParams.MODULE_GROUPID ) );
        part.field( RestParams.MODULE_ARTIFACTID, QUERY_PARAMS.get( RestParams.MODULE_ARTIFACTID ) );
        part.field( RestParams.MODULE_VERSION, QUERY_PARAMS.get( RestParams.MODULE_VERSION ) );
        part.field( RestParams.USERNAME, QUERY_PARAMS.get( RestParams.USERNAME ) );
        part.field( RestParams.VCS_REPO_URL, "ssh://git@stash.safehaus.org:7999/chop/main.git" );
        part.field( RestParams.TEST_PACKAGE, QUERY_PARAMS.get( RestParams.TEST_PACKAGE ) );
        part.field( RestParams.MD5, "d7d4829506f6cb8c0ab2da9cb1daca02" );

        File tmpFile = File.createTempFile("runner", "jar");
        FileInputStream in = new FileInputStream( tmpFile );
        FormDataBodyPart body = new FormDataBodyPart( RestParams.CONTENT, in, MediaType.APPLICATION_OCTET_STREAM_TYPE );
        part.bodyPart( body );

        ClientResponse response = testParams.addQueryParameters( QUERY_PARAMS )
                .setEndpoint( UploadResource.ENDPOINT )
                .newWebResource()
                .path( "/runner" )
                .type( MediaType.MULTIPART_FORM_DATA )
                .accept( MediaType.TEXT_PLAIN )
                .post( ClientResponse.class, part );

        assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );

        assertEquals( UploadResource.SUCCESSFUL_TEST_MESSAGE, response.getEntity( String.class ) );

        tmpFile.delete();
    }


    static void testStoreResults( TestParams testParams ) throws Exception {
        FormDataMultiPart part = new FormDataMultiPart();
        File tmpFile = File.createTempFile("results", "tmp");
        FileInputStream in = new FileInputStream( tmpFile );
        FormDataBodyPart body = new FormDataBodyPart( RestParams.CONTENT, in, MediaType.APPLICATION_OCTET_STREAM_TYPE );
        part.bodyPart( body );

        ClientResponse response = testParams.addQueryParameters( QUERY_PARAMS )
                .setEndpoint( RunManagerResource.ENDPOINT )
                .newWebResource()
                .queryParam( RestParams.RUNNER_HOSTNAME, "localhost" )
                .queryParam( RestParams.RUN_ID, "112316437" )
                .queryParam( RestParams.RUN_NUMBER, "3" )
                .path( "/store" )
                .type( MediaType.MULTIPART_FORM_DATA_TYPE )
                .accept( MediaType.APPLICATION_JSON )
                .post( ClientResponse.class, part );

        tmpFile.delete();

        assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );

        assertEquals( UploadResource.SUCCESSFUL_TEST_MESSAGE, response.getEntity( String.class ) );
    }


    static void testRunCompleted( TestParams testParams ) {
        ClientResponse response = testParams.addQueryParameters( QUERY_PARAMS )
                .setEndpoint( RunManagerResource.ENDPOINT )
                .newWebResource()
                .queryParam( RestParams.RUNNER_HOSTNAME, "localhost" )
                .queryParam( RestParams.RUN_NUMBER, "1" )
                .queryParam( RestParams.TEST_CLASS, "org.apache.usergrid.chop.example.MechanicalWatchTest" )
                .path( "/completed" )
                .type( MediaType.APPLICATION_JSON )
                .accept( MediaType.APPLICATION_JSON )
                .get( ClientResponse.class );

        assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );

        assertEquals( Boolean.TRUE, response.getEntity( Boolean.class ) );
    }


    static void testGet(TestParams testParams) {
        String result = testParams
                .setEndpoint(TestGetResource.ENDPOINT_URL)
                .newWebResource()
                .accept(MediaType.TEXT_PLAIN)
                .get(String.class);

        assertEquals(TestGetResource.TEST_MESSAGE, result);
    }


    static void testAuthGet(TestParams testParams) {
        String result = testParams.addQueryParameters(QUERY_PARAMS)
                .setEndpoint(AuthResource.ENDPOINT_URL)
                .newWebResource()
                .accept(MediaType.APPLICATION_JSON)
                .get(String.class);

        assertEquals(AuthResource.GET_MESSAGE, result);
    }


    static void testAuthPost(TestParams testParams) {
        String result = testParams.addQueryParameters(QUERY_PARAMS)
                .setEndpoint(AuthResource.ENDPOINT_URL)
                .newWebResource()
                .accept(MediaType.APPLICATION_JSON)
                .post(String.class);

        assertEquals(AuthResource.POST_MESSAGE, result);
    }


    static void testAuthGetWithWrongCredentials(TestParams testParams) {
        testParams.addQueryParameters(WRONG_USER_PARAMS)
                .setEndpoint(AuthResource.ENDPOINT_URL)
                .newWebResource()
                .accept(MediaType.APPLICATION_JSON)
                .get(String.class);
    }


    static void testAuthPostWithAllowedRole(TestParams testParams) {
        String result = testParams.addQueryParameters(QUERY_PARAMS)
                .setEndpoint(AuthResource.ENDPOINT_URL + AuthResource.ALLOWED_ROLE_PATH)
                .newWebResource()
                .accept(MediaType.APPLICATION_JSON)
                .post(String.class);

        assertEquals(AuthResource.POST_WITH_ALLOWED_ROLE_MESSAGE, result);
    }


    static void testAuthPostWithWrongCredentials(TestParams testParams) {
        testParams.addQueryParameters(WRONG_USER_PARAMS)
                .setEndpoint(AuthResource.ENDPOINT_URL)
                .newWebResource()
                .accept(MediaType.APPLICATION_JSON)
                .post(String.class);
    }


    static void testAuthPostWithUnallowedRole(TestParams testParams) {
        testParams.addQueryParameters(QUERY_PARAMS)
                .setEndpoint(AuthResource.ENDPOINT_URL + AuthResource.UNALLOWED_ROLE_PATH)
                .newWebResource()
                .accept(MediaType.APPLICATION_JSON)
                .post(String.class);
    }


    static void testAuthGetWithAllowedRole(TestParams testParams) {
        String result = testParams.addQueryParameters(QUERY_PARAMS)
                .setEndpoint(AuthResource.ENDPOINT_URL + AuthResource.ALLOWED_ROLE_PATH)
                .newWebResource()
                .accept(MediaType.APPLICATION_JSON)
                .get(String.class);

        assertEquals(AuthResource.GET_WITH_ALLOWED_ROLE_MESSAGE, result);
    }


    static void testAuthGetWithUnallowedRole(TestParams testParams) {
        testParams.addQueryParameters(QUERY_PARAMS)
                .setEndpoint(AuthResource.ENDPOINT_URL + AuthResource.UNALLOWED_ROLE_PATH)
                .newWebResource()
                .accept(MediaType.APPLICATION_JSON)
                .get(String.class);
    }

}
