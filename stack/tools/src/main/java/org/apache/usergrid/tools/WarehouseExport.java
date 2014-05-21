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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.AsyncBlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.jclouds.blobstore.options.PutOptions;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.utils.InflectionUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Module;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import static org.apache.usergrid.persistence.Schema.getDefaultSchema;
import org.apache.usergrid.persistence.index.query.Query.Level;


/**
 * Exports all known (non-Dynamic) fields from Schema that are primitive, Date, or String into a pipe-delimited file.
 * Also includes (hard-coded for now) fields from Notification, Notifier, and Receipt.  With no -startTime, scans the
 * existing *.csv files in the output directory and starts from last end date found. With no -endTime, ends at current
 * time - 1 hour. Explicitly sets "cassandra.readcl=ONE" for efficiency.
 */
public class WarehouseExport extends ExportingToolBase {

    private static final Logger LOG = LoggerFactory.getLogger( WarehouseExport.class );
    private static final char SEPARATOR = '|';

    public static final String BUCKET_PROPNAME = "usergrid.warehouse-export-bucket";
    public static final String ACCESS_ID_PROPNAME = "usergrid.warehouse-export-access-id";
    public static final String SECRET_KEY_PROPNAME = "usergrid.warehouse-export-secret-key";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS" );

    private static final String[] BASE_ATTRIBUTES =
            { "uuid", "organization", "application", "type", "created", "modified" };

    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";
    private static final String UPLOAD = "upload";

    private static final String[] NOTIFICATION_ATTRIBUTES = {
            "payloads", "queued", "started", "finished", "deliver", "expire", "canceled", "errorMessage", "statistics"
    };

    private static final String[] NOTIFIER_ATTRIBUTES = { "provider", "environment" };
    private static final String[] RECEIPT_ATTRIBUTES =
            { "payload", "sent", "errorCode", "errorMessage", "notifierId", "notificationUUID" };

    private static final Map<String, String[]> URAP_ATTRIBUTES = new HashMap<String, String[]>();


    static {
        URAP_ATTRIBUTES.put( "notification", NOTIFICATION_ATTRIBUTES );
        URAP_ATTRIBUTES.put( "notifier", NOTIFIER_ATTRIBUTES );
        URAP_ATTRIBUTES.put( "receipt", RECEIPT_ATTRIBUTES );
    }


    private CSVWriter writer;
    private String[] collectionNames;
    private Map<String, String[]> collectionFieldMap;
    private Date startTime, endTime;


    @Override
    public void runTool( CommandLine line ) throws Exception {

        // keep it light and fast
        System.setProperty( "cassandra.readcl", "ONE" );

        startSpring();
        setVerbose( line );

        applyOrgId( line );
        prepareBaseOutputFileName( line );
        outputDir = createOutputParentDir();
        LOG.info( "Export directory: {}", outputDir.getAbsolutePath() );

        // create writer
        applyStartTime( line );
        applyEndTime( line );
        LOG.error( "startTime: {}, endTime: {}", startTime, endTime );
        if ( startTime.getTime() >= endTime.getTime() ) {
            LOG.error( "startTime must be before endTime. exiting." );
            System.exit( 1 );
        }

        // create "modified" query to select data
        StringBuilder builder = new StringBuilder();
        builder.append( "modified >= " ).append( startTime.getTime() ).append( " and " );
        builder.append( "modified <= " ).append( endTime.getTime() );
        String queryString = builder.toString();

        // create writer
        String dateString = DATE_FORMAT.format( new Date() );
        String fileName = outputDir.getAbsolutePath() + "/" + dateString + ".csv";
        FileWriter fw = new FileWriter( fileName );
        writer = new CSVWriter( fw, SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, '\'' );

        try {
            writeMetadata();
            writeHeaders();

            // Loop through the organizations
            Map<UUID, String> organizations = getOrganizations();
            for ( Entry<UUID, String> orgIdAndName : organizations.entrySet() ) {
                exportApplicationsForOrg( orgIdAndName, queryString );
            }
        }
        finally {
            writer.close();
        }

        // now that file is written, copy it to S3
        if ( line.hasOption( "upload" ) ) {
            LOG.info( "Copy to S3" );
            copyToS3( fileName );
        }
    }


    private void copyToS3( String fileName ) {

        String bucketName = ( String ) properties.get( BUCKET_PROPNAME );
        String accessId = ( String ) properties.get( ACCESS_ID_PROPNAME );
        String secretKey = ( String ) properties.get( SECRET_KEY_PROPNAME );

        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessId );
        overrides.setProperty( "s3" + ".credential", secretKey );

        final Iterable<? extends Module> MODULES = ImmutableSet
                .of( new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(),
                        new NettyPayloadModule() );

        BlobStoreContext context =
                ContextBuilder.newBuilder( "s3" ).credentials( accessId, secretKey ).modules( MODULES )
                              .overrides( overrides ).buildView( BlobStoreContext.class );

