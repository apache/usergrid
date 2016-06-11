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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.thrift.TBaseHelper;

import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.cassandra.ManagementServiceImpl;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.utils.UUIDUtils;

import com.google.common.collect.BiMap;

import me.prettyprint.cassandra.service.RangeSlicesIterator;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.RangeSlicesQuery;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_UNIQUE;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
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
public class ApplicationAudit extends ToolBase {

    /**
     *
     */
    private static final int PAGE_SIZE = 100;


    private static final Logger logger = LoggerFactory.getLogger( ApplicationAudit.class );

    private static final String APP_VALUE = "app";


    private static final String ENTITY_UNIQUE_PROPERTY_VALUE = "value";


    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {


        Options options = new Options();

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        options.addOption( hostOption );

        Option appOption =
                OptionBuilder.withArgName( "app" ).hasArg().isRequired( false ).withDescription( "Application uuid" )
                             .create( "app" );

        options.addOption( appOption );

        Option entityUniquePropertyValue =
                OptionBuilder.withArgName( ENTITY_UNIQUE_PROPERTY_VALUE ).hasArg().isRequired( false )
                             .withDescription( "Entity Unique Property Value" ).create( ENTITY_UNIQUE_PROPERTY_VALUE );
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

        if ( line.hasOption( ENTITY_UNIQUE_PROPERTY_VALUE ) ) {
            deleteInvalidValuesForUniqueProperty( m, line );
        }
        else {
            //maybe put a byte buffer infront.
            RangeSlicesQuery<ByteBuffer, ByteBuffer, ByteBuffer> rangeSlicesQuery =
                    HFactory.createRangeSlicesQuery( ko, be, be, be ).setColumnFamily( ENTITY_UNIQUE.getColumnFamily() )
                            //not sure if I trust the lower two settings as it might iterfere with paging or set
                            // arbitrary limits and what I want to retrieve.
                            //That needs to be verified.
                            .setKeys( null, null ).setRange( null, null, false, PAGE_SIZE );


            RangeSlicesIterator rangeSlicesIterator = new RangeSlicesIterator( rangeSlicesQuery, null, null );

            while ( rangeSlicesIterator.hasNext() ) {
                Row rangeSliceValue = rangeSlicesIterator.next();


                ByteBuffer buf = ( TBaseHelper.rightSize( ( ByteBuffer ) rangeSliceValue.getKey() ) );
                //Cassandra client library returns ByteBuffers that are views on top of a larger byte[]. These larger
                // ones return garbage data.
                //Discovered thanks due to https://issues.apache.org/jira/browse/NUTCH-1591
                String returnedRowKey = new String( buf.array(), buf.arrayOffset() + buf.position(), buf.remaining(),
                        Charset.defaultCharset() ).trim();


                //defensive programming, don't have to have to parse the string if it doesn't contain users.
                if (returnedRowKey.contains("applications") && returnedRowKey.contains( "name" )) {

                    String[] parsedRowKey = returnedRowKey.split( ":" );

                    //if the rowkey contains more than 4 parts then it may have some garbage appended to the front.
                    if ( parsedRowKey.length > 4 ) {
                        parsedRowKey = garbageRowKeyParser( parsedRowKey );

                        if ( parsedRowKey == null ) {
                            logger.error( "{} is a invalid row key, and unparseable. Skipped...", returnedRowKey );
                            continue;
                        }
                    }
                    //if the rowkey contains less than four parts then it is completely invalid
                    else if ( parsedRowKey.length < 4 ) {
                        logger.error( "{} is a invalid row key, and unparseable. Skipped...", returnedRowKey );
                        continue;
                    }

                    UUID applicationId = null;
                    try {
                        applicationId = UUID.fromString( uuidGarbageParser( parsedRowKey[0] ) );
                    }
                    catch ( Exception e ) {
                        logger.error( "could not parse {} despite earlier parsing. Skipping...", parsedRowKey[0] );
                        continue;
                    }
                    String collectionName = parsedRowKey[1];
                    String uniqueValueKey = parsedRowKey[2];
                    String uniqueValue = parsedRowKey[3];


                   // if ( collectionName.equals( "users" ) ) {

                    //ColumnSlice<ByteBuffer, ByteBuffer> columnSlice=rangeSliceValue.getColumnSlice();
                    //if ( columnSlice.getColumns().size() != 0 ) {
                   // List<HColumn<ByteBuffer, ByteBuffer>> cols=columnSlice.getColumns();

                    entityStateLogger( applicationId,uniqueValue);
                    //}
                }
            }
        }
        logger.info( "Completed logging successfully" );
    }


