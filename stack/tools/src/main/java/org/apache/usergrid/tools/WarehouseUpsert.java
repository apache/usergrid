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


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.IOUtils;


/** Upserts data from files found in an S3 bucket. */
public class WarehouseUpsert extends ExportingToolBase {

    private static final Logger LOG = LoggerFactory.getLogger( WarehouseUpsert.class );

    public static final String DBHOST_PROPNAME = "usergrid.warehouse-export-dbhost";
    public static final String DBPORT_PROPNAME = "usergrid.warehouse-export-dbport";
    public static final String DBNAME_PROPNAME = "usergrid.warehouse-export-dbname";
    public static final String DBUSER_PROPNAME = "usergrid.warehouse-export-dbuser";
    public static final String DBPASSWORD_PROPNAME = "usergrid.warehouse-export-dbpassword";
    public static final String STAGING_TABLE_PROPNAME = "usergrid.warehouse-export-staging-table";
    public static final String MAIN_TABLE_PROPNAME = "usergrid.warehouse-export-main-table";

    String accessId;
    String secretKey;

    String bucketName;

    String dbusername;
    String dbpassword;
    String dbhost;
    String dbname;
    String dbport;

    String tableSchema;


    @Override
    public void runTool( CommandLine line ) throws Exception {

        startSpring();
        setVerbose( line );

        accessId = ( String ) properties.get( WarehouseExport.ACCESS_ID_PROPNAME );
        secretKey = ( String ) properties.get( WarehouseExport.SECRET_KEY_PROPNAME );

        bucketName = ( String ) properties.get( WarehouseExport.BUCKET_PROPNAME );

        dbusername = ( String ) properties.get( DBUSER_PROPNAME );
        dbpassword = ( String ) properties.get( DBPASSWORD_PROPNAME );
        dbhost = ( String ) properties.get( DBHOST_PROPNAME );
        dbname = ( String ) properties.get( DBNAME_PROPNAME );
        dbport = ( String ) properties.get( DBPORT_PROPNAME );

        tableSchema = IOUtils.toString( getClass().getResourceAsStream( "/warehouse-schema.sql" ) );

        String constr =
                String.format( "jdbc:postgresql://%s:%s/%s?user=%s&password=%s", dbhost, dbport, dbname, dbusername,
                        dbpassword );
        Class.forName( "org.postgresql.Driver" );
        Connection con = DriverManager.getConnection( constr );

        // create main table
        String mainTableName = ( String ) properties.get( MAIN_TABLE_PROPNAME );
        try {
            con.createStatement().execute( createWarehouseTable( mainTableName ) );
            LOG.info( "Created main table " + mainTableName );
        }
        catch ( SQLException ex ) {
            if ( !ex.getMessage().contains( "already exists" ) ) {
                LOG.error( "Error creating main table: " + ex.getMessage(), ex );
            }
            else {
                LOG.info( "Using existing main table " + mainTableName );
            }
        }

        // drop any existing staging table
        String stagingTableName = ( String ) properties.get( STAGING_TABLE_PROPNAME );
        String dropStagingTable = String.format( "drop table %s", stagingTableName );
        try {
            con.createStatement().execute( dropStagingTable );
            LOG.info( "Dropped existing staging table " + stagingTableName );
        }
        catch ( SQLException ex ) {
            if ( !ex.getMessage().contains( "does not exist" ) ) {
                LOG.error( "Error dropping staging table: " + ex.getMessage(), ex );
            }
            else {
                LOG.info( "Using existing staging table " + stagingTableName );
            }
        }

        // create staging table
        LOG.info( "Creating new staging table" );
        con.createStatement().execute( createWarehouseTable( stagingTableName ) );

        // copy data from S3 into staging table
        LOG.info( "Copying data from S3" );
        String copyFromS3 = String.format( "COPY %s FROM 's3://%s' "
                + "CREDENTIALS 'aws_access_key_id=%s;aws_secret_access_key=%s' IGNOREHEADER 2 EMPTYASNULL",
                stagingTableName, bucketName, accessId, secretKey );
        LOG.debug( copyFromS3 );
        con.createStatement().execute( copyFromS3 );

        // run update portion of upsert process
        LOG.info( "Upsert: updating" );
        String upsertUpdate =
                String.format( "UPDATE %s SET id = s.id FROM %s s WHERE %s.created = s.created ", mainTableName,
                        stagingTableName, mainTableName );
        LOG.debug( upsertUpdate );
        con.createStatement().execute( upsertUpdate );

        // insert new values in staging table into main table
        LOG.info( "Upsert: inserting" );
        String upsertInsert =
                String.format( "INSERT INTO %s SELECT s.* FROM %s s LEFT JOIN %s n ON s.id = n.id WHERE n.id IS NULL",
                        mainTableName, stagingTableName, mainTableName );
        LOG.debug( upsertInsert );
        con.createStatement().execute( upsertInsert );

        // drop staging table
        LOG.info( "Dropping existing staging table" );
        con.createStatement().execute( dropStagingTable );

        // done!
    }


    String createWarehouseTable( String name ) {
        String ddl = tableSchema.replaceAll( "\\{tableName\\}", name );
        ddl = ddl.replaceAll( "\\{accessId\\}", accessId );
        ddl = ddl.replaceAll( "\\{secretKey\\}", secretKey );
        return ddl;
    }
}
