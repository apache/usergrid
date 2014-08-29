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


import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.usergrid.chop.stack.BasicIpRule;
import org.apache.usergrid.chop.stack.BasicIpRuleSet;
import org.apache.usergrid.chop.stack.IpRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;


/**
 * These tests require some AWS information in order to run.
 *
 * If 'aws.access.key' and 'aws.secret.key' fields are provided in a profile in maven settings.xml file,
 * or if they are directly entered in the config.properties file, these tests are run in the given keys' account.
 *
 * Otherwise, tests are automatically skipped!
 */
public class AmazonIpRuleManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger( AmazonIpRuleManagerTest.class );

    private static AmazonFig amazonFig;
    private static AmazonIpRuleManager manager;
    private static BasicIpRuleSet ipRuleSet;


    @BeforeClass
    public static void setUpTestEnv() {
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

            ipRuleSet = new BasicIpRuleSet();
            ipRuleSet.setName( "test-" + UUID.randomUUID().toString() );

            BasicIpRule ipRule = new BasicIpRule();
            ipRule.withFromPort( 8443 )
                  .withToPort( 8443 )
                  .withIpProtocol( "tcp" )
                  .withIpRanges( "0.0.0.0/32" );

            ipRuleSet.addInboundRule( ipRule );

            ipRule = new BasicIpRule();
            ipRule.withFromPort( 80 )
                  .withToPort( 8080 )
                  .withIpProtocol( "udp" )
                  .withIpRanges( "0.0.0.0/32" );

            ipRuleSet.addInboundRule( ipRule );

            manager = injector.getInstance( AmazonIpRuleManager.class );
            manager.setDataCenter( "us-east-1a" );
        }
    }


    @AfterClass
    public static void tearDown() {
        if( manager != null && ipRuleSet != null ) {
            boolean deleted = manager.deleteRuleSet( ipRuleSet.getName() );
            if( ! deleted ) {
                LOG.warn( "The security group {} may not be deleted properly!", ipRuleSet.getName() );
            }
        }
    }


    @Before
    public void setUp() {
        assumeNotNull( manager, ipRuleSet );

        manager.applyIpRuleSet( ipRuleSet );
    }


    @Test
    public void exists() {
        assertTrue( "Security group should've existed", manager.exists( ipRuleSet.getName() ) );
    }


    @Test
    public void getIpRuleSet() {
        IpRuleSet set = manager.getIpRuleSet( ipRuleSet.getName() );

        assertTrue( ipRuleSet.equals( set ) );
    }


}