    //Returns a functioning rowkey if it can otherwise returns null
    public String[] garbageRowKeyParser( String[] parsedRowKey ) {
        String[] modifiedRowKey = parsedRowKey.clone();
        while ( modifiedRowKey != null ) {
            if ( modifiedRowKey.length < 4 ) {
                return null;
            }

            String recreatedRowKey = uuidStringVerifier( modifiedRowKey[0] );
            if ( recreatedRowKey == null ) {
                recreatedRowKey = "";
                modifiedRowKey = getStrings( modifiedRowKey, recreatedRowKey );
            }
            else {
                recreatedRowKey = recreatedRowKey.concat( ":" );
                modifiedRowKey = getStrings( modifiedRowKey, recreatedRowKey );
                break;
            }
        }
        return modifiedRowKey;
    }


    private String[] getStrings( String[] modifiedRowKey, String recreatedRowKey ) {
        for ( int i = 1; i < modifiedRowKey.length; i++ ) {

            recreatedRowKey = recreatedRowKey.concat( modifiedRowKey[i] );
            if ( i + 1 != modifiedRowKey.length ) {
                recreatedRowKey = recreatedRowKey.concat( ":" );
            }
        }
        modifiedRowKey = recreatedRowKey.split( ":" );
        return modifiedRowKey;
    }


    private void entityStateLogger(final UUID applicationUUID ,final String uniqueValue ) throws Exception {

        EntityManager em = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );

        ApplicationInfo applicationInfo = managementService.getApplicationInfo( applicationUUID );

