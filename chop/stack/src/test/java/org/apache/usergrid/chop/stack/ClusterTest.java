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
package org.apache.usergrid.chop.stack;


import java.io.StringReader;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;


/**
 * Tests Json serialization-deserialization operations for ICoordinatedCluster.
 */
public class ClusterTest {
    private static final Logger LOG = LoggerFactory.getLogger( ClusterTest.class );

    private static ICoordinatedCluster cluster;


    @BeforeClass
    public static void setup() throws Exception {
        final BasicInstanceSpec spec = new BasicInstanceSpec();
        spec.setKeyName( "TestKeyPair" );
        spec.setImageId( "ami-2131231" );
        spec.setType( "t1.micro" );
        spec.setScriptEnvProperty( "JAVA_HOME", "/user/lib/jvm/default" );
        spec.addSetupScript( new URL( "file://./install_cassandra.sh" ) );

        BasicCluster delegate = new BasicCluster();
        delegate.setInstanceSpec( spec );
        delegate.setName( "TestCluster" );
        delegate.setSize( 2 );

        Instance i1 = new BasicInstance(
                "i-37b10467",
                spec,
                InstanceState.Running,
                "ip-172-31-23-194.ec2.internal",
                "ec2-54-209-172-50.compute-1.amazonaws.com",
                "172.31.23.194",
                "54.209.172.50"
        );

        Instance i2 = new BasicInstance(
                "i-a281c0f2",
                spec,
                InstanceState.Running,
                "ip-172-31-18-20.ec2.internal",
                "ec2-54-207-172-40.compute-1.amazonaws.com",
                "172.31.18.20",
                "54.207.172.40"
        );

        cluster = new CoordinatedCluster( delegate );
        cluster.add( i1 );
        cluster.add( i2 );

    }


    @Test
    public void testBasicWrite() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString( cluster );
        LOG.info( json );

        assertTrue( json.startsWith( "{\"name\":\"TestCluster\",\"size\":2" ) );
    }


    @Test
    public void testReversibility() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString( cluster );

        ICoordinatedCluster derived = mapper.readValue( new StringReader( json ), ICoordinatedCluster.class );

        assertEquals( cluster.getName(), derived.getName() );
        assertEquals( cluster.getSize(), derived.getSize() );
        assertEquals( cluster.getInstanceSpec().getImageId(), derived.getInstanceSpec().getImageId() );
        assertEquals( cluster.getInstanceSpec().getKeyName(), derived.getInstanceSpec().getKeyName() );
        assertEquals( cluster.getInstanceSpec().getType(), derived.getInstanceSpec().getType() );
        assertEquals( cluster.getInstanceSpec().getSetupScripts().size(),
                derived.getInstanceSpec().getSetupScripts().size() );

        assertEquals( 2, derived.getInstances().size() );
    }

}
