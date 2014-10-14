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
package org.apache.usergrid.persistence.core.astyanax;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;



/**
 * Cassandra configuration interface.
 */
@FigSingleton
public interface CassandraFig extends GuicyFig {


    public static final String READ_CL = "usergrid.read.cl";

    public static final String WRITE_CL = "usergrid.write.cl";

    @Key( "cassandra.hosts" )
    String getHosts();

    @Key( "cassandra.version" )
    @Default( "1.2" )
    String getVersion();

    @Key( "cassandra.cluster_name" )
    @Default( "Usergrid" )
    String getClusterName();

    @Key( "collections.keyspace" )
    @Default( "Usergrid_Collections" )
    String getKeyspaceName();

    @Key( "cassandra.port" )
    @Default( "9160" )
    int getThriftPort();

    @Key( "cassandra.connections" )
    @Default( "100" )
    int getConnections();

    @Key( "cassandra.timeout" )
    @Default( "5000" )
    int getTimeout();

    @Key("cassandra.discovery")
    @Default( "RING_DESCRIBE" )
    String getDiscoveryType();    
    
    @Key("cassandra.embedded")
    @Default( "false" )
    boolean isEmbedded();


    @Default("CL_QUORUM")
    @Key(READ_CL)
    String getReadCL();

    @Default("CL_QUORUM")
    @Key(WRITE_CL)
    String getWriteCL();

}
