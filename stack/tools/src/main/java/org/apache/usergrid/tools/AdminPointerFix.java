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


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.User;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


/**
 * Fixes issues where admin data is in organizations but can't accessed or reset the password of. This usually returns
 * the error "Could not find organization for email" because the user can't be found but when the org is queried the
 * user shows up there. If you see that then run this tool.
 */
public class AdminPointerFix extends ExportingToolBase {
    private static final int PAGE_SIZE = 100;

    private static final Logger logger = LoggerFactory.getLogger( AdminPointerFix.class );


    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );



        Options options = new Options();
        options.addOption( hostOption );

        return options;
    }


    @Override
    public void runTool( CommandLine line ) throws Exception {


        startSpring();

        logger.info( "Starting crawl of all admins" );

        EntityManager em = emf.getEntityManager( CassandraService.MANAGEMENT_APPLICATION_ID );
        Application app = em.getApplication();

        // search for all orgs

        Query query = new Query();
        query.setLimit( PAGE_SIZE );
        Results r = null;

        Multimap<String, UUID> emails = HashMultimap.create();
        Multimap<String, UUID> usernames = HashMultimap.create();
        do {

            //get all users in the management app and page for each set of a PAGE_SIZE
            r = em.searchCollection( app, "users", query );

            for ( Entity entity : r.getEntities() ) {
                //grab all emails returned
                emails.put( entity.getProperty( "email" ).toString().toLowerCase(), entity.getUuid() );
                //grab all usernames returned.
                usernames.put( entity.getProperty( "username" ).toString().toLowerCase(), entity.getUuid() );
            }

            query.setCursor( r.getCursor() );

            logger.info( "Searching next page" );
        }
        while ( r != null && r.size() == PAGE_SIZE );


        //do  a get on a specific username, if it shows up more than once then remove it
        for ( String username : usernames.keySet() ) {
            Collection<UUID> ids = usernames.get( username );

            if ( ids.size() > 1 ) {
                logger.info( "Found multiple users with the username {}", username );
            }
        }

        for ( String email : emails.keySet() ) {
            Collection<UUID> ids = emails.get( email );

            if ( ids.size() > 1 ) {
                logger.info( "Found multiple users with the email {}", email );
            }


            UserInfo targetUser = managementService.getAdminUserByEmail( email );

            if ( targetUser == null ) {
                //This means that the org is mis associated with the user.
                List<UUID> tempIds = new ArrayList<UUID>( ids );
                //Collections.sort( tempIds );

                UUID toLoad = tempIds.get( 0 );

                logger.warn( "Could not load target user by email {}, loading by UUID {} instead", email, toLoad );
                targetUser = managementService.getAdminUserByUuid( toLoad );
                User targetUserEntity = em.get( targetUser.getUuid(), User.class );
                em.update( targetUserEntity );

            }
        }

        logger.info( "Repair complete" );
    }
}
