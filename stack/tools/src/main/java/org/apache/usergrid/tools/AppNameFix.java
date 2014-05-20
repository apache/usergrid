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


import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.entities.Application;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


/** @author tnine */
public class AppNameFix extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger( AppNameFix.class );


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );


        Options options = new Options();
        options.addOption( hostOption );

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

        for ( Entry<UUID, String> org : managementService.getOrganizations().entrySet() ) {

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
}
