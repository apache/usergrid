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


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;



/**
 * Tools class which takes a json file as an input.  Each property in the input is then set into each entity that is
 * returned from the query. Used for performing data migrations.  It requires an app name or ID, the collection name,
 * the input update file and the query to find all matches to update. For instance, to set every admin to approved =
 * true and confirmed = true.  You would have this json file.
 * <p/>
 * <pre>
 * {
 * "activated":true,
 * "confirmed":true
 * }
 * </pre>
 * <p/>
 * And execute the following options
 * <p/>
 * EntityUpdate -host 127.0.0.1:9160 -app 00000000-0000-0000-0000-000000000001 -col users -query "select * where
 * activated = false or confirmed = false" -update update.json
 *
 * @author tnine
 */
public class EntityUpdate extends ToolBase {

    /**
     *
     */
    private static final String QUERY_ARG = "query";
    /**
     *
     */
    private static final String APPLICATION_ARG = "app";

    /**
     *
     */
    private static final String COLLECTION_ARG = "col";

    /** Used to provide the update file */
    private static final String UPDATE_ARG = "update";

    //parse the content as a json entity
    private static ObjectMapper MAPPER = new ObjectMapper();

    /**
     *
     */
    private static final int PAGE_SIZE = 100;
    private static final Logger logger = LoggerFactory.getLogger( EntityUpdate.class );


    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();

        setVerbose( line );


        String collectionName = line.getOptionValue( COLLECTION_ARG );

        String queryString = line.getOptionValue( QUERY_ARG );

        String updateFile = line.getOptionValue( UPDATE_ARG );


        Query query = Query.fromQL( queryString );

        logger.info( "Parsing the file at {}", updateFile );


        File file = new File( updateFile );

        if ( !file.exists() ) {
            logger.error( "The file {} does not exist.  Please make sure you have read access so this file.  Exiting.",
                    updateFile );
            System.exit( 1 );
        }

        DynamicEntity update = MAPPER.readValue( file, DynamicEntity.class );


        if ( update.getProperties().size() == 0 ) {
            logger.error( "The update in file {} has no properties.  Exiting", updateFile );
            System.exit( 2 );
        }


        String appName = line.getOptionValue( APPLICATION_ARG );

        ApplicationInfo app = managementService.getApplicationInfo( Identifier.from( appName ) );

        if ( app == null ) {
            logger.error( "Could not find application with id or name {}", appName );
            System.exit( 3 );
        }

        EntityManager entityManager = emf.getEntityManager( app.getId() );


        Results results = entityManager.searchCollection( entityManager.getApplicationRef(), collectionName, query );

        PagingResultsIterator itr = new PagingResultsIterator( results, Query.Level.ALL_PROPERTIES );

        long count = 0;

        for ( Object next : itr ) {

            Entity entity = ( Entity ) next;

            //set all props
            for ( Map.Entry<String, Object> entry : update.getProperties().entrySet() ) {
                entity.setProperty( entry.getKey(), entry.getValue() );
            }


            entityManager.update( entity );

            logger.info( "Updated entity in application {} with id {} of type {}",
                    new Object[] { app.getId(), entity.getUuid(), entity.getType() } );

            count++;
        }

        logger.info( "Process complete.  Updated {} entities", count );
    }


    @Override
    public Options createOptions() {
        Options options = super.createOptions();

        @SuppressWarnings("static-access") Option queryOption =
                OptionBuilder.withArgName( QUERY_ARG ).hasArg().isRequired( true )
                             .withDescription( "Query to execute when searching for organizations" )
                             .create( QUERY_ARG );
        options.addOption( queryOption );


        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        options.addOption( hostOption );

        Option appOption = OptionBuilder.withArgName( APPLICATION_ARG ).hasArg().isRequired( true )
                                        .withDescription( "application id or app name" ).create( APPLICATION_ARG );


        options.addOption( appOption );

        Option collectionOption = OptionBuilder.withArgName( COLLECTION_ARG ).hasArg().isRequired( true )
                                               .withDescription( "colleciton name" ).create( COLLECTION_ARG );

        options.addOption( collectionOption );

        Option updateOption =
                OptionBuilder.withArgName( UPDATE_ARG ).hasArg().isRequired( true ).withDescription( "Update file" )
                             .create( UPDATE_ARG );

        options.addOption( updateOption );

        return options;
    }
}