        if(applicationInfo==null){
            if( !applicationUUID.equals( UUID.fromString( "00000000-0000-0000-0000-000000000010" ) )) {

                //logger.error( "Application uuid: {} and name: {} has return null.", applicationUUID, uniqueValue );

                String orgName = uniqueValue.split( "/" )[0];
                OrganizationInfo organizationInfo = managementService.getOrganizationByName( orgName );
                if(organizationInfo == null) {

                    logger.error( "Application uuid: {} with name: {} returned null and org name: {} cannot be found.",
                            new Object[]{applicationUUID, uniqueValue,orgName });
                }
                else{
                    logger.error("Org name: {} and uuid: {} is no longer associated with App uuid: {} and name: {} because APP NO LONGER EXISTS. RECREATING.",
                            new Object[]{organizationInfo.getName(),organizationInfo.getUuid(),applicationUUID,uniqueValue});




                    Map<String,Object> applicationProperties = new HashMap<String, Object>(  );
                    applicationProperties.put( "name",uniqueValue );
                    try {
                        em.create( applicationUUID, ManagementServiceImpl.APPLICATION_INFO, applicationProperties);
                    }catch(Exception e){
                        logger.error("Found the following error: {} when trying to recreate.",e.getMessage());
                    }
                    applicationInfo = managementService.getApplicationInfo( applicationUUID );
                    if(applicationInfo!=null){
                        logger.info("Org name: {} and uuid: {}  with App uuid: {} and name: {} has been Repaired.",
                                new Object[]{organizationInfo.getName(),organizationInfo.getUuid(),applicationInfo.getId(),applicationInfo.getName()});
                    }
                    else{
                        logger.error("Org name: {} and uuid: {}  with App uuid: {} and name: {} repair has failed.",
                                new Object[]{organizationInfo.getName(),organizationInfo.getUuid(),applicationUUID,uniqueValue});
                    }
                }


            }
        }
        else{
            OrganizationInfo organizationInfo = managementService.getOrganizationForApplication( applicationUUID );
            if(organizationInfo==null) {
                if(!applicationUUID.equals( UUID.fromString( "00000000-0000-0000-0000-000000000001"))) {

                    String orgName = applicationInfo.getName().split( "/" )[0];
                    organizationInfo = managementService.getOrganizationByName( orgName );
                    if(organizationInfo == null) {

                        logger.error( "Application uuid: {} with name: {} is lost and org name: {} cannot be found.",
                                new Object[]{applicationUUID, applicationInfo.getName(),orgName });
                    }
                    else{
                        logger.error("Org name: {} and uuid: {} is no longer associated with App uuid: {} and name: {}. Restablishing connection...",
                                new Object[]{organizationInfo.getName(),organizationInfo.getUuid(),applicationUUID,applicationInfo.getName()});

                        //add the application to organization, then add the organization to application.
                        managementService.addApplicationToOrganization( organizationInfo.getUuid(), applicationUUID);
                        //need to turn this into entity_refs.
                        em.createConnection(new SimpleEntityRef( "application_info", applicationUUID ) ,"owns",new SimpleEntityRef( "group",organizationInfo.getUuid() ) );

                        organizationInfo = managementService.getOrganizationForApplication( applicationUUID );

                        if(organizationInfo!=null){
                            logger.info("Org name: {} and uuid: {} with App uuid: {} and name: {} connection has been re-established.",
                                    new Object[]{organizationInfo.getName(),organizationInfo.getUuid(),applicationUUID,applicationInfo.getName()});
                        }
                        else{
                            logger.error("Org name: {} and uuid: {} is no longer associated with App uuid: {} and name: {}. Could not be re-estabished",
                                    new Object[]{organizationInfo.getName(),organizationInfo.getUuid(),applicationUUID,applicationInfo.getName()});
                        }

                    }
                }
            }
            else{
                EntityRef orgRef = new SimpleEntityRef("group",organizationInfo.getUuid() );
                orgVerification(applicationUUID ,uniqueValue, em, orgRef, organizationInfo.getUuid(),
                        "Application with uuid: {} with name: {} is missing their organization: {}" );
            }
        }
    }


    private void orgVerification(final UUID applicationUUID ,final String uniqueValue, final EntityManager em, final EntityRef orgRef,
                                  final UUID uuid, final String s2 ) throws Exception {
        try {
            em.getDictionaryAsMap( orgRef, "orgProperties" );

            Object[] loggingObject = new Object[3];
            loggingObject[0] = applicationUUID;
            loggingObject[1] = uniqueValue;
            loggingObject[2] = uuid;
            logger.info( "Application with uuid: {} and name: {} was returned from orgUUID: {} ",loggingObject );

        }
        catch ( EntityNotFoundException enfe ) {
            Object[] notFoundLogger = new Object[3];
            notFoundLogger[0] = applicationUUID;
            notFoundLogger[1] = uniqueValue;
            notFoundLogger[2] = uuid;

            logger.error( s2, uniqueValue, uuid );

            BiMap<UUID,String> applicationBiMap = managementService.getApplicationsForOrganization( uuid );
            if(applicationBiMap==null || applicationBiMap.size() == 0){
                logger.error("Applications didn't return for orgUUID: {}",uuid);
            }
            else if (applicationBiMap.size()>0){
                for ( Map.Entry<UUID, String> app : applicationBiMap.entrySet() ) {
                    Object[] loggingObject = new Object[3];
                    loggingObject[0] = app.getKey();
                    loggingObject[1] = app.getValue();
                    loggingObject[2] = uuid;
                    logger.info( "Application with uuid: {} and name: {} was returned from orgUUID: {} but that orgUUID cannot be found.",loggingObject );
                }
            }

        }
    }


    //really only deletes ones that aren't existant for a specific value
    private void deleteInvalidValuesForUniqueProperty( Mutator<ByteBuffer> m, CommandLine line ) throws Exception {
        UUID applicationId = UUID.fromString(line.getOptionValue( APP_VALUE ));
        String collectionName = "applications"; //line.getOptionValue( COLLECTION_ARG );
        String uniqueValueKey = "name"; //line.getOptionValue( ENTITY_UNIQUE_PROPERTY_NAME );
        String uniqueValue = line.getOptionValue( ENTITY_UNIQUE_PROPERTY_VALUE );

        //PLEASE ADD VERIFICATION.

        Object key = key( applicationId, collectionName,uniqueValueKey, uniqueValue );


//        List<HColumn<ByteBuffer, ByteBuffer>> cols = cass.getColumns( cass.getApplicationKeyspace( applicationId), ENTITY_UNIQUE, key, null, null, 1000,
//                false );


//        if ( cols.size() == 0 ) {
//            logger.error( "This row key: {} has zero columns", key.toString() );
//        }

        entityStateLogger( applicationId,uniqueValue );
    }


    private String uuidGarbageParser( final String garbageString ) {
        int index = 1;
        String stringToBeTruncated = garbageString;
        while ( !UUIDUtils.isUUID( stringToBeTruncated ) ) {
            if ( stringToBeTruncated.length() > 36 ) {
                stringToBeTruncated = stringToBeTruncated.substring( index );
            }
            else {
                logger.error( "{} is unparsable", garbageString );
                break;
            }
        }
        return stringToBeTruncated;
    }


    private String uuidStringVerifier( final String garbageString ) {
        int index = 1;
        String stringToBeTruncated = garbageString;
        while ( !UUIDUtils.isUUID( stringToBeTruncated ) ) {
            if ( stringToBeTruncated.length() > 36 ) {
                stringToBeTruncated = stringToBeTruncated.substring( index );
            }
            else {
                return null;
            }
        }
        return stringToBeTruncated;
    }

}
