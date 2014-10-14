/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.cassandra.service.CassandraDaemon;
import static org.apache.usergrid.cassandra.CassandraResource.LOG;
import static org.apache.usergrid.cassandra.CassandraResource.NATIVE_TRANSPORT_PORT_KEY;
import static org.apache.usergrid.cassandra.CassandraResource.RPC_PORT_KEY;
import static org.apache.usergrid.cassandra.CassandraResource.SSL_STORAGE_PORT_KEY;
import static org.apache.usergrid.cassandra.CassandraResource.STORAGE_PORT_KEY;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple wrapper for starting "embedded" Tomcat as it's own process, for testing.
 */
public class CassandraMain {
    
    private static final Logger log = LoggerFactory.getLogger( CassandraMain.class );

    public static void main(String[] args) throws Exception {

        String yamlFileName =     args[0];
        String tmpDirName =       args[1];
        String log4jConfig =      args[2];
        int rpcPort =             Integer.parseInt( args[3] );
        int storagePort =         Integer.parseInt( args[4] );
        int sslStoragePort =      Integer.parseInt( args[5] );
        int nativeTransportPort = Integer.parseInt( args[6] );

        System.setProperty( "cassandra." + RPC_PORT_KEY, Integer.toString( rpcPort ) );
        System.setProperty( "cassandra." + STORAGE_PORT_KEY, Integer.toString( storagePort ) );
        System.setProperty( "cassandra." + SSL_STORAGE_PORT_KEY, Integer.toString( sslStoragePort ) );
        System.setProperty( "cassandra." + NATIVE_TRANSPORT_PORT_KEY, Integer.toString( nativeTransportPort ) );

        System.setProperty( "cassandra.url", "localhost:" + Integer.toString( rpcPort ) );
        System.setProperty( "cassandra-foreground", "true" );
        System.setProperty( "log4j.defaultInitOverride", "true" );
        System.setProperty( "log4j.configuration", "file:" + log4jConfig );
        System.setProperty( "cassandra.ring_delay_ms", "100" );
        System.setProperty( "cassandra.config", yamlFileName );
        System.setProperty( "cassandra.tempName", tmpDirName );

        LOG.info("Starting forked Cassandra: test, setting system properties for ports : "
                + "[rpc, storage, sslStorage, native] = [{}, {}, {}, {}]", 
                new Object[] {rpcPort, storagePort, sslStoragePort, nativeTransportPort});

        CassandraDaemon cassandraDaemon = new CassandraDaemon();
        cassandraDaemon.activate();

        while ( true ) {
            Thread.sleep(1000);
        }
    }

}
