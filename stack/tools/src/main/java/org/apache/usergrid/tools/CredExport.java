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


import java.util.UUID;

import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.utils.UUIDUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


/** @author tnine */
public class CredExport extends ToolBase {

    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        Option appOption =
                OptionBuilder.withArgName( "app" ).hasArg().isRequired( true ).withDescription( "Application Name" )
                             .create( "app" );

        Option orgOption =
                OptionBuilder.withArgName( "org" ).hasArg().isRequired( true ).withDescription( "Application Name" )
                             .create( "org" );

        Options options = new Options();
        options.addOption( hostOption );
        options.addOption( appOption );
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
        String appOption = line.getOptionValue( "app" );
        String orgOption = line.getOptionValue( "org" );

        startSpring();

        UUID orgId = UUIDUtils.tryExtractUUID( orgOption );

        OrganizationInfo org = null;

        if ( orgId != null ) {
            org = managementService.getOrganizationByUuid( orgId );
        }
        else {
            org = managementService.getOrganizationByName( orgOption );
        }

        if ( org == null ) {
            System.out.println( String.format( "Unable to find org with name or id %s", orgOption ) );
            System.exit( 1 );
        }

        UUID appID = UUIDUtils.tryExtractUUID( appOption );

        ApplicationInfo app = null;

        if ( appID != null ) {
            app = managementService.getApplicationInfo( appID );
        }
        else {
            app = managementService.getApplicationInfo( orgOption + "/" + appOption );
        }

        if ( app == null ) {
            System.err.println( String.format( "Could not find an appliation with the name or id of %s", appOption ) );
            System.exit( 2 );
        }

        System.out.println( String.format( "Org Id: %s", org.getUuid() ) );

        System.out.println( String.format( "Org Name: %s", org.getName() ) );

        System.out.println(
                String.format( "Org Client Id: %s", managementService.getClientIdForOrganization( org.getUuid() ) ) );
        System.out.println( String.format( "Org Client Secret: %s",
                managementService.getClientSecretForOrganization( org.getUuid() ) ) );

        System.out.println( String.format( "App Id: %s", app.getId() ) );

        System.out.println( String.format( "App Name: %s", app.getName() ) );

        System.out.println(
                String.format( "App Client Id: %s", managementService.getClientIdForApplication( app.getId() ) ) );
        System.out.println( String.format( "App Client Secret: %s",
                managementService.getClientSecretForApplication( app.getId() ) ) );
    }
}
