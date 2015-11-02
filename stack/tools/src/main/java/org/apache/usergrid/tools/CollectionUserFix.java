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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Identifier;
import org.apache.usergrid.persistence.PagingResultsIterator;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


/**
 * Fixes issues where admin data is in organizations but can't accessed or reset the password of. This usually returns
 * the error "Could not find organization for email" because the user can't be found but when the org is queried the
 * user shows up there. If you see that then run this tool.
 */
//If index corruptions show up then the tool won't complete.
public class CollectionUserFix extends ExportingToolBase {
    private static final int PAGE_SIZE = 1000;

    private static final Logger logger = LoggerFactory.getLogger( CollectionUserFix.class );

    /**
     *
     */
    private static final String ORGANIZATION_ARG = "org";

    /**
     *
     */
    private static final String APPLICATION_ARG = "app";

    /**
     *
     */
    private static final String QUERY_ARG = "query";

    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        Option orgOption = OptionBuilder.withArgName( ORGANIZATION_ARG ).hasArg().isRequired( false )
                                        .withDescription( "organization id" ).create( ORGANIZATION_ARG );
        Option appOption = OptionBuilder.withArgName( APPLICATION_ARG ).hasArg().isRequired( false )
                                        .withDescription( "application uuid" ).create( APPLICATION_ARG );
        Option queryOption = OptionBuilder.withArgName( QUERY_ARG ).hasArg().isRequired( false )
                                        .withDescription( "query" ).create( QUERY_ARG );

        //add a query to it

        Options options = new Options();
        options.addOption( hostOption );
        options.addOption( orgOption );
        options.addOption( appOption );
        options.addOption( queryOption );

