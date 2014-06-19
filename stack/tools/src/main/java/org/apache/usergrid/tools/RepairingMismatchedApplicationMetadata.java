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
package org.apache.usergrid.tools;


import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

import org.apache.usergrid.management.OrganizationInfo;

import com.google.common.collect.BiMap;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.mutation.Mutator;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.batchExecute;
import static org.apache.usergrid.persistence.cassandra.CassandraService.APPLICATIONS_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.RETRY_COUNT;


public class RepairingMismatchedApplicationMetadata extends ToolBase {

    public static final ByteBufferSerializer be = new ByteBufferSerializer();

    private static final Logger logger = LoggerFactory.getLogger( RepairingMismatchedApplicationMetadata.class );


    @Override
    public Options createOptions() {
        Options options = super.createOptions();
        return options;
    }


    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();

        //sucks, but it's not picking up the configuration
        LogManager.getLogger( RepairingMismatchedApplicationMetadata.class ).setLevel( Level.INFO );

        UUID orgId = null;
        List<OrganizationInfo> orgs;

        final int size = 1000;


        do {
            orgs = managementService.getOrganizations( orgId, size );


            for ( OrganizationInfo org : orgs ) {

                orgId = org.getUuid();

                logger.info( "Auditing org {}", org.getName() );

                try {
                    BiMap<UUID, String> apps = managementService.getApplicationsForOrganization( org.getUuid() );


                    for ( Map.Entry<UUID, String> app : apps.entrySet() ) {

                        logger.info( "Auditing org {} app {}", org.getName(), app.getValue() );

                        UUID applicationId = emf.lookupApplication( app.getValue() );
                        if ( applicationId == null ) {
                            String appName = app.getValue();
                            Keyspace ko = cass.getSystemKeyspace();
                            Mutator<ByteBuffer> m = createMutator( ko, be );
                            long timestamp = cass.createTimestamp();
                            addInsertToMutator( m, APPLICATIONS_CF, appName, PROPERTY_UUID, app.getKey(), timestamp );
                            addInsertToMutator( m, APPLICATIONS_CF, appName, PROPERTY_NAME, appName, timestamp );
                            batchExecute( m, RETRY_COUNT );
                            logger.info( "Repairing alias with app uuid {}, and name {}", app.getKey(),
                                    app.getValue() );
                        }
                    }
                }
                catch ( Exception e ) {
                    logger.error( "Unable to process applications for organization {}", org, e );
                }
            }
        }
        while ( orgs != null && orgs.size() == size );

        logger.info( "Completed repairing aliases" );
        Thread.sleep( 1000 * 60 );
    }
}
