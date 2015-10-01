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


import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.utils.UUIDUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


/** @author tnine
 *
 * Fixes org/app names when they are corrupted.
 *
 *
 */
public class AppNameFix extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger( AppNameFix.class );


    /**
     *
     */
    private static final String ORGANIZATION_ARG = "org";



    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        Option orgOption = OptionBuilder.withArgName( ORGANIZATION_ARG ).hasArg().isRequired( false )
                                               .withDescription( "organization id or org name" ).create( ORGANIZATION_ARG );


        Options options = new Options();
        options.addOption( hostOption );
        options.addOption( orgOption );

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


        EntityManager rootEm = emf.getEntityManager( emf.getManagementAppId() );

        final Map<UUID, String> orgs = getOrgs( line, rootEm );


        for ( Entry<UUID, String> org : orgs.entrySet() ) {

            for ( Entry<UUID, String> app : managementService.getApplicationsForOrganization( org.getKey() )
                                                             .entrySet() ) {

                Application application = rootEm.get( app.getKey(), Application.class );


                if ( application == null ) {
                    logger.error( "Could not load app with id {}", app.getKey() );
                    continue;
                }

                String appName = application.getName();

                //nothing to do, it's right
                if ( appName.contains( "/" ) ) {
                    logger.info( "Application name is correct: {}", appName );
                    continue;
                }

                String correctAppName = org.getValue() + "/" + app.getValue();

                correctAppName = correctAppName.toLowerCase();

                application.setName( correctAppName );

                rootEm.setProperty( application, "name", correctAppName, true );

                Application changedApp = rootEm.get( app.getKey(), Application.class );

                if ( correctAppName.equals( changedApp.getName() ) ) {
                    logger.info( "Application name corrected.  {} : {}", appName, correctAppName );
                }
                else {
                    logger.error( "Could not correct Application with id {} to {}", app.getKey(), correctAppName );
                }
            }
        }
    }

    private Map<UUID, String> getOrgs(CommandLine line, EntityManager rootEm) throws Exception {

        String optionValue = line.getOptionValue( ORGANIZATION_ARG ) ;

        if(optionValue == null){
            return  managementService.getOrganizations();
        }


        UUID id = UUIDUtils.tryExtractUUID(optionValue );
        OrganizationInfo org;

        if(id != null){
            org = managementService.getOrganizationByUuid( id );
        }
        else{
            org = managementService.getOrganizationByName( optionValue );
        }

        if(org == null){
            throw new NullPointerException( String.format("Org with identifier %s does not exist", optionValue) );
        }

        Map<UUID, String> entries = new HashMap<UUID, String>();
        entries.put( org.getUuid(), org.getName() );

        return entries;

    }
}
