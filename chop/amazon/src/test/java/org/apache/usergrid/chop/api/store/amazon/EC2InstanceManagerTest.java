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
package org.apache.usergrid.chop.api.store.amazon;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.api.Module;

import org.apache.usergrid.chop.spi.LaunchResult;
import org.apache.usergrid.chop.stack.CoordinatedStack;
import org.apache.usergrid.chop.stack.ICoordinatedCluster;
import org.apache.usergrid.chop.stack.Instance;
import org.apache.usergrid.chop.stack.InstanceSpec;
import org.apache.usergrid.chop.stack.InstanceState;
import org.apache.usergrid.chop.stack.Stack;
import org.apache.usergrid.chop.stack.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.junit.Assume.assumeNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * These tests require some AWS information in order to run.
 * <p>
 * If 'aws.access.key' and 'aws.secret.key' fields are provided in a profile in maven settings.xml file,
 * or if they are directly entered in the config.properties file, these tests are run in the given keys' account.
 * <p>
 * Otherwise, tests are automatically skipped!
 * <p>
 * Other than access and secret keys, your AWS settings has to be compatible with the fields in test-stack.json file;
 * keyName(Key Pair name), imageId (AMI id), ipRuleSet.name (Security Group name) and dataCenter (availability zone)
 * should all be compatible/existent with/in your AWS account.
 */
public class EC2InstanceManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger( EC2InstanceManagerTest.class );

    private static final int RUNNER_COUNT = 2;

    private static AmazonFig amazonFig;

    private static EC2InstanceManager manager;

    private static CoordinatedStack stack;

    private static Commit commit = mock( Commit.class );
    private static Module module = mock( Module.class );


    @BeforeClass
    public static void setUpData() {
        Injector injector = Guice.createInjector( new AmazonModule() );
        amazonFig = injector.getInstance( AmazonFig.class );

        String accessKey = amazonFig.getAwsAccessKey();
        String secretKey = amazonFig.getAwsSecretKey();

        if( accessKey == null || accessKey.equals( "${aws.access.key}" ) || accessKey.isEmpty() ||
            secretKey == null || secretKey.equals( "${aws.secret.key}" ) || secretKey.isEmpty() ) {

            LOG.warn( "EC2InstanceManagerTest tests are not run, " +
                    "Provided AWS secret or access key values are invalid or no values are provided" );
        }
        else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                InputStream is = EC2InstanceManagerTest.class.getClassLoader().getResourceAsStream( "test-stack.json" );
                Stack basicStack = mapper.readValue( is, Stack.class );

                /** Commit mock object get method values */
                when( commit.getCreateTime() ).thenReturn( new Date() );
                when( commit.getMd5() ).thenReturn( "742e2a76a6ba161f9efb87ce58a9187e" );
                when( commit.getModuleId() ).thenReturn( "2000562494" );
                when( commit.getRunnerPath() ).thenReturn( "/some/dummy/path" );
                when( commit.getId() ).thenReturn( "cc471b502aca2791c3a068f93d15b79ff6b7b827" );

                /** Module mock object get method values */
                when( module.getGroupId() ).thenReturn( "org.apache.usergrid.chop" );
                when( module.getArtifactId() ).thenReturn( "chop-maven-plugin" );
                when( module.getVersion() ).thenReturn( "1.0-SNAPSHOT" );
                when( module.getVcsRepoUrl() ).thenReturn( "https://stash.safehaus.org/scm/chop/main.git" );
                when( module.getTestPackageBase() ).thenReturn( "org.apache.usergrid.chop" );
                when( module.getId() ).thenReturn( "2000562494" );

                stack = new CoordinatedStack( basicStack, new User( "user", "pass" ), commit, module, RUNNER_COUNT );
            }
            catch ( Exception e ) {
                LOG.error( "Error while reading test stack json resource", e );
                return;
            }

            manager = injector.getInstance( EC2InstanceManager.class );
        }
    }


    @AfterClass
    public static void cleanup() {

    }


    @Before
    public void checkCredentialsExist() {
        assumeNotNull( manager );
    }


    @Test
    public void testCluster() {

        ICoordinatedCluster cluster = stack.getClusters().get( 0 );
        LOG.info( "Launching cluster {}'s {} instances...", cluster.getName(), cluster.getSize()  );

        LaunchResult result = manager.launchCluster( stack, cluster, 100000 );

        assertEquals( cluster.getSize(), result.getCount() );

        Collection<Instance> instances = manager.getClusterInstances( stack, cluster );

        assertEquals( "Number of launched instances is different than expected", cluster.getSize(), instances.size() );

        LOG.info( "Instances are successfully launched, now terminating..." );

        Collection<String> instanceIds = new ArrayList<String>( instances.size() );
        for( Instance i : instances ) {
            instanceIds.add( i.getId() );
        }

        manager.terminateInstances( instanceIds );
        boolean terminated = manager.waitUntil( instanceIds, InstanceState.ShuttingDown, 100000 );

        if( ! terminated ) {
            instances = manager.getClusterInstances( stack, cluster );
            assertEquals( "Some instances could not be terminated! You may need to manually terminate the instances",
                    0, instances.size() );
        }
    }


    @Test
    public void testRunners() {

        InstanceSpec iSpec = stack.getClusters().get( 0 ).getInstanceSpec();
        LaunchResult result = manager.launchRunners( stack, iSpec, 100000 );

        assertEquals( RUNNER_COUNT, result.getCount() );

        Collection<Instance> instances = manager.getRunnerInstances( stack );

        assertEquals( "Number of launched instances is different than expected", RUNNER_COUNT,
                instances.size() );

        LOG.info( "Instances are successfully launched, now terminating..." );

        Collection<String> instanceIds = new ArrayList<String>( instances.size() );
        for( Instance i : instances ) {
            instanceIds.add( i.getId() );
        }

        manager.terminateInstances( instanceIds );
        boolean terminated = manager.waitUntil( instanceIds, InstanceState.ShuttingDown, 100000 );

        if( ! terminated ) {
            instances = manager.getRunnerInstances( stack );
            assertEquals( "Some instances could not be terminated! You may need to manually terminate the instances",
                    0, instances.size() );
        }

    }


}