        // Create Container (the bucket in s3)
        try {
            AsyncBlobStore blobStore = context.getAsyncBlobStore(); // it can be changed to sync
            // BlobStore (returns false if it already exists)
            ListenableFuture<Boolean> container = blobStore.createContainerInLocation( null, bucketName );
            if ( container.get() ) {
                LOG.info( "Created bucket " + bucketName );
            }
        }
        catch ( Exception ex ) {
            logger.error( "Could not start binary service: {}", ex.getMessage() );
            throw new RuntimeException( ex );
        }

        try {
            File file = new File( fileName );
            AsyncBlobStore blobStore = context.getAsyncBlobStore();
            BlobBuilder blobBuilder =
                    blobStore.blobBuilder( file.getName() ).payload( file ).calculateMD5().contentType( "text/plain" )
                             .contentLength( file.length() );

            Blob blob = blobBuilder.build();

            ListenableFuture<String> futureETag = blobStore.putBlob( bucketName, blob, PutOptions.Builder.multipart() );

            LOG.info( "Uploaded file etag=" + futureETag.get() );
        }
        catch ( Exception e ) {
            LOG.error( "Error uploading to blob store", e );
        }
    }


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Options options = super.createOptions();

        Option startTime =
                OptionBuilder.hasArg().withDescription( "minimum modified time -startTime" ).create( START_TIME );

        Option endTime = OptionBuilder.hasArg().withDescription( "maximum modified time -endTime" ).create( END_TIME );

        Option upload = OptionBuilder.withDescription( "upload files to blob-store" ).create( UPLOAD );

        options.addOption( startTime );
        options.addOption( endTime );
        options.addOption( upload );

        return options;
    }


    private void applyStartTime( CommandLine line ) throws Exception {

        if ( line.hasOption( START_TIME ) ) {
            startTime = new Date( Long.parseLong( line.getOptionValue( START_TIME ) ) );
        }
        else {
            // attempt to read last end time from directory
            File[] files = outputDir.listFiles( new FilenameFilter() {
                @Override
                public boolean accept( File dir, String name ) {
                    return name.endsWith( ".csv" );
                }
            } );
            long lastEndTime = 0;
            for ( File file : files ) {
                long endTime = readEndTime( file );
                if ( endTime > lastEndTime ) {
                    lastEndTime = endTime;
                }
            }
            startTime = new Date( lastEndTime + 1 );
        }
    }


    private void applyEndTime( CommandLine line ) {
        if ( line.hasOption( END_TIME ) ) {
            endTime = new Date( Long.parseLong( line.getOptionValue( END_TIME ) ) );
        }
        else {
            endTime = new Date( System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert( 1L, TimeUnit.HOURS ) );
        }
    }


    private long readEndTime( File file ) throws Exception {
        CSVReader reader = new CSVReader( new FileReader( file ), SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, '\'' );
        try {
            String[] firstLine = reader.readNext();
            if ( "start".equals( firstLine[0] ) && "end".equals( firstLine[2] ) ) {
                return Long.parseLong( firstLine[3] );
            }
        }
        finally {
            reader.close();
        }
        return 0;
    }


    private void writeMetadata() {
        writer.writeNext( new String[] { "start", "" + startTime.getTime(), "end", "" + endTime.getTime() } );
    }


    private void writeHeaders() {
        writer.writeNext( getHeaders() );
    }


    private String[] getHeaders() {

        List<String> headers = new ArrayList<String>();
        headers.addAll( Arrays.asList( BASE_ATTRIBUTES ) );

        Map<String, String[]> cfm = getCollectionFieldMap();
        for ( Map.Entry<String, String[]> entry : cfm.entrySet() ) {
            String collection = entry.getKey();
            String[] attributes = entry.getValue();
            for ( String attribute : attributes ) {
                headers.add( collection + "_" + attribute );
            }
        }

        String[] stringHeaders = new String[headers.size()];
        return headers.toArray( stringHeaders );
    }


    private Map<String, String[]> getCollectionFieldMap() {

        if ( collectionFieldMap != null ) {
            return collectionFieldMap;
        }

        // get basic stuff from Schema
        String[] collectionTypes = getCollectionTypes();
        collectionFieldMap = new TreeMap<String, String[]>();
        for ( String type : collectionTypes ) {
            Set<String> propertyNames = Schema.getDefaultSchema().getPropertyNames( type );
            for ( String attr : BASE_ATTRIBUTES ) {
                propertyNames.remove( attr );
            }

            Iterator<String> i = propertyNames.iterator();
            while ( i.hasNext() ) {
                String property = i.next();
                Class cls = Schema.getDefaultSchema().getPropertyType( type, property );
                if ( !cls.isPrimitive() && cls != String.class && cls != Date.class ) {
                    i.remove();
                }
            }
            String[] props = new String[propertyNames.size()];
            propertyNames.toArray( props );
            Arrays.sort( props );
            collectionFieldMap.put( type, props );
        }

        // add URAP stuff that's not visible to usergrid-stack
        for ( Map.Entry<String, String[]> entry : URAP_ATTRIBUTES.entrySet() ) {
            String type = entry.getKey();
            String[] attributes = entry.getValue();
            Arrays.sort( attributes );
            collectionFieldMap.put( type, attributes );
        }

        return collectionFieldMap;
    }


    /** @return Map of Organization UUID -> Name */
    private Map<UUID, String> getOrganizations() throws Exception {

        Map<UUID, String> organizationNames;

        if ( orgId == null ) {
            organizationNames = managementService.getOrganizations();
        }
        else {

            OrganizationInfo info = managementService.getOrganizationByUuid( orgId );

            if ( info == null ) {
                LOG.error( "Organization info is null!" );
                System.exit( 1 );
            }

            organizationNames = new HashMap<UUID, String>();
            organizationNames.put( orgId, info.getName() );
        }

        return organizationNames;
    }


    private String[] getCollectionTypes() {

        if ( collectionNames != null ) {
            return collectionNames;
        }

        Collection<CollectionInfo> system_collections =
                getDefaultSchema().getCollections( Application.ENTITY_TYPE ).values();

        ArrayList<String> collections = new ArrayList<String>( system_collections.size() );
        for ( CollectionInfo collection : system_collections ) {
            if ( !Schema.isAssociatedEntityType( collection.getType() ) ) {
                collections.add( collection.getType() );
            }
        }

        collectionNames = new String[collections.size()];
        Collections.sort( collections );
        return collections.toArray( collectionNames );
    }


    private void exportApplicationsForOrg( Entry<UUID, String> orgIdAndName, String queryString ) throws Exception {

        LOG.info( "organization: {} / {}", orgIdAndName.getValue(), orgIdAndName.getKey() );

        String orgName = orgIdAndName.getValue();

        BiMap<UUID, String> applications = managementService.getApplicationsForOrganization( orgIdAndName.getKey() );
        for ( Entry<UUID, String> appIdAndName : applications.entrySet() ) {

            String appName = appIdAndName.getValue();
            appName = appName.substring( appName.indexOf( '/' ) + 1 );

            LOG.info( "application {} / {}", appName, appIdAndName.getKey() );

            EntityManager em = emf.getEntityManager( appIdAndName.getKey() );
            Map<String, String[]> cfm = getCollectionFieldMap();

            // Loop through the collections of the Application
            Set<String> collections = em.getApplicationCollections();
            for ( String collectionName : collections ) {

                // set up for retrieving only the necessary properties
                String entityType = InflectionUtils.singularize( collectionName );
                String[] props = cfm.get( entityType );
                Collection<String> properties =
                        new ArrayList<String>( BASE_ATTRIBUTES.length + ( props != null ? props.length : 0 ) );
                properties.addAll( Arrays.asList( BASE_ATTRIBUTES ) );
                if ( props != null ) {
                    properties.addAll( Arrays.asList( props ) );
                }

                Query query = Query.fromQL( queryString );
                query.setLimit( MAX_ENTITY_FETCH );
                query.setResultsLevel( Level.REFS );
                Results results = em.searchCollection( em.getApplicationRef(), collectionName, query );

                while ( results.size() > 0 ) {

                    List<Entity> entities = em.getPartialEntities( results.getIds(), properties );

                    for ( Entity entity : entities ) {
                        write( orgName, appName, entity, em );
                    }

                    if ( results.getCursor() == null ) {
                        break;
                    }

                    query.setCursor( results.getCursor() );
                    results = em.searchCollection( em.getApplicationRef(), collectionName, query );
                }
            }
        }
    }


    private void write( String orgName, String appName, Entity entity, EntityManager em ) throws Exception {

        Map<String, String[]> cfm = getCollectionFieldMap();

        String uuid = entity.getUuid().toString();
        String created = DATE_FORMAT.format( entity.getCreated() );
        String modified = DATE_FORMAT.format( entity.getModified() );
        String type = entity.getType();

        List<String> values = new ArrayList<String>( 30 );
        values.add( uuid );
        values.add( orgName );
        values.add( appName );
        values.add( entity.getType() );
        values.add( created );
        values.add( modified );

        for ( Map.Entry<String, String[]> entry : cfm.entrySet() ) {
            String collection = entry.getKey();
            String[] attributes = entry.getValue();
            if ( collection.equals( type ) ) {
                for ( String attribute : attributes ) {
                    Object prop = entity.getProperty( attribute );
                    values.add( prop != null ? prop.toString() : null );
                }
            }
            else {
                for ( String attribute : attributes ) {
                    values.add( null );
                }
            }
        }

        String[] stringValues = new String[values.size()];
        values.toArray( stringValues );
        writer.writeNext( stringValues );
    }
}
