/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.core.astyanax;


import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.google.common.collect.ImmutableMap;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.model.ColumnFamily;


/**
 * Utilities for Cassandra tests
 */
public class TestUtils {

    private static final Logger log = LoggerFactory.getLogger( TestUtils.class );

    /**
     * Create the kespace, ignore exceptions if it already exists
     * @param keyspace
     */
    public static void createKeyspace(final Keyspace keyspace){

        ImmutableMap.Builder<Object, Object> strategyOptions = ImmutableMap.builder().put( "replication_factor", "1" );

        ImmutableMap<String, Object> options = ImmutableMap.<String, Object>builder().put( "strategy_class",
                "org.apache.cassandra.locator.SimpleStrategy" ).put( "strategy_options", strategyOptions.build() )
                                                           .build();


        try {
            keyspace.createKeyspace( options );
        }
        catch ( Throwable t ) {
          log.info( "Error on creating keyspace, ignoring", t );
        }



    }


    public static <K, C> void createColumnFamiliy(final Keyspace keyspace, final ColumnFamily<K, C> columnFamily, final Map<String, Object> options){
        try{
            keyspace.createColumnFamily( columnFamily, new HashMap<String, Object>() );
        }catch(Exception e){
           log.error( "Error on creating column family, ignoring" , e);
        }
    }
}
