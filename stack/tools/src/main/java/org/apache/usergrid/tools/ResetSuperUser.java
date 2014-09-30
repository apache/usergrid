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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.AccountCreationProps;
import org.apache.usergrid.management.cassandra.AccountCreationPropsImpl;
import org.apache.usergrid.management.cassandra.ManagementServiceImpl;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_ALLOWED;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_EMAIL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_NAME;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_PASSWORD;


/**
 * This is a utility to load all entities in an application and re-save them, this forces the secondary indexing to be
 * updated.
 *
 * @author tnine
 */
public class ResetSuperUser extends ToolBase {

    /**
     *
     */
    private static final int PAGE_SIZE = 100;


    private static final Logger logger = LoggerFactory.getLogger( ResetSuperUser.class );


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        Option userOption = OptionBuilder.withArgName( "username" ).hasArg().isRequired( true )
                                         .withDescription( "superuser username" ).create( "username" );

        Option passwordOption = OptionBuilder.withArgName( "password" ).hasArg().isRequired( true )
                                             .withDescription( "superuser password" ).create( "password" );

        Option emailOption =
                OptionBuilder.withArgName( "email" ).hasArg().isRequired( true ).withDescription( "superuser email" )
                             .create( "email" );

        Options options = new Options();
        options.addOption( hostOption );
        options.addOption( userOption );
        options.addOption( passwordOption );
        options.addOption( emailOption );

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

        System.out.println( "Starting superuser provision" );

        try {
            ( ( ManagementServiceImpl ) managementService )
                    .resetSuperUser( (String)line.getOptionValue( "username" ), (String)line.getOptionValue( "password" ),
                            (String) line.getOptionValue( "email" ) );
        }catch(Exception e){
                    throw new Exception( e.toString());
        }

        System.out.println("ResetSuperUser has been reset");

    }
}
