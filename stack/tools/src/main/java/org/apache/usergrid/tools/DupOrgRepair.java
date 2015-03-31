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
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.utils.JsonUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


/**
 * This is a utility to load all entities in an application and re-save them, this forces the secondary indexing to be
 * updated.
 *
 * @author tnine
 */
public class DupOrgRepair extends ExportingToolBase {

    /**
     *
     */
    private static final int PAGE_SIZE = 100;

    private static final Logger logger = LoggerFactory.getLogger( DupOrgRepair.class );


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        Option outputOption =
                OptionBuilder.withArgName( "output" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "output" );

        Options options = new Options();
        options.addOption( hostOption );
        options.addOption( outputOption );

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
        String outputDir = line.getOptionValue( "output" );

        createDir( outputDir );

        startSpring();

        logger.info( "Starting crawl of all admins" );

        EntityManager em = emf.getEntityManager( emf.getManagementAppId() );
        Application app = em.getApplication();

        // search for all orgs

        Query query = new Query();
        query.setLimit( PAGE_SIZE );
        Results r = null;

        Multimap<String, UUID> orgs = HashMultimap.create();

        do {

            r = em.searchCollection( app, "groups", query );

            for ( Entity entity : r.getEntities() ) {
                String name = entity.getProperty( "path" ).toString().toLowerCase();
                orgs.put( name, entity.getUuid() );
            }

            query.setCursor( r.getCursor() );

            logger.info( "Searching next page" );
        }
        while ( r != null && r.size() == PAGE_SIZE );

        // now go through and print out duplicate emails

        for ( String name : orgs.keySet() ) {
            Collection<UUID> ids = orgs.get( name );

            if ( ids.size() > 1 ) {
                logger.warn( "Found multiple orgs with the name {}", name );

                // look this up the same way the REST tier does. This way we will always
                // map the same way and the user will not notice a background merge
                OrganizationInfo orgInfo = managementService.getOrganizationByName( name );

                UUID targetOrgId = orgInfo.getUuid();

                ids.remove( targetOrgId );

                for ( UUID sourceId : ids ) {
                    mergeOrganizations( outputDir, sourceId, targetOrgId );
                }
            }
        }

        logger.info( "Merge complete" );
    }


    /**
     * Merge the source orgId into the targetId in the following way.
     * <p/>
     * 1) link all admins from the source org to the target org 2) link all apps from the source org to the target or 3)
     * delete the target org
     */
    @SuppressWarnings("unchecked")
    private void mergeOrganizations( String outputDir, UUID sourceOrgId, UUID targetOrgId ) throws Exception {

        OrganizationInfo sourceOrgInfo = managementService.getOrganizationByUuid( sourceOrgId );

        Map<String, Object> sourceOrg = managementService.getOrganizationData( sourceOrgInfo );

        OrganizationInfo targetOrgInfo = managementService.getOrganizationByUuid( targetOrgId );

        Map<String, Object> targetOrg = managementService.getOrganizationData( targetOrgInfo );

        // Dump the output on these two orgs
        FileWriter file =
                new FileWriter( String.format( "%s/%s.%s.orig", outputDir, sourceOrgInfo.getName(), sourceOrgId ) );

        file.write( JsonUtils.mapToFormattedJsonString( sourceOrg ) );

        file.write( "\n\n" );

        file.write( JsonUtils.mapToFormattedJsonString( targetOrg ) );

        file.flush();
        file.close();

        // BiMap<UUID, String> targetApps =
        // managementService.getApplicationsForOrganization(targetOrgId);

        // now perform the merge

        // add all the admins
        Map<String, UserInfo> admins = ( Map<String, UserInfo> ) sourceOrg.get( "users" );

        for ( Entry<String, UserInfo> adminEntry : admins.entrySet() ) {
            UserInfo admin = adminEntry.getValue();

            logger.info( "adding admin with uuid {} and email {} to org with name {} and uuid {}", new Object[] {
                    admin.getUuid(), admin.getEmail(), targetOrgInfo.getName(), targetOrgInfo.getUuid()
            } );

            // copy the admins over
            managementService.addAdminUserToOrganization( admin, targetOrgInfo, false );
        }

        // get the root entity manager
        EntityManager em = emf.getEntityManager( emf.getManagementAppId() );

        // Add the apps to the org
        Map<String, UUID> sourceApps = ( Map<String, UUID> ) sourceOrg.get( "applications" );

        Map<String, UUID> targetApps = ( Map<String, UUID> ) targetOrg.get( "applications" );

        for ( Entry<String, UUID> app : sourceApps.entrySet() ) {

            Entity appEntity = null;

            // we have colliding app names
            if ( targetApps.get( app.getKey() ) != null ) {

                // already added, skip it
                if ( app.getValue().equals( targetApps.get( app.getKey() ) ) ) {
                    continue;
                }

                // check to see if this orgname/appname lookup returns the app we're
                // about to re-assign. If it does NOT, then we need to rename this app
                // before performing the link.
                UUID appIdToKeep = emf.lookupApplication( app.getKey() );

                UUID appIdToChange =
                        appIdToKeep.equals( app.getValue() ) ? targetApps.get( app.getKey() ) : app.getValue();

                // get the existing target entity
                appEntity = em.get( new SimpleEntityRef("application", appIdToChange));

                if ( appEntity != null ) {

                    String oldName = appEntity.getProperty( "name" ).toString();
                    String newName = oldName + appEntity.getUuid();

                    //force the property to be updated
                    em.setProperty( appEntity, "name", newName, true );

                    logger.info( "Renamed app from {} to {}", oldName, newName );
                }
            }

            logger.info( "Adding application with name {} and id {} to organization with uuid {}", new Object[] {
                    app.getKey(), app.getValue(), targetOrgId
            } );

            managementService.addApplicationToOrganization( targetOrgId, app.getValue(), appEntity);
        }

        // now delete the original org

        logger.info( "Deleting org with the name {} and uuid {}", sourceOrgInfo.getName(), sourceOrgInfo.getUuid() );

        // delete the source org
        em.delete( new SimpleEntityRef( "group", sourceOrgId ) );

        // re-dump the target from the cassandra stat
        targetOrgInfo = managementService.getOrganizationByUuid( targetOrgId );

        targetOrg = managementService.getOrganizationData( targetOrgInfo );

        file = new FileWriter( String.format( "%s/%s.%s.new", outputDir, targetOrgInfo.getName(), targetOrgId ) );

        file.write( JsonUtils.mapToFormattedJsonString( targetOrg ) );

        file.flush();
        file.close();
    }
}
