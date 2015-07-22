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


import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.apache.usergrid.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.util.*;


/**
 * This is a utility to load all entities in an application and re-save them, this forces the secondary indexing to be
 * updated.
 *
 * @author tnine
 */
public class DupAdminRepair extends ExportingToolBase {

    /**
     *
     */
    private static final int PAGE_SIZE = 100;

    private static final Logger logger = LoggerFactory.getLogger( DupAdminRepair.class );


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

        String emailsDir = String.format( "%s/emails", outputDir );
        String usernamesDir = String.format( "%s/usernames", outputDir );
        createDir( emailsDir );
        createDir( usernamesDir );

        startSpring();

        logger.info( "Starting crawl of all admins" );

        EntityManager em = emf.getEntityManager( emf.getManagementAppId() );
        Application app = em.getApplication();

        // search for all orgs

        Query query = new Query();
        query.setLimit( PAGE_SIZE );
        Results r = null;

        Multimap<String, UUID> emails = HashMultimap.create();
        Multimap<String, UUID> usernames = HashMultimap.create();
        do {

            r = em.searchCollection( app, "users", query );

            for ( Entity entity : r.getEntities() ) {
                emails.put( entity.getProperty( "email" ).toString().toLowerCase(), entity.getUuid() );
                usernames.put( entity.getProperty( "username" ).toString().toLowerCase(), entity.getUuid() );
            }

            query.setCursor( r.getCursor() );

            logger.info( "Searching next page" );
        }
        while ( r != null && r.size() == PAGE_SIZE );

        // now go through and print out duplicate emails

        for ( String username : usernames.keySet() ) {
            Collection<UUID> ids = usernames.get( username );

            if ( ids.size() > 1 ) {
                logger.info( "Found multiple users with the username {}", username );

                // force the username to be reset to the user's email
                resolveUsernameConflicts( usernamesDir, username, ids );
            }
        }

        for ( String email : emails.keySet() ) {
            Collection<UUID> ids = emails.get( email );

            if ( ids.size() > 1 ) {
                // get the admin the same way as the rest tier, this way the OTHER
                // admins will be removed
                UserInfo targetUser = managementService.getAdminUserByEmail( email );

                if ( targetUser == null ) {

                    List<UUID> tempIds = new ArrayList<UUID>( ids );
                    Collections.sort( tempIds );

                    UUID toLoad = tempIds.get( 0 );

                    logger.warn( "Could not load target user by email {}, loading by UUID {} instead", email, toLoad );
                    targetUser = managementService.getAdminUserByUuid( toLoad );

                    ids.remove( toLoad );
                }

                UUID targetId = targetUser.getUuid();

                ids.remove( targetId );

                logger.warn( "Found multiple admins with the email {}.  Retaining uuid {}", email, targetId );

                FileWriter file = new FileWriter( String.format( "%s/%s.all", emailsDir, email ) );

                Map<String, Object> userOrganizationData = managementService.getAdminUserOrganizationData( targetId );

                file.write( JsonUtils.mapToFormattedJsonString( userOrganizationData ) );

                for ( UUID id : ids ) {

                    userOrganizationData = managementService.getAdminUserOrganizationData( id );

                    file.write( JsonUtils.mapToFormattedJsonString( userOrganizationData ) );

                    file.write( "\n\n" );

                    mergeAdmins( emailsDir, id, targetId );
                }

                file.flush();
                file.close();

                // force the index update after all other admins have been merged
                logger.info( "Forcing re-index of admin with email {} and id {}", email, targetId );
                User targetUserEntity = em.get( targetUser.getUuid(), User.class );
                em.update( targetUserEntity );

                FileWriter merged = new FileWriter( String.format( "%s/%s.merged", emailsDir, email ) );

                userOrganizationData = managementService.getAdminUserOrganizationData( targetUser.getUuid() );

                merged.write( JsonUtils.mapToFormattedJsonString( userOrganizationData ) );
                merged.flush();
                merged.close();
            }
        }

        logger.info( "Repair complete" );
    }


    /**
     * When our usernames are equal, we need to check if our emails are equal. If they're not, we need to change the one
     * that DOES NOT get returned on a lookup by username
     */
    private void resolveUsernameConflicts( String targetDir, String userName, Collection<UUID> ids ) throws Exception {
        // lookup the admin id
        UserInfo existing = managementService.getAdminUserByUsername( userName );

        if ( existing == null ) {
            logger.warn( "Could not determine an admin for colliding username '{}'.  Skipping", userName );
            return;
        }

        ids.remove( existing.getUuid() );

        boolean collision = false;

        EntityManager em = emf.getEntityManager( emf.getManagementAppId() );

        for ( UUID id : ids ) {
            UserInfo other = managementService.getAdminUserByUuid( id );

            // same username and email, these will be merged later in the process,
            // skip it
            if ( other != null && other.getEmail() != null && other.getEmail().equals( existing.getEmail() ) ) {
                logger.info(
                        "Users with the same username '{}' have the same email '{}'. This will be resolved later in "
                                + "the process, skipping", userName, existing.getEmail() );
                continue;
            }

            // if we get here, the emails do not match, but the usernames do. Force
            // both usernames to emails
            collision = true;

            setUserName( em, other, other.getEmail() );
        }

        if ( collision ) {
            setUserName( em, existing, existing.getEmail() );
        }
    }


    /** Set the username to the one provided, if we can't due to duplicate property issues, we fall back to user+uuid */
    private void setUserName( EntityManager em, UserInfo other, String newUserName ) throws Exception {
        logger.info( "Setting username to {} for user with username {} and id {}", new Object[] {
                newUserName, other.getUsername(), other.getUuid()
        } );

        try {
            em.setProperty( new SimpleEntityRef( "user", other.getUuid() ), "username", newUserName, true );
        }
        catch ( DuplicateUniquePropertyExistsException e ) {
            logger.warn( "More than 1 user has the username of {}.  Setting the username to their username+UUID as a "
                    + "fallback", newUserName );

            setUserName( em, other, String.format( "%s-%s", other.getUsername(), other.getUuid() ) );
        }
    }


    /** Merge the source admin to the target admin by copying oranizations. Then deletes the source admin */
    private void mergeAdmins( String targetDir, UUID sourceId, UUID targetId ) throws Exception {

        EntityManager em = emf.getEntityManager( emf.getManagementAppId() );

        User sourceUser = em.get( sourceId, User.class );

        // may have already been deleted, do nothing
        if ( sourceUser == null ) {
            logger.warn( "Source admin with uuid {} does not exist in cassandra", sourceId );
            return;
        }

        UserInfo targetUserInfo = managementService.getAdminUserByUuid( targetId );

        @SuppressWarnings("unchecked") Map<String, Map<String, UUID>> sourceOrgs =
                ( Map<String, Map<String, UUID>> ) managementService.getAdminUserOrganizationData( sourceId )
                                                                    .get( "organizations" );

        for ( String orgName : sourceOrgs.keySet() ) {
            UUID orgId = sourceOrgs.get( orgName ).get( "uuid" );

            OrganizationInfo org = managementService.getOrganizationByUuid( orgId );

            logger.info( "Adding organization {} to admin with email {} and id {}",
                    new Object[] { org.getName(), sourceUser.getEmail(), sourceUser.getUuid() } );

            // copy it over to the target admin
            managementService.addAdminUserToOrganization( targetUserInfo, org, false );
        }

        logger.info( "Deleting admin with email {} and id {}", sourceUser.getEmail(), sourceUser.getUuid() );

        em.delete( sourceUser );
    }
}
