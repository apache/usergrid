/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.qakka;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;


/**
 * Created by Dave Johnson (snoopdave@apache.org) on 9/9/16.
 */
public class KeyspaceDropper {

    private static final Logger logger = LoggerFactory.getLogger( KeyspaceDropper.class );

    static { dropTestKeyspaces(); }


    public static void dropTestKeyspaces() {

        String propsFileName = "qakka.properties";

        Properties props = new Properties();
        try {
            props.load( App.class.getResourceAsStream( "/" + propsFileName ) );
        } catch (IOException e) {
            throw new RuntimeException( "Unable to load " + propsFileName + " file!" );
        }

        String keyspaceApp =     (String)props.get("cassandra.keyspace.application");
        String keyspaceQueue =     (String)props.get("cassandra.keyspace.queue-message");
        String hosts[] =              props.getProperty( "cassandra.hosts", "127.0.0.1" ).split(",");
        int port = Integer.parseInt(  props.getProperty( "cassandra.port", "9042" ));

        dropTestKeyspace( keyspaceApp, hosts, port );
        dropTestKeyspace( keyspaceQueue, hosts, port );

        // drop local test keyspaces
        dropTestKeyspace(keyspaceApp + "_", hosts, port);
        dropTestKeyspace(keyspaceQueue + "_", hosts, port);
    }

    public static void dropTestKeyspace( String keyspace, String[] hosts, int port ) {

        Cluster.Builder builder = Cluster.builder();
        for ( String host : hosts ) {
            builder = builder.addContactPoint( host ).withPort( port );
        }

        final QueryOptions queryOptions = new QueryOptions().setConsistencyLevel( ConsistencyLevel.LOCAL_QUORUM );
        builder.withQueryOptions( queryOptions );
        Cluster cluster = builder.build();

        Session session = cluster.connect();
        logger.info("Dropping test keyspace: {}", keyspace);
        session.execute( "DROP KEYSPACE IF EXISTS " + keyspace );
    }

}
