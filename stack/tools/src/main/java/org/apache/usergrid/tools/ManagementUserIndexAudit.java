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


import java.io.File;
import java.nio.ByteBuffer;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.mutation.Mutator;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.usergrid.persistence.Results.Level.REFS;
import static org.apache.usergrid.persistence.SimpleEntityRef.ref;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.apache.usergrid.persistence.cassandra.Serializers.be;


/**
 * This utility audits all values in the ENTITY_UNIQUE column family. If it finds any duplicates of users then it
 * deletes the non existing columns from the row. If there are no more columns in the row then it deletes the row. If
 * there exists more than one existing column then the one with the most recent timestamp wins and the other is
 * deleted.
 *
 * If you want the run the tool on their cluster the following is what you need to do nohup java
 * -Dlog4j.configuration=file:log4j.properties -jar usergrid-tools-1.0.2.jar UserUniqueIndexCleanup -host
 * <cassandra_host_here>  > log.txt
 *
 * if there is a specific value you want to run the tool on then you need the following
 *
 * nohup java -Dlog4j.configuration=file:log4j.properties -jar usergrid-tools-1.0.2.jar UserUniqueIndexCleanup -host
 * <cassandra_host_here> -app <applicationUUID> -col <collection_name> -property <unique_property_key> -value
 * <unique_property_value> > log.txt
 *
 * @author grey
 */
public class ManagementUserIndexAudit extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger( ManagementUserIndexAudit.class );

    private static final String ENTITY_UNIQUE_PROPERTY_VALUE = "file";


    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {


        Options options = new Options();

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        options.addOption( hostOption );

        Option entityUniquePropertyValue =
                OptionBuilder.withArgName( ENTITY_UNIQUE_PROPERTY_VALUE ).hasArg().isRequired( true )
                             .withDescription( "file path" ).create( ENTITY_UNIQUE_PROPERTY_VALUE );
        options.addOption( entityUniquePropertyValue );


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

        logger.info( "Starting entity unique checker" );


        // go through each collection and audit the values
        Keyspace ko = cass.getUsergridApplicationKeyspace();
        Mutator<ByteBuffer> m = createMutator( ko, be );

        EntityManager em = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );

        ObjectMapper objectMapper = new ObjectMapper(  );

        File jsonObjectFile = new File(line.getOptionValue( "file" ));

        JsonNode node =  objectMapper.readTree( jsonObjectFile );

        JsonNode users = node.get( "user" );

        for(JsonNode email:users) {

            String extractedEmail = email.get( "name" ).getTextValue();

            Query query = new Query();
            query.setEntityType( "user" );
            query.addEqualityFilter( "email", extractedEmail );
            //maybe this could be changed to detect duplicates
            query.setLimit( 1 );
            query.setResultsLevel( REFS );

            Results r = emf.getEntityManager( MANAGEMENT_APPLICATION_ID ).getRelationManager( ref( MANAGEMENT_APPLICATION_ID ) ).searchCollection( "users", query );
            if ( r != null && r.getRef() != null ) {
                if ( em.get( r.getRef().getUuid() ) == null ) {
                    logger.error( "Email: {} with uuid: {} doesn't exist in ENTITY_PROPERTIES.", extractedEmail,
                            r.getRef().getUuid() );
                }
                else {
                    logger.info( "Email: {}  with uuid: {} exists in ENTITY_PROPERTIES", extractedEmail,
                            r.getRef().getUuid() );
                }
            }
            else{
                logger.error( "Email: {} doesn't exist in ENTITY_INDEX.",extractedEmail );
            }
        }
        logger.info( "Completed logging successfully" );
    }
}
