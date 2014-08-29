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


import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.Results;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import au.com.bytecode.opencsv.CSVWriter;


/**
 * Tools class which dumps metrics for tracking Usergrid developer adoption and high-level application usage.
 * <p/>
 * Can be called thusly: mvn exec:java -Dexec.mainClass="org.apache.usergrid.tools.Command" -Dexec.args="Metrics -host
 * localhost -outputDir ./output"
 *
 * @author zznate
 */
public class OrganizationExport extends ExportingToolBase {

    /**
     *
     */
    private static final String QUERY_ARG = "query";
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm" );


    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();

        setVerbose( line );

        prepareBaseOutputFileName( line );

        outputDir = createOutputParentDir();

        String queryString = line.getOptionValue( QUERY_ARG );

        Query query = Query.fromQL( queryString );

        logger.info( "Export directory: {}", outputDir.getAbsolutePath() );

        CSVWriter writer = new CSVWriter( new FileWriter( outputDir.getAbsolutePath() + "/admins.csv" ), ',' );

        writer.writeNext( new String[] { "Org uuid", "Org Name", "Admin uuid", "Admin Name", "Admin Email", "Admin Created Date" } );

        Results organizations = null;

        do {

            organizations = getOrganizations( query );

            for ( Entity organization : organizations.getEntities() ) {
                final String orgName = organization.getProperty( "path" ).toString();
                final UUID orgId = organization.getUuid();

                logger.info( "Org Name: {} key: {}", orgName, orgId );

                for ( UserInfo user : managementService.getAdminUsersForOrganization( organization.getUuid() ) ) {

                    Entity admin = managementService.getAdminUserEntityByUuid( user.getUuid() );

                    Long createdDate = ( Long ) admin.getProperties().get( "created" );

                    writer.writeNext( new String[] { orgId.toString(),
                            orgName, user.getUuid().toString(), user.getName(), user.getEmail(),
                            createdDate == null ? "Unknown" : sdf.format( new Date( createdDate ) )
                    } );
                }
            }

            query.setCursor( organizations.getCursor() );
        }
        while ( organizations != null && organizations.hasCursor() );

        logger.info( "Completed export" );

        writer.flush();
        writer.close();
    }


    @Override
    public Options createOptions() {
        Options options = super.createOptions();

        @SuppressWarnings("static-access") Option queryOption =
                OptionBuilder.withArgName( QUERY_ARG ).hasArg().isRequired( true )
                             .withDescription( "Query to execute when searching for organizations" )
                             .create( QUERY_ARG );
        options.addOption( queryOption );

        return options;
    }


    private Results getOrganizations( Query query ) throws Exception {

        EntityManager em = emf.getEntityManager( emf.getManagementAppId() );
        return em.searchCollection( em.getApplicationRef(), "groups", query );
    }
}
