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
package org.apache.usergrid.cassandra;


import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.Assert.assertTrue;


/** This tests the CassandraResource. */
@Concurrent()
public class CassandraResourceTest {
    public static final Logger LOG = LoggerFactory.getLogger( CassandraResourceTest.class );
    public static final long WAIT = 200L;


    /** Tests to make sure port overrides works properly. */
    @Test
    public void testPortOverride() throws Throwable {
        int rpcPort;
        int storagePort;
        int sslStoragePort;
        int nativeTransportPort;

        do {
            rpcPort = AvailablePortFinder.getNextAvailable( CassandraResource.DEFAULT_RPC_PORT + 1 );
        }
        while ( rpcPort == CassandraResource.DEFAULT_RPC_PORT );
        LOG.info( "Setting rpc_port to {}", rpcPort );

        do {
            storagePort = AvailablePortFinder.getNextAvailable( CassandraResource.DEFAULT_STORAGE_PORT + 1 );
        }
        while ( storagePort == CassandraResource.DEFAULT_STORAGE_PORT || storagePort == rpcPort );
        LOG.info( "Setting storage_port to {}", storagePort );

        do {
            sslStoragePort = AvailablePortFinder.getNextAvailable( CassandraResource.DEFAULT_SSL_STORAGE_PORT + 1 );
        }
        while ( sslStoragePort == CassandraResource.DEFAULT_SSL_STORAGE_PORT || storagePort == sslStoragePort );
        LOG.info( "Setting ssl_storage_port to {}", sslStoragePort );

        do {
            nativeTransportPort =
                    AvailablePortFinder.getNextAvailable( CassandraResource.DEFAULT_NATIVE_TRANSPORT_PORT + 1 );
        }
        while ( nativeTransportPort == CassandraResource.DEFAULT_NATIVE_TRANSPORT_PORT
                || sslStoragePort == nativeTransportPort );
        LOG.info( "Setting native_transport_port to {}", nativeTransportPort );

        final CassandraResource cassandraResource =
                new CassandraResource( rpcPort, storagePort, sslStoragePort, nativeTransportPort );

        cassandraResource.before();

        // test here to see if we can access cassandra's ports
        // TODO - add some test code here using Hector

        cassandraResource.after();
        LOG.info( "Got the test bean: " );
    }





    /**
     * Fires up two Cassandra instances on the same machine.
     *
     * @throws Exception if this don't work
     */
    @Test
    public void testDoubleTrouble() throws Throwable {
        CassandraResource c1 = CassandraResource.newWithAvailablePorts();
        LOG.info( "Starting up first Cassandra instance: {}", c1 );
        c1.before();

        LOG.debug( "Waiting for the new instance to come online." );
        while ( !c1.isReady() ) {
            Thread.sleep( WAIT );
        }

        CassandraResource c2 = CassandraResource.newWithAvailablePorts();
        LOG.debug( "Starting up second Cassandra instance: {}", c2 );
        c2.before();

        LOG.debug( "Waiting a few seconds for second instance to be ready before shutting down." );
        while ( !c2.isReady() ) {
            Thread.sleep( WAIT );
        }

        LOG.debug( "Shutting Cassandra instances down." );
        c1.after();
        c2.after();
    }
}
