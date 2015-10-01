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


import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.utils.UUIDUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.EntityRef;


/**
 * Index rebuild utility for Usergrid. Can be used to rebuild the index for a specific 
 * application, a specific application's collection or for an entire Usergrid system.
 */
public class IndexRebuild extends ToolBase {

    private static final String APPLICATION_ARG = "app";

    private static final String COLLECTION_ARG = "col";

    private static final String ALL_ARG = "all";

    private static final int PAGE_SIZE = 100;


    private static final Logger logger = LoggerFactory.getLogger( IndexRebuild.class );


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOpt = OptionBuilder.withArgName( "host" ).hasArg().isRequired( true )
                .withDescription( "Cassandra host" ).create( "host" );

        Option esHostsOpt = OptionBuilder.withArgName( "host" ).hasArg().isRequired( true )
                .withDescription( "ElasticSearch host" ).create( "eshost" );

        Option esClusterOpt = OptionBuilder.withArgName( "host" ).hasArg().isRequired( true )
                .withDescription( "ElasticSearch cluster name" ).create( "escluster" );

        Option appOpt = OptionBuilder.withArgName( APPLICATION_ARG ).hasArg().isRequired( false )
                .withDescription( "Application id or app name" ).create( APPLICATION_ARG );

        Option collOpt = OptionBuilder.withArgName( COLLECTION_ARG ).hasArg().isRequired( false )
                .withDescription( "Collection name" ).create( COLLECTION_ARG );

        Option allOpt = OptionBuilder.withType( Boolean.class )
                .withArgName( ALL_ARG ).hasArg().isRequired( false )
                .withDescription( "True to reindex all application" ).create( ALL_ARG );

        Options options = new Options();
        options.addOption( hostOpt );
        options.addOption( esHostsOpt );
        options.addOption( esClusterOpt );
        options.addOption( appOpt );
        options.addOption( collOpt );
        options.addOption( allOpt );

        return options;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.usergrid.tools.ToolBase#runTool(org.apache.commons.cli.CommandLine)
     */
    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();

        logger.info( "Starting index rebuild" );

        EntityManagerFactory.ProgressObserver po = new EntityManagerFactory.ProgressObserver() {
            
            @Override
            public void onProgress(EntityRef entity) {
                logger.info("Indexing entity {}:{}", entity.getType(), entity.getUuid());
            }

            @Override
            public long getWriteDelayTime() {
                return 100;
            }
        };

        emf.rebuildInternalIndexes( po ); 
        emf.refreshIndex();

        if ( line.getOptionValue("all") != null && line.getOptionValue("all").equalsIgnoreCase("true") ) {
            emf.rebuildAllIndexes( po );

        } else if ( line.getOptionValue( APPLICATION_ARG ) != null ) {

            // Goes through each app id specified
            for ( UUID appId : getAppIds( line ) ) {

                logger.info( "Reindexing for app id: {}", appId );
                Set<String> collections = getCollections( line, appId );

                for ( String collection : collections ) {
                    emf.rebuildCollectionIndex( appId, collection, po );
                    emf.refreshIndex();
                }
            }

        } else {

            Map<String, UUID> ids = emf.getApplications();
            System.out.println( "Printing all apps" );
            for ( Entry<String, UUID> entry : ids.entrySet() ) {
                System.out.println( entry.getKey() + " appid=" + entry.getValue() );
            }
        }

        logger.info( "Finished index rebuild" );
    }


    /** Get all app id */
    private Collection<UUID> getAppIds( CommandLine line ) throws Exception {

        String appId = line.getOptionValue( APPLICATION_ARG );

        Map<String, UUID> ids = emf.getApplications();

        if ( appId != null ) {
            UUID id = UUIDUtils.tryExtractUUID( appId );
            if ( id == null ) {
                logger.debug("Got applications: " + ids );
                id = emf.getApplications().get( appId );
            }
            return Collections.singleton( id );
        }

        return ids.values();
    }


    /** Get collection names. If none are specified, all are returned */
    private Set<String> getCollections( CommandLine line, UUID appId ) throws Exception {

        String passedName = line.getOptionValue( COLLECTION_ARG );

        if ( passedName != null ) {
            return Collections.singleton( passedName );
        }

        EntityManager em = emf.getEntityManager( appId );

        return em.getApplicationCollections();
    }
}
