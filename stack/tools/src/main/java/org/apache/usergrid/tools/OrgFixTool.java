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


import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.cassandra.RelationManagerImpl;
import org.apache.usergrid.persistence.query.ir.result.ScanColumn;
import org.apache.usergrid.persistence.query.ir.result.SecondaryIndexSliceParser;
import org.apache.usergrid.persistence.query.ir.result.UUIDColumn;

import me.prettyprint.hector.api.beans.DynamicComposite;

import static org.apache.usergrid.persistence.SimpleEntityRef.ref;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.apache.usergrid.persistence.cassandra.CassandraService.dce;


/**
 * Created by ApigeeCorporation on 12/15/15.
 */
public class OrgFixTool extends ToolBase {


    private static final Logger logger = LoggerFactory.getLogger( OrgFixTool.class );

    private static final String FILE_PATH = "file";

    private static final String DUPLICATE_EMAIL = "dup";

    private static final String ROW_KEY = "row";


    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {


        Options options = new Options();

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        options.addOption( hostOption );


        Option duplicate_email = OptionBuilder.withArgName( DUPLICATE_EMAIL ).hasArg().isRequired( false )
                                              .withDescription( "duplicate org uuid to examine" )
                                              .create( DUPLICATE_EMAIL );
        options.addOption( duplicate_email );

        return options;
    }


    @Override
    public void runTool( final CommandLine line ) throws Exception {
        startSpring();

        EntityManager em = emf.getEntityManager( MANAGEMENT_APPLICATION_ID);
        RelationManagerImpl relationManager =
                ( RelationManagerImpl ) em.getRelationManager( ref( MANAGEMENT_APPLICATION_ID ) );

        Query query = new Query();
        query.setEntityType( "group" );
        query.setLimit( MAX_ENTITY_FETCH );
        query.setResultsLevel( Results.Level.REFS );

        if(line.getOptionValue( "dup" )!=null) {
            query.addEqualityFilter( "uuid",line.getOptionValue( "dup" )  );

            logger.info( "This is what returns from the call to managements: {}",
                    managementService.getApplicationsForOrganization( UUID.fromString( line.getOptionValue( "dup" ) ) ) );
            //managementService.getOrganizationByIdentifier(  )

        }
        else {
            List<ScanColumn> scanColumnList = relationManager.searchRawCollection( "groups", query ); //em.searchCollection( em.getApplicationRef(), "groups", query );

            for ( ScanColumn scanColumn : scanColumnList ) {
                logger.info( "This is what you get back: {}", scanColumn );
                //            SecondaryIndexSliceParser.SecondaryIndexColumn secondaryIndexColumn =
                //                    ( SecondaryIndexSliceParser.SecondaryIndexColumn ) scanColumn;
                UUIDColumn uuidColumn = ( UUIDColumn ) scanColumn;

                //byte buffer from the scan column is the column name needed to delete from ENTITY_INDEX
                //DynamicComposite columnName = dce.fromByteBuffer( uuidColumn.getByteBuffer() );


                //logger.info("This is what you get back from the buffer: {}, from a buffer of size: {}",sc);

                logger.info( "This is what returns from the call to managements: {}",
                        managementService.getApplicationsForOrganization( uuidColumn.getUUID() ) );


                //            if(em.get((UUID ) columnName.get( 2 ) ) == null){
                //                logger.error( "This organization is broken and doesn't appear in entity Properties: {}",columnName );
                //            }
                //            else{
                //                logger.info("This organization works: {}",columnName);
                //            }
            }
        }
    }
}