        return options;
    }


    /**
     * psudeo code
     * if org id is present then ignore the application id
     *      go through everysingle application/users entity. And check to see if it can be queried by username.
     * else if app id is present
     *      go through the applications/users
     *
     *
     * Take the list of applications to go through and go through them one by one. ( in the latter else case we will only
     *
     * @param line
     * @throws Exception
     */


    @Override
    public void runTool( CommandLine line ) throws Exception {


        startSpring();

        logger.info( "Starting crawl of all users" );
        System.out.println( "Starting crawl of all users" );
        Set<Application> applicationSet = new HashSet<Application>(  );
        EntityManager em = null;



        if(line.getOptionValue( ORGANIZATION_ARG )==null||line.getOptionValue( ORGANIZATION_ARG ).isEmpty()) {
            em = emf.getEntityManager( UUID.fromString( line.getOptionValue( APPLICATION_ARG ) ) );
            applicationSet.add( em.getApplication() );
        }
        else{
            BiMap applicationsForOrganization =
                    managementService
                            .getApplicationsForOrganization( UUID.fromString(line.getOptionValue( ORGANIZATION_ARG )));

            applicationSet = applicationsForOrganization.keySet();
        }
        startCollectionFlow( em, applicationSet, line.getOptionValue( QUERY_ARG ) );


        logger.info( "Repair complete" );
        System.out.println("Repair Complete");
    }


    //make
    private void startCollectionFlow(final EntityManager entityManager, final Set<Application> app, final String queryString )
            throws Exception {// search for all orgs

        Query query = new Query();
        if(queryString!=null){
            query = query.fromQL( queryString );
        }
        query.setLimit( PAGE_SIZE );
        Results r = null;
        EntityManager em = null;
        int numberOfUsers = 0;
        Identifier identifier = new Identifier();


        for ( Application application : app ) {
            //This will hold all of the applications users. This will be stored in memory to do a simple check to see if
            //there are any duped usernames in the collection.
            //Memory concerns means that

            //This means that we need to set it for each and every single application thus it gets set here instead of
            //the method that calls us.
            if(entityManager == null){
                em = emf.getEntityManager( application.getUuid() );
            }
            else {
                em = entityManager;
            }
//
//            do {
//                Multimap<String, UUID> usernames = HashMultimap.create();
//
//
//                //get all users in the management app and page for each set of a PAGE_SIZE
//                r = em.searchCollection( application, "users", query );
//                numberOfUsers+=r.size();
//                System.out.println("found "+numberOfUsers+" users");
//
//                for ( Entity entity : r.getEntities() ) {
//                    //grab all usernames returned.
//                    usernames.put( entity.getProperty( "username" ).toString().toLowerCase(), entity.getUuid() );
//                }
//
//                query.setCursor( r.getCursor() );
//
//                System.out.println("Starting username crawl of "+usernames.size()+" number of usernames");
//                usernameVerificationFix( em, usernames );
//
//
//            }
//            while ( r != null && r.size() == PAGE_SIZE);

            r = em.searchCollection( application, "users", query );
            PagingResultsIterator pagingResultsIterator = new PagingResultsIterator( r );

            while(pagingResultsIterator.hasNext()){
                Entity entity = ( Entity ) pagingResultsIterator.next();
                String username =  entity.getProperty( "username" ).toString().toLowerCase();
                em.getUserByIdentifier( identifier.fromName( username ) );

            }
//
//            for ( Entity entity : r.getEntities() ) {
//                //grab all usernames returned.
//                usernames.put( entity.getProperty( "username" ).toString().toLowerCase(), entity.getUuid() );
//            }
//
//            query.setCursor( r.getCursor() );
//
//            System.out.println("Starting username crawl of "+usernames.size()+" number of usernames");
//            usernameVerificationFix( em,  entity.getProperty( "username" ).toString().toLowerCase());







            System.out.println("Repair Complete");
            //do  a get on a specific username, if it shows up more than once then remove it
        }
    }


    private void usernameVerificationFix( final EntityManager em, final Multimap<String, UUID> usernames )
            throws Exception {
        Identifier identifier = new Identifier();
        for ( String username : usernames.keySet() ) {
          //  Collection<UUID> ids = usernames.get( username );

//            if ( ids.size() > 1 ) {
//                logger.info( "Found multiple users with the username {}", username );
//                System.out.println( "Found multiple users with the username: " + username );
//            }

            //UserInfo targetUser = managementService.getAdminUserByEmail( email );
            em.getUserByIdentifier( identifier.fromName( username ) );



//            if ( targetUser == null ) {
//                //This means that the username isn't properly associated with targetUser
//                List<UUID> tempIds = new ArrayList<UUID>( ids );
//                //Collections.sort( tempIds );
//
//                UUID toLoad = tempIds.get( 0 );
//
//                logger.warn( "Could not load target user by username {}, loading by UUID {} instead", username, toLoad );
//                System.out.println( "Could not load the target user by username: " + username
//                        + ". Loading by the following uuid instead: " + toLoad.toString() );
//
//                User targetUserEntity = null;
//                try {
//                    targetUserEntity = em.get( toLoad, User.class );
//                }catch(Exception e){
//                    System.out.println("The follow uuid has no data in this cassandra node: "+toLoad.toString());
//                    throw e;
//                }
//
//
//                try {
//                    if ( targetUserEntity != null&& targetUserEntity.getUuid().equals( toLoad )) {
//                        System.out.println("Updating uuid: "+targetUserEntity.getUuid().toString());
//                        em.update( targetUserEntity );
//                    }
//                }
//                catch ( DuplicateUniquePropertyExistsException dup ) {
//                    System.out.println( "Found duplicate unique property: " + dup.getPropertyName() + ". "
//                            + "Duplicate property is: "
//                            + dup.getPropertyValue() );
//                    //if there are duplicate unique properties then
//                    if ( dup.getPropertyName().equals( "username" ) ) {
//                        System.out.println("can I replace this with a different value since these are duplicated in the code base");
//                        //targetUserEntity.setUsername( targetUserEntity.getUsername() );
//                    }
//                    //else throw dup;
//                }
//                catch (Exception e){
//                    System.out.println("There was an issue with updating: "+e.getMessage());
//                }
//            }
        }
    }
}
